package com.saif.classbuzz.calendar

import com.saif.classbuzz.Config
import java.time.Instant

data class ClassEvent(val uid: String, val start: Instant, val end: Instant, val info: ClassInfo)

/** Picks your events out of a shared batch calendar. */
object MyClasses {
    private val ALLOWED = Config.ALLOWED_BATCHES.map(::normalize)

    /** Untagged events are for everyone; tagged ones only if the tag is one of your batches. */
    fun isMine(batchTag: String?): Boolean {
        if (batchTag == null) return true
        val tag = normalize(batchTag)
        return ALLOWED.any { tag.contains(it) }
    }

    fun select(events: List<IcsEvent>, now: Instant): List<ClassEvent> {
        val until = now.plus(Config.HORIZON)
        return events
            .filter { !it.isAllDay && !it.isCancelled && it.end.isAfter(now) && it.start.isBefore(until) }
            .map { ClassEvent(it.uid, it.start, it.end, ClassTitle.parse(it.summary)) }
            .filter { isMine(it.info.batchTag) }
            .sortedBy { it.start }
    }

    private fun normalize(text: String): String = text.lowercase().filter { it.isLetterOrDigit() }
}
