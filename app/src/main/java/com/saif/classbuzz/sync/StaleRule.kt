package com.saif.classbuzz.sync

import com.saif.classbuzz.Config
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

/** When to warn that the calendar hasn't downloaded for a while. */
object StaleRule {
    fun shouldWarn(lastSuccess: Instant?, warnedThisStretch: Boolean, now: ZonedDateTime): Boolean {
        val isStale = lastSuccess == null || Duration.between(lastSuccess, now.toInstant()) > Config.STALE_AFTER
        val isDaytime = now.hour >= Config.WARN_FROM_HOUR && now.hour < Config.WARN_UNTIL_HOUR
        return isStale && !warnedThisStretch && isDaytime
    }
}
