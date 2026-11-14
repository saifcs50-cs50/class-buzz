package com.saif.classbuzz.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class RefresherTest {
    private val india = ZoneId.of("Asia/Kolkata")
    private var now = Instant.parse("2026-11-04T04:30:00Z") // 10:00 IST
    private val repo = FakeRepository()
    private val scheduler = FakeScheduler()
    private var body: Result<String> = Result.success(calendar(withMyClass = true))
    private var fetches = 0
    private val staleWarnings = mutableListOf<Instant?>()
    private var bootCount = 1

    private val refresher = Refresher(
        repo = repo,
        scheduler = scheduler,
        fetch = { fetches++; body },
        clock = { now },
        zone = india,
        log = {},
        warnStale = { staleWarnings += it },
        bootCount = { bootCount },
    )

    private fun calendar(withMyClass: Boolean, complete: Boolean = true): String {
        val mine = listOf(
            "BEGIN:VEVENT",
            "DTSTART:20261105T030000Z", // tomorrow 08:30 IST
            "DTEND:20261105T050000Z",
            "UID:u1@example.com",
            "SUMMARY:ABC\\;Chem-Ionic Equilibrium-5\\;Batch-Batch A",
            "END:VEVENT",
        )
        val other = listOf(
            "BEGIN:VEVENT",
            "DTSTART:20261105T080000Z",
            "DTEND:20261105T100000Z",
            "UID:u2@example.com",
            "SUMMARY:MNO\\;GHI\\;Phy lab midterm Exam\\; Batch - Batch D",
            "END:VEVENT",
        )
        val lines = listOf("BEGIN:VCALENDAR", "X-WR-TIMEZONE:Asia/Kolkata") +
            (if (withMyClass) mine else emptyList()) + other +
            (if (complete) listOf("END:VCALENDAR") else emptyList())
        return lines.joinToString("\r\n")
    }

    @Test
    fun `good calendar saves my events and schedules buzzes 5 minutes early`() {
        val outcome = refresher.refresh()
        assertEquals(RefreshOutcome.Updated(mine = 1), outcome)
        assertEquals(listOf("u1@example.com"), repo.loadEvents().map { it.uid })
        assertEquals(listOf(Instant.parse("2026-11-05T02:55:00Z")), scheduler.scheduled.map { it.triggerAt })
        assertEquals(now, repo.lastSuccess())
    }

    @Test
    fun `html page keeps old schedule and alarms`() {
        refresher.refresh()
        val before = repo.loadEvents()
        body = Result.success("<html><body>Sign in to Wi-Fi</body></html>")
        val outcome = refresher.refresh()
        assertTrue(outcome is RefreshOutcome.Failed)
        assertEquals(before, repo.loadEvents())
        assertEquals(emptyList<String>(), scheduler.cancelled)
        assertEquals(1, scheduler.scheduled.size)
    }

    @Test
    fun `cut off download keeps old schedule`() {
        refresher.refresh()
        val before = repo.loadEvents()
        body = Result.success(calendar(withMyClass = false, complete = false))
        assertTrue(refresher.refresh() is RefreshOutcome.Failed)
        assertEquals(before, repo.loadEvents())
        assertEquals(emptyList<String>(), scheduler.cancelled)
    }

    @Test
    fun `network failure keeps old schedule`() {
        refresher.refresh()
        body = Result.failure(java.io.IOException("timeout"))
        assertTrue(refresher.refresh() is RefreshOutcome.Failed)
        assertEquals(listOf("u1@example.com"), repo.loadEvents().map { it.uid })
    }

    @Test
    fun `class removed from calendar loses its buzz`() {
        refresher.refresh()
        body = Result.success(calendar(withMyClass = false))
        assertEquals(RefreshOutcome.Updated(mine = 0), refresher.refresh())
        assertEquals(listOf("u1@example.com@1793847600"), scheduler.cancelled)
        assertEquals(emptyMap<String, Instant>(), repo.scheduled())
    }

    @Test
    fun `no link gives NoLink and does not fetch`() {
        val noLinkRepo = FakeRepository(url = null)
        val noLink = Refresher(noLinkRepo, scheduler, { fetches++; body }, { now }, india, {}, {}, { 1 })
        assertEquals(RefreshOutcome.NoLink, noLink.refresh())
        assertEquals(0, fetches)
    }

    @Test
    fun `failed refresh 7 hours after last success at 10 00 warns once`() {
        refresher.refresh()
        val lastGood = now
        now = now.plusSeconds(7 * 3600) // 17:00 IST
        body = Result.failure(java.io.IOException("no signal"))
        refresher.refresh()
        refresher.refresh()
        assertEquals(listOf<Instant?>(lastGood), staleWarnings)
    }

    @Test
    fun `a good download after a warning allows a new warning later`() {
        refresher.refresh()
        now = now.plusSeconds(7 * 3600)
        body = Result.failure(java.io.IOException("no signal"))
        refresher.refresh()
        body = Result.success(calendar(withMyClass = true))
        refresher.refresh()
        assertEquals(false, repo.staleWarned())
    }

    @Test
    fun `after a restart a reschedule sets every alarm again`() {
        refresher.refresh()
        scheduler.scheduled.clear()
        bootCount = 2 // the watch restarted and Android dropped every alarm
        refresher.reschedule()
        assertEquals(listOf("u1@example.com@1793847600"), scheduler.scheduled.map { it.key })
    }

    @Test
    fun `same boot does not set alarms twice`() {
        refresher.refresh()
        scheduler.scheduled.clear()
        refresher.reschedule()
        assertEquals(emptyList<String>(), scheduler.scheduled.map { it.key })
    }

    @Test
    fun `opening the app soon after a good refresh does not download again`() {
        refresher.refresh()
        now = now.plusSeconds(5 * 60)
        assertEquals(null, refresher.refreshUnlessRecent(java.time.Duration.ofMinutes(10)))
        assertEquals(1, fetches)
    }

    @Test
    fun `opening the app after a while downloads again`() {
        refresher.refresh()
        now = now.plusSeconds(15 * 60)
        assertEquals(RefreshOutcome.Updated(mine = 1), refresher.refreshUnlessRecent(java.time.Duration.ofMinutes(10)))
        assertEquals(2, fetches)
    }
}
