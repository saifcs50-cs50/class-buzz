package com.saif.classbuzz.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class StaleRuleTest {
    private val india = ZoneId.of("Asia/Kolkata")

    private fun at(hour: Int, minute: Int = 0) = ZonedDateTime.of(2026, 11, 5, hour, minute, 0, 0, india)

    @Test
    fun `fresh download never warns`() {
        val now = at(10)
        assertFalse(StaleRule.shouldWarn(now.minusHours(2).toInstant(), false, now))
    }

    @Test
    fun `seven hours old during the day warns`() {
        val now = at(10)
        assertTrue(StaleRule.shouldWarn(now.minusHours(7).toInstant(), false, now))
    }

    @Test
    fun `just under six hours old does not warn`() {
        val now = at(10)
        assertFalse(StaleRule.shouldWarn(now.minusHours(6).plusMinutes(1).toInstant(), false, now))
    }

    @Test
    fun `no warning late at night`() {
        val now = at(22, 30)
        assertFalse(StaleRule.shouldWarn(now.minusHours(7).toInstant(), false, now))
    }

    @Test
    fun `no warning before seven in the morning`() {
        val now = at(6, 59)
        assertFalse(StaleRule.shouldWarn(now.minusHours(7).toInstant(), false, now))
    }

    @Test
    fun `warning window ends at nine at night`() {
        assertTrue(StaleRule.shouldWarn(at(20, 59).minusHours(7).toInstant(), false, at(20, 59)))
        assertFalse(StaleRule.shouldWarn(at(21).minusHours(7).toInstant(), false, at(21)))
    }

    @Test
    fun `only one warning per stale stretch`() {
        val now = at(10)
        assertFalse(StaleRule.shouldWarn(now.minusHours(7).toInstant(), true, now))
    }

    @Test
    fun `never downloaded warns during the day`() {
        assertTrue(StaleRule.shouldWarn(null, false, at(10)))
    }
}
