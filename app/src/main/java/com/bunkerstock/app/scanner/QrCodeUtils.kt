package com.bunkerstock.app.scanner

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import androidx.print.PrintHelper
import com.bunkerstock.app.data.InventoryItem
import com.bunkerstock.app.domain.InventoryLogic
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.ceil

object QrCodeUtils {

    const val BATCH_COLS = 4
    const val BATCH_ROWS = 6
    const val BATCH_PER_PAGE = BATCH_COLS * BATCH_ROWS // 24 scannable ~30mm QRs per A4 sheet

    fun batchPageCount(itemCount: Int): Int =
        if (itemCount <= 0) 0 else ceil(itemCount.toDouble() / BATCH_PER_PAGE).toInt()

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? = runCatching {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val dark = Color.parseColor("#090D16")
        val light = Color.WHITE
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bmp.setPixel(x, y, if (matrix[x, y]) dark else light)
            }
        }
        bmp
    }.getOrNull()

    fun generatePrintableLabelBitmap(
        header: String,
        title: String,
        meta: String,
        qrContent: String
    ): Bitmap? {
        val qr = generateQrBitmap(qrContent, 420) ?: return null
        val width = 620
        val height = 700
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        val borderPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawRect(16f, 16f, width - 16f, height - 16f, borderPaint)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 22f
            typeface = Typeface.MONOSPACE
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(header, width / 2f, 68f, headerPaint)

        canvas.drawBitmap(qr, (width - qr.width) / 2f, 96f, null)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#090D16")
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val truncatedTitle = if (title.length > 34) title.take(33) + "…" else title
        canvas.drawText(truncatedTitle, width / 2f, 572f, titlePaint)

        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            textSize = 20f
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(meta, width / 2f, 624f, metaPaint)

        return bmp
    }

    fun printLabel(context: Context, jobName: String, bitmap: Bitmap) {
        val helper = PrintHelper(context).apply {
            scaleMode = PrintHelper.SCALE_MODE_FIT
        }
        helper.printBitmap(jobName, bitmap)
    }

    fun shareLabelPng(context: Context, fileName: String, bitmap: Bitmap): Boolean = runCatching {
        val dir = File(context.cacheDir, "qr_labels").apply { mkdirs() }
        val file = File(dir, "$fileName.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, fileName))
        true
    }.getOrDefault(false)

    /**
     * Generates a multi-page A4 (595x842 pt) PDF containing up to 24 optimally sized (~30mm)
     * scannable QR labels per page, flowing cleanly onto additional pages when needed.
     */
    fun writeBatchQrPdf(
        items: List<InventoryItem>,
        outputStream: OutputStream,
        headerBrand: String,
        notApplicableLabel: String,
        expiredDays: Int = 0,
        criticalDays: Int = 30,
        warningDays: Int = 90
    ): Boolean = runCatching {
        if (items.isEmpty()) return false

        val pageWidth = 595   // A4 width at 72 pt/in (210 mm)
        val pageHeight = 842  // A4 height at 72 pt/in (297 mm)
        val marginX = 20f
        val marginTop = 18f
        val headerHeight = 24f
        val marginBottom = 18f
        val gapX = 8f
        val gapY = 8f

        val cellW = (pageWidth - 2f * marginX - (BATCH_COLS - 1) * gapX) / BATCH_COLS
        val cellH = (pageHeight - marginTop - headerHeight - marginBottom - (BATCH_ROWS - 1) * gapY) / BATCH_ROWS
        val qrDrawSize = minOf(cellW - 18f, cellH - 36f).coerceAtLeast(64f)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            textSize = 9f
            typeface = Typeface.MONOSPACE
            isFakeBoldText = true
            textAlign = Paint.Align.LEFT
        }
        val pageNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 8.5f
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.RIGHT
        }
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 0.75f
        }
        val cutBorderPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            style = Paint.Style.STROKE
            strokeWidth = 0.65f
            pathEffect = DashPathEffect(floatArrayOf(4f, 3f), 0f)
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#090D16")
            textSize = 7.8f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 6.2f
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }
        val bmpPaint = Paint(Paint.FILTER_BITMAP_FLAG)

        fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
            if (paint.measureText(text) <= maxWidth) return text
            val count = paint.breakText(text, true, (maxWidth - paint.measureText("…")).coerceAtLeast(10f), null)
            return if (count <= 0) "…" else text.take(count).trimEnd() + "…"
        }

        val totalPages = batchPageCount(items.size)
        val doc = PdfDocument()
        try {
            for (pageIdx in 0 until totalPages) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIdx + 1).create()
                val page = doc.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)

                // Top header bar
                val headerY = marginTop + 11f
                canvas.drawText("$headerBrand · BATCH QR", marginX, headerY, headerPaint)
                canvas.drawText("${pageIdx + 1} / $totalPages", pageWidth - marginX, headerY, pageNumPaint)
                canvas.drawLine(marginX, marginTop + 17f, pageWidth - marginX, marginTop + 17f, dividerPaint)

                val startIdx = pageIdx * BATCH_PER_PAGE
                val endIdx = minOf(startIdx + BATCH_PER_PAGE, items.size)

                for (i in startIdx until endIdx) {
                    val slot = i - startIdx
                    val col = slot % BATCH_COLS
                    val row = slot / BATCH_COLS
                    val left = marginX + col * (cellW + gapX)
                    val top = marginTop + headerHeight + row * (cellH + gapY)
                    val right = left + cellW
                    val bottom = top + cellH

                    // Dashed cut guide around label
                    canvas.drawRect(left, top, right, bottom, cutBorderPaint)

                    val item = items[i]
                    val code = item.barcode.ifBlank { item.id }
                    val qrBmp = generateQrBitmap(code, 256)
                    if (qrBmp != null) {
                        val qrLeft = left + (cellW - qrDrawSize) / 2f
                        val qrTop = top + 5f
                        val src = Rect(0, 0, qrBmp.width, qrBmp.height)
                        val dst = RectF(qrLeft, qrTop, qrLeft + qrDrawSize, qrTop + qrDrawSize)
                        canvas.drawBitmap(qrBmp, src, dst, bmpPaint)
                        qrBmp.recycle()
                    }

                    val centerX = left + cellW / 2f
                    val maxTextW = cellW - 8f
                    val title = ellipsize(item.name, titlePaint, maxTextW)
                    canvas.drawText(title, centerX, top + qrDrawSize + 15.5f, titlePaint)

                    val info = InventoryLogic.getExpiryInfo(
                        item = item,
                        notApplicableLabel = notApplicableLabel,
                        expiredDays = expiredDays,
                        criticalDays = criticalDays,
                        warningDays = warningDays
                    )
                    val meta = ellipsize("$code · ${info.formattedDate}", metaPaint, maxTextW)
                    canvas.drawText(meta, centerX, top + qrDrawSize + 25f, metaPaint)
                }

                doc.finishPage(page)
            }
            doc.writeTo(outputStream)
            true
        } finally {
            doc.close()
        }
    }.getOrDefault(false)

    fun shareBatchQrPdf(
        context: Context,
        items: List<InventoryItem>,
        headerBrand: String,
        notApplicableLabel: String,
        expiredDays: Int = 0,
        criticalDays: Int = 30,
        warningDays: Int = 90
    ): Boolean = runCatching {
        val dir = File(context.cacheDir, "qr_labels").apply { mkdirs() }
        val file = File(dir, "BunkerStock_QRs_${items.size}.pdf")
        val ok = FileOutputStream(file).use { out ->
            writeBatchQrPdf(
                items = items,
                outputStream = out,
                headerBrand = headerBrand,
                notApplicableLabel = notApplicableLabel,
                expiredDays = expiredDays,
                criticalDays = criticalDays,
                warningDays = warningDays
            )
        }
        if (!ok) return false
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, file.name))
        true
    }.getOrDefault(false)
}
