package com.saif.classbuzz.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarLinkTest {
    private val good = "https://calendar.google.com/calendar/ical/c_abc123%40group.calendar.google.com/public/basic.ics"

    @Test
    fun `google ical link is accepted`() {
        assertTrue(CalendarLink.isValid(good))
        assertTrue(CalendarLink.isValid("  $good  "))
    }

    @Test
    fun `other links are rejected`() {
        assertFalse(CalendarLink.isValid(good.replace("https://", "http://")))
        assertFalse(CalendarLink.isValid("https://evil.example/calendar/ical/x/basic.ics"))
        assertFalse(CalendarLink.isValid("https://calendar.google.com/calendar/ical/x/public/basic.html"))
        assertFalse(CalendarLink.isValid("https://calendar.google.com/calendar/embed?src=x"))
        assertFalse(CalendarLink.isValid(""))
    }

    @Test
    fun `masked link shows only its last 12 characters`() {
        assertEquals("…ic/basic.ics", CalendarLink.masked(good))
        assertEquals("…short", CalendarLink.masked("short"))
    }
}
