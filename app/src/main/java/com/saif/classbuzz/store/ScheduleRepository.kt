package com.saif.classbuzz.store

import com.saif.classbuzz.calendar.ClassEvent
import java.time.Instant

/** Everything Class Buzz keeps between runs. */
interface ScheduleRepository {
    fun calendarUrl(): String?
    fun setCalendarUrl(url: String)

    /** The last good download's events (your classes only). */
    fun loadEvents(): List<ClassEvent>
    fun saveEvents(events: List<ClassEvent>, fetchedAt: Instant)
    fun lastSuccess(): Instant?

    /** Buzz key to the time its alarm is set for. */
    fun scheduled(): Map<String, Instant>
    fun saveScheduled(scheduled: Map<String, Instant>)

    /** Keys of buzzes that already went off, so a refresh never repeats them. */
    fun fired(): Set<String>
    fun markFired(key: String)

    fun staleWarned(): Boolean
    fun setStaleWarned(warned: Boolean)

    /** The watch's boot count when [scheduled] was last true; a different count means every alarm is gone. */
    fun lastBootCount(): Int?
    fun setLastBootCount(count: Int)
}
