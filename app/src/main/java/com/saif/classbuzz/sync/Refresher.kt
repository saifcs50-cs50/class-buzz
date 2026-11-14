package com.saif.classbuzz.sync

import com.saif.classbuzz.Config
import com.saif.classbuzz.alarm.BuzzPlan
import com.saif.classbuzz.alarm.PlannedBuzz
import com.saif.classbuzz.calendar.IcsParser
import com.saif.classbuzz.calendar.MyClasses
import com.saif.classbuzz.store.ScheduleRepository
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** The alarm system, as the refresher sees it. */
interface BuzzScheduling {
    fun schedule(buzz: PlannedBuzz)
    fun cancel(key: String)
}

sealed interface RefreshOutcome {
    data class Updated(val mine: Int) : RefreshOutcome
    data class Failed(val reason: String) : RefreshOutcome
    data object NoLink : RefreshOutcome
}

/**
 * Downloads the calendar, keeps your events, and makes the alarms match them.
 * A bad or partial download never touches the saved schedule.
 */
class Refresher(
    private val repo: ScheduleRepository,
    private val scheduler: BuzzScheduling,
    private val fetch: (String) -> Result<String>,
    private val clock: () -> Instant,
    private val zone: ZoneId,
    private val log: (String) -> Unit,
    private val warnStale: (Instant?) -> Unit,
    private val bootCount: () -> Int,
) {
    fun refresh(): RefreshOutcome {
        val outcome = download()
        log("refresh $outcome")
        if (outcome is RefreshOutcome.Updated) {
            repo.setStaleWarned(false)
        } else {
            checkStale()
        }
        reschedule()
        return outcome
    }

    /**
     * For opening the app: skips the download when the last good one is younger than [minAge]
     * (returns null then), but always makes sure the alarms are set.
     */
    fun refreshUnlessRecent(minAge: Duration): RefreshOutcome? {
        val last = repo.lastSuccess()
        if (last != null && Duration.between(last, clock()) < minAge) {
            reschedule()
            return null
        }
        return refresh()
    }

    /** Makes the alarms match the saved events: new and moved buzzes set, vanished ones cancelled. */
    fun reschedule() {
        // A restart wipes every alarm, and Huawei may not tell the app it restarted, so check here.
        val boot = bootCount()
        if (repo.lastBootCount() != boot) {
            repo.saveScheduled(emptyMap())
            repo.setLastBootCount(boot)
            log("new boot $boot: setting every alarm again")
        }
        val now = clock()
        val plan = BuzzPlan.plan(repo.loadEvents(), now, Config.LEAD, repo.fired())
        val diff = BuzzPlan.diff(repo.scheduled(), plan)
        diff.toCancel.forEach(scheduler::cancel)
        diff.toSchedule.forEach(scheduler::schedule)
        repo.saveScheduled(plan.associate { it.key to it.triggerAt })
        if (diff.toSchedule.isNotEmpty() || diff.toCancel.isNotEmpty()) {
            log("alarms: +${diff.toSchedule.size} -${diff.toCancel.size}, ${plan.size} pending")
        }
    }

    private fun download(): RefreshOutcome {
        val url = repo.calendarUrl() ?: return RefreshOutcome.NoLink
        val text = fetch(url).getOrElse { return RefreshOutcome.Failed(it.javaClass.simpleName + ": " + it.message) }
        if (!IcsParser.isCompleteCalendar(text)) {
            return RefreshOutcome.Failed("not a complete calendar (${text.length} chars)")
        }
        val now = clock()
        val mine = MyClasses.select(IcsParser.parse(text), now)
        repo.saveEvents(mine, now)
        return RefreshOutcome.Updated(mine.size)
    }

    private fun checkStale() {
        val lastSuccess = repo.lastSuccess()
        if (StaleRule.shouldWarn(lastSuccess, repo.staleWarned(), clock().atZone(zone))) {
            warnStale(lastSuccess)
            repo.setStaleWarned(true)
        }
    }
}
