package com.saif.classbuzz

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.saif.classbuzz.alarm.AlarmBuzzScheduler
import com.saif.classbuzz.log.EventLog
import com.saif.classbuzz.store.ScheduleStore
import com.saif.classbuzz.sync.CalendarDownloader
import com.saif.classbuzz.sync.RefreshOutcome
import com.saif.classbuzz.sync.RefreshReceiver
import com.saif.classbuzz.sync.Refresher
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Wires the pure pieces to the watch: real store, real alarms, real downloads. */
object ClassBuzz {
    private const val STATUS_CHANNEL = "class_buzz_status"
    private const val STALE_NOTIFICATION_ID = 1
    private val APP_OPEN_MIN_AGE: java.time.Duration = java.time.Duration.ofMinutes(10)
    private val lock = Any()

    fun store(context: Context): ScheduleStore = ScheduleStore(context.applicationContext)

    fun refresher(context: Context): Refresher {
        val app = context.applicationContext
        return Refresher(
            repo = store(app),
            scheduler = AlarmBuzzScheduler(app),
            fetch = CalendarDownloader::download,
            clock = Instant::now,
            zone = ZoneId.systemDefault(),
            log = { EventLog.add(app, it) },
            warnStale = { postStaleWarning(app, it) },
            bootCount = { Settings.Global.getInt(app.contentResolver, Settings.Global.BOOT_COUNT, 0) },
        )
    }

    /** Refreshes one at a time, whichever of the alarm, boot or the app asked. Call off the main thread. */
    fun runRefresh(context: Context): RefreshOutcome = synchronized(lock) { refresher(context).refresh() }

    fun runReschedule(context: Context) = synchronized(lock) { refresher(context).reschedule() }

    /** Opening the app: download only if the last good one is over 10 minutes old; always re-arm the alarms. */
    fun runAppOpenRefresh(context: Context): RefreshOutcome? =
        synchronized(lock) { refresher(context).refreshUnlessRecent(APP_OPEN_MIN_AGE) }

    /** The hourly refresh is a chain: each run sets the next one. */
    fun scheduleNextRefresh(context: Context) {
        val app = context.applicationContext
        val pending = PendingIntent.getBroadcast(
            app, 0, Intent(app, RefreshReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val at = Instant.now().plus(Config.REFRESH_EVERY).toEpochMilli()
        app.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
    }

    private fun postStaleWarning(context: Context, lastSuccess: Instant?) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(STATUS_CHANNEL, "Class Buzz status", NotificationManager.IMPORTANCE_DEFAULT),
        )
        val since = lastSuccess?.let {
            DateTimeFormatter.ofPattern("EEE H:mm").withZone(ZoneId.systemDefault()).format(it)
        }
        val text = if (since == null) {
            "Class Buzz hasn't downloaded your calendar yet. Check Wi-Fi or LTE."
        } else {
            "Class Buzz hasn't updated since $since. Check Wi-Fi or LTE."
        }
        val notification = Notification.Builder(context, STATUS_CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
            .setContentTitle("Class Buzz")
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        manager.notify(STALE_NOTIFICATION_ID, notification)
        EventLog.add(context, "stale warning posted")
    }
}
