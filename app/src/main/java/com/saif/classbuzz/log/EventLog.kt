package com.saif.classbuzz.log

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Small on-watch log, newest first, so problems can be read back over adb:
 * `adb shell run-as com.saif.classbuzz cat shared_prefs/class_buzz_log.xml`
 */
object EventLog {
    private const val PREFS = "class_buzz_log"
    private const val KEY_LINES = "lines"
    private const val MAX_LINES = 200
    private const val TAG = "ClassBuzz"
    private val lock = Any()

    fun add(context: Context, message: String) {
        val stamp = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(Date())
        val line = "$stamp $message"
        Log.i(TAG, line)
        synchronized(lock) {
            val lines = (listOf(line) + read(context)).take(MAX_LINES)
            prefs(context).edit().putString(KEY_LINES, lines.joinToString("\n")).commit()
        }
    }

    fun read(context: Context): List<String> =
        prefs(context).getString(KEY_LINES, null)?.lines()?.filter { it.isNotBlank() }.orEmpty()

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
