package com.example.kinetiq.ui.progress

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.example.kinetiq.data.local.entity.ExerciseLogEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StrengthTrajectoryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var exerciseLogs: List<ExerciseLogEntity> = emptyList()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E676")
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val dotInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F1115")
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00DAF3")
        textSize = 28f
        isFakeBoldText = true
    }

    fun setExerciseLogs(logs: List<ExerciseLogEntity>) {
        this.exerciseLogs = logs.sortedBy { it.completedAt }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val paddingLeft = 40f
        val paddingRight = 40f
        val paddingTop = 40f
        val paddingBottom = 60f

        val graphW = w - paddingLeft - paddingRight
        val graphH = h - paddingTop - paddingBottom

        if (exerciseLogs.isEmpty()) {
            drawDemoStrengthCurve(canvas, paddingLeft, paddingTop, graphW, graphH)
            return
        }

        val points1RM = exerciseLogs.map { log ->
            val reps = log.loggedSets.substringBefore('x').trim().substringAfter('x').trim().toIntOrNull()
                ?: log.loggedSets.substringAfter('x').trim().substringBefore('@').trim().toIntOrNull() ?: 10
            val weightStr = if (log.loggedSets.contains("@")) log.loggedSets.substringAfter("@").removeSuffix("kg").trim() else "0"
            val weight = weightStr.toDoubleOrNull() ?: 0.0

            val estimated1RM = if (weight > 0) weight * (1.0 + reps / 30.0) else reps.toDouble()
            log.completedAt to estimated1RM
        }

        val min1RM = points1RM.minOf { it.second } - 2.0
        val max1RM = points1RM.maxOf { it.second } + 2.0
        val range = (max1RM - min1RM).coerceAtLeast(1.0)

        val points = mutableListOf<Pair<Float, Float>>()
        val count = points1RM.size

        points1RM.forEachIndexed { index, pair ->
            val x = paddingLeft + (if (count == 1) graphW / 2f else index.toFloat() / (count - 1) * graphW)
            val yFactor = (pair.second - min1RM) / range
            val y = paddingTop + graphH - (yFactor * graphH).toFloat()
            points.add(x to y)
        }

        val path = Path()
        val fillPath = Path()

        path.moveTo(points[0].first, points[0].second)
        fillPath.moveTo(points[0].first, paddingTop + graphH)
        fillPath.lineTo(points[0].first, points[0].second)

        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val cx = (p1.first + p2.first) / 2f
            path.cubicTo(cx, p1.second, cx, p2.second, p2.first, p2.second)
            fillPath.cubicTo(cx, p1.second, cx, p2.second, p2.first, p2.second)
        }

        fillPath.lineTo(points.last().first, paddingTop + graphH)
        fillPath.close()

        fillPaint.shader = LinearGradient(
            0f, paddingTop, 0f, paddingTop + graphH,
            Color.parseColor("#3300DAF3"), Color.parseColor("#0000DAF3"),
            Shader.TileMode.CLAMP
        )

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)

        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        points.forEachIndexed { index, pt ->
            canvas.drawCircle(pt.first, pt.second, 10f, linePaint)
            canvas.drawCircle(pt.first, pt.second, 5f, dotInnerPaint)

            if (count <= 5 || index == 0 || index == count - 1 || index % (count / 4) == 0) {
                val label = if (index == count - 1) "Today" else dateFormat.format(Date(points1RM[index].first))
                textPaint.color = Color.parseColor("#00DAF3")
                textPaint.textAlign = when (index) {
                    0 -> Paint.Align.LEFT
                    count - 1 -> Paint.Align.RIGHT
                    else -> Paint.Align.CENTER
                }
                canvas.drawText(label, pt.first, paddingTop + graphH + 45f, textPaint)
            }
        }
    }

    private fun drawDemoStrengthCurve(canvas: Canvas, px: Float, py: Float, gw: Float, gh: Float) {
        val demoPoints = listOf(
            px to py + gh * 0.75f,
            px + gw * 0.33f to py + gh * 0.55f,
            px + gw * 0.66f to py + gh * 0.35f,
            px + gw to py + gh * 0.15f
        )
        val demoLabels = listOf("Wk 1", "Wk 2", "Wk 3", "Wk 4")

        val path = Path()
        val fillPath = Path()

        path.moveTo(demoPoints[0].first, demoPoints[0].second)
        fillPath.moveTo(demoPoints[0].first, py + gh)
        fillPath.lineTo(demoPoints[0].first, demoPoints[0].second)

        for (i in 0 until demoPoints.size - 1) {
            val p1 = demoPoints[i]
            val p2 = demoPoints[i + 1]
            val cx = (p1.first + p2.first) / 2f
            path.cubicTo(cx, p1.second, cx, p2.second, p2.first, p2.second)
            fillPath.cubicTo(cx, p1.second, cx, p2.second, p2.first, p2.second)
        }

        fillPath.lineTo(demoPoints.last().first, py + gh)
        fillPath.close()

        fillPaint.shader = LinearGradient(
            0f, py, 0f, py + gh,
            Color.parseColor("#3300DAF3"), Color.parseColor("#0000DAF3"),
            Shader.TileMode.CLAMP
        )

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)

        demoPoints.forEachIndexed { idx, pt ->
            canvas.drawCircle(pt.first, pt.second, 10f, linePaint)
            canvas.drawCircle(pt.first, pt.second, 5f, dotInnerPaint)

            textPaint.color = Color.parseColor("#00DAF3")
            textPaint.textAlign = when (idx) {
                0 -> Paint.Align.LEFT
                demoPoints.size - 1 -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }
            canvas.drawText(demoLabels[idx], pt.first, py + gh + 45f, textPaint)
        }
    }
}