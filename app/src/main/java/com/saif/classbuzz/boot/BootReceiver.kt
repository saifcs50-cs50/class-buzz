package com.saif.classbuzz.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.saif.classbuzz.ClassBuzz
import com.saif.classbuzz.log.EventLog
import com.saif.classbuzz.sync.RefreshJob
import kotlin.concurrent.thread

/**
 * After a restart (or an app update) put the buzzes back from the saved schedule and restart the
 * refresh chain. Huawei may never deliver this; the boot-count check in the refresher covers that.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        ClassBuzz.scheduleNextRefresh(context)
        RefreshJob.enqueue(context)
        val pending = goAsync()
        thread(name = "class-buzz-boot") {
            try {
                EventLog.add(context, "boot: ${intent.action?.substringAfterLast('.')}")
                runCatching { ClassBuzz.runReschedule(context) }
                    .onFailure { EventLog.add(context, "boot reschedule crashed: $it") }
            } finally {
                pending.finish()
            }
        }
    }
}
