package io.github.kaulith.helpdeskanalytics.data.report

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportResult
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** MIME type constants for the supported export formats. */
object ExportMime {
    const val CSV = "text/csv"
    const val PDF = "application/pdf"
}

/**
 * Renders a [ReportResult] to a file in the app cache and hands it to the
 * Android share sheet. CSV is a plain text writer; PDF is drawn with the
 * platform [PdfDocument], with no third-party dependency.
 */
object ReportExporter {

    fun csvText(result: ReportResult): String {
        val sb = StringBuilder()
        sb.append(result.headers.joinToString(",") { csvEscape(it) }).append("\r\n")
        result.groups.forEach { group ->
            if (group.label != null) sb.append(csvEscape(group.label)).append("\r\n")
            group.rows.forEach { row ->
                sb.append(row.joinToString(",") { csvEscape(it) }).append("\r\n")
            }
        }
        return sb.toString()
    }

    fun exportCsv(context: Context, result: ReportResult, reportName: String): File =
        outFile(context, reportName, "csv").also { it.writeText(csvText(result), Charsets.UTF_8) }

    fun exportPdf(context: Context, result: ReportResult, reportName: String): File =
        outFile(context, reportName, "pdf").also { file ->
            FileOutputStream(file).use { writePdf(result, reportName, it) }
        }

    fun writePdf(result: ReportResult, reportName: String, out: OutputStream) {
        val cols = result.headers
        val landscape = cols.size > 6
        val pageW = if (landscape) 842 else 595
        val pageH = if (landscape) 595 else 842
        val margin = 36f
        val tableW = pageW - 2 * margin
        val colW = if (cols.isEmpty()) tableW else tableW / cols.size
        val tableRight = margin + colW * cols.size

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 15f; isFakeBoldText = true; color = Color.rgb(31, 33, 38)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f; color = Color.rgb(122, 124, 130)
        }
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 8.5f; isFakeBoldText = true; color = Color.WHITE
        }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 8.5f; color = Color.rgb(55, 58, 64)
        }
        val groupPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f; isFakeBoldText = true; color = Color.rgb(31, 33, 38)
        }
        val headerBg = Paint().apply { color = Color.rgb(31, 33, 38) }
        val groupBg = Paint().apply { color = Color.rgb(241, 241, 242) }
        val gridPaint = Paint().apply { color = Color.rgb(224, 225, 226); strokeWidth = 0.5f }

        val headerH = 24f
        val rowH = 22f
        val doc = PdfDocument()

        var pageNum = 0
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y = 0f

        fun startPage() {
            page?.let { doc.finishPage(it) }
            pageNum++
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
            canvas = page!!.canvas
            y = margin
        }

        fun drawHeaderRow() {
            val cv = canvas!!
            cv.drawRect(margin, y, tableRight, y + headerH, headerBg)
            cols.forEachIndexed { i, col ->
                val x = margin + i * colW
                cv.drawText(fit(col, headerTextPaint, colW - 8f), x + 4f, y + headerH * 0.66f, headerTextPaint)
            }
            y += headerH
        }

        fun ensureSpace(needed: Float) {
            if (y + needed > pageH - margin) {
                startPage()
                drawHeaderRow()
            }
        }

        startPage()
        canvas!!.drawText(reportName, margin, y + 12f, titlePaint)
        y += 22f
        canvas!!.drawText("${result.totalRows} rows, generated ${timestamp()}", margin, y, subtitlePaint)
        y += 14f
        drawHeaderRow()

        result.groups.forEach { group ->
            if (group.label != null) {
                ensureSpace(rowH * 2)
                val cv = canvas!!
                cv.drawRect(margin, y, tableRight, y + rowH, groupBg)
                cv.drawText(
                    fit("${group.label}  (${group.rows.size})", groupPaint, colW * cols.size - 8f),
                    margin + 4f, y + rowH * 0.68f, groupPaint
                )
                y += rowH
            }
            group.rows.forEach { row ->
                ensureSpace(rowH)
                val cv = canvas!!
                row.forEachIndexed { i, cell ->
                    if (i < cols.size) {
                        val x = margin + i * colW
                        cv.drawText(fit(cell, cellPaint, colW - 8f), x + 4f, y + rowH * 0.68f, cellPaint)
                    }
                }
                cv.drawLine(margin, y + rowH, tableRight, y + rowH, gridPaint)
                y += rowH
            }
        }

        if (result.totalRows == 0) {
            ensureSpace(rowH)
            canvas!!.drawText("No rows match the report filters.", margin + 4f, y + rowH * 0.68f, cellPaint)
        }

        page?.let { doc.finishPage(it) }
        doc.writeTo(out)
        doc.close()
    }

    fun writeToDocument(
        context: Context,
        uri: Uri,
        result: ReportResult,
        reportName: String,
        asPdf: Boolean
    ) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            if (asPdf) writePdf(result, reportName, out) else out.write(csvText(result).toByteArray())
        }
    }

    fun share(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Share report").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    private fun outFile(context: Context, reportName: String, ext: String): File {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val safe = reportName.trim().ifEmpty { "report" }
            .replace(Regex("[^A-Za-z0-9 _-]"), "")
            .replace(' ', '_')
            .take(48)
            .ifEmpty { "report" }
        return File(dir, "$safe.$ext")
    }

    private fun fit(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var t = text
        while (t.isNotEmpty() && paint.measureText("$t...") > maxWidth) t = t.dropLast(1)
        return if (t.isEmpty()) "..." else "$t..."
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

    private fun csvEscape(s: String): String {
        // Ticket text is customer-authored, and a spreadsheet reads a leading =, +, @ or
        // control character as a formula; the apostrophe forces the cell to stay text.
        val cell = if (s.firstOrNull() in FORMULA_LEAD_CHARS) "'$s" else s
        return if (cell.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + cell.replace("\"", "\"\"") + "\""
        } else {
            cell
        }
    }

    private val FORMULA_LEAD_CHARS = setOf('=', '+', '@', '\t', '\r')
}
