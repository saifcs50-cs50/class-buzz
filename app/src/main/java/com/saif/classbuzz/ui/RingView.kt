package com.saif.classbuzz.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

/** The countdown ring: a dark track with an arc from 12 o'clock that empties as the class gets closer. */
class RingView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val stroke = STROKE_DP * density
    private val margin = MARGIN_DP * density
    private val bounds = RectF()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        color = TRACK_COLOR
    }
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
    }

    var fraction: Float = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    var color: Int = TRACK_COLOR
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val inset = margin + stroke / 2
        val size = minOf(width, height).toFloat()
        val left = (width - size) / 2 + inset
        val top = (height - size) / 2 + inset
        bounds.set(left, top, left + size - 2 * inset, top + size - 2 * inset)
        canvas.drawOval(bounds, trackPaint)
        if (fraction > 0f) {
            arcPaint.color = color
            canvas.drawArc(bounds, START_ANGLE, 360f * fraction, false, arcPaint)
        }
    }

    private companion object {
        const val STROKE_DP = 8f
        const val MARGIN_DP = 3f
        const val START_ANGLE = -90f
        const val TRACK_COLOR = 0xFF1F2937.toInt()
    }
}
