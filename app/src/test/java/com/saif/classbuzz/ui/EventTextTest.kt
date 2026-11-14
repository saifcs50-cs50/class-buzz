package com.saif.classbuzz.ui

import com.saif.classbuzz.calendar.ClassEvent
import com.saif.classbuzz.calendar.ClassInfo
import com.saif.classbuzz.calendar.Subject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class EventTextTest {
    private val india = ZoneId.of("Asia/Kolkata")
    private val lead = Duration.ofMinutes(5)

    private fun info(
        subject: Subject? = Subject.CHEMISTRY,
        topic: String = "Ionic Equilibrium",
        teachers: List<String> = listOf("ABC"),
        isTest: Boolean = false,
    ) = ClassInfo(subject, topic, teachers, "Batch A", isTest, isNoBuzz = false)

    private fun event(start: String, end: String, info: ClassInfo) =
        ClassEvent("u1", Instant.parse(start), Instant.parse(end), info)

    private val start = Instant.parse("2026-11-05T03:00:00Z")

    @Test
    fun `minutes round up`() {
        assertEquals(5L, EventText.minutesLeft(start, start.minusSeconds(4 * 60 + 10)))
        assertEquals(1L, EventText.minutesLeft(start, start.minusSeconds(1)))
    }

    @Test
    fun `minutes never negative`() {
        assertEquals(0L, EventText.minutesLeft(start, start.plusSeconds(90)))
    }

    @Test
    fun `headline for chem`() {
        assertEquals("MIN TO CHEM", EventText.headline(info()))
    }

    @Test
    fun `headline for unit test with physics`() {
        assertEquals("MIN TO PHY", EventText.headline(info(Subject.PHYSICS, "II Unit Test", emptyList(), isTest = true)))
    }

    @Test
    fun `headline for part test`() {
        assertEquals("MIN TO TEST", EventText.headline(info(null, "Part 8 test", emptyList(), isTest = true)))
    }

    @Test
    fun `headline for a plain event`() {
        assertEquals("MIN TO CLASS", EventText.headline(info(null, "Study session", emptyList())))
    }

    @Test
    fun `meta for class`() {
        val e = event("2026-11-05T03:00:00Z", "2026-11-05T05:00:00Z", info())
        assertEquals("ABC · 8:30 – 10:30", EventText.meta(e, india))
    }

    @Test
    fun `meta for test`() {
        val e = event("2026-11-05T10:30:00Z", "2026-11-05T12:00:00Z", info(Subject.PHYSICS, "II Unit Test", emptyList(), isTest = true))
        assertEquals("TEST · 16:00 – 17:30", EventText.meta(e, india))
    }

    @Test
    fun `meta for two teachers`() {
        val e = event("2026-11-05T08:00:00Z", "2026-11-05T10:00:00Z", info(teachers = listOf("MNO", "GHI")))
        assertEquals("MNO/GHI · 13:30 – 15:30", EventText.meta(e, india))
    }

    @Test
    fun `meta without teacher is just the times`() {
        val e = event("2026-11-05T03:00:00Z", "2026-11-05T05:00:00Z", info(Subject.PHYSICS, "Study session", emptyList()))
        assertEquals("8:30 – 10:30", EventText.meta(e, india))
    }

    @Test
    fun `topic falls back to subject then class`() {
        assertEquals("Ionic Equilibrium", EventText.topic(info()))
        assertEquals("Chemistry", EventText.topic(info(topic = "")))
        assertEquals("Class", EventText.topic(info(subject = null, topic = "")))
    }

    @Test
    fun `ring is full at buzz time and empty at start`() {
        assertEquals(1f, EventText.ringFraction(start, start.minus(lead), lead), 0.0001f)
        assertEquals(0.5f, EventText.ringFraction(start, start.minusSeconds(150), lead), 0.0001f)
        assertEquals(0f, EventText.ringFraction(start, start, lead), 0.0001f)
        assertEquals(0f, EventText.ringFraction(start, start.plusSeconds(60), lead), 0.0001f)
        assertEquals(1f, EventText.ringFraction(start, start.minus(lead).minusSeconds(60), lead), 0.0001f)
    }

    // Wednesday 4 Nov 2026 in India: classes at 8:30, 10:45, 13:30, test at 16:00; Thursday 8:30.
    private val day = listOf(
        event("2026-11-04T03:00:00Z", "2026-11-04T05:00:00Z", info()).copy(uid = "wed-0830"),
        event("2026-11-04T05:15:00Z", "2026-11-04T07:15:00Z", info()).copy(uid = "wed-1045"),
        event("2026-11-04T08:00:00Z", "2026-11-04T10:00:00Z", info()).copy(uid = "wed-1330"),
        event("2026-11-04T10:30:00Z", "2026-11-04T12:00:00Z", info()).copy(uid = "wed-1600"),
        event("2026-11-05T03:00:00Z", "2026-11-05T05:00:00Z", info()).copy(uid = "thu-0830"),
        event("2026-11-05T05:15:00Z", "2026-11-05T07:15:00Z", info()).copy(uid = "thu-1045"),
    )

    @Test
    fun `laterList skips the next class and keeps the rest of today`() {
        val now = Instant.parse("2026-11-04T04:00:00Z") // 9:30, during the 8:30 class
        assertEquals(listOf("wed-1045", "wed-1330", "wed-1600"), EventText.laterList(day, now, india).map { it.uid })
    }

    @Test
    fun `laterList falls back to tomorrow's first class`() {
        val now = Instant.parse("2026-11-04T10:10:00Z") // 15:40, the 13:30 class is over; next is the 16:00 test
        assertEquals(listOf("thu-0830"), EventText.laterList(day, now, india).map { it.uid })
    }

    @Test
    fun `laterList previews the day after when the next class is tomorrow`() {
        val now = Instant.parse("2026-11-04T13:00:00Z") // 18:30, next is Thursday 8:30
        assertEquals(listOf("thu-1045"), EventText.laterList(day, now, india).map { it.uid })
    }

    @Test
    fun `laterList is empty when nothing is coming`() {
        assertEquals(emptyList<ClassEvent>(), EventText.laterList(day, Instant.parse("2026-11-06T00:00:00Z"), india))
        assertEquals(emptyList<ClassEvent>(), EventText.laterList(emptyList(), start, india))
    }

    @Test
    fun `updated line`() {
        val now = Instant.parse("2026-11-04T10:00:00Z")
        assertEquals("Never updated", EventText.updatedLine(null, now))
        assertEquals("Updated just now", EventText.updatedLine(now.minusSeconds(20), now))
        assertEquals("Updated 12 min ago", EventText.updatedLine(now.minusSeconds(12 * 60 + 30), now))
        assertEquals("Updated 3 h ago", EventText.updatedLine(now.minusSeconds(3 * 3600 + 600), now))
        assertEquals("Not updated for 7 h", EventText.updatedLine(now.minusSeconds(7 * 3600 + 100), now))
    }

    @Test
    fun `stale after six hours or never`() {
        val now = Instant.parse("2026-11-04T10:00:00Z")
        assertEquals(true, EventText.isStale(null, now))
        assertEquals(false, EventText.isStale(now.minusSeconds(6 * 3600), now))
        assertEquals(true, EventText.isStale(now.minusSeconds(6 * 3600 + 1), now))
    }

    @Test
    fun `colour for chem is green, for no subject is neutral`() {
        assertEquals(0xFF4ADE80.toInt(), EventText.color(info()))
        assertEquals(0xFFA1A1AA.toInt(), EventText.color(info(subject = null)))
    }
}
