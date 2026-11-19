package com.bunkerstock.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.DisposableEffect
import com.bunkerstock.app.data.AppLanguage
import com.bunkerstock.app.data.AppThemeMode
import com.bunkerstock.app.data.ExpiryNotificationHelper
import com.bunkerstock.app.ui.BunkerTab
import com.bunkerstock.app.ui.BunkerViewModel
import com.bunkerstock.app.ui.components.AppUpdateDialog
import com.bunkerstock.app.ui.components.BatchQrPdfDialog
import com.bunkerstock.app.ui.components.ConfirmActionDialog
import com.bunkerstock.app.ui.components.FamilyGroupDialog
import com.bunkerstock.app.ui.components.FirstTimeSetupDialog
import com.bunkerstock.app.ui.components.ItemEditorSheet
import com.bunkerstock.app.ui.components.ProductDetailOverlay
import com.bunkerstock.app.ui.components.QrLabelDialog
import com.bunkerstock.app.ui.components.QuickScanActionSheet
import com.bunkerstock.app.scanner.QrCodeUtils
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.bunkerstock.app.ui.screens.ScannerScreen
import com.bunkerstock.app.ui.screens.SettingsScreen
import com.bunkerstock.app.ui.screens.WarehouseScreen
import com.bunkerstock.app.ui.theme.BunkerGrainOverlay
import com.bunkerstock.app.ui.theme.BunkerRadius
import com.bunkerstock.app.ui.theme.BunkerStockTheme
import com.bunkerstock.app.ui.theme.LocalAppStrings
import com.bunkerstock.app.ui.theme.LocalBunkerColors
import com.bunkerstock.app.ui.theme.stringsFor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: BunkerViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val windowSize = calculateWindowSizeClass(this)
            val isExpanded = windowSize.widthSizeClass != WindowWidthSizeClass.Compact
            val strings = remember(state.settings.language) { stringsFor(state.settings.language) }

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

            CompositionLocalProvider(LocalAppStrings provides strings) {
                BunkerStockTheme(themeMode = state.settings.themeMode) {
                    BunkerStockApp(
                        viewModel = viewModel,
                        isExpandedWidth = isExpanded
                    )
                }
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val data = intent.dataString
        if (!data.isNullOrBlank() && data.startsWith("bunkerstock://join", ignoreCase = true)) {
            viewModel.joinFamilyByInvite(data)
        }
        if (intent.getBooleanExtra(ExpiryNotificationHelper.EXTRA_OPEN_ALERTS, false)) {
            viewModel.quickFilterAlerts()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BunkerStockApp(
    viewModel: BunkerViewModel,
    isExpandedWidth: Boolean
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalAppStrings.current
    val colors = LocalBunkerColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    val warehouseGridState = rememberLazyGridState()
    val fabExpanded by remember {
        derivedStateOf {
            warehouseGridState.firstVisibleItemIndex == 0 &&
                warehouseGridState.firstVisibleItemScrollOffset < 60
        }
    }
    var scannerUnlockCounter by remember { mutableIntStateOf(0) }
    var dataMenuExpanded by remember { mutableStateOf(false) }
    var pendingBatchQrIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isGeneratingBatchPdf by remember { mutableStateOf(false) }
    val appScope = rememberCoroutineScope()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.setAppForeground(true)
                Lifecycle.Event.ON_PAUSE -> viewModel.setAppForeground(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // SAF launchers for JSON/CSV Export & JSON Import
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(viewModel.buildJsonExport().toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                viewModel.notifyMessage(strings.toastJsonExported)
            }.onFailure {
                viewModel.notifyMessage(strings.toastExportError)
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(viewModel.buildCsvExport().toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                viewModel.notifyMessage(strings.toastCsvExported)
            }.onFailure {
                viewModel.notifyMessage(strings.toastExportError)
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            }.onSuccess { content ->
                if (content != null) viewModel.importJsonText(content)
            }.onFailure {
                viewModel.notifyMessage(strings.toastJsonInvalid)
            }
        }
    }

    val exportBatchPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val ids = pendingBatchQrIds
        if (uri != null && ids.isNotEmpty()) {
            appScope.launch {
                isGeneratingBatchPdf = true
                val items = viewModel.prepareItemsForBatchQr(ids)
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            QrCodeUtils.writeBatchQrPdf(
                                items = items,
                                outputStream = out,
                                headerBrand = strings.qrHeaderBrand,
                                notApplicableLabel = strings.notApplicable,
                                expiredDays = state.settings.expiredThresholdDays,
                                criticalDays = state.settings.criticalThresholdDays,
                                warningDays = state.settings.warningThresholdDays
                            )
                        } ?: false
                    }.getOrDefault(false)
                }
                isGeneratingBatchPdf = false
                if (ok) {
                    viewModel.closeBatchQrDialog()
                    viewModel.notifyMessage(strings.toastBatchPdfSaved)
                } else {
                    viewModel.notifyMessage(strings.toastExportError)
                }
            }
        }
    }

    fun triggerExportJson() {
        val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        exportJsonLauncher.launch("inventario_supervivencia_$date.json")
    }

    fun triggerExportCsv() {
        val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        exportCsvLauncher.launch("inventario_almacen_$date.csv")
    }

    fun triggerImportJson() {
        importJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
    }

    // Back gesture from inline form scanner returns to form sheet cleanly
    BackHandler(enabled = state.scannerForForm) {
        viewModel.closeFormScanner()
    }

    val todayFormatted = remember(state.settings.language) {
        val locale = if (state.settings.language == AppLanguage.ES) Locale("es", "ES") else Locale.US
        LocalDate.now()
            .format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", locale))
            .uppercase(locale)
    }

    val navItems = listOf(
        Triple(BunkerTab.WAREHOUSE, strings.navWarehouse, Icons.Default.Inventory2),
        Triple(BunkerTab.SCANNER, strings.navScanner, Icons.Default.QrCodeScanner),
        Triple(BunkerTab.SETTINGS, strings.navSettings, Icons.Default.Settings)
    )

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.canvas,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Compact 50dp tactical header bar
            Surface(
                color = colors.cardSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.scannerForForm) {
                            IconButton(
                                onClick = { viewModel.closeFormScanner() },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = strings.cancel)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .padding(start = 6.dp, end = 8.dp)
                                    .size(30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_bunker_shield),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = strings.brandBunker,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    )
                                )
                                Text(
                                    text = strings.brandStock,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    ),
                                    color = colors.emerald
                                )
                                Surface(
                                    shape = RoundedCornerShape(BunkerRadius.Badge),
                                    color = colors.cardElevated,
                                    border = BorderStroke(1.dp, colors.borderSubtle)
                                ) {
                                    Text(
                                        text = "V3.3",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp
                                        ),
                                        color = colors.mutedText,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (isExpandedWidth) "${strings.brandSubtitle} · $todayFormatted" else todayFormatted,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                color = colors.mutedText
                            )
                        }

                        if (!state.scannerForForm && state.activeTab == BunkerTab.WAREHOUSE) {
                            if (state.familyGroup.isJoined) {
                                IconButton(
                                    onClick = { viewModel.syncFamilyNow(silent = false) },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = strings.familySyncNow,
                                        tint = colors.emerald,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    scannerUnlockCounter++
                                    viewModel.selectTab(BunkerTab.SCANNER)
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = strings.scanCode,
                                    tint = colors.amber,
                                    modifier = Modifier.size(21.dp)
                                )
                            }

                            Box {
                                IconButton(
                                    onClick = { dataMenuExpanded = true },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = strings.dataMenu,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = dataMenuExpanded,
                                    onDismissRequest = { dataMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(strings.batchQrMenu, fontWeight = FontWeight.SemiBold) },
                                        onClick = {
                                            dataMenuExpanded = false
                                            viewModel.openBatchQrDialog()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(strings.exportJson) },
                                        onClick = {
                                            dataMenuExpanded = false
                                            triggerExportJson()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(strings.exportCsv) },
                                        onClick = {
                                            dataMenuExpanded = false
                                            triggerExportCsv()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(strings.importJson) },
                                        onClick = {
                                            dataMenuExpanded = false
                                            triggerImportJson()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(strings.restoreDemo) },
                                        onClick = {
                                            dataMenuExpanded = false
                                            viewModel.requestRestoreDemo()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(strings.clearAll, color = colors.danger) },
                                        onClick = {
                                            dataMenuExpanded = false
                                            viewModel.requestClearAll()
                                        }
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.7f))
                }
            }
        },
        bottomBar = {
            if (!isExpandedWidth && !state.scannerForForm) {
                // Compact 56dp tactical bottom bar
                Surface(
                    color = colors.cardSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.navigationBarsPadding()) {
                        HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.7f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            navItems.forEach { (tab, label, icon) ->
                                val selected = state.activeTab == tab
                                val fg = if (selected) colors.emerald else colors.mutedText
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .clickable {
                                            if (tab == BunkerTab.SCANNER) scannerUnlockCounter++
                                            viewModel.selectTab(tab)
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (selected) colors.emeraldContainer else Color.Transparent,
                                                RoundedCornerShape(BunkerRadius.Control)
                                            )
                                            .padding(horizontal = 14.dp, vertical = 3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = fg,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = fg
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!state.scannerForForm && state.activeTab == BunkerTab.WAREHOUSE) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openAddItem() },
                    expanded = fabExpanded,
                    shape = RoundedCornerShape(BunkerRadius.Card),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.Add, contentDescription = strings.addItem) },
                    text = { Text(strings.addItem, fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isExpandedWidth && !state.scannerForForm) {
                NavigationRail(containerColor = colors.cardSurface) {
                    Spacer(Modifier.size(12.dp))
                    navItems.forEach { (tab, label, icon) ->
                        NavigationRailItem(
                            selected = state.activeTab == tab,
                            onClick = {
                                if (tab == BunkerTab.SCANNER) scannerUnlockCounter++
                                viewModel.selectTab(tab)
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = colors.emerald,
                                selectedTextColor = colors.emerald,
                                indicatorColor = colors.emeraldContainer,
                                unselectedIconColor = colors.mutedText,
                                unselectedTextColor = colors.mutedText
                            )
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(colors.canvas)
            ) {
                // Subtle dark matte grain underneath all UI (never over text/controls)
                BunkerGrainOverlay(isDark = colors.isDark)

                when {
                    state.scannerForForm || state.activeTab == BunkerTab.SCANNER -> {
                        ScannerScreen(
                            detectedCandidate = state.detectedCandidate,
                            detectedExistingItem = state.detectedExistingItem,
                            shoppingModeActive = state.shoppingModeActive,
                            shoppingEntries = state.shoppingEntries,
                            showShoppingToggle = !state.scannerForForm,
                            unlockTrigger = scannerUnlockCounter,
                            onCodeScanned = { code -> viewModel.onCodeScanned(code) },
                            onToggleShoppingMode = viewModel::toggleShoppingMode,
                            onAdjustShoppingEntry = viewModel::adjustShoppingEntry,
                            onUndoShoppingEntry = viewModel::undoShoppingEntry,
                            onOpenShoppingItemDetail = { id -> viewModel.openItemDetail(id, 0.5f, 0.8f) },
                            onFinishShoppingSession = viewModel::finishShoppingSession,
                            onDismissCandidate = viewModel::dismissDetectedCandidate,
                            onConfirmCandidate = viewModel::confirmDetectedCandidate,
                            onQuickAddExisting = { id, amt -> viewModel.adjustQuantity(id, amt, notifyQuickAdd = true) },
                            onImageScanError = viewModel::notifyMessage
                        )
                    }

                    state.activeTab == BunkerTab.WAREHOUSE -> {
                        WarehouseScreen(
                            state = state,
                            isExpandedWidth = isExpandedWidth,
                            onSearchChange = viewModel::updateSearch,
                            onCategorySelect = viewModel::selectCategory,
                            onToggleCategory = viewModel::toggleCategory,
                            onStatusSelect = viewModel::selectStatus,
                            onToggleStatus = viewModel::toggleStatus,
                            onSortSelect = viewModel::selectSort,
                            onQuickFilterAlerts = viewModel::quickFilterAlerts,
                            onResetFilters = viewModel::resetFilters,
                            onDismissBanner = viewModel::dismissBanner,
                            onAdjustQty = { id, delta -> viewModel.adjustQuantity(id, delta) },
                            onOpenDetail = viewModel::openItemDetail,
                            onOpenQr = viewModel::openQrLabel,
                            onEditItem = viewModel::openEditItem,
                            onDeleteItem = viewModel::requestDeleteItem,
                            gridState = warehouseGridState
                        )
                    }

                    state.activeTab == BunkerTab.SETTINGS -> {
                        SettingsScreen(
                            settings = state.settings,
                            familyGroup = state.familyGroup,
                            isSyncingFamily = state.isSyncingFamily,
                            onOpenFamilyDialog = viewModel::openFamilyDialog,
                            onSyncFamilyNow = { viewModel.syncFamilyNow(silent = false) },
                            onFamilySyncPrefsChange = viewModel::setFamilySyncPrefs,
                            onUpdateMemberName = viewModel::updateFamilyMemberName,
                            onLanguageChange = viewModel::setLanguage,
                            onThemeModeChange = viewModel::setThemeMode,
                            onWaterConfigChange = viewModel::setWaterConfig,
                            onExpiryThresholdsChange = viewModel::setExpiryThresholds,
                            onExpiryNotifChange = viewModel::setExpiryNotifications,
                            onAutoUpdateChange = viewModel::setAutoCheckUpdates,
                            onCheckUpdatesNow = { viewModel.checkForUpdates(silent = false) },
                            onOpenBatchQr = viewModel::openBatchQrDialog,
                            onExportJson = ::triggerExportJson,
                            onExportCsv = ::triggerExportCsv,
                            onImportJson = ::triggerImportJson,
                            onRestoreDemo = viewModel::requestRestoreDemo,
                            onClearAll = viewModel::requestClearAll
                        )
                    }
                }
            }
        }
    }

    // Full-Screen Animated Product Detail Page (zooms smoothly from product icon upward — zero window X!)
    ProductDetailOverlay(
        item = state.detailItem,
        originX = state.detailOriginX,
        originY = state.detailOriginY,
        expiredDays = state.settings.expiredThresholdDays,
        criticalDays = state.settings.criticalThresholdDays,
        warningDays = state.settings.warningThresholdDays,
        onUpdateItem = viewModel::updateItemLive,
        onOpenFullEditor = { item ->
            viewModel.closeItemDetail()
            viewModel.openEditItem(item)
        },
        onMessage = viewModel::notifyMessage,
        onDismiss = viewModel::closeItemDetail
    )
    } // end root Box

    // Add / Edit Bottom Sheet (hidden while inline form scanner is active)
    if (state.showItemSheet && !state.scannerForForm) {
        ItemEditorSheet(
            editingItem = state.editingItem,
            prefill = state.itemPrefill,
            onDismiss = viewModel::closeItemSheet,
            onScanBarcodeClick = {
                scannerUnlockCounter++
                viewModel.launchScannerForForm()
            },
            onSave = viewModel::saveItem,
            onValidationError = viewModel::notifyMessage
        )
    }

    // Quick Scan Action Bottom Sheet
    val quickItem = state.quickScanItem
    if (quickItem != null) {
        QuickScanActionSheet(
            item = quickItem,
            onAddStock = { amt -> viewModel.adjustQuantity(quickItem.id, amt, notifyQuickAdd = true) },
            onEditItem = viewModel::editFromQuickScan,
            onScanAnother = {
                viewModel.closeQuickScanSheet()
                scannerUnlockCounter++
            },
            onDismiss = {
                viewModel.closeQuickScanSheet()
                scannerUnlockCounter++
            }
        )
    }

    // Printable QR Label Dialog
    val qrItem = state.qrLabelItem
    if (qrItem != null) {
        QrLabelDialog(
            item = qrItem,
            onDismiss = viewModel::closeQrLabel,
            onMessage = viewModel::notifyMessage
        )
    }

    // Confirmation Dialog
    val confirm = state.pendingConfirm
    if (confirm != null) {
        ConfirmActionDialog(
            pending = confirm,
            onConfirm = viewModel::executePendingConfirm,
            onDismiss = viewModel::dismissConfirm
        )
    }

    // Auto-Update Dialog (read-only GitHub Releases)
    val update = state.availableUpdate
    if (update != null) {
        AppUpdateDialog(
            update = update,
            isDownloading = state.isDownloadingUpdate,
            onInstall = viewModel::downloadAndInstallUpdate,
            onDismiss = viewModel::dismissUpdateDialog
        )
    }

    // Family Group Sync Dialog (QR + Shareable Link, AES-256 E2E)
    if (state.showFamilyDialog) {
        FamilyGroupDialog(
            familyGroup = state.familyGroup,
            isSyncing = state.isSyncingFamily,
            onCreateGroup = viewModel::createFamilyGroup,
            onJoinGroup = viewModel::joinFamilyByInvite,
            onScanInviteQr = {
                viewModel.closeFamilyDialog()
                scannerUnlockCounter++
                viewModel.selectTab(BunkerTab.SCANNER)
            },
            onSyncNow = { viewModel.syncFamilyNow(silent = false) },
            onLeaveGroup = viewModel::leaveFamilyGroup,
            onDismiss = viewModel::closeFamilyDialog
        )
    }

    // Batch QR to Multi-Page A4 PDF Dialog
    if (state.showBatchQrDialog) {
        BatchQrPdfDialog(
            items = state.inventory,
            isBusy = isGeneratingBatchPdf,
            onSavePdf = { ids ->
                pendingBatchQrIds = ids
                val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                exportBatchPdfLauncher.launch("BunkerStock_QRs_$date.pdf")
            },
            onSharePdf = { ids ->
                appScope.launch {
                    isGeneratingBatchPdf = true
                    val items = viewModel.prepareItemsForBatchQr(ids)
                    val ok = withContext(Dispatchers.IO) {
                        QrCodeUtils.shareBatchQrPdf(
                            context = context,
                            items = items,
                            headerBrand = strings.qrHeaderBrand,
                            notApplicableLabel = strings.notApplicable,
                            expiredDays = state.settings.expiredThresholdDays,
                            criticalDays = state.settings.criticalThresholdDays,
                            warningDays = state.settings.warningThresholdDays
                        )
                    }
                    isGeneratingBatchPdf = false
                    if (ok) {
                        viewModel.closeBatchQrDialog()
                        viewModel.notifyMessage(strings.toastBatchPdfShared)
                    } else {
                        viewModel.notifyMessage(strings.toastExportError)
                    }
                }
            },
            onDismiss = viewModel::closeBatchQrDialog
        )
    }

    // First-Time Setup Language Selection Dialog (Spanish / English)
    if (!state.settings.onboardingCompleted) {
        FirstTimeSetupDialog(
            selectedLanguage = state.settings.language,
            onLanguageSelect = viewModel::setLanguage,
            onComplete = viewModel::completeOnboarding
        )
    }
}
