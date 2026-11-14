package com.saif.classbuzz.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.saif.classbuzz.ClassBuzz

/**
 * The hourly refresh alarm. Sets the next alarm first, so nothing that happens to this run can end
 * the chain, then hands the download to [RefreshJob] and returns straight away.
 */
class RefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ClassBuzz.scheduleNextRefresh(context)
        RefreshJob.enqueue(context)
    }
}
