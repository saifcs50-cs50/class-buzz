package com.saif.classbuzz.store

import android.content.Context
import com.saif.classbuzz.calendar.ClassEvent
import java.io.File
import java.time.Duration
import java.time.Instant

/** Events in a file (replaced whole, never half-written); everything else in SharedPreferences. */
class ScheduleStore(context: Context) : ScheduleRepository {
    private val eventsFile = File(context.filesDir, "schedule.tsv")
    private val prefs = context.getSharedPreferences("class_buzz_store", Context.MODE_PRIVATE)

    override fun calendarUrl(): String? = prefs.getString(KEY_URL, null)

    override fun setCalendarUrl(url: String) {
        prefs.edit().putString(KEY_URL, url).commit()
    }

    override fun loadEvents(): List<ClassEvent> =
        runCatching { EventCodec.decode(eventsFile.readText()) }.getOrDefault(emptyList())

    override fun saveEvents(events: List<ClassEvent>, fetchedAt: Instant) {
        val temp = File(eventsFile.parentFile, "${eventsFile.name}.tmp")
        temp.writeText(EventCodec.encode(events))
        check(temp.renameTo(eventsFile)) { "could not replace ${eventsFile.name}" }
        prefs.edit().putLong(KEY_LAST_SUCCESS, fetchedAt.epochSecond).commit()
    }

    override fun lastSuccess(): Instant? =
        prefs.getLong(KEY_LAST_SUCCESS, -1L).takeIf { it >= 0 }?.let(Instant::ofEpochSecond)

    override fun scheduled(): Map<String, Instant> =
        prefs.getStringSet(KEY_SCHEDULED, emptySet()).orEmpty().mapNotNull { entry ->
            val key = entry.substringBeforeLast('=')
            entry.substringAfterLast('=').toLongOrNull()?.let { key to Instant.ofEpochSecond(it) }
        }.toMap()

    override fun saveScheduled(scheduled: Map<String, Instant>) {
        val entries = scheduled.map { (key, at) -> "$key=${at.epochSecond}" }.toSet()
        prefs.edit().putStringSet(KEY_SCHEDULED, entries).commit()
    }

    override fun fired(): Set<String> = prefs.getStringSet(KEY_FIRED, emptySet()).orEmpty().toSet()

    /** Keeps only keys for classes that started in the last two days, so the set stays small. */
    override fun markFired(key: String) {
        val cutoff = Instant.now().minus(FIRED_KEEP).epochSecond
        val kept = (fired() + key).filter { (it.substringAfterLast('@').toLongOrNull() ?: 0L) > cutoff }.toSet()
        prefs.edit().putStringSet(KEY_FIRED, kept).commit()
    }

    override fun staleWarned(): Boolean = prefs.getBoolean(KEY_STALE_WARNED, false)

    override fun setStaleWarned(warned: Boolean) {
        prefs.edit().putBoolean(KEY_STALE_WARNED, warned).commit()
    }

    override fun lastBootCount(): Int? = prefs.getInt(KEY_BOOT_COUNT, -1).takeIf { it >= 0 }

    override fun setLastBootCount(count: Int) {
        prefs.edit().putInt(KEY_BOOT_COUNT, count).commit()
    }

    private companion object {
        const val KEY_BOOT_COUNT = "boot_count"
        const val KEY_URL = "calendar_url"
        const val KEY_LAST_SUCCESS = "last_success"
        const val KEY_SCHEDULED = "scheduled"
        const val KEY_FIRED = "fired"
        const val KEY_STALE_WARNED = "stale_warned"
        val FIRED_KEEP: Duration = Duration.ofDays(2)
    }
}
