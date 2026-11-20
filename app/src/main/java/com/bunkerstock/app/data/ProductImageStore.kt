package com.bunkerstock.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.bunkerstock.app.domain.InventoryLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

object ProductImageStore {

    private val negativeCache = ConcurrentHashMap<String, Boolean>()

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
                val scaled = scaleDown(bmp, 1200)
                val dir = File(context.filesDir, "product_images").apply { mkdirs() }
                val file = File(dir, "custom_${itemId}_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                file.absolutePath
            }.getOrNull()
        }

    /**
     * Loads a local path or cached remote URL bitmap.
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

            downloadAndCacheBitmap(source, cacheFile)
        }.getOrNull()
    }

    /**
     * Resolves a product photo automatically without needing images transferred over Family Sync:
     * 1. Local disk cache by barcode/name
     * 2. Open Food Facts by barcode (if 8-14 digits)
     * 3. Open Food Facts search by product name
     * 4. First valid Google Images thumbnail fallback
     */
    suspend fun resolveForProduct(
        context: Context,
        barcode: String,
        name: String
    ): Bitmap? = withContext(Dispatchers.IO) {
        val lookupKey = "${barcode.trim()}|${name.trim().lowercase()}"
        if (lookupKey == "|" || negativeCache.containsKey(lookupKey)) return@withContext null

        val cacheDir = File(context.cacheDir, "product_img_cache").apply { mkdirs() }
        val cacheFile = File(cacheDir, "auto_hd_${hashKey(lookupKey)}.jpg")
        if (cacheFile.exists() && cacheFile.length() > 0) {
            return@withContext BitmapFactory.decodeFile(cacheFile.absolutePath)
        }

        // 1. Open Food Facts by barcode
        val cleanBarcode = barcode.trim()
        if (Regex("^\\d{8,14}$").matches(cleanBarcode)) {
            val off = OpenFoodFactsClient.lookup(cleanBarcode)
            val url = off?.nutrition?.imageUrl
            if (!url.isNullOrBlank()) {
                val bmp = downloadAndCacheBitmap(url, cacheFile)
                if (bmp != null) return@withContext bmp
            }
        }

        // 2. Open Food Facts search by product name
        val cleanName = name.trim()
        if (cleanName.length >= 3) {
            val offSearchUrl = searchOpenFoodFactsImage(cleanName)
            if (!offSearchUrl.isNullOrBlank()) {
                val bmp = downloadAndCacheBitmap(offSearchUrl, cacheFile)
                if (bmp != null) return@withContext bmp
            }

            // 3. First valid Google Images thumbnail fallback
            val webUrl = searchGoogleFirstImage(cleanName)
            if (!webUrl.isNullOrBlank()) {
                val bmp = downloadAndCacheBitmap(webUrl, cacheFile)
                if (bmp != null) return@withContext bmp
            }
        }

        negativeCache[lookupKey] = true
        null
    }

    private fun searchOpenFoodFactsImage(query: String): String? = runCatching {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://world.openfoodfacts.org/cgi/search.pl?search_terms=$encoded&search_simple=1&action=process&json=1&page_size=3&fields=image_front_url,image_url,image_front_small_url"
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 3500
            readTimeout = 3500
            setRequestProperty("User-Agent", "BunkerStock-Android/3.5")
        }
        try {
            if (conn.responseCode !in 200..299) return@runCatching null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val root = InventoryLogic.json.parseToJsonElement(body) as? JsonObject ?: return@runCatching null
            val products = root["products"] as? JsonArray ?: return@runCatching null
            for (el in products) {
                val obj = el as? JsonObject ?: continue
                val img = obj["image_front_url"]?.jsonPrimitive?.contentOrNull
                    ?: obj["image_url"]?.jsonPrimitive?.contentOrNull
                    ?: obj["image_front_small_url"]?.jsonPrimitive?.contentOrNull
                if (!img.isNullOrBlank() && img.startsWith("http")) return@runCatching img
            }
            null
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    private fun searchGoogleFirstImage(query: String): String? = runCatching {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://www.google.com/search?tbm=isch&q=$encoded&safe=active"
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 3500
            readTimeout = 3500
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            )
        }
        try {
            if (conn.responseCode !in 200..299) return@runCatching null
            val html = conn.inputStream.bufferedReader().use { it.readText() }
            val thumbRegex = Regex("https://encrypted-tbn0\\.gstatic\\.com/images\\?q=tbn:[^\"'&\\s<>]+")
            thumbRegex.find(html)?.value?.replace("\\u003d", "=")?.replace("\\u0026", "&")
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    private fun downloadAndCacheBitmap(url: String, cacheFile: File): Bitmap? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 4000
            readTimeout = 4000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "BunkerStock-Android/3.5")
        }
        return try {
            if (conn.responseCode !in 200..299) return null
            val raw = conn.inputStream.use { BitmapFactory.decodeStream(it) } ?: return null
            val scaled = scaleDown(raw, 1200)
            FileOutputStream(cacheFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            scaled
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun scaleDown(src: Bitmap, maxDim: Int): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= maxDim && h <= maxDim) return src
        val ratio = maxDim.toFloat() / maxOf(w, h).toFloat()
        return Bitmap.createScaledBitmap(
            src,
            (w * ratio).toInt().coerceAtLeast(1),
            (h * ratio).toInt().coerceAtLeast(1),
            true
        )
    }
}
