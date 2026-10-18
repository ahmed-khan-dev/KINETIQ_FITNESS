package com.example.kinetiq.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.kinetiq.ui.workout.WorkoutDayUi
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates a multi-page, A4 weekly summary PDF and opens the share sheet.
 * Public API is unchanged: generateAndShareWeeklyPdf(context, userName, splitTitle, weeklyPlan)
 */
object WeeklyPdfExporter {

    fun generateAndShareWeeklyPdf(
        context: Context,
        userName: String,
        splitTitle: String,
        weeklyPlan: List<WorkoutDayUi>
    ) {
        val dateStr = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())
        val fileStamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())

        // Pass 1 (no drawing): count pages so every footer can say "Page X of Y".
        val totalPages = ReportBuilder(null, 0, userName, splitTitle, dateStr, weeklyPlan).build()

        val document = PdfDocument()
        try {
            // Pass 2: real render.
            ReportBuilder(document, totalPages, userName, splitTitle, dateStr, weeklyPlan).build()

            val pdfDir = File(context.filesDir, "pdfs").apply { if (!exists()) mkdirs() }
            val pdfFile = File(pdfDir, "Kinetiq_Weekly_Report_$fileStamp.pdf")
            pdfFile.outputStream().use { out -> document.writeTo(out) }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Kinetiq Weekly Summary — $dateStr")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share Kinetiq Weekly PDF Report")
            if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Could not generate PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        } finally {
            document.close()
        }
    }
}

/* ------------------------------------------------------------------------------------------ */
/*  Layout constants & palette                                                                */
/* ------------------------------------------------------------------------------------------ */

private const val PAGE_W = 595f
private const val PAGE_H = 842f
private const val MARGIN = 36f
private const val CONTENT_W = PAGE_W - 2 * MARGIN
private const val BOTTOM_LIMIT = PAGE_H - 52f

private const val DAY_HEADER_H = 30f
private const val COL_HEAD_H = 18f
private const val LINE_H = 13f
private const val ROW_PAD = 7f
private const val DAY_GAP = 16f

// Column geometry inside the exercise table (relative to MARGIN)
private const val COL_NAME_X = 12f
private const val COL_NAME_W = 190f
private const val COL_PLAN_X = 212f
private const val COL_LOAD_X = 284f
private const val COL_LOGGED_X = 350f
private const val COL_LOGGED_W = CONTENT_W - COL_LOGGED_X - 10f

private val C_INK = Color.parseColor("#0F1115")
private val C_BRAND = Color.parseColor("#00E676")
private val C_MUTED = Color.parseColor("#6B7280")
private val C_SOFT = Color.parseColor("#9CA3AF")
private val C_LINE = Color.parseColor("#E5E7EB")
private val C_CARD = Color.parseColor("#F3F5F7")
private val C_ZEBRA = Color.parseColor("#FAFAFB")
private val C_BLUE = Color.parseColor("#38BDF8")
private val C_ORANGE = Color.parseColor("#FB923C")
private val C_AMBER = Color.parseColor("#F59E0B")
private val C_INDIGO = Color.parseColor("#6366F1")

private data class RowData(
    val name: String,
    val plan: String,
    val load: String,
    val logged: String
)

private fun textPaint(
    size: Float,
    color: Int,
    bold: Boolean = false,
    spacing: Float = 0f,
    align: Paint.Align = Paint.Align.LEFT
): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    textSize = size
    this.color = color
    typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    letterSpacing = spacing
    textAlign = align
}

private fun fillPaint(color: Int): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.FILL
    this.color = color
}

private fun ellipsize(text: String, paint: TextPaint, width: Float): String =
    TextUtils.ellipsize(text, paint, width.coerceAtLeast(1f), TextUtils.TruncateAt.END).toString()

private fun wrap(text: String, paint: TextPaint, width: Float, maxLines: Int): List<String> {
    val words = text.replace("\n", " ").split(" ").filter { it.isNotEmpty() }
    if (words.isEmpty()) return listOf("")

    val lines = mutableListOf<String>()
    var current = ""
    for (original in words) {
        var word = original
        // Break words that are wider than the column.
        while (paint.measureText(word) > width) {
            val n = paint.breakText(word, true, width, null)
            if (n <= 0) break
            if (current.isNotEmpty()) {
                lines.add(current)
                current = ""
            }
            lines.add(word.substring(0, n))
            word = word.substring(n)
        }
        val candidate = if (current.isEmpty()) word else "$current $word"
        if (paint.measureText(candidate) <= width) {
            current = candidate
        } else {
            if (current.isNotEmpty()) lines.add(current)
            current = word
        }
    }
    if (current.isNotEmpty()) lines.add(current)

    if (lines.size > maxLines) {
        val kept = lines.take(maxLines).toMutableList()
        kept[maxLines - 1] = ellipsize(kept[maxLines - 1] + "…", paint, width)
        return kept
    }
    return lines
}

