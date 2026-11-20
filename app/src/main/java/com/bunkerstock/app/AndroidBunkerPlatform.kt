package com.bunkerstock.app

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.bunkerstock.app.data.AppUpdateInfo
import com.bunkerstock.app.data.BunkerRepository
import com.bunkerstock.app.data.ExpiryNotificationHelper
import com.bunkerstock.app.data.InventoryItem
import com.bunkerstock.app.data.ProductImageStore
import com.bunkerstock.app.data.UpdateChecker
import com.bunkerstock.app.platform.BunkerPlatform
import com.bunkerstock.app.platform.CameraOverlayControls
import com.bunkerstock.app.scanner.QrCodeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity-level callbacks (SAF document pickers, permission launchers, photo picker)
 * bound while MainActivity is active.
 */
class AndroidActivityBridge {
    var activityProvider: (() -> Activity?)? = null
    var onRequestNotificationPermission: (((Boolean) -> Unit) -> Unit)? = null
    var onPickCustomPhoto: ((String, (String?) -> Unit) -> Unit)? = null
    var onExportJson: ((String, String, (Boolean) -> Unit) -> Unit)? = null
    var onExportCsv: ((String, String, (Boolean) -> Unit) -> Unit)? = null
    var onImportJson: (((String?) -> Unit) -> Unit)? = null
    var onSaveBatchPdf: ((String, List<InventoryItem>, String, String, Int, Int, Int, (Boolean) -> Unit) -> Unit)? = null
    var onStartActivity: ((Intent) -> Unit)? = null
}

/**
 * 100% Native Android implementation of BunkerPlatform:
 * Uses CameraX + bundled offline ML Kit, WorkManager, DataStore, ProductImageStore,
 * Android PdfDocument, PrintHelper, FileProvider, SAF, ToneGenerator, and VibratorManager.
 */
class AndroidBunkerPlatform(
    private val app: Application,
    private val repository: BunkerRepository,
    private val scope: CoroutineScope
) : BunkerPlatform {

    val activityBridge = AndroidActivityBridge()

    override val supportsNativeNotifications: Boolean = true
    override val supportsApkSelfUpdate: Boolean = true
    override val versionTag: String = "V${BuildConfig.VERSION_NAME}"
    override val webInviteBaseUrl: String? = null // Native Android uses bunkerstock://join?...

    override fun playScanFeedback() {
        runCatching {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 130)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = app.getSystemService(VibratorManager::class.java)
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(90L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = app.getSystemService(Vibrator::class.java)
                v?.vibrate(VibrationEffect.createOneShot(90L, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }

    override suspend fun evaluateExpiryNotifications() {
        ExpiryNotificationHelper.evaluateAndNotify(app, repository)
    }

    override fun schedulePeriodicExpiryCheck() {
        ExpiryNotificationHelper.schedulePeriodicCheck(app)
    }

    override fun requestNotificationPermission(onResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activityBridge.onRequestNotificationPermission?.invoke(onResult) ?: onResult(true)
        } else {
            onResult(true)
        }
    }

    override suspend fun checkForAppUpdate(): AppUpdateInfo? = UpdateChecker.checkForUpdate()

    override suspend fun downloadAndInstallAppUpdate(update: AppUpdateInfo): Boolean =
        UpdateChecker.downloadAndInstallApk(app, update)

    override suspend fun loadProductImage(source: String): ImageBitmap? =
        ProductImageStore.loadBitmap(app, source)?.asImageBitmap()

    override suspend fun resolveProductImage(barcode: String, name: String): ImageBitmap? =
        ProductImageStore.resolveForProduct(app, barcode, name)?.asImageBitmap()

    override fun pickCustomProductPhoto(itemId: String, onPicked: (String?) -> Unit) {
        activityBridge.onPickCustomPhoto?.invoke(itemId, onPicked) ?: onPicked(null)
    }

    override fun generateQrImage(content: String, sizePx: Int): ImageBitmap? =
        QrCodeUtils.generateQrBitmap(content, sizePx)?.asImageBitmap()

    override fun printSingleQrLabel(header: String, title: String, meta: String, qrContent: String) {
        val ctx = activityBridge.activityProvider?.invoke() ?: return
        val printable = QrCodeUtils.generatePrintableLabelBitmap(header, title, meta, qrContent) ?: return
        QrCodeUtils.printLabel(ctx, "BunkerStock-$qrContent", printable)
    }

    override fun shareSingleQrLabel(header: String, title: String, meta: String, qrContent: String): Boolean {
        val ctx = activityBridge.activityProvider?.invoke() ?: app
        val printable = QrCodeUtils.generatePrintableLabelBitmap(header, title, meta, qrContent) ?: return false
        return QrCodeUtils.shareLabelPng(ctx, "qr_$qrContent", printable)
    }

    override fun shareText(title: String, text: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(sendIntent, title)
        val launcher = activityBridge.onStartActivity
        if (launcher != null) {
            launcher(chooser)
        } else {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(chooser)
        }
    }

    override fun exportJson(suggestedFileName: String, content: String, onResult: (Boolean) -> Unit) {
        activityBridge.onExportJson?.invoke(suggestedFileName, content, onResult) ?: onResult(false)
    }

    override fun exportCsv(suggestedFileName: String, content: String, onResult: (Boolean) -> Unit) {
        activityBridge.onExportCsv?.invoke(suggestedFileName, content, onResult) ?: onResult(false)
    }

    override fun importJson(onContent: (String?) -> Unit) {
        activityBridge.onImportJson?.invoke(onContent) ?: onContent(null)
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
        activityBridge.onSaveBatchPdf?.invoke(
            suggestedFileName,
            items,
            headerBrand,
            notApplicableLabel,
            expiredDays,
            criticalDays,
            warningDays,
            onResult
        ) ?: onResult(false)
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
        scope.launch {
            val ctx = activityBridge.activityProvider?.invoke() ?: app
            val ok = withContext(Dispatchers.IO) {
                QrCodeUtils.shareBatchQrPdf(
                    context = ctx,
                    items = items,
                    headerBrand = headerBrand,
                    notApplicableLabel = notApplicableLabel,
                    expiredDays = expiredDays,
                    criticalDays = criticalDays,
                    warningDays = warningDays
                )
            }
            onResult(ok)
        }
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
        AndroidCameraViewfinder(
            unlockTrigger = unlockTrigger,
            onCodeScanned = onCodeScanned,
            onImageScanError = onImageScanError,
            modifier = modifier,
            overlayContent = overlayContent
        )
    }
}
