package com.bunkerstock.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.bunkerstock.app.data.InventoryItem
import com.bunkerstock.app.data.OpenFoodFactsClient
import com.bunkerstock.app.domain.InventoryLogic
import com.bunkerstock.app.platform.BunkerPlatform
import com.bunkerstock.app.platform.CameraOverlayControls
import com.bunkerstock.app.platform.PlatformCrypto
import com.bunkerstock.app.platform.PlatformHttp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import kotlin.js.JsString
import kotlin.js.Promise

private fun jsPlayBeep(): Unit = js("window.BunkerWeb.playBeep()")
private fun jsGetWebBaseUrl(): String = js("window.BunkerWeb.getWebBaseUrl()")
private fun jsGenerateQrFlat(content: String): String = js("window.BunkerWeb.generateQrFlat(content)")
private fun jsPrintSingleQr(header: String, title: String, meta: String, qrContent: String): Unit =
    js("window.BunkerWeb.printSingleQr(header, title, meta, qrContent)")
private fun jsDownloadSingleQrPng(header: String, title: String, meta: String, qrContent: String): Boolean =
    js("window.BunkerWeb.downloadSingleQrPng(header, title, meta, qrContent)")
private fun jsPrintBatchQrA4(itemsJson: String, headerBrand: String): Boolean =
    js("window.BunkerWeb.printBatchQrA4(itemsJson, headerBrand)")
private fun jsDownloadText(filename: String, content: String, mime: String): Boolean =
    js("window.BunkerWeb.downloadText(filename, content, mime)")
private fun jsPickTextFile(): Promise<JsString> = js("window.BunkerWeb.pickTextFile()")
private fun jsPickPhotoDataUrl(maxDim: Int): Promise<JsString> = js("window.BunkerWeb.pickPhotoDataUrl(maxDim)")
private fun jsShareText(title: String, text: String): Unit = js("window.BunkerWeb.shareText(title, text)")

@Serializable
private data class BatchQrPayloadItem(val code: String, val name: String, val exp: String)

