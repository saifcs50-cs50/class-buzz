package com.saif.classbuzz.alarm

import com.saif.classbuzz.calendar.ClassEvent
import java.time.Duration
import java.time.Instant

/** One buzz to schedule. The key includes the start time, so a moved class gets a fresh buzz. */
data class PlannedBuzz(val key: String, val uid: String, val triggerAt: Instant)

data class BuzzDiff(val toSchedule: List<PlannedBuzz>, val toCancel: List<String>)

object BuzzPlan {
    fun keyFor(event: ClassEvent): String = "${event.uid}@${event.start.epochSecond}"

    /** Buzzes for every class not yet started, not marked no-buzz, and not already buzzed. */
    fun plan(events: List<ClassEvent>, now: Instant, lead: Duration, fired: Set<String>): List<PlannedBuzz> =
        events
            .filter { !it.info.isNoBuzz && it.start.isAfter(now) && keyFor(it) !in fired }
            .map { event ->
                val early = event.start.minus(lead)
                // A class inside the lead window that never buzzed buzzes right away.
                val triggerAt = if (early.isAfter(now)) early else now.plusSeconds(1)
                PlannedBuzz(keyFor(event), event.uid, triggerAt)
            }

    /** What to change so the scheduled alarms match the plan. */
    fun diff(scheduled: Map<String, Instant>, next: List<PlannedBuzz>): BuzzDiff {
        val nextKeys = next.map { it.key }.toSet()
        return BuzzDiff(
            toSchedule = next.filter { scheduled[it.key] != it.triggerAt },
            toCancel = scheduled.keys.filter { it !in nextKeys },
        )
    }
}
