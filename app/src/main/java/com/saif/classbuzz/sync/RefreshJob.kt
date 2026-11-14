package com.saif.classbuzz.sync

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import com.saif.classbuzz.ClassBuzz
import com.saif.classbuzz.log.EventLog
import kotlin.concurrent.thread

/**
 * The download runs as a job, not inside the alarm broadcast: a job gets network even when the watch
 * is dozing, has minutes instead of seconds, and never holds up a buzz waiting in the broadcast queue.
 */
class RefreshJob : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        thread(name = "class-buzz-refresh") {
            runCatching { ClassBuzz.runRefresh(this) }
                .onFailure { EventLog.add(this, "refresh crashed: $it") }
            jobFinished(params, false)
        }
        return true
    }

    // The next hourly alarm is already set, so a stopped job is simply retried then.
    override fun onStopJob(params: JobParameters): Boolean = false

    companion object {
        private const val JOB_ID = 1
        // Runs as soon as there is network; at the latest after this, so a failed run still checks staleness.
        private const val LATEST_MS = 10 * 60 * 1000L

        fun enqueue(context: Context) {
            val job = JobInfo.Builder(JOB_ID, ComponentName(context, RefreshJob::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setOverrideDeadline(LATEST_MS)
                .build()
            context.getSystemService(JobScheduler::class.java).schedule(job)
        }
    }
}
