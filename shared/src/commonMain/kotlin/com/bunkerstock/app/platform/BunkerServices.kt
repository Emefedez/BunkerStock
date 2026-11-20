package com.bunkerstock.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import com.bunkerstock.app.data.AppLanguage
import com.bunkerstock.app.data.AppSettings
import com.bunkerstock.app.data.AppThemeMode
import com.bunkerstock.app.data.AppUpdateInfo
import com.bunkerstock.app.data.FamilyGroupConfig
import com.bunkerstock.app.data.InventoryItem
import kotlinx.coroutines.flow.Flow

interface BunkerStorage {
    val inventoryFlow: Flow<List<InventoryItem>>
    val deletedIdsFlow: Flow<Map<String, Long>>
    val familyGroupFlow: Flow<FamilyGroupConfig>
    val settingsFlow: Flow<AppSettings>

    suspend fun ensureInitialized()
    suspend fun saveInventory(items: List<InventoryItem>)
    suspend fun saveDeletedIds(deleted: Map<String, Long>)
    suspend fun saveFamilyGroup(config: FamilyGroupConfig)
    suspend fun clearFamilyGroup()
    suspend fun updateLanguage(language: AppLanguage)
    suspend fun updateThemeMode(mode: AppThemeMode)
    suspend fun updateWaterConfig(persons: Int, litersPerDay: Int)
    suspend fun updateAutoCheckUpdates(enabled: Boolean)
    suspend fun updateExpiryNotifications(enabled: Boolean)
    suspend fun updateExpiryThresholds(expiredDays: Int, criticalDays: Int, warningDays: Int)
    suspend fun markOnboardingCompleted()
    suspend fun updateFamilySyncPrefs(enabled: Boolean, intervalSec: Int)
}

interface BunkerPlatform {
    val supportsNativeNotifications: Boolean get() = false
    val supportsApkSelfUpdate: Boolean get() = false
    val versionTag: String get() = "V3.4"

    /** Optional HTTPS invite base URL (used on Web so shared links open the PWA directly). */
    val webInviteBaseUrl: String? get() = null

    fun playScanFeedback() {}

    suspend fun evaluateExpiryNotifications() {}
    fun schedulePeriodicExpiryCheck() {}
    fun requestNotificationPermission(onResult: (Boolean) -> Unit) { onResult(false) }

    suspend fun checkForAppUpdate(): AppUpdateInfo? = null
    suspend fun downloadAndInstallAppUpdate(update: AppUpdateInfo): Boolean = false

    suspend fun loadProductImage(source: String): ImageBitmap? = null
    suspend fun resolveProductImage(barcode: String, name: String): ImageBitmap? = null
    fun pickCustomProductPhoto(itemId: String, onPicked: (String?) -> Unit) { onPicked(null) }

    fun generateQrImage(content: String, sizePx: Int = 420): ImageBitmap? = null
    fun printSingleQrLabel(header: String, title: String, meta: String, qrContent: String) {}
    fun shareSingleQrLabel(header: String, title: String, meta: String, qrContent: String): Boolean = false

    fun shareText(title: String, text: String) {}

    fun exportJson(suggestedFileName: String, content: String, onResult: (Boolean) -> Unit) { onResult(false) }
    fun exportCsv(suggestedFileName: String, content: String, onResult: (Boolean) -> Unit) { onResult(false) }
    fun importJson(onContent: (String?) -> Unit) { onContent(null) }

    fun saveBatchQrPdf(
        suggestedFileName: String,
        items: List<InventoryItem>,
        headerBrand: String,
        notApplicableLabel: String,
        expiredDays: Int,
        criticalDays: Int,
        warningDays: Int,
        onResult: (Boolean) -> Unit
    ) { onResult(false) }

    fun shareBatchQrPdf(
        items: List<InventoryItem>,
        headerBrand: String,
        notApplicableLabel: String,
        expiredDays: Int,
        criticalDays: Int,
        warningDays: Int,
        onResult: (Boolean) -> Unit
    ) { onResult(false) }

    /**
     * Platform-specific live camera viewfinder surface (CameraX + ML Kit on Android;
     * HTML <video playsinline> + zxing-wasm/barcode-detector on Web/iOS).
     */
    @Composable
    fun CameraViewfinder(
        unlockTrigger: Int,
        shoppingModeActive: Boolean,
        onCodeScanned: (String) -> Unit,
        onImageScanError: (String) -> Unit,
        modifier: Modifier,
        overlayContent: @Composable (CameraOverlayControls) -> Unit
    )
}

data class CameraOverlayControls(
    val hasPermission: Boolean,
    val cameraError: Boolean,
    val hasFlash: Boolean,
    val torchEnabled: Boolean,
    val zoomActive: Boolean = false,
    val supportsZoom: Boolean = false,
    val onRequestPermission: () -> Unit,
    val onToggleTorch: () -> Unit,
    val onSwitchCamera: () -> Unit,
    val onToggleZoom: () -> Unit = {},
    val onPickImageToScan: () -> Unit,
    val onResetDebounce: () -> Unit
)

val LocalBunkerPlatform = staticCompositionLocalOf<BunkerPlatform> {
    error("LocalBunkerPlatform not provided")
}

val LocalScreenSizePx = staticCompositionLocalOf { IntSize(1080, 2400) }