class WebBunkerPlatform(
    private val scope: CoroutineScope
) : BunkerPlatform {

    private val imageCache = LinkedHashMap<String, ImageBitmap>()
    private val negativeImageCache = HashSet<String>()

    override val supportsNativeNotifications: Boolean = false
    override val supportsApkSelfUpdate: Boolean = false
    override val versionTag: String = "V3.4 WEB"
    override val webInviteBaseUrl: String?
        get() = runCatching { jsGetWebBaseUrl() }.getOrNull()?.takeIf { it.startsWith("http") }

    override fun playScanFeedback() {
        jsPlayBeep()
    }

    override suspend fun loadProductImage(source: String): ImageBitmap? {
        if (source.isBlank()) return null
        imageCache[source]?.let { return it }
        if (negativeImageCache.contains(source)) return null

        val bytes = when {
            source.startsWith("data:image/") -> {
                val b64 = source.substringAfter("base64,", "")
                if (b64.isBlank()) null else runCatching { PlatformCrypto.base64Decode(b64) }.getOrNull()
            }
            source.startsWith("http://") || source.startsWith("https://") -> {
                PlatformHttp.getBytes(source, timeoutMs = 5000)
            }
            else -> null
        }

        val bitmap = bytes?.let {
            runCatching { Image.makeFromEncoded(it).toComposeImageBitmap() }.getOrNull()
        }
        if (bitmap != null) {
            if (imageCache.size > 120) imageCache.remove(imageCache.keys.first())
            imageCache[source] = bitmap
        } else {
            negativeImageCache.add(source)
        }
        return bitmap
    }

    override suspend fun resolveProductImage(barcode: String, name: String): ImageBitmap? {
        val cleanBarcode = barcode.trim()
        if (!Regex("^\\d{8,14}$").matches(cleanBarcode)) return null
        val key = "auto:$cleanBarcode"
        imageCache[key]?.let { return it }
        if (negativeImageCache.contains(key)) return null

        val off = OpenFoodFactsClient.lookup(cleanBarcode)
        val url = off?.nutrition?.imageUrl
        if (!url.isNullOrBlank()) {
            val bmp = loadProductImage(url)
            if (bmp != null) {
                imageCache[key] = bmp
                return bmp
            }
        }
        negativeImageCache.add(key)
        return null
    }

    override fun pickCustomProductPhoto(itemId: String, onPicked: (String?) -> Unit) {
        scope.launch {
            val dataUrl = jsPickPhotoDataUrl(900).awaitString()
            onPicked(dataUrl.ifBlank { null })
        }
    }

    override fun generateQrImage(content: String, sizePx: Int): ImageBitmap? = runCatching {
        val flat = jsGenerateQrFlat(content)
        val sep = flat.indexOf(':')
        if (sep <= 0) return null
        val n = flat.substring(0, sep).toIntOrNull() ?: return null
        val bits = flat.substring(sep + 1)
        if (bits.length < n * n) return null

        val margin = 2
        val totalModules = n + margin * 2
        val scale = (sizePx / totalModules).coerceAtLeast(2)
        val dim = totalModules * scale
        val rgba = ByteArray(dim * dim * 4)

        for (y in 0 until dim) {
            val my = y / scale - margin
            for (x in 0 until dim) {
                val mx = x / scale - margin
                val dark = my in 0 until n && mx in 0 until n && bits[my * n + mx] == '1'
                val idx = (y * dim + x) * 4
                if (dark) {
                    rgba[idx] = 0x09.toByte()
                    rgba[idx + 1] = 0x0D.toByte()
                    rgba[idx + 2] = 0x16.toByte()
                    rgba[idx + 3] = 0xFF.toByte()
                } else {
                    rgba[idx] = 0xFF.toByte()
                    rgba[idx + 1] = 0xFF.toByte()
                    rgba[idx + 2] = 0xFF.toByte()
                    rgba[idx + 3] = 0xFF.toByte()
                }
            }
        }
        val info = ImageInfo(dim, dim, ColorType.RGBA_8888, ColorAlphaType.OPAQUE)
        Image.makeRaster(info, rgba, dim * 4).toComposeImageBitmap()
    }.getOrNull()

    override fun printSingleQrLabel(header: String, title: String, meta: String, qrContent: String) {
        jsPrintSingleQr(header, title, meta, qrContent)
    }

    override fun shareSingleQrLabel(header: String, title: String, meta: String, qrContent: String): Boolean =
        jsDownloadSingleQrPng(header, title, meta, qrContent)

    override fun shareText(title: String, text: String) {
        jsShareText(title, text)
    }

    override fun exportJson(suggestedFileName: String, content: String, onResult: (Boolean) -> Unit) {
        onResult(jsDownloadText(suggestedFileName, content, "application/json;charset=utf-8"))
    }

    override fun exportCsv(suggestedFileName: String, content: String, onResult: (Boolean) -> Unit) {
        onResult(jsDownloadText(suggestedFileName, content, "text/csv;charset=utf-8"))
    }

    override fun importJson(onContent: (String?) -> Unit) {
        scope.launch {
            val text = jsPickTextFile().awaitString()
            onContent(text.ifBlank { null })
        }
    }

    override fun saveBatchQrPdf(
        suggestedFileName: String,
        items: List<InventoryItem>,
        headerBrand: String,
        notApplicableLabel: String,
        expiredDays: Int,
        criticalDays: Int,
        warningDays: Int,
        onResult: (Boolean) -> Unit
    ) {
        val payload = items.map { item ->
            val info = InventoryLogic.getExpiryInfo(
                item = item,
                notApplicableLabel = notApplicableLabel,
                expiredDays = expiredDays,
                criticalDays = criticalDays,
                warningDays = warningDays
            )
            BatchQrPayloadItem(
                code = item.barcode.ifBlank { item.id },
                name = item.name,
                exp = info.formattedDate
            )
        }
        val ok = jsPrintBatchQrA4(InventoryLogic.json.encodeToString(payload), headerBrand)
        onResult(ok)
    }

    override fun shareBatchQrPdf(
        items: List<InventoryItem>,
        headerBrand: String,
        notApplicableLabel: String,
        expiredDays: Int,
        criticalDays: Int,
        warningDays: Int,
        onResult: (Boolean) -> Unit
    ) {
        saveBatchQrPdf(
            suggestedFileName = "BunkerStock_QRs.pdf",
            items = items,
            headerBrand = headerBrand,
            notApplicableLabel = notApplicableLabel,
            expiredDays = expiredDays,
            criticalDays = criticalDays,
            warningDays = warningDays,
            onResult = onResult
        )
    }

    @Composable
    override fun CameraViewfinder(
        unlockTrigger: Int,
        shoppingModeActive: Boolean,
        onCodeScanned: (String) -> Unit,
        onImageScanError: (String) -> Unit,
        modifier: Modifier,
        overlayContent: @Composable (CameraOverlayControls) -> Unit
    ) {
        WebCameraViewfinder(
            unlockTrigger = unlockTrigger,
            onCodeScanned = onCodeScanned,
            onImageScanError = onImageScanError,
            modifier = modifier,
            overlayContent = overlayContent
        )
    }
}
