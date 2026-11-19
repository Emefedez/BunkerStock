package com.bunkerstock.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object ProductImageStore {

    private fun hashKey(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.take(12).joinToString("") { "%02x".format(it) }
    }

    /**
     * Copies a user-selected gallery image into internal app storage so it persists offline forever.
     */
    suspend fun saveCustomImage(context: Context, uri: Uri, itemId: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val bmp = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                } ?: return@runCatching null
                val scaled = scaleDown(bmp, 512)
                val dir = File(context.filesDir, "product_images").apply { mkdirs() }
                val file = File(dir, "custom_${itemId}_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 86, out)
                }
                file.absolutePath
            }.getOrNull()
        }

    /**
     * Loads a local path or cached remote URL bitmap (downloads & caches remote Open Food Facts photos).
     */
    suspend fun loadBitmap(context: Context, source: String): Bitmap? = withContext(Dispatchers.IO) {
        if (source.isBlank()) return@withContext null
        runCatching {
            if (!source.startsWith("http://") && !source.startsWith("https://")) {
                val file = File(source)
                return@runCatching if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
            }

            val cacheDir = File(context.cacheDir, "product_img_cache").apply { mkdirs() }
            val cacheFile = File(cacheDir, "${hashKey(source)}.jpg")
            if (cacheFile.exists() && cacheFile.length() > 0) {
                return@runCatching BitmapFactory.decodeFile(cacheFile.absolutePath)
            }

            val conn = (URL(source).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4500
                readTimeout = 4500
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "BunkerStock-Android/2.8")
            }
            try {
                if (conn.responseCode !in 200..299) return@runCatching null
                val raw = conn.inputStream.use { BitmapFactory.decodeStream(it) } ?: return@runCatching null
                val scaled = scaleDown(raw, 400)
                FileOutputStream(cacheFile).use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                scaled
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    private fun scaleDown(src: Bitmap, maxDim: Int): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= maxDim && h <= maxDim) return src
        val ratio = maxDim.toFloat() / maxOf(w, h).toFloat()
        return Bitmap.createScaledBitmap(src, (w * ratio).toInt().coerceAtLeast(1), (h * ratio).toInt().coerceAtLeast(1), true)
    }
}
