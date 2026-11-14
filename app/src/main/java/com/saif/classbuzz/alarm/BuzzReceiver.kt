package com.saif.classbuzz.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.saif.classbuzz.ClassBuzz
import com.saif.classbuzz.Config
import com.saif.classbuzz.calendar.ClassEvent
import com.saif.classbuzz.log.EventLog
import com.saif.classbuzz.ui.EventText
import java.time.Instant
import java.time.ZoneId

/** Fires a buzz when its alarm goes off: a class from the schedule, or a test buzz. */
class BuzzReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val key = intent.getStringExtra(BuzzAlert.EXTRA_KEY)
        if (key == null) {
            EventLog.add(context, "buzz alarm without a key, ignored")
            return
        }
        val uid = intent.getStringExtra(BuzzAlert.EXTRA_UID)
        if (uid == null) fireTest(context, key, intent) else fireClass(context, key, uid)
    }

    private fun fireClass(context: Context, key: String, uid: String) {
        val store = ClassBuzz.store(context)
        val event = store.loadEvents().find { it.uid == uid }
        val now = Instant.now()
        when {
            event == null -> EventLog.add(context, "buzz $key: event no longer in schedule, skipped")
            !event.end.isAfter(now) -> EventLog.add(context, "buzz $key: class already over, skipped")
            else -> {
                store.markFired(key)
                val lateSeconds = now.epochSecond - event.start.minus(Config.LEAD).epochSecond
                EventLog.add(context, "buzz $key fired ${lateSeconds}s after its time")
                BuzzAlert.fire(context, classAlert(key, event))
            }
        }
    }

    private fun fireTest(context: Context, key: String, intent: Intent) {
        val dueAt = intent.getLongExtra(AlarmBuzzScheduler.EXTRA_DUE_AT, -1L)
        val lateSeconds = (System.currentTimeMillis() - dueAt) / 1000
        EventLog.add(context, "buzz $key fired ${lateSeconds}s late")
        BuzzAlert.fire(context, AlertContent(key, "Test buzz", "Fired ${lateSeconds}s late"))
    }

    /** The notification text; the full-screen ring reads the class itself through [AlertContent.uid]. */
    private fun classAlert(key: String, event: ClassEvent): AlertContent {
        val info = event.info
        val headline = "${EventText.minutesLeft(event.start, Instant.now())} ${EventText.headline(info)}"
        val detail = "${EventText.topic(info)} · ${EventText.meta(event, ZoneId.systemDefault())}"
        return AlertContent(key, headline, detail, event.uid)
    }
}

/**
 * A buzz N seconds from now, for checking the watch delivers it (adb: --es cmd testbuzz --ei seconds N).
 * With a [uid] it shows that class's real ring screen.
 */
object TestBuzz {
    fun schedule(context: Context, seconds: Int, uid: String?) {
        val dueAt = System.currentTimeMillis() + seconds * 1000L
        val key = "test@${dueAt / 1000}"
        val pending = AlarmBuzzScheduler.pendingIntent(context, key, uid = uid, dueAt = dueAt)
        val isAlarmClock = AlarmBuzzScheduler.setAt(context, dueAt, pending)
        EventLog.add(context, "test buzz $key scheduled in ${seconds}s (alarmClock=$isAlarmClock)")
    }
}
