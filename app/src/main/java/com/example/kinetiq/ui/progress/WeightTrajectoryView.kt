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
import com.example.kinetiq.data.local.entity.WeightLogEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeightTrajectoryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var weightLogs: List<WeightLogEntity> = emptyList()

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
        color = Color.parseColor("#00E676")
        textSize = 28f
        isFakeBoldText = true
    }

    fun setWeightLogs(logs: List<WeightLogEntity>) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val aggregated = logs
            .groupBy { dateFormat.format(Date(it.loggedAt)) }
            .mapNotNull { entry -> entry.value.maxByOrNull { it.loggedAt } }
            .sortedBy { it.loggedAt }

        this.weightLogs = aggregated
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

        if (weightLogs.isEmpty()) {
            drawDemoCurve(canvas, paddingLeft, paddingTop, graphW, graphH)
            return
        }

        val minWeight = weightLogs.minOf { it.weightKg } - 1.0
        val maxWeight = weightLogs.maxOf { it.weightKg } + 1.0
        val weightRange = (maxWeight - minWeight).coerceAtLeast(1.0)

        val points = mutableListOf<Pair<Float, Float>>()
        val count = weightLogs.size

        weightLogs.forEachIndexed { index, log ->
            val x = paddingLeft + (if (count == 1) graphW / 2f else index.toFloat() / (count - 1) * graphW)
            val yFactor = (log.weightKg - minWeight) / weightRange
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
            Color.parseColor("#3300E676"), Color.parseColor("#0000E676"),
            Shader.TileMode.CLAMP
        )

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)

        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        points.forEachIndexed { index, pt ->
            canvas.drawCircle(pt.first, pt.second, 10f, linePaint)
            canvas.drawCircle(pt.first, pt.second, 5f, dotInnerPaint)

            if (count <= 5 || index == 0 || index == count - 1 || index % (count / 4) == 0) {
                val label = if (index == count - 1) "Today" else dateFormat.format(Date(weightLogs[index].loggedAt))
                textPaint.color = Color.parseColor("#00E676")
                textPaint.textAlign = when (index) {
                    0 -> Paint.Align.LEFT
                    count - 1 -> Paint.Align.RIGHT
                    else -> Paint.Align.CENTER
                }
                canvas.drawText(label, pt.first, paddingTop + graphH + 45f, textPaint)
            }
        }
    }

    private fun drawDemoCurve(canvas: Canvas, px: Float, py: Float, gw: Float, gh: Float) {
        val demoPoints = listOf(
            px to py + gh * 0.2f,
            px + gw * 0.33f to py + gh * 0.45f,
            px + gw * 0.66f to py + gh * 0.65f,
            px + gw to py + gh * 0.85f
        )
        val demoLabels = listOf("Sep 10", "Sep 17", "Sep 24", "Today")

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
            Color.parseColor("#3300E676"), Color.parseColor("#0000E676"),
            Shader.TileMode.CLAMP
        )

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)

        demoPoints.forEachIndexed { idx, pt ->
            canvas.drawCircle(pt.first, pt.second, 10f, linePaint)
            canvas.drawCircle(pt.first, pt.second, 5f, dotInnerPaint)

            textPaint.color = Color.parseColor("#00E676")
            textPaint.textAlign = when (idx) {
                0 -> Paint.Align.LEFT
                demoPoints.size - 1 -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }
            canvas.drawText(demoLabels[idx], pt.first, py + gh + 45f, textPaint)
        }
    }
}