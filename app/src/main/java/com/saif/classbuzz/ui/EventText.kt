package com.saif.classbuzz.ui

import com.saif.classbuzz.Config
import com.saif.classbuzz.calendar.ClassEvent
import com.saif.classbuzz.calendar.ClassInfo
import com.saif.classbuzz.calendar.NEUTRAL_COLOR
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Every piece of text and colour on the countdown ring, kept free of Android so it can be tested. */
object EventText {
    private const val MILLIS_PER_MINUTE = 60_000L
    private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

    fun minutesLeft(start: Instant, now: Instant): Long {
        val millis = Duration.between(now, start).toMillis()
        return if (millis <= 0) 0 else (millis + MILLIS_PER_MINUTE - 1) / MILLIS_PER_MINUTE
    }

    fun headline(info: ClassInfo): String = "MIN TO " + when {
        info.subject != null -> info.subject.short
        info.isTest -> "TEST"
        else -> "CLASS"
    }

    fun meta(event: ClassEvent, zone: ZoneId): String {
        val times = "${TIME.format(event.start.atZone(zone))} – ${TIME.format(event.end.atZone(zone))}"
        val who = if (event.info.isTest) "TEST" else event.info.teachers.joinToString("/")
        return if (who.isEmpty()) times else "$who · $times"
    }

    fun topic(info: ClassInfo): String = info.topic.ifEmpty { info.subject?.label ?: "Class" }

    /** 1 when the buzz goes off, 0 when the class starts. */
    fun ringFraction(start: Instant, now: Instant, lead: Duration): Float {
        val left = Duration.between(now, start).toMillis().toFloat()
        return (left / lead.toMillis()).coerceIn(0f, 1f)
    }

    fun color(info: ClassInfo): Int = info.subject?.color ?: NEUTRAL_COLOR

    /**
     * What to list under the ring: the rest of the next class's day. If that day has nothing more,
     * a preview of the first class on the following day that has any.
     */
    fun laterList(events: List<ClassEvent>, now: Instant, zone: ZoneId): List<ClassEvent> {
        val upcoming = events.filter { it.end.isAfter(now) }.sortedBy { it.start }
        val next = upcoming.firstOrNull() ?: return emptyList()
        val nextDay = next.start.atZone(zone).toLocalDate()
        val after = upcoming.drop(1)
        val sameDay = after.filter { it.start.atZone(zone).toLocalDate() == nextDay }
        if (sameDay.isNotEmpty()) return sameDay
        return listOfNotNull(after.firstOrNull { it.start.atZone(zone).toLocalDate().isAfter(nextDay) })
    }

    fun isStale(lastSuccess: Instant?, now: Instant): Boolean =
        lastSuccess == null || Duration.between(lastSuccess, now) > Config.STALE_AFTER

    fun updatedLine(lastSuccess: Instant?, now: Instant): String {
        if (lastSuccess == null) return "Never updated"
        val age = Duration.between(lastSuccess, now)
        return when {
            isStale(lastSuccess, now) -> "Not updated for ${age.toHours()} h"
            age.toMinutes() < 1 -> "Updated just now"
            age.toMinutes() < 60 -> "Updated ${age.toMinutes()} min ago"
            else -> "Updated ${age.toHours()} h ago"
        }
    }
}
