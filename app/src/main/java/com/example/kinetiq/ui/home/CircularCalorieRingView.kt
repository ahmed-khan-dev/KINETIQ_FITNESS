package com.example.kinetiq.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

class CircularCalorieRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var consumedCalories: Int = 0
    private var targetCalories: Int = 2000

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#282A2E")
        strokeWidth = 20f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 20f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val consumedTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E2E2E8")
        textSize = 44f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private val targetTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#BACBB9")
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }

    private val unitTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00DAF3")
        textSize = 18f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private val badgeBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#282A2E")
        style = Paint.Style.FILL
    }

    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00DAF3")
        textSize = 20f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private val arcBounds = RectF()
    private val badgeBounds = RectF()

    fun setCalories(consumed: Int, target: Int) {
        this.consumedCalories = consumed.coerceAtLeast(0)
        this.targetCalories = target.coerceAtLeast(1)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val size = Math.min(w, h - 50f)
        val stroke = 20f
        val radius = (size - stroke) / 2f
        val cx = w / 2f
        val cy = size / 2f + 10f

        arcBounds.set(cx - radius, cy - radius, cx + radius, cy + radius)

        // Draw background ring track
        canvas.drawArc(arcBounds, 0f, 360f, false, trackPaint)

        // Draw active gradient progress arc
        val ratio = (consumedCalories.toFloat() / targetCalories.toFloat()).coerceIn(0f, 1f)
        val sweepAngle = ratio * 360f

        progressPaint.shader = LinearGradient(
            arcBounds.left, arcBounds.top, arcBounds.right, arcBounds.bottom,
            Color.parseColor("#00DAF3"), Color.parseColor("#75FF9E"),
            Shader.TileMode.CLAMP
        )

        canvas.drawArc(arcBounds, -90f, sweepAngle, false, progressPaint)

        // Draw Inner Texts
        val consumedStr = if (consumedCalories > 0) String.format("%,d", consumedCalories) else "0"
        val targetStr = "/ ${String.format("%,d", targetCalories)}"

        canvas.drawText(consumedStr, cx, cy - 8f, consumedTextPaint)
        canvas.drawText(targetStr, cx, cy + 22f, targetTextPaint)
        canvas.drawText("KCAL", cx, cy + 45f, unitTextPaint)

        // Draw Remaining Calorie Badge below ring
        val remaining = targetCalories - consumedCalories
        val badgeText = if (remaining >= 0) "$remaining KCAL LEFT" else "${Math.abs(remaining)} KCAL OVER"

        val badgeWidth = 200f
        val badgeHeight = 40f
        val badgeTop = size + 10f

        badgeBounds.set(cx - badgeWidth / 2f, badgeTop, cx + badgeWidth / 2f, badgeTop + badgeHeight)
        canvas.drawRoundRect(badgeBounds, 20f, 20f, badgeBackgroundPaint)

        badgeTextPaint.color = if (remaining >= 0) Color.parseColor("#00DAF3") else Color.parseColor("#FFB6B1")
        canvas.drawText(badgeText, cx, badgeTop + 27f, badgeTextPaint)
    }
}