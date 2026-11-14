package com.saif.classbuzz

import java.time.Duration

/** Every tunable number and name in one place. */
object Config {
    val LEAD: Duration = Duration.ofMinutes(5)
    val REFRESH_EVERY: Duration = Duration.ofMinutes(60)
    val STALE_AFTER: Duration = Duration.ofHours(6)
    const val WARN_FROM_HOUR = 7
    const val WARN_UNTIL_HOUR = 21
    val HORIZON: Duration = Duration.ofDays(7)

    /**
     * Batch tags whose events are yours. Untagged events are yours too. Set these to the batch names that
     * appear in your calendar's event titles (`Batch-<name>`) before you build; the values here are placeholders.
     */
    val ALLOWED_BATCHES = listOf("Batch A", "Batch B")

    const val URL_PREFIX = "https://calendar.google.com/calendar/ical/"
}
