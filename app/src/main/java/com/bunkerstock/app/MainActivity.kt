package com.bunkerstock.app

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bunkerstock.app.data.AppThemeMode
import com.bunkerstock.app.data.ExpiryNotificationHelper
import com.bunkerstock.app.data.FamilySyncClient
import com.bunkerstock.app.data.InventoryItem
import com.bunkerstock.app.data.ProductImageStore
import com.bunkerstock.app.scanner.QrCodeUtils
import com.bunkerstock.app.ui.BunkerStockRoot
import com.bunkerstock.app.ui.BunkerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val viewModel: BunkerViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            val state by viewModel.controller.uiState.collectAsStateWithLifecycle()
            val windowSize = calculateWindowSizeClass(this)
            val isExpanded = windowSize.widthSizeClass != WindowWidthSizeClass.Compact
            val scope = rememberCoroutineScope()

            val isDark = when (state.settings.themeMode) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            LaunchedEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
            }

            // Lifecycle foreground/background tracking for diff-aware Family Sync polling
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val obs = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> viewModel.controller.setAppForeground(true)
                        Lifecycle.Event.ON_PAUSE -> viewModel.controller.setAppForeground(false)
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(obs)
                onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
            }

            // Native Android Activity Result Launchers (SAF, Photo Picker, Notifications)
            // Stored in a remembered holder so recompositions (e.g. background sync) never drop callbacks.
            val pending = remember { PendingActivityCallbacks() }

            val notifLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                pending.notifCb?.invoke(granted)
                pending.notifCb = null
            }

            val photoLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri ->
                val cb = pending.photoCb
                val itemId = pending.photoItemId
                pending.photoCb = null
                if (uri != null && cb != null) {
                    scope.launch {
                        val saved = ProductImageStore.saveCustomImage(this@MainActivity, uri, itemId)
                        cb(saved)
                    }
                } else {
                    cb?.invoke(null)
                }
            }

            val exportJsonLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                val cb = pending.jsonCb
                val payload = pending.jsonPayload
                pending.jsonCb = null
                if (uri != null) {
                    val ok = runCatching {
                        contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(payload.toByteArray(Charsets.UTF_8))
                        }
                    }.isSuccess
                    cb?.invoke(ok)
                }
            }

            val exportCsvLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("text/csv")
            ) { uri ->
                val cb = pending.csvCb
                val payload = pending.csvPayload
                pending.csvCb = null
                if (uri != null) {
                    val ok = runCatching {
                        contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(payload.toByteArray(Charsets.UTF_8))
                        }
                    }.isSuccess
                    cb?.invoke(ok)
                }
            }

            val importJsonLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                val cb = pending.importCb
                pending.importCb = null
                if (uri != null && cb != null) {
                    val content = runCatching {
                        contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    }.getOrNull()
                    cb(content)
                }
            }

            val exportBatchPdfLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/pdf")
            ) { uri ->
                val req = pending.batchPdf
                pending.batchPdf = null
                if (uri != null && req != null) {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            runCatching {
                                contentResolver.openOutputStream(uri)?.use { out ->
                                    QrCodeUtils.writeBatchQrPdf(
                                        items = req.items,
                                        outputStream = out,
                                        headerBrand = req.headerBrand,
                                        notApplicableLabel = req.notApplicableLabel,
                                        expiredDays = req.expiredDays,
                                        criticalDays = req.criticalDays,
                                        warningDays = req.warningDays
                                    )
                                } ?: false
                            }.getOrDefault(false)
                        }
                        req.callback(ok)
                    }
                } else {
                    req?.callback(false)
                }
            }

            DisposableEffect(Unit) {
                val bridge = viewModel.platform.activityBridge
                bridge.activityProvider = { this@MainActivity }
                bridge.onRequestNotificationPermission = { cb ->
                    pending.notifCb = cb
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                bridge.onPickCustomPhoto = { itemId, cb ->
                    pending.photoItemId = itemId
                    pending.photoCb = cb
                    photoLauncher.launch("image/*")
                }
                bridge.onExportJson = { name, content, cb ->
                    pending.jsonPayload = content
                    pending.jsonCb = cb
                    exportJsonLauncher.launch(name)
                }
                bridge.onExportCsv = { name, content, cb ->
                    pending.csvPayload = content
                    pending.csvCb = cb
                    exportCsvLauncher.launch(name)
                }
                bridge.onImportJson = { cb ->
                    pending.importCb = cb
                    importJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                }
                bridge.onSaveBatchPdf = { fileName, items, header, na, exp, crit, warn, cb ->
                    pending.batchPdf = BatchPdfRequest(items, header, na, exp, crit, warn, cb)
                    exportBatchPdfLauncher.launch(fileName)
                }
                bridge.onStartActivity = { intent -> startActivity(intent) }

                onDispose {
                    bridge.activityProvider = null
                    bridge.onRequestNotificationPermission = null
                    bridge.onPickCustomPhoto = null
                    bridge.onExportJson = null
                    bridge.onExportCsv = null
                    bridge.onImportJson = null
                    bridge.onSaveBatchPdf = null
                    bridge.onStartActivity = null
                }
            }

            BunkerStockRoot(
                controller = viewModel.controller,
                platform = viewModel.platform,
                isExpandedWidth = isExpanded
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val data = intent.dataString
        if (!data.isNullOrBlank() && FamilySyncClient.isInviteLink(data)) {
            viewModel.controller.joinFamilyByInvite(data)
        }
        if (intent.getBooleanExtra(ExpiryNotificationHelper.EXTRA_OPEN_ALERTS, false)) {
            viewModel.controller.quickFilterAlerts()
        }
    }
}

private data class BatchPdfRequest(
    val items: List<InventoryItem>,
    val headerBrand: String,
    val notApplicableLabel: String,
    val expiredDays: Int,
    val criticalDays: Int,
    val warningDays: Int,
    val callback: (Boolean) -> Unit
)

private class PendingActivityCallbacks {
    var notifCb: ((Boolean) -> Unit)? = null
    var photoItemId: String = "new"
    var photoCb: ((String?) -> Unit)? = null
    var jsonPayload: String = ""
    var jsonCb: ((Boolean) -> Unit)? = null
    var csvPayload: String = ""
    var csvCb: ((Boolean) -> Unit)? = null
    var importCb: ((String?) -> Unit)? = null
    var batchPdf: BatchPdfRequest? = null
}
