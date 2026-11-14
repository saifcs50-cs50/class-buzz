package com.saif.classbuzz.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.saif.classbuzz.sync.BuzzScheduling
import com.saif.classbuzz.ui.MainActivity

/** Real alarms for planned buzzes. Each buzz gets its own alarm, told apart by a data URI. */
class AlarmBuzzScheduler(private val context: Context) : BuzzScheduling {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    override fun schedule(buzz: PlannedBuzz) {
        setAt(context, buzz.triggerAt.toEpochMilli(), pendingIntent(context, buzz.key, buzz.uid))
    }

    override fun cancel(key: String) {
        alarms.cancel(pendingIntent(context, key, uid = null))
    }

    companion object {
        const val EXTRA_DUE_AT = "due_at"

        fun canScheduleExact(alarms: AlarmManager): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

        /**
         * Sets a buzz as an alarm-clock alarm. Off the charger the watch held plain exact alarms until the
         * screen woke (93 s late, 2026-11-03); alarm clocks are the one kind power saving must deliver on time.
         * Returns false when only an inexact alarm was possible.
         */
        fun setAt(context: Context, atMillis: Long, pending: PendingIntent): Boolean {
            val alarms = context.getSystemService(AlarmManager::class.java)
            if (!canScheduleExact(alarms)) {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
                return false
            }
            val showApp = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarms.setAlarmClock(AlarmManager.AlarmClockInfo(atMillis, showApp), pending)
            return true
        }

        /** Extras don't count when Android matches PendingIntents; the key in the data URI does. */
        fun pendingIntent(context: Context, key: String, uid: String?, dueAt: Long? = null): PendingIntent {
            val intent = Intent(context, BuzzReceiver::class.java)
                .setData(Uri.parse("classbuzz://buzz/" + Uri.encode(key)))
                .putExtra(BuzzAlert.EXTRA_KEY, key)
            uid?.let { intent.putExtra(BuzzAlert.EXTRA_UID, it) }
            dueAt?.let { intent.putExtra(EXTRA_DUE_AT, it) }
            return PendingIntent.getBroadcast(
                context, key.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