/* ------------------------------------------------------------------------------------------ */
/*  Report builder                                                                            */
/* ------------------------------------------------------------------------------------------ */

private class ReportBuilder(
    private val doc: PdfDocument?,          // null = measuring pass (nothing is drawn)
    private val totalPages: Int,
    private val userName: String,
    private val splitTitle: String,
    private val dateStr: String,
    private val plan: List<WorkoutDayUi>
) {
    private var pageCount = 0
    private var page: PdfDocument.Page? = null
    private var canvas: Canvas? = null
    private var y = 0f

    // Summary numbers (same definitions as the original exporter)
    private val completedSessions = plan.count { it.isComplete }
    private val totalSessions = plan.size
    private val totalSets = plan.sumOf { it.totalSets }
    private val completedSets = plan.sumOf { it.completedSets }
    private val completionFraction: Float =
        if (totalSets.toFloat() > 0f) (completedSets.toFloat() / totalSets.toFloat()).coerceIn(0f, 1f) else 0f
    private val completionPct = Math.round(completionFraction * 100f)

    // Paints
    private val fInk = fillPaint(C_INK)
    private val fBrand = fillPaint(C_BRAND)
    private val fCard = fillPaint(C_CARD)
    private val fZebra = fillPaint(C_ZEBRA)
    private val fLine = fillPaint(C_LINE)
    private val fChip = fillPaint(Color.TRANSPARENT)
    private val fAccent = fillPaint(C_BRAND)

    private val pBrandLabel = textPaint(11f, C_BRAND, bold = true, spacing = 0.3f)
    private val pCoverTitle = textPaint(26f, Color.WHITE, bold = true)
    private val pCoverMeta = textPaint(11f, C_SOFT)
    private val pChip = textPaint(8.5f, C_BRAND, bold = true, spacing = 0.08f)
    private val pStatValue = textPaint(20f, C_INK, bold = true)
    private val pStatLabel = textPaint(8f, C_MUTED, bold = true, spacing = 0.12f)
    private val pSmallCaps = textPaint(8.5f, C_MUTED, bold = true, spacing = 0.12f)
    private val pPctRight = textPaint(9.5f, C_INK, bold = true, align = Paint.Align.RIGHT)
    private val pSection = textPaint(10.5f, C_INK, bold = true, spacing = 0.15f)
    private val pDayName = textPaint(12f, C_INK, bold = true)
    private val pDayTitle = textPaint(10.5f, C_MUTED)
    private val pBody = textPaint(10.5f, C_INK)
    private val pBodyBold = textPaint(10.5f, C_INK, bold = true)
    private val pLogged = textPaint(9.5f, C_MUTED)
    private val pFoot = textPaint(8.5f, C_SOFT)
    private val pFootRight = textPaint(8.5f, C_SOFT, align = Paint.Align.RIGHT)
    private val pRunning = textPaint(8.5f, C_MUTED, bold = true, spacing = 0.12f)

    /** Builds the whole report and returns the number of pages used. */
    fun build(): Int {
        newPage()

        if (plan.isEmpty()) {
            canvas?.drawText("No workout plan data available for this week.", MARGIN, y + 14f, pBody)
            closePage()
            return pageCount
        }

        sectionTitle("DAILY BREAKDOWN")
        plan.forEach { drawDay(it) }
        closePage()
        return pageCount
    }

    /* ---------------- pages ---------------- */

    private fun newPage() {
        closePage()
        pageCount++
        if (doc != null) {
            val info = PdfDocument.PageInfo.Builder(PAGE_W.toInt(), PAGE_H.toInt(), pageCount).create()
            val p = doc.startPage(info)
            page = p
            canvas = p.canvas
        }
        y = if (pageCount == 1) drawCover() else drawRunningHeader()
    }

    private fun closePage() {
        if (pageCount == 0) return
        if (page != null || doc == null) drawFooter()
        page?.let { doc?.finishPage(it) }
        page = null
        canvas = null
    }

    private fun ensure(height: Float) {
        if (y + height > BOTTOM_LIMIT) newPage()
    }

    private fun drawFooter() {
        val c = canvas ?: return
        c.drawRect(MARGIN, PAGE_H - 40f, PAGE_W - MARGIN, PAGE_H - 39f, fLine)
        c.drawText("Generated by Kinetiq Fitness & Diet Coach", MARGIN, PAGE_H - 24f, pFoot)
        c.drawText("Page $pageCount of $totalPages", PAGE_W - MARGIN, PAGE_H - 24f, pFootRight)
    }

    private fun drawRunningHeader(): Float {
        val c = canvas
        c?.drawText("KINETIQ  ·  WEEKLY SUMMARY", MARGIN, 32f, pRunning)
        c?.drawRect(MARGIN, 40f, PAGE_W - MARGIN, 41f, fLine)
        return 60f
    }

    /* ---------------- cover / summary ---------------- */

    private fun drawCover(): Float {
        val c = canvas
        val bandH = 120f

        c?.drawRect(0f, 0f, PAGE_W, bandH, fInk)
        c?.drawRect(0f, bandH, PAGE_W, bandH + 4f, fBrand)

        c?.drawText("KINETIQ", MARGIN, 40f, pBrandLabel)
        c?.drawText("Weekly Summary Report", MARGIN, 76f, pCoverTitle)
        c?.drawText(ellipsize("$userName  •  $dateStr", pCoverMeta, CONTENT_W), MARGIN, 100f, pCoverMeta)

        // Program split chip (top-right)
        val splitText = ellipsize(splitTitle.uppercase(Locale.getDefault()), pChip, 220f)
        drawChip(PAGE_W - MARGIN, 34f, splitText, Color.parseColor("#1F2933"), C_BRAND)

        var top = bandH + 4f + 22f

        // Stat cards
        val gap = 10f
        val cw = (CONTENT_W - 2 * gap) / 3f
        val ch = 64f
        val stats = listOf(
            Triple("$completedSessions/$totalSessions", "SESSIONS DONE", C_BRAND),
            Triple("$completedSets/$totalSets", "SETS COMPLETED", C_BLUE),
            Triple("$completionPct%", "WEEK COMPLETION", C_ORANGE)
        )
        stats.forEachIndexed { i, (value, label, accent) ->
            val x = MARGIN + i * (cw + gap)
            c?.drawRoundRect(RectF(x, top, x + cw, top + ch), 10f, 10f, fCard)
            fAccent.color = accent
            c?.drawRoundRect(RectF(x + 12f, top + 12f, x + 15f, top + ch - 12f), 1.5f, 1.5f, fAccent)
            c?.drawText(ellipsize(value, pStatValue, cw - 34f), x + 24f, top + 35f, pStatValue)
            c?.drawText(label, x + 24f, top + 50f, pStatLabel)
        }
        top += ch + 20f

        // Overall progress bar
        c?.drawText("OVERALL COMPLETION", MARGIN, top + 8f, pSmallCaps)
        c?.drawText("$completionPct%", PAGE_W - MARGIN, top + 9f, pPctRight)
        val barTop = top + 16f
        c?.drawRoundRect(RectF(MARGIN, barTop, MARGIN + CONTENT_W, barTop + 8f), 4f, 4f, fLine)
        if (completionFraction > 0f) {
            val fillW = (CONTENT_W * completionFraction).coerceAtLeast(8f)
            c?.drawRoundRect(RectF(MARGIN, barTop, MARGIN + fillW, barTop + 8f), 4f, 4f, fBrand)
        }
        return barTop + 8f + 28f
    }

    private fun sectionTitle(text: String) {
        ensure(40f)
        val c = canvas
        c?.drawRoundRect(RectF(MARGIN, y, MARGIN + 3f, y + 12f), 1.5f, 1.5f, fBrand)
        c?.drawText(text, MARGIN + 10f, y + 10f, pSection)
        y += 28f
    }

    /* ---------------- day blocks ---------------- */

    private fun drawDay(day: WorkoutDayUi) {
        val rows = day.exercises.map { ex ->
            val w = ex.loggedWeightKg
            RowData(
                name = ex.name,
                plan = "${ex.sets} × ${ex.reps}",
                load = if (w > 0.0) "${formatKg(w)} kg" else "—",
                logged = ex.loggedSetsText
            )
        }
        val isRest = rows.isEmpty()

        val firstRowH = if (isRest) 26f else rowHeight(rows.first())
        ensure(DAY_HEADER_H + (if (isRest) 0f else COL_HEAD_H) + firstRowH + 6f)

        drawDayHeader(day, isRest, continued = false)

        if (isRest) {
            canvas?.drawText("Rest / Active Recovery", MARGIN + 14f, y + 17f, pDayTitle)
            y += 26f + DAY_GAP
            return
        }

        drawColumnHeader()
        rows.forEachIndexed { index, row ->
            val h = rowHeight(row)
            if (y + h > BOTTOM_LIMIT) {
                newPage()
                drawDayHeader(day, isRest, continued = true)
                drawColumnHeader()
            }
            drawRow(row, index, h)
            y += h
        }
        y += DAY_GAP
    }

    private fun drawDayHeader(day: WorkoutDayUi, isRest: Boolean, continued: Boolean) {
        val c = canvas

        val (statusText, chipBg, chipFg, accent) = when {
            isRest -> StatusStyle("REST", Color.parseColor("#E0E7FF"), Color.parseColor("#4F46E5"), C_INDIGO)
            day.isComplete -> StatusStyle("COMPLETE", Color.parseColor("#DCFCE7"), Color.parseColor("#15803D"), C_BRAND)
            day.completedSets > 0 -> StatusStyle(
                "${day.completedSets}/${day.totalSets} SETS",
                Color.parseColor("#FEF3C7"), Color.parseColor("#B45309"), C_AMBER
            )
            else -> StatusStyle("PENDING", Color.parseColor("#E5E7EB"), Color.parseColor("#4B5563"), C_SOFT)
        }

        c?.drawRoundRect(RectF(MARGIN, y, MARGIN + CONTENT_W, y + DAY_HEADER_H), 8f, 8f, fCard)
        fAccent.color = accent
        c?.drawRoundRect(RectF(MARGIN, y + 5f, MARGIN + 4f, y + DAY_HEADER_H - 5f), 2f, 2f, fAccent)

        val chipW = drawChip(MARGIN + CONTENT_W - 10f, y + DAY_HEADER_H / 2f, statusText, chipBg, chipFg)

        val nameX = MARGIN + 16f
        val dayName = day.dayName.toString()
        val nameW = pDayName.measureText(dayName)
        c?.drawText(ellipsize(dayName, pDayName, 120f), nameX, y + 19f, pDayName)

        val titleX = nameX + minOf(nameW, 120f) + 10f
        val titleAvail = MARGIN + CONTENT_W - 10f - chipW - 10f - titleX
        val title = day.title.toString() + if (continued) "  (continued)" else ""
        c?.drawText(ellipsize(title, pDayTitle, titleAvail), titleX, y + 19f, pDayTitle)

        y += DAY_HEADER_H + 4f
    }

    private fun drawColumnHeader() {
        val c = canvas
        c?.drawText("EXERCISE", MARGIN + COL_NAME_X, y + 12f, pSmallCaps)
        c?.drawText("PLAN", MARGIN + COL_PLAN_X, y + 12f, pSmallCaps)
        c?.drawText("LOAD", MARGIN + COL_LOAD_X, y + 12f, pSmallCaps)
        c?.drawText("LOGGED SETS", MARGIN + COL_LOGGED_X, y + 12f, pSmallCaps)
        c?.drawRect(MARGIN, y + COL_HEAD_H - 1f, MARGIN + CONTENT_W, y + COL_HEAD_H, fLine)
        y += COL_HEAD_H
    }

    private fun nameLines(row: RowData) = wrap(row.name, pBodyBold, COL_NAME_W, 3)

    private fun loggedLines(row: RowData) =
        wrap(row.logged.ifBlank { "—" }, pLogged, COL_LOGGED_W, 5)

    private fun rowHeight(row: RowData): Float {
        val lines = maxOf(nameLines(row).size, loggedLines(row).size)
        return maxOf(26f, lines * LINE_H + 2 * ROW_PAD)
    }

    private fun drawRow(row: RowData, index: Int, height: Float) {
        val c = canvas ?: return
        if (index % 2 == 1) c.drawRect(MARGIN, y, MARGIN + CONTENT_W, y + height, fZebra)

        val firstBaseline = y + ROW_PAD + 10f
        nameLines(row).forEachIndexed { i, line ->
            c.drawText(line, MARGIN + COL_NAME_X, firstBaseline + i * LINE_H, pBodyBold)
        }
        c.drawText(row.plan, MARGIN + COL_PLAN_X, firstBaseline, pBody)

        val loadPaint = if (row.load == "—") pLogged else pBody
        c.drawText(row.load, MARGIN + COL_LOAD_X, firstBaseline, loadPaint)

        loggedLines(row).forEachIndexed { i, line ->
            c.drawText(line, MARGIN + COL_LOGGED_X, firstBaseline + i * LINE_H, pLogged)
        }
        c.drawRect(MARGIN, y + height - 0.5f, MARGIN + CONTENT_W, y + height, fLine)
    }

    /* ---------------- helpers ---------------- */

    /** Draws a rounded chip whose right edge is at [rightX]; returns its width. */
    private fun drawChip(rightX: Float, centerY: Float, text: String, bg: Int, fg: Int): Float {
        pChip.color = fg
        val w = pChip.measureText(text) + 16f
        val h = 16f
        fChip.color = bg
        canvas?.drawRoundRect(RectF(rightX - w, centerY - h / 2f, rightX, centerY + h / 2f), 8f, 8f, fChip)
        canvas?.drawText(text, rightX - w + 8f, centerY + 3f, pChip)
        return w
    }

    private fun formatKg(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString()
        else String.format(Locale.getDefault(), "%.1f", value)

    private data class StatusStyle(val text: String, val chipBg: Int, val chipFg: Int, val accent: Int)
}