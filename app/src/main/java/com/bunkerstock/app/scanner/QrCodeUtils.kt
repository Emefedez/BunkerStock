package com.bunkerstock.app.scanner

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import androidx.print.PrintHelper
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream

object QrCodeUtils {

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
}
