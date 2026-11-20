package com.bunkerstock.app.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

actual val isWebPlatform: Boolean = false

actual fun todayBunkerDate(): BunkerDate {
    val now = LocalDate.now()
    return BunkerDate(now.year, now.monthValue, now.dayOfMonth)
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun formatLocalTimeDayMonth(epochMillis: Long): String =
    SimpleDateFormat("HH:mm · dd/MM", Locale.getDefault()).format(Date(epochMillis))

actual object PlatformHttp {
    actual suspend fun getText(
        url: String,
        headers: Map<String, String>,
        timeoutMs: Int
    ): String? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = true
                for ((k, v) in headers) setRequestProperty(k, v)
            }
            if (conn.responseCode !in 200..299) return@withContext null
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    actual suspend fun postText(
        url: String,
        body: String,
        contentType: String,
        headers: Map<String, String>,
        timeoutMs: Int
    ): Boolean = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                doOutput = true
                setRequestProperty("Content-Type", contentType)
                for ((k, v) in headers) setRequestProperty(k, v)
            }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            conn.responseCode in 200..299
        } catch (_: Exception) {
            false
        } finally {
            conn?.disconnect()
        }
    }

    actual suspend fun getBytes(
        url: String,
        timeoutMs: Int
    ): ByteArray? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "BunkerStock-Android/3.5")
            }
            if (conn.responseCode !in 200..299) return@withContext null
            conn.inputStream.use { it.readBytes() }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}

actual object PlatformCrypto {
    private const val GCM_IV_BYTES = 12
    private const val GCM_TAG_BITS = 128

    actual suspend fun sha256Hex(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    actual fun randomBytes(count: Int): ByteArray =
        ByteArray(count).also { SecureRandom().nextBytes(it) }

    actual fun base64Encode(bytes: ByteArray): String =
        Base64.getEncoder().encodeToString(bytes)

    actual fun base64Decode(input: String): ByteArray =
        Base64.getDecoder().decode(input)

    actual fun base64UrlEncodeNoPad(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun deriveAesKey(secretKey: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256").digest(secretKey.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(digest, "AES")
    }

    actual suspend fun encryptGzipAesGcm(plainUtf8: ByteArray, secretKey: String): ByteArray =
        withContext(Dispatchers.Default) {
            val compressed = ByteArrayOutputStream().also { out ->
                GZIPOutputStream(out).use { it.write(plainUtf8) }
            }.toByteArray()
            val iv = randomBytes(GCM_IV_BYTES)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, deriveAesKey(secretKey), GCMParameterSpec(GCM_TAG_BITS, iv))
            val encrypted = cipher.doFinal(compressed)
            ByteArray(iv.size + encrypted.size).also { combined ->
                System.arraycopy(iv, 0, combined, 0, iv.size)
                System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
            }
        }

    actual suspend fun decryptAesGcmMaybeGunzip(
        combined: ByteArray,
        secretKey: String,
        gunzip: Boolean
    ): ByteArray = withContext(Dispatchers.Default) {
        val iv = combined.copyOfRange(0, GCM_IV_BYTES)
        val encrypted = combined.copyOfRange(GCM_IV_BYTES, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveAesKey(secretKey), GCMParameterSpec(GCM_TAG_BITS, iv))
        val plain = cipher.doFinal(encrypted)
        if (gunzip) {
            GZIPInputStream(ByteArrayInputStream(plain)).use { it.readBytes() }
        } else {
            plain
        }
    }
}
