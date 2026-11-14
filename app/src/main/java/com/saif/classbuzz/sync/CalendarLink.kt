package com.saif.classbuzz.sync

import com.saif.classbuzz.Config

/** Checks the calendar link sent from the Mac, and hides most of it in logs. */
object CalendarLink {
    private const val VISIBLE_CHARS = 12

    fun isValid(url: String): Boolean {
        val link = url.trim()
        return link.startsWith(Config.URL_PREFIX) && link.endsWith(".ics")
    }

    fun masked(url: String): String = "…" + url.trim().takeLast(VISIBLE_CHARS)
}
