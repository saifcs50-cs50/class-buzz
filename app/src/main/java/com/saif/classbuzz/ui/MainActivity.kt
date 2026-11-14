package com.saif.classbuzz.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.saif.classbuzz.ClassBuzz
import com.saif.classbuzz.alarm.TestBuzz
import com.saif.classbuzz.calendar.ClassEvent
import com.saif.classbuzz.log.EventLog
import com.saif.classbuzz.sync.CalendarLink
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread

/** Next class as the countdown ring, the rest of the day below it, and when the calendar last updated. */
class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var ring: RingScreen
    private lateinit var hero: LinearLayout
    private lateinit var rows: LinearLayout
    private lateinit var updated: TextView
    private val density by lazy { resources.displayMetrics.density }

    private val tick = object : Runnable {
        override fun run() {
            bind()
            handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ring = RingScreen(this)
        val screen = resources.displayMetrics.heightPixels
        hero = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(MATCH, screen)
        }
        rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        updated = TextView(this).apply {
            textSize = 11f
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, 0)
        }
        val below = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(44), dp(8), dp(44), dp(72))
            addView(rows)
            addView(updated)
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(hero)
            addView(below)
        }
        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(column)
        })
        if (savedInstanceState == null) runAdbCommand(intent)
    }

    override fun onResume() {
        super.onResume()
        // Re-arms the hourly chain in case something broke it.
        ClassBuzz.scheduleNextRefresh(this)
        tick.run()
        refreshInBackground()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        runAdbCommand(intent)
    }

    private fun bind() {
        val store = ClassBuzz.store(this)
        val events = store.loadEvents()
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val next = events.firstOrNull { it.end.isAfter(now) }

        hero.removeAllViews()
        if (next == null) {
            hero.addView(TextView(this).apply {
                text = "No classes coming up"
                textSize = 16f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
            })
        } else {
            ring.bind(next, now, zone)
            hero.addView(ring.view, LinearLayout.LayoutParams(MATCH, MATCH))
        }

        rows.removeAllViews()
        EventText.laterList(events, now, zone).forEach { rows.addView(row(it, zone)) }

        val lastSuccess = store.lastSuccess()
        updated.text = EventText.updatedLine(lastSuccess, now)
        updated.setTextColor(if (EventText.isStale(lastSuccess, now)) STALE_RED else GREY)
    }

    /** `13:30 ● PHY · Kinematics…` on one line. */
    private fun row(event: ClassEvent, zone: ZoneId) = TextView(this).apply {
        val info = event.info
        val label = listOfNotNull(info.subject?.short ?: "TEST".takeIf { info.isTest }, EventText.topic(info))
            .joinToString(" · ")
        text = SpannableStringBuilder().apply {
            append(ROW_TIME.format(event.start.atZone(zone))).append("  ")
            val dotStart = length
            append("●")
            setSpan(ForegroundColorSpan(EventText.color(info)), dotStart, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            append(" ").append(label)
        }
        textSize = 13f
        setTextColor(Color.WHITE)
        setSingleLine()
        ellipsize = TextUtils.TruncateAt.END
        setPadding(0, dp(4), 0, dp(4))
    }

    /** Commands sent from the Mac with `am start ... --es <name> <value>`. */
    private fun runAdbCommand(intent: Intent) {
        intent.getStringExtra("calendar_url")?.let(::setLink)
        when (intent.getStringExtra("cmd")) {
            "testbuzz" -> {
                val next = ClassBuzz.store(this).loadEvents().firstOrNull { it.end.isAfter(Instant.now()) }
                TestBuzz.schedule(this, intent.getIntExtra("seconds", 60), next?.uid)
            }
            "refresh" -> refreshInBackground(force = true)
        }
    }

    private fun setLink(url: String) {
        if (!CalendarLink.isValid(url)) {
            EventLog.add(this, "link rejected ${CalendarLink.masked(url)}")
            return
        }
        ClassBuzz.store(this).setCalendarUrl(url.trim())
        EventLog.add(this, "link set ${CalendarLink.masked(url)}")
        refreshInBackground(force = true)
    }

    /** [force] for the Mac's commands; opening the app skips the download if one just happened. */
    private fun refreshInBackground(force: Boolean = false) {
        thread(name = "class-buzz-open") {
            runCatching { if (force) ClassBuzz.runRefresh(this) else ClassBuzz.runAppOpenRefresh(this) }
                .onFailure { EventLog.add(this, "refresh crashed: $it") }
            runOnUiThread { if (!isFinishing) bind() }
        }
    }

    private fun dp(value: Int) = (value * density).toInt()

    private companion object {
        const val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
        const val TICK_MS = 15_000L
        const val GREY = 0xFF9CA3AF.toInt()
        const val STALE_RED = 0xFFF87171.toInt()
        val ROW_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")
    }
}
