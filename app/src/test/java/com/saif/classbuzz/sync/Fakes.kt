package com.saif.classbuzz.sync

import com.saif.classbuzz.alarm.PlannedBuzz
import com.saif.classbuzz.calendar.ClassEvent
import com.saif.classbuzz.store.ScheduleRepository
import java.time.Instant

/** In-memory stand-in for the on-watch store. */
class FakeRepository(private var url: String? = "https://calendar.google.com/calendar/ical/test/public/basic.ics") :
    ScheduleRepository {
    private var events: List<ClassEvent> = emptyList()
    private var lastSuccess: Instant? = null
    private var scheduled: Map<String, Instant> = emptyMap()
    private var fired: Set<String> = emptySet()
    private var staleWarned = false
    private var bootCount: Int? = null

    override fun calendarUrl() = url
    override fun setCalendarUrl(url: String) { this.url = url }
    override fun loadEvents() = events
    override fun saveEvents(events: List<ClassEvent>, fetchedAt: Instant) {
        this.events = events
        lastSuccess = fetchedAt
    }
    override fun lastSuccess() = lastSuccess
    override fun scheduled() = scheduled
    override fun saveScheduled(scheduled: Map<String, Instant>) { this.scheduled = scheduled }
    override fun fired() = fired
    override fun markFired(key: String) { fired = fired + key }
    override fun staleWarned() = staleWarned
    override fun setStaleWarned(warned: Boolean) { staleWarned = warned }
    override fun lastBootCount() = bootCount
    override fun setLastBootCount(count: Int) { bootCount = count }
}

/** Records what the refresher asked the alarm system to do. */
class FakeScheduler : com.saif.classbuzz.sync.BuzzScheduling {
    val scheduled = mutableListOf<PlannedBuzz>()
    val cancelled = mutableListOf<String>()

    override fun schedule(buzz: PlannedBuzz) { scheduled += buzz }
    override fun cancel(key: String) { cancelled += key }
}
