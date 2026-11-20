package com.bunkerstock.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.JsAny
import kotlin.js.JsString
import kotlin.js.Promise
import kotlin.js.toJsString
import kotlin.random.Random

private fun jsNowMs(): Double = js("Date.now()")
private fun jsLocalYear(): Int = js("new Date().getFullYear()")
private fun jsLocalMonth(): Int = js("new Date().getMonth() + 1")
private fun jsLocalDay(): Int = js("new Date().getDate()")
private fun jsFormatLocalTimeDayMonth(ms: Double): String = js(
    """(function(t){
        var d = new Date(t);
        var hh = String(d.getHours()).padStart(2,'0');
        var mm = String(d.getMinutes()).padStart(2,'0');
        var dd = String(d.getDate()).padStart(2,'0');
        var mo = String(d.getMonth()+1).padStart(2,'0');
        return hh + ':' + mm + ' · ' + dd + '/' + mo;
    })(ms)"""
)

private fun jsFetchText(url: String, timeoutMs: Int): Promise<JsString?> =
    js("window.BunkerWeb.fetchText(url, timeoutMs)")

private fun jsPostText(url: String, body: String, contentType: String, timeoutMs: Int): Promise<JsAny> =
    js("window.BunkerWeb.postText(url, body, contentType, timeoutMs)")

private fun jsFetchBase64(url: String, timeoutMs: Int): Promise<JsString?> =
    js("window.BunkerWeb.fetchBase64(url, timeoutMs)")

private fun jsSha256Hex(input: String): Promise<JsString> =
    js("window.BunkerWeb.sha256Hex(input)")

private fun jsRandomBase64(count: Int): String =
    js("window.BunkerWeb.randomBase64(count)")

private fun jsEncryptGzipAesGcm(plainBase64: String, secretKey: String): Promise<JsString> =
    js("window.BunkerWeb.encryptGzipAesGcm(plainBase64, secretKey)")

private fun jsDecryptAesGcm(combinedBase64: String, secretKey: String, gunzip: Boolean): Promise<JsString> =
    js("window.BunkerWeb.decryptAesGcm(combinedBase64, secretKey, gunzip)")

private fun jsIsTrue(v: JsAny?): Boolean = js("Boolean(v)")

private suspend fun <T : JsAny?> Promise<T>.awaitJs(): T = suspendCancellableCoroutine { cont ->
    then(
        onFulfilled = { value ->
            cont.resume(value)
            null
        },
        onRejected = { err ->
            cont.resumeWithException(RuntimeException(err.toString()))
            null
        }
    )
}

actual val isWebPlatform: Boolean = true

actual fun todayBunkerDate(): BunkerDate = BunkerDate(jsLocalYear(), jsLocalMonth(), jsLocalDay())

actual fun currentTimeMillis(): Long = jsNowMs().toLong()

actual fun formatLocalTimeDayMonth(epochMillis: Long): String =
    jsFormatLocalTimeDayMonth(epochMillis.toDouble())

actual object PlatformHttp {
    actual suspend fun getText(
        url: String,
        headers: Map<String, String>,
        timeoutMs: Int
    ): String? = runCatching {
        jsFetchText(url, timeoutMs).awaitJs()?.toString()
    }.getOrNull()

    actual suspend fun postText(
        url: String,
        body: String,
        contentType: String,
        headers: Map<String, String>,
        timeoutMs: Int
    ): Boolean = runCatching {
        jsIsTrue(jsPostText(url, body, contentType, timeoutMs).awaitJs())
    }.getOrDefault(false)

    actual suspend fun getBytes(
        url: String,
        timeoutMs: Int
    ): ByteArray? = runCatching {
        val b64 = jsFetchBase64(url, timeoutMs).awaitJs()?.toString() ?: return null
        if (b64.isEmpty()) null else PlatformCrypto.base64Decode(b64)
    }.getOrNull()
}

