package com.saif.classbuzz.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MyClassesTest {

    @Test
    fun `untagged and allowed batch tags are mine`() {
        assertTrue(MyClasses.isMine(null))
        assertTrue(MyClasses.isMine("Batch A"))
        assertTrue(MyClasses.isMine("batch a"))
        assertTrue(MyClasses.isMine("PCM-Batch B"))
        assertTrue(MyClasses.isMine("Batch B & Batch C"))
    }

    @Test
    fun `other sections are not mine`() {
        assertFalse(MyClasses.isMine("Batch D"))
        assertFalse(MyClasses.isMine(" Batch E"))
        assertFalse(MyClasses.isMine("KCET"))
        assertFalse(MyClasses.isMine("Batch F & Batch G"))
    }

    // 2026-11-04 10:30 in India.
    private val now = Instant.parse("2026-11-04T05:00:00Z")

    private fun ics(uid: String, start: String, end: String, summary: String, allDay: Boolean = false, cancelled: Boolean = false) =
        IcsEvent(uid, Instant.parse(start), Instant.parse(end), summary, allDay, cancelled)

    @Test
    fun `select drops all-day, cancelled, other batches, past and far events and sorts by start`() {
        val feed = listOf(
            ics("class", "2026-11-05T03:00:00Z", "2026-11-05T05:00:00Z", "ABC;Chem-Ionic Equilibrium-5;Batch-Batch A"),
            ics("test", "2026-11-04T10:30:00Z", "2026-11-04T12:00:00Z", "II Unit Test; Chemistry"),
            ics("birthday", "2026-11-04T00:00:00Z", "2026-11-05T00:00:00Z", "Birthday", allDay = true),
            ics("cancelled", "2026-11-04T08:00:00Z", "2026-11-04T10:00:00Z", "DEF;Math-Circles-II;Batch-Batch A", cancelled = true),
            ics("other", "2026-11-04T08:00:00Z", "2026-11-04T10:00:00Z", "MNO;GHI;Phy lab midterm Exam; Batch - Batch D"),
            ics("past", "2026-11-03T03:00:00Z", "2026-11-03T05:00:00Z", "ABC;Chem-Ionic Equilibrium-1;Batch-Batch A"),
            ics("far", "2026-11-12T03:00:00Z", "2026-11-12T05:00:00Z", "ABC;Chem-Mole Concept;Batch-Batch A"),
        )
        val mine = MyClasses.select(feed, now)
        assertEquals(listOf("test", "class"), mine.map { it.uid })
        assertEquals(Subject.CHEMISTRY, mine.last().info.subject)
    }

    @Test
    fun `class already running is kept until it ends`() {
        val running = ics("now", "2026-11-04T03:00:00Z", "2026-11-04T05:30:00Z", "ABC;Chem-Ionic Equilibrium-3;Batch-Batch A")
        assertEquals(listOf("now"), MyClasses.select(listOf(running), now).map { it.uid })
    }
}
