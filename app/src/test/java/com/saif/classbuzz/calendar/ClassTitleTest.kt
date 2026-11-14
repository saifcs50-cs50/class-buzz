package com.saif.classbuzz.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** One case per title shape the app has to read. */
class ClassTitleTest {

    @Test
    fun `class with teacher subject topic and batch`() {
        val info = ClassTitle.parse("ABC;Chem-Ionic Equilibrium-3. pH ,pOH and their effects;Batch-Batch A")
        assertEquals(Subject.CHEMISTRY, info.subject)
        assertEquals(listOf("ABC"), info.teachers)
        assertEquals("Batch A", info.batchTag)
        assertEquals("Ionic Equilibrium-3. pH ,pOH and their effects", info.topic)
        assertFalse(info.isTest)
        assertFalse(info.isNoBuzz)
    }

    @Test
    fun `maths class keeps the dash inside the topic`() {
        val info = ClassTitle.parse("DEF;Math-Circles-I;Batch-Batch A")
        assertEquals(Subject.MATHS, info.subject)
        assertEquals("Circles-I", info.topic)
    }

    @Test
    fun `physics class with trailing space before the batch`() {
        val info = ClassTitle.parse("GHI;Phy-Kinematics - 1D - 1 ;Batch-Batch A")
        assertEquals(Subject.PHYSICS, info.subject)
        assertEquals("Kinematics - 1D - 1", info.topic)
    }

    @Test
    fun `unit test names its subject in a separate part`() {
        val info = ClassTitle.parse("II Unit Test; Physics")
        assertEquals(Subject.PHYSICS, info.subject)
        assertEquals(emptyList<String>(), info.teachers)
        assertNull(info.batchTag)
        assertTrue(info.isTest)
        assertEquals("II Unit Test", info.topic)
    }

    @Test
    fun `part test with no subject`() {
        val info = ClassTitle.parse("Part 8 test")
        assertNull(info.subject)
        assertTrue(info.isTest)
        assertEquals("Part 8 test", info.topic)
    }

    @Test
    fun `older format with a separate batch part`() {
        val info = ClassTitle.parse("JKL;Chem;Batch;PCM-Batch B")
        assertEquals(Subject.CHEMISTRY, info.subject)
        assertEquals(listOf("JKL"), info.teachers)
        assertEquals("PCM-Batch B", info.batchTag)
        assertEquals("", info.topic)
    }

    @Test
    fun `lab exam with two teachers and a spaced batch tag`() {
        val info = ClassTitle.parse("MNO;GHI;Phy lab midterm Exam; Batch - Batch D")
        assertEquals(listOf("MNO", "GHI"), info.teachers)
        assertEquals(Subject.PHYSICS, info.subject)
        assertEquals("lab midterm Exam", info.topic)
        assertTrue(info.isTest)
        assertEquals("Batch D", info.batchTag)
    }

    @Test
    fun `slash separated teachers`() {
        assertEquals(listOf("PQR", "ST", "UVW"), ClassTitle.parse("PQR/ST/UVW;Elective A/Elective B/Elective C").teachers)
    }

    @Test
    fun `study session takes its topic from the next part`() {
        val info = ClassTitle.parse("Physics;Study session")
        assertEquals(Subject.PHYSICS, info.subject)
        assertEquals("Study session", info.topic)
    }

    @Test
    fun `holiday gets no buzz`() {
        val info = ClassTitle.parse("Rain Holiday")
        assertTrue(info.isNoBuzz)
        assertNull(info.subject)
        assertEquals("Rain Holiday", info.topic)
    }

    @Test
    fun `cancelled in the title gets no buzz`() {
        assertTrue(ClassTitle.parse("ABC;Chem-Thermodynamics (Cancelled);Batch-Batch A").isNoBuzz)
    }

    @Test
    fun `physical is not physics`() {
        assertNull(ClassTitle.parse("Physical Education").subject)
    }

    @Test
    fun `empty parts do not crash`() {
        val info = ClassTitle.parse("weird;;;")
        assertEquals("weird", info.topic)
        assertNull(info.subject)
    }
}