actual object PlatformCrypto {
    private const val B64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    actual suspend fun sha256Hex(input: String): String =
        jsSha256Hex(input).awaitJs().toString()

    actual fun randomBytes(count: Int): ByteArray =
        base64Decode(jsRandomBase64(count))

    actual fun base64Encode(bytes: ByteArray): String {
        val out = StringBuilder(((bytes.size + 2) / 3) * 4)
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i++].toInt() and 0xFF
            val has1 = i < bytes.size
            val b1 = if (has1) bytes[i++].toInt() and 0xFF else 0
            val has2 = i < bytes.size
            val b2 = if (has2) bytes[i++].toInt() and 0xFF else 0
            out.append(B64_CHARS[b0 ushr 2])
            out.append(B64_CHARS[((b0 and 0x03) shl 4) or (b1 ushr 4)])
            out.append(if (has1) B64_CHARS[((b1 and 0x0F) shl 2) or (b2 ushr 6)] else '=')
            out.append(if (has2) B64_CHARS[b2 and 0x3F] else '=')
        }
        return out.toString()
    }

    actual fun base64Decode(input: String): ByteArray {
        val clean = input.replace("\n", "").replace("\r", "").replace(" ", "")
            .replace('-', '+').replace('_', '/')
        val padded = when (clean.length % 4) {
            2 -> "$clean=="
            3 -> "$clean="
            else -> clean
        }
        if (padded.isEmpty()) return ByteArray(0)
        val padLen = if (padded.endsWith("==")) 2 else if (padded.endsWith("=")) 1 else 0
        val out = ByteArray((padded.length / 4) * 3 - padLen)
        var outIdx = 0
        var i = 0
        while (i < padded.length) {
            val c0 = decodeChar(padded[i++])
            val c1 = decodeChar(padded[i++])
            val c2 = decodeChar(padded[i++])
            val c3 = decodeChar(padded[i++])
            val triple = (c0 shl 18) or (c1 shl 12) or (c2 shl 6) or c3
            if (outIdx < out.size) out[outIdx++] = ((triple ushr 16) and 0xFF).toByte()
            if (outIdx < out.size) out[outIdx++] = ((triple ushr 8) and 0xFF).toByte()
            if (outIdx < out.size) out[outIdx++] = (triple and 0xFF).toByte()
        }
        return out
    }

    private fun decodeChar(ch: Char): Int = when (ch) {
        in 'A'..'Z' -> ch - 'A'
        in 'a'..'z' -> ch - 'a' + 26
        in '0'..'9' -> ch - '0' + 52
        '+' -> 62
        '/' -> 63
        else -> 0
    }

    actual fun base64UrlEncodeNoPad(bytes: ByteArray): String =
        base64Encode(bytes).replace('+', '-').replace('/', '_').trimEnd('=')

    actual suspend fun encryptGzipAesGcm(plainUtf8: ByteArray, secretKey: String): ByteArray {
        val outB64 = jsEncryptGzipAesGcm(base64Encode(plainUtf8), secretKey).awaitJs().toString()
        return base64Decode(outB64)
    }

    actual suspend fun decryptAesGcmMaybeGunzip(
        combined: ByteArray,
        secretKey: String,
        gunzip: Boolean
    ): ByteArray {
        val outB64 = jsDecryptAesGcm(base64Encode(combined), secretKey, gunzip).awaitJs().toString()
        return base64Decode(outB64)
    }
}

actual fun createGrainImageBitmap(isDark: Boolean): ImageBitmap? = runCatching {
    val size = 160
    val rng = Random(20261119)
    val bytes = ByteArray(size * size * 4)
    var idx = 0
    for (p in 0 until size * size) {
        val v = (if (isDark) (14 + rng.nextInt(26)) else (160 + rng.nextInt(45))).toByte()
        val a = (if (isDark) (4 + rng.nextInt(9)) else (4 + rng.nextInt(7))).toByte()
        bytes[idx++] = v
        bytes[idx++] = v
        bytes[idx++] = v
        bytes[idx++] = a
    }
    val info = ImageInfo(size, size, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL)
    Image.makeRaster(info, bytes, size * 4).toComposeImageBitmap()
}.getOrNull()

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Web uses on-screen navigation buttons and Escape/UI close actions
}
