package com.saif.classbuzz.alarm

import com.saif.classbuzz.calendar.ClassEvent
import com.saif.classbuzz.calendar.ClassInfo
import com.saif.classbuzz.calendar.Subject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class BuzzPlanTest {
    private val lead = Duration.ofMinutes(5)

    // 08:30 IST on 2026-11-05.
    private val classStart = Instant.parse("2026-11-05T03:00:00Z")

    private fun event(uid: String, start: Instant, noBuzz: Boolean = false) = ClassEvent(
        uid = uid,
        start = start,
        end = start.plus(Duration.ofHours(2)),
        info = ClassInfo(Subject.CHEMISTRY, "Ionic Equilibrium", listOf("ABC"), "Batch A", isTest = false, isNoBuzz = noBuzz),
    )

    @Test
    fun `buzz is 5 minutes before start`() {
        val now = Instant.parse("2026-11-05T01:30:00Z") // 07:00 IST
        val plan = BuzzPlan.plan(listOf(event("u1", classStart)), now, lead, emptySet())
        assertEquals(listOf(PlannedBuzz("u1@1793847600", "u1", Instant.parse("2026-11-05T02:55:00Z"))), plan)
    }

    @Test
    fun `class starting in 3 minutes buzzes right away`() {
        val now = classStart.minus(Duration.ofMinutes(3))
        val plan = BuzzPlan.plan(listOf(event("u1", classStart)), now, lead, emptySet())
        assertEquals(now.plusSeconds(1), plan.single().triggerAt)
    }

    @Test
    fun `already fired class is not planned again`() {
        val now = classStart.minus(Duration.ofMinutes(3))
        val plan = BuzzPlan.plan(listOf(event("u1", classStart)), now, lead, setOf("u1@1793847600"))
        assertEquals(emptyList<PlannedBuzz>(), plan)
    }

    @Test
    fun `started class gets no buzz`() {
        val now = classStart.plusSeconds(60)
        assertEquals(emptyList<PlannedBuzz>(), BuzzPlan.plan(listOf(event("u1", classStart)), now, lead, emptySet()))
    }

    @Test
    fun `holiday gets no buzz`() {
        val now = Instant.parse("2026-11-05T01:30:00Z")
        val plan = BuzzPlan.plan(listOf(event("rain", classStart, noBuzz = true)), now, lead, emptySet())
        assertEquals(emptyList<PlannedBuzz>(), plan)
    }

    @Test
    fun `two classes at the same time both get a buzz with different keys`() {
        val now = Instant.parse("2026-11-05T01:30:00Z")
        val plan = BuzzPlan.plan(listOf(event("class", classStart), event("test", classStart)), now, lead, emptySet())
        assertEquals(setOf("class@1793847600", "test@1793847600"), plan.map { it.key }.toSet())
    }

    @Test
    fun `moved class cancels old key and schedules new key`() {
        val now = Instant.parse("2026-11-05T01:30:00Z")
        val movedStart = classStart.plus(Duration.ofMinutes(45))
        val scheduled = mapOf("u1@1793847600" to Instant.parse("2026-11-05T02:55:00Z"))
        val diff = BuzzPlan.diff(scheduled, BuzzPlan.plan(listOf(event("u1", movedStart)), now, lead, emptySet()))
        assertEquals(listOf("u1@1793847600"), diff.toCancel)
        assertEquals(listOf(PlannedBuzz("u1@1793850300", "u1", Instant.parse("2026-11-05T03:40:00Z"))), diff.toSchedule)
    }

    @Test
    fun `deleted class is cancelled`() {
        val diff = BuzzPlan.diff(mapOf("u2@x" to classStart), emptyList())
        assertEquals(listOf("u2@x"), diff.toCancel)
        assertTrue(diff.toSchedule.isEmpty())
    }

    @Test
    fun `unchanged buzz is not rescheduled`() {
        val buzz = PlannedBuzz("u1@1793847600", "u1", Instant.parse("2026-11-05T02:55:00Z"))
        assertEquals(BuzzDiff(emptyList(), emptyList()), BuzzPlan.diff(mapOf(buzz.key to buzz.triggerAt), listOf(buzz)))
    }

    @Test
    fun `same key with a new time is rescheduled`() {
        val buzz = PlannedBuzz("u1@1793847600", "u1", Instant.parse("2026-11-05T02:57:00Z"))
        val diff = BuzzPlan.diff(mapOf(buzz.key to Instant.parse("2026-11-05T02:55:00Z")), listOf(buzz))
        assertEquals(listOf(buzz), diff.toSchedule)
        assertTrue(diff.toCancel.isEmpty())
    }
}
