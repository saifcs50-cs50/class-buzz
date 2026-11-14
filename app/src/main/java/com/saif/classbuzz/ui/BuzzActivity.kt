package com.saif.classbuzz.ui

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.TextView
import com.saif.classbuzz.ClassBuzz
import com.saif.classbuzz.alarm.BuzzAlert
import com.saif.classbuzz.calendar.ClassEvent
import com.saif.classbuzz.log.EventLog
import java.time.Instant
import java.time.ZoneId

/** The full-screen buzz: the countdown ring for the class, ticking until it starts. Tap to close. */
class BuzzActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var ringScreen: RingScreen? = null
    private var event: ClassEvent? = null

    private val tick = object : Runnable {
        override fun run() {
            val screen = ringScreen ?: return
            val shown = event ?: return
            screen.bind(shown, Instant.now(), ZoneId.systemDefault())
            handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventLog.add(this, "alert screen shown ${intent.getStringExtra(BuzzAlert.EXTRA_KEY)}")
        val uid = intent.getStringExtra(BuzzAlert.EXTRA_UID)
        event = uid?.let { id -> ClassBuzz.store(this).loadEvents().find { it.uid == id } }
        val content = if (event != null) {
            RingScreen(this).also { ringScreen = it }.view
        } else {
            plainText()
        }
        content.setOnClickListener { finish() }
        setContentView(content)
    }

    override fun onResume() {
        super.onResume()
        tick.run()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
    }

    /** For test buzzes with no class attached. */
    private fun plainText() = TextView(this).apply {
        val headline = intent.getStringExtra(BuzzAlert.EXTRA_HEADLINE).orEmpty()
        val detail = intent.getStringExtra(BuzzAlert.EXTRA_DETAIL).orEmpty()
        text = "$headline\n$detail"
        textSize = 18f
        gravity = Gravity.CENTER
    }

    private companion object {
        const val TICK_MS = 15_000L
    }
}
