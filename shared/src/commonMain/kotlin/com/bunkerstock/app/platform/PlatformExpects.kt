package com.bunkerstock.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

expect val isWebPlatform: Boolean

expect fun todayBunkerDate(): BunkerDate

expect fun currentTimeMillis(): Long

expect fun formatLocalTimeDayMonth(epochMillis: Long): String

expect object PlatformHttp {
    suspend fun getText(
        url: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Int = 8000
    ): String?

    suspend fun postText(
        url: String,
        body: String,
        contentType: String = "text/plain",
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Int = 10000
    ): Boolean

    suspend fun getBytes(
        url: String,
        timeoutMs: Int = 5000
    ): ByteArray?
}

expect object PlatformCrypto {
    suspend fun sha256Hex(input: String): String
    fun randomBytes(count: Int): ByteArray
    fun base64Encode(bytes: ByteArray): String
    fun base64Decode(input: String): ByteArray
    fun base64UrlEncodeNoPad(bytes: ByteArray): String

    /**
     * GZIP-compresses [plainUtf8] and encrypts with AES-256-GCM (key = SHA-256(secretKey), 12-byte IV, 128-bit tag).
     * Returns `iv || ciphertext_with_tag`.
     */
    suspend fun encryptGzipAesGcm(plainUtf8: ByteArray, secretKey: String): ByteArray

    /**
     * Decrypts `iv || ciphertext_with_tag` with AES-256-GCM and optionally gunzips.
     */
    suspend fun decryptAesGcmMaybeGunzip(combined: ByteArray, secretKey: String, gunzip: Boolean): ByteArray
}

expect fun createGrainImageBitmap(isDark: Boolean): ImageBitmap?

@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
