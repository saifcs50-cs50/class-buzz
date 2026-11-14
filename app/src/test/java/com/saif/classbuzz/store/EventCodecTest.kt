package com.saif.classbuzz.store

import com.saif.classbuzz.calendar.ClassEvent
import com.saif.classbuzz.calendar.ClassInfo
import com.saif.classbuzz.calendar.Subject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class EventCodecTest {
    private val chem = ClassEvent(
        uid = "u1@example.com",
        start = Instant.parse("2026-11-05T03:00:00Z"),
        end = Instant.parse("2026-11-05T05:00:00Z"),
        info = ClassInfo(Subject.CHEMISTRY, "Ionic Equilibrium-5. Hydrolysis", listOf("ABC"), "Batch A", isTest = false, isNoBuzz = false),
    )
    private val partTest = ClassEvent(
        uid = "u2@example.com",
        start = Instant.parse("2026-11-06T10:00:00Z"),
        end = Instant.parse("2026-11-06T12:00:00Z"),
        info = ClassInfo(null, "Part 8 test", listOf("MNO", "GHI"), null, isTest = true, isNoBuzz = true),
    )

    @Test
    fun `round trip keeps every field`() {
        assertEquals(listOf(chem, partTest), EventCodec.decode(EventCodec.encode(listOf(chem, partTest))))
    }

    @Test
    fun `no teachers survives a round trip`() {
        val noTeacher = chem.copy(info = chem.info.copy(teachers = emptyList()))
        assertEquals(listOf(noTeacher), EventCodec.decode(EventCodec.encode(listOf(noTeacher))))
    }

    @Test
    fun `tabs and newlines in topic become spaces`() {
        val messy = chem.copy(info = chem.info.copy(topic = "Ionic\tEquilibrium\nRevision"))
        val decoded = EventCodec.decode(EventCodec.encode(listOf(messy)))
        assertEquals("Ionic Equilibrium Revision", decoded.single().info.topic)
    }

    @Test
    fun `malformed line is skipped`() {
        val text = EventCodec.encode(listOf(chem)) + "\ngarbage\nu3\tnot-a-number\t1\t\tx\t\t\tfalse\tfalse"
        assertEquals(listOf(chem), EventCodec.decode(text))
    }

    @Test
    fun `empty text gives empty list`() {
        assertEquals(emptyList<ClassEvent>(), EventCodec.decode(""))
    }
}
