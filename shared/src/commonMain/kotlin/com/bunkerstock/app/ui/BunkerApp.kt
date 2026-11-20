package com.bunkerstock.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bunkerstock.app.platform.BunkerPlatform
import com.bunkerstock.app.platform.LocalBunkerPlatform
import com.bunkerstock.app.platform.LocalScreenSizePx
import com.bunkerstock.app.platform.PlatformBackHandler
import com.bunkerstock.app.platform.isWebPlatform
import com.bunkerstock.app.platform.todayBunkerDate
import com.bunkerstock.app.ui.components.AppUpdateDialog
import com.bunkerstock.app.ui.components.BatchQrPdfDialog
import com.bunkerstock.app.ui.components.ConfirmActionDialog
import com.bunkerstock.app.ui.components.FamilyGroupDialog
import com.bunkerstock.app.ui.components.FirstTimeSetupDialog
import com.bunkerstock.app.ui.components.ItemEditorSheet
import com.bunkerstock.app.ui.components.ProductDetailOverlay
import com.bunkerstock.app.ui.components.QrLabelDialog
import com.bunkerstock.app.ui.components.QuickScanActionSheet
import com.bunkerstock.app.ui.components.ShoppingHaulReviewOverlay
import com.bunkerstock.app.ui.screens.ScannerScreen
import com.bunkerstock.app.ui.screens.SettingsScreen
import com.bunkerstock.app.ui.screens.WarehouseScreen
import com.bunkerstock.app.ui.theme.BunkerGrainOverlay
import com.bunkerstock.app.ui.theme.BunkerRadius
import com.bunkerstock.app.ui.theme.BunkerShieldLogo
import com.bunkerstock.app.ui.theme.BunkerStockTheme
import com.bunkerstock.app.ui.theme.LocalAppStrings
import com.bunkerstock.app.ui.theme.LocalBunkerColors
import com.bunkerstock.app.ui.theme.stringsFor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BunkerStockRoot(
    controller: BunkerController,
    platform: BunkerPlatform,
    isExpandedWidth: Boolean
) {
    val state by controller.uiState.collectAsState()
    val strings = remember(state.settings.language) { stringsFor(state.settings.language) }
    var screenSize by remember { mutableStateOf(IntSize(1080, 2400)) }

    CompositionLocalProvider(
        LocalAppStrings provides strings,
        LocalBunkerPlatform provides platform,
        LocalScreenSizePx provides screenSize
    ) {
        BunkerStockTheme(themeMode = state.settings.themeMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { screenSize = it }
            ) {
                BunkerStockAppContent(
                    controller = controller,
                    state = state,
                    isExpandedWidth = isExpandedWidth
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BunkerStockAppContent(
    controller: BunkerController,
    state: BunkerUiState,
    isExpandedWidth: Boolean
) {
    val platform = LocalBunkerPlatform.current
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
    var isGeneratingBatchPdf by remember { mutableStateOf(false) }
    val appScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        controller.toastEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    fun triggerExportJson() {
        val date = todayBunkerDate().toIso()
        platform.exportJson("inventario_supervivencia_$date.json", controller.buildJsonExport()) { ok ->
            controller.notifyMessage(if (ok) strings.toastJsonExported else strings.toastExportError)
        }
    }

    fun triggerExportCsv() {
        val date = todayBunkerDate().toIso()
        platform.exportCsv("inventario_almacen_$date.csv", controller.buildCsvExport()) { ok ->
            controller.notifyMessage(if (ok) strings.toastCsvExported else strings.toastExportError)
        }
    }

    fun triggerImportJson() {
        platform.importJson { content ->
            if (content != null) controller.importJsonText(content)
            else controller.notifyMessage(strings.toastJsonInvalid)
        }
    }

    PlatformBackHandler(enabled = state.scannerForForm) {
        controller.closeFormScanner()
    }

    val todayFormatted = remember(state.settings.language) {
        todayBunkerDate().formatHeader(state.settings.language)
    }

    val navItems = listOf(
        Triple(BunkerTab.WAREHOUSE, strings.navWarehouse, Icons.Default.Inventory2),
        Triple(BunkerTab.SCANNER, strings.navScanner, Icons.Default.QrCodeScanner),
        Triple(BunkerTab.SETTINGS, strings.navSettings, Icons.Default.Settings)
    )

    // On Web/iOS Scanner tab, keep Scaffold/content container transparent so the HTML <video> behind canvas shines through!
    val isLiveWebScanner = isWebPlatform &&
        (state.scannerForForm || state.activeTab == BunkerTab.SCANNER) &&
        !state.showShoppingReview &&
        state.detailItem == null

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = if (isLiveWebScanner) Color.Transparent else colors.canvas,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
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
                                    onClick = { controller.closeFormScanner() },
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
                                    BunkerShieldLogo(modifier = Modifier.size(28.dp))
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
                                            text = platform.versionTag,
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
                                        onClick = { controller.syncFamilyNow(silent = false) },
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
                                        controller.selectTab(BunkerTab.SCANNER)
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
                                                controller.openBatchQrDialog()
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
                                                controller.requestRestoreDemo()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(strings.clearAll, color = colors.danger) },
                                            onClick = {
                                                dataMenuExpanded = false
                                                controller.requestClearAll()
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
                                                controller.selectTab(tab)
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
                        onClick = { controller.openAddItem() },
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
                                    controller.selectTab(tab)
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
                        .background(if (isLiveWebScanner) Color.Transparent else colors.canvas)
                ) {
                    if (!isLiveWebScanner) {
                        BunkerGrainOverlay(isDark = colors.isDark)
                    }

                    when {
                        state.scannerForForm || state.activeTab == BunkerTab.SCANNER -> {
                            ScannerScreen(
                                detectedCandidate = state.detectedCandidate,
                                detectedExistingItem = state.detectedExistingItem,
                                shoppingModeActive = state.shoppingModeActive,
                                shoppingEntries = state.shoppingEntries,
                                showShoppingToggle = !state.scannerForForm,
                                searchQuery = state.scannerSearchQuery,
                                localSearchMatches = state.scannerLocalMatches,
                                remoteSearchResults = state.scannerRemoteResults,
                                isSearchingRemote = state.isSearchingRemoteProducts,
                                unlockTrigger = scannerUnlockCounter,
                                onCodeScanned = controller::onCodeScanned,
                                onSearchQueryChange = controller::updateScannerSearchQuery,
                                onSearchSubmit = controller::submitScannerSearchBar,
                                onClearSearch = controller::clearScannerSearch,
                                onSelectLocalResult = controller::selectLocalSearchResult,
                                onSelectRemoteResult = controller::selectRemoteSearchResult,
                                onToggleShoppingMode = controller::toggleShoppingMode,
                                onOpenShoppingReview = controller::openShoppingReview,
                                onAdjustShoppingEntry = controller::adjustShoppingEntry,
                                onUndoShoppingEntry = controller::undoShoppingEntry,
                                onOpenShoppingItemDetail = { id -> controller.openItemDetail(id, 0.5f, 0.8f) },
                                onFinishShoppingSession = controller::finishShoppingSession,
                                onDismissCandidate = controller::dismissDetectedCandidate,
                                onConfirmCandidate = controller::confirmDetectedCandidate,
                                onQuickAddExisting = { id, amt -> controller.adjustQuantity(id, amt, notifyQuickAdd = true) },
                                onImageScanError = controller::notifyMessage
                            )
                        }

                        state.activeTab == BunkerTab.WAREHOUSE -> {
                            WarehouseScreen(
                                state = state,
                                isExpandedWidth = isExpandedWidth,
                                onSearchChange = controller::updateSearch,
                                onCategorySelect = controller::selectCategory,
                                onToggleCategory = controller::toggleCategory,
                                onStatusSelect = controller::selectStatus,
                                onToggleStatus = controller::toggleStatus,
                                onSortSelect = controller::selectSort,
                                onQuickFilterAlerts = controller::quickFilterAlerts,
                                onResetFilters = controller::resetFilters,
                                onDismissBanner = controller::dismissBanner,
                                onAdjustQty = { id, delta -> controller.adjustQuantity(id, delta) },
                                onOpenDetail = controller::openItemDetail,
                                onOpenQr = controller::openQrLabel,
                                onEditItem = controller::openEditItem,
                                onDeleteItem = controller::requestDeleteItem,
                                gridState = warehouseGridState
                            )
                        }

                        state.activeTab == BunkerTab.SETTINGS -> {
                            SettingsScreen(
                                settings = state.settings,
                                familyGroup = state.familyGroup,
                                isSyncingFamily = state.isSyncingFamily,
                                onOpenFamilyDialog = controller::openFamilyDialog,
                                onSyncFamilyNow = { controller.syncFamilyNow(silent = false) },
                                onFamilySyncPrefsChange = controller::setFamilySyncPrefs,
                                onUpdateMemberName = controller::updateFamilyMemberName,
                                onLanguageChange = controller::setLanguage,
                                onThemeModeChange = controller::setThemeMode,
                                onWaterConfigChange = controller::setWaterConfig,
                                onExpiryThresholdsChange = controller::setExpiryThresholds,
                                onExpiryNotifChange = controller::setExpiryNotifications,
                                onAutoUpdateChange = controller::setAutoCheckUpdates,
                                onCheckUpdatesNow = { controller.checkForUpdates(silent = false) },
                                onOpenBatchQr = controller::openBatchQrDialog,
                                onExportJson = ::triggerExportJson,
                                onExportCsv = ::triggerExportCsv,
                                onImportJson = ::triggerImportJson,
                                onRestoreDemo = controller::requestRestoreDemo,
                                onClearAll = controller::requestClearAll
                            )
                        }
                    }
                }
            }
        }

        // Stacked Shopping Haul Batch-Review & Grouping Overlay
        if (state.showShoppingReview && state.shoppingEntries.isNotEmpty()) {
            ShoppingHaulReviewOverlay(
                entries = state.shoppingEntries,
                inventory = state.inventory,
                expiredDays = state.settings.expiredThresholdDays,
                criticalDays = state.settings.criticalThresholdDays,
                warningDays = state.settings.warningThresholdDays,
                onAdjustEntry = controller::adjustShoppingEntry,
                onUndoEntry = controller::undoShoppingEntry,
                onOpenItemDetail = { id -> controller.openItemDetail(id, 0.5f, 0.5f) },
                onBatchUpdate = { ids, loc, exp, noExp, cat ->
                    controller.batchUpdateItems(ids, loc, exp, noExp, cat)
                },
                onKeepScanning = controller::closeShoppingReview,
                onFinishHaul = controller::finishShoppingSession
            )
        }

        // Full-Screen Animated Product Detail Page
        ProductDetailOverlay(
            item = state.detailItem,
            originX = state.detailOriginX,
            originY = state.detailOriginY,
            expiredDays = state.settings.expiredThresholdDays,
            criticalDays = state.settings.criticalThresholdDays,
            warningDays = state.settings.warningThresholdDays,
            onUpdateItem = controller::updateItemLive,
            onOpenFullEditor = { item ->
                controller.closeItemDetail()
                controller.openEditItem(item)
            },
            onMessage = controller::notifyMessage,
            onDismiss = controller::closeItemDetail
        )
    }

    if (state.showItemSheet && !state.scannerForForm) {
        ItemEditorSheet(
            editingItem = state.editingItem,
            prefill = state.itemPrefill,
            onDismiss = controller::closeItemSheet,
            onScanBarcodeClick = {
                scannerUnlockCounter++
                controller.launchScannerForForm()
            },
            onSave = controller::saveItem,
            onValidationError = controller::notifyMessage
        )
    }

    val quickItem = state.quickScanItem
    if (quickItem != null) {
        QuickScanActionSheet(
            item = quickItem,
            onAddStock = { amt -> controller.adjustQuantity(quickItem.id, amt, notifyQuickAdd = true) },
            onEditItem = controller::editFromQuickScan,
            onScanAnother = {
                controller.closeQuickScanSheet()
                scannerUnlockCounter++
            },
            onDismiss = {
                controller.closeQuickScanSheet()
                scannerUnlockCounter++
            }
        )
    }

    val qrItem = state.qrLabelItem
    if (qrItem != null) {
        QrLabelDialog(
            item = qrItem,
            onDismiss = controller::closeQrLabel,
            onMessage = controller::notifyMessage
        )
    }

    val confirm = state.pendingConfirm
    if (confirm != null) {
        ConfirmActionDialog(
            pending = confirm,
            onConfirm = controller::executePendingConfirm,
            onDismiss = controller::dismissConfirm
        )
    }

    val update = state.availableUpdate
    if (update != null) {
        AppUpdateDialog(
            update = update,
            isDownloading = state.isDownloadingUpdate,
            onInstall = controller::downloadAndInstallUpdate,
            onDismiss = controller::dismissUpdateDialog
        )
    }

    if (state.showFamilyDialog) {
        FamilyGroupDialog(
            familyGroup = state.familyGroup,
            isSyncing = state.isSyncingFamily,
            onCreateGroup = controller::createFamilyGroup,
            onJoinGroup = controller::joinFamilyByInvite,
            onScanInviteQr = {
                controller.closeFamilyDialog()
                scannerUnlockCounter++
                controller.selectTab(BunkerTab.SCANNER)
            },
            onSyncNow = { controller.syncFamilyNow(silent = false) },
            onLeaveGroup = controller::leaveFamilyGroup,
            onDismiss = controller::closeFamilyDialog
        )
    }

    if (state.showBatchQrDialog) {
        BatchQrPdfDialog(
            items = state.inventory,
            isBusy = isGeneratingBatchPdf,
            onSavePdf = { ids ->
                appScope.launch {
                    isGeneratingBatchPdf = true
                    val items = controller.prepareItemsForBatchQr(ids)
                    val date = todayBunkerDate().toIso()
                    platform.saveBatchQrPdf(
                        suggestedFileName = "BunkerStock_QRs_$date.pdf",
                        items = items,
                        headerBrand = strings.qrHeaderBrand,
                        notApplicableLabel = strings.notApplicable,
                        expiredDays = state.settings.expiredThresholdDays,
                        criticalDays = state.settings.criticalThresholdDays,
                        warningDays = state.settings.warningThresholdDays
                    ) { ok ->
                        isGeneratingBatchPdf = false
                        if (ok) {
                            controller.closeBatchQrDialog()
                            controller.notifyMessage(strings.toastBatchPdfSaved)
                        } else {
                            controller.notifyMessage(strings.toastExportError)
                        }
                    }
                }
            },
            onSharePdf = { ids ->
                appScope.launch {
                    isGeneratingBatchPdf = true
                    val items = controller.prepareItemsForBatchQr(ids)
                    platform.shareBatchQrPdf(
                        items = items,
                        headerBrand = strings.qrHeaderBrand,
                        notApplicableLabel = strings.notApplicable,
                        expiredDays = state.settings.expiredThresholdDays,
                        criticalDays = state.settings.criticalThresholdDays,
                        warningDays = state.settings.warningThresholdDays
                    ) { ok ->
                        isGeneratingBatchPdf = false
                        if (ok) {
                            controller.closeBatchQrDialog()
                            controller.notifyMessage(strings.toastBatchPdfShared)
                        } else {
                            controller.notifyMessage(strings.toastExportError)
                        }
                    }
                }
            },
            onDismiss = controller::closeBatchQrDialog
        )
    }

    if (!state.settings.onboardingCompleted) {
        FirstTimeSetupDialog(
            selectedLanguage = state.settings.language,
            onLanguageSelect = controller::setLanguage,
            onComplete = controller::completeOnboarding
        )
    }
}
