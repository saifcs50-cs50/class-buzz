package com.saif.classbuzz.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class IcsParserTest {

    // Hand-written, shaped like the real Google feed: CRLF endings, folded lines, escaped separators.
    private val fixture = listOf(
        "BEGIN:VCALENDAR",
        "PRODID:-//Google Inc//Google Calendar 70.9054//EN",
        "VERSION:2.0",
        "X-WR-CALNAME:Test batch",
        "X-WR-TIMEZONE:Asia/Kolkata",
        "BEGIN:VEVENT",
        "DTSTART:20261104T030000Z",
        "DTEND:20261104T050000Z",
        "UID:u1@example.com",
        "DESCRIPTION:Join with Google Meet: https://meet.example/abc\\nOr dial",
        "SUMMARY:ABC\\;Chem-Ionic Equil",
        " ibrium-3. pH \\,pOH and their effects\\;Batch-Batch A",
        "STATUS:CONFIRMED",
        "END:VEVENT",
        "BEGIN:VEVENT",
        "DTSTART;TZID=Asia/Kolkata:20261104T083000",
        "DTEND;TZID=Asia/Kolkata:20261104T103000",
        "UID:u2@example.com",
        "SUMMARY:DEF\\;Math-Circles-I\\;Batch-Batch A",
        "STATUS:CANCELLED",
        "END:VEVENT",
        "BEGIN:VEVENT",
        "DTSTART;VALUE=DATE:20000101",
        "DTEND;VALUE=DATE:20000102",
        "RRULE:FREQ=YEARLY;WKST=MO",
        "UID:u3@example.com",
        "SUMMARY:Birthday",
        "END:VEVENT",
        "BEGIN:VEVENT",
        "UID:u4@example.com",
        "SUMMARY:No start time",
        "END:VEVENT",
        "END:VCALENDAR",
    ).joinToString("\r\n")

    private val events by lazy { IcsParser.parse(fixture) }

    private fun event(uid: String) = events.single { it.uid == uid }

    @Test
    fun `utc start and end become instants`() {
        assertEquals(Instant.parse("2026-11-04T03:00:00Z"), event("u1@example.com").start)
        assertEquals(Instant.parse("2026-11-04T05:00:00Z"), event("u1@example.com").end)
    }

    @Test
    fun `folded summary is joined and unescaped`() {
        assertEquals(
            "ABC;Chem-Ionic Equilibrium-3. pH ,pOH and their effects;Batch-Batch A",
            event("u1@example.com").summary,
        )
    }

    @Test
    fun `tzid start is read in its zone`() {
        assertEquals(Instant.parse("2026-11-04T03:00:00Z"), event("u2@example.com").start)
        assertEquals(Instant.parse("2026-11-04T05:00:00Z"), event("u2@example.com").end)
    }

    @Test
    fun `floating time uses the calendar timezone`() {
        val floating = fixture.replace("DTSTART:20261104T030000Z", "DTSTART:20261104T083000")
        val start = IcsParser.parse(floating).single { it.uid == "u1@example.com" }.start
        assertEquals(Instant.parse("2026-11-04T03:00:00Z"), start)
    }

    @Test
    fun `all day birthday is flagged`() {
        assertTrue(event("u3@example.com").isAllDay)
        assertFalse(event("u1@example.com").isAllDay)
    }

    @Test
    fun `cancelled status is flagged`() {
        assertTrue(event("u2@example.com").isCancelled)
        assertFalse(event("u1@example.com").isCancelled)
    }

    @Test
    fun `event without dtstart is skipped`() {
        assertEquals(listOf("u1@example.com", "u2@example.com", "u3@example.com"), events.map { it.uid })
    }

    @Test
    fun `missing dtend ends at the start`() {
        val noEnd = fixture.replace("DTEND:20261104T050000Z\r\n", "")
        val parsed = IcsParser.parse(noEnd).single { it.uid == "u1@example.com" }
        assertEquals(Instant.parse("2026-11-04T03:00:00Z"), parsed.end)
    }

    @Test
    fun `complete calendar check`() {
        assertTrue(IcsParser.isCompleteCalendar(fixture))
        assertFalse(IcsParser.isCompleteCalendar("<html><body>Sign in</body></html>"))
        assertFalse(IcsParser.isCompleteCalendar(fixture.substringBefore("END:VCALENDAR")))
    }
}
