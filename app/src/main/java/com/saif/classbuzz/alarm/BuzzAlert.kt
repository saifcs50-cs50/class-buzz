package com.saif.classbuzz.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.saif.classbuzz.log.EventLog
import com.saif.classbuzz.ui.BuzzActivity

/** [uid] names the class to show on the ring screen; null for a plain test buzz. */
data class AlertContent(val key: String, val headline: String, val detail: String, val uid: String? = null)

/** The buzz itself: an alarm-class vibration, a notification, and the full-screen alert screen. */
object BuzzAlert {
    const val EXTRA_KEY = "key"
    const val EXTRA_HEADLINE = "headline"
    const val EXTRA_DETAIL = "detail"
    const val EXTRA_UID = "uid"
    private const val CHANNEL_ID = "class_buzz"
    private val PATTERN = longArrayOf(0, 600, 200, 600, 200, 600)

    // Alarm usage is what lets the vibration through Do Not Disturb.
    private val ALARM_AUDIO: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    fun fire(context: Context, alert: AlertContent) {
        runCatching { vibrate(context) }
            .onSuccess { EventLog.add(context, "vibrated ${alert.key}") }
            .onFailure { EventLog.add(context, "vibrate FAILED ${alert.key}: $it") }
        runCatching { notify(context, alert) }
            .onSuccess { EventLog.add(context, "notified ${alert.key} ($it)") }
            .onFailure { EventLog.add(context, "notify FAILED ${alert.key}: $it") }
    }

    private fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        @Suppress("DEPRECATION")
        vibrator.vibrate(VibrationEffect.createWaveform(PATTERN, -1), ALARM_AUDIO)
    }

    /** Returns a short note on the Do Not Disturb state, for the log. */
    private fun notify(context: Context, alert: AlertContent): String {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_ID, "Class buzz", NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true)
            vibrationPattern = PATTERN
            setBypassDnd(true)
        }
        manager.createNotificationChannel(channel)

        val screen = PendingIntent.getActivity(
            context,
            alert.key.hashCode(),
            Intent(context, BuzzActivity::class.java)
                .putExtra(EXTRA_KEY, alert.key)
                .putExtra(EXTRA_HEADLINE, alert.headline)
                .putExtra(EXTRA_DETAIL, alert.detail)
                .putExtra(EXTRA_UID, alert.uid)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(alert.headline)
            .setContentText(alert.detail)
            .setCategory(Notification.CATEGORY_ALARM)
            .setFullScreenIntent(screen, true)
            .setContentIntent(screen)
            .setAutoCancel(true)
            .build()
        manager.notify(alert.key.hashCode(), notification)
        return "dndFilter=${manager.currentInterruptionFilter} dndAccess=${manager.isNotificationPolicyAccessGranted}"
    }
}
