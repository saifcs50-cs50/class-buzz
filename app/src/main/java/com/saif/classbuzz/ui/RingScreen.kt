package com.saif.classbuzz.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.saif.classbuzz.Config
import com.saif.classbuzz.calendar.ClassEvent
import java.time.Instant
import java.time.ZoneId

/** The countdown screen: minutes left big in the middle, ring around the edge, topic and meta below. */
class RingScreen(context: Context) {
    private val density = context.resources.displayMetrics.density
    private val ring = RingView(context)
    private val minutes = text(context, 40f, bold = true)
    private val headline = text(context, 11f, color = GREY).apply { letterSpacing = 0.06f }
    private val topic = text(context, 14f, bold = true).apply {
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
    }
    private val meta = text(context, 12f, color = GREY).apply { setSingleLine() }

    val view: View = FrameLayout(context).apply {
        setBackgroundColor(Color.BLACK)
        addView(ring, FrameLayout.LayoutParams(MATCH, MATCH))
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val side = (SIDE_PADDING_DP * density).toInt()
            setPadding(side, 0, side, 0)
            addView(minutes)
            addView(headline, spaced(bottom = 8))
            addView(topic)
            addView(meta, spaced(top = 6))
        }
        addView(column, FrameLayout.LayoutParams(MATCH, MATCH, Gravity.CENTER))
    }

    fun bind(event: ClassEvent, now: Instant, zone: ZoneId) {
        val info = event.info
        val accent = EventText.color(info)
        val started = !now.isBefore(event.start)
        minutes.text = if (started) "NOW" else EventText.minutesLeft(event.start, now).toString()
        minutes.setTextColor(accent)
        headline.text = if (started) "CLASS STARTED" else EventText.headline(info)
        topic.text = EventText.topic(info)
        meta.text = EventText.meta(event, zone)
        ring.color = accent
        ring.fraction = EventText.ringFraction(event.start, now, Config.LEAD)
    }

    private fun spaced(top: Int = 0, bottom: Int = 0) = LinearLayout.LayoutParams(WRAP, WRAP).apply {
        topMargin = (top * density).toInt()
        bottomMargin = (bottom * density).toInt()
        gravity = Gravity.CENTER_HORIZONTAL
    }

    private fun text(context: Context, sizeSp: Float, bold: Boolean = false, color: Int = Color.WHITE) =
        TextView(context).apply {
            textSize = sizeSp
            gravity = Gravity.CENTER
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private companion object {
        const val MATCH = FrameLayout.LayoutParams.MATCH_PARENT
        const val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT
        const val SIDE_PADDING_DP = 40
        const val GREY = 0xFF9CA3AF.toInt()
    }
}
