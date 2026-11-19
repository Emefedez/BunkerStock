package com.bunkerstock.app

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bunkerstock.app.data.AppLanguage
import com.bunkerstock.app.data.AppThemeMode
import com.bunkerstock.app.ui.BunkerTab
import com.bunkerstock.app.ui.BunkerViewModel
import com.bunkerstock.app.ui.components.ConfirmActionDialog
import com.bunkerstock.app.ui.components.ItemEditorSheet
import com.bunkerstock.app.ui.components.QrLabelDialog
import com.bunkerstock.app.ui.components.QuickScanActionSheet
import com.bunkerstock.app.ui.screens.ScannerScreen
import com.bunkerstock.app.ui.screens.SettingsScreen
import com.bunkerstock.app.ui.screens.WarehouseScreen
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
    var scannerUnlockCounter by remember { mutableIntStateOf(0) }
    var dataMenuExpanded by remember { mutableStateOf(false) }

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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.canvas,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.cardSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    if (state.scannerForForm) {
                        IconButton(onClick = { viewModel.closeFormScanner() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = strings.cancel)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .padding(start = 12.dp, end = 6.dp)
                                .size(38.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_bunker_shield),
                                contentDescription = null,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                },
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = strings.brandBunker,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                            )
                            Text(
                                text = strings.brandStock,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = colors.emerald
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colors.cardElevated,
                                border = BorderStroke(1.dp, colors.borderSubtle)
                            ) {
                                Text(
                                    text = "V2.4",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    ),
                                    color = colors.mutedText,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isExpandedWidth) "${strings.brandSubtitle} · $todayFormatted" else todayFormatted,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = colors.mutedText
                        )
                    }
                },
                actions = {
                    if (!state.scannerForForm && state.activeTab == BunkerTab.WAREHOUSE) {
                        IconButton(
                            onClick = {
                                scannerUnlockCounter++
                                viewModel.selectTab(BunkerTab.SCANNER)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = strings.scanCode,
                                tint = colors.amber
                            )
                        }

                        Box {
                            IconButton(onClick = { dataMenuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = strings.dataMenu
                                )
                            }
                            DropdownMenu(
                                expanded = dataMenuExpanded,
                                onDismissRequest = { dataMenuExpanded = false }
                            ) {
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
            )
        },
        bottomBar = {
            if (!isExpandedWidth && !state.scannerForForm) {
                NavigationBar(containerColor = colors.cardSurface) {
                    navItems.forEach { (tab, label, icon) ->
                        NavigationBarItem(
                            selected = state.activeTab == tab,
                            onClick = {
                                if (tab == BunkerTab.SCANNER) scannerUnlockCounter++
                                viewModel.selectTab(tab)
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (!state.scannerForForm && state.activeTab == BunkerTab.WAREHOUSE) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openAddItem() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
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
                            label = { Text(label) }
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
                when {
                    state.scannerForForm || state.activeTab == BunkerTab.SCANNER -> {
                        ScannerScreen(
                            isLookingUp = state.isLookingUpProduct,
                            unlockTrigger = scannerUnlockCounter,
                            onCodeScanned = { code -> viewModel.onCodeScanned(code) }
                        )
                    }

                    state.activeTab == BunkerTab.WAREHOUSE -> {
                        WarehouseScreen(
                            state = state,
                            isExpandedWidth = isExpandedWidth,
                            onSearchChange = viewModel::updateSearch,
                            onCategorySelect = viewModel::selectCategory,
                            onStatusSelect = viewModel::selectStatus,
                            onSortSelect = viewModel::selectSort,
                            onQuickFilterAlerts = viewModel::quickFilterAlerts,
                            onResetFilters = viewModel::resetFilters,
                            onDismissBanner = viewModel::dismissBanner,
                            onAdjustQty = { id, delta -> viewModel.adjustQuantity(id, delta) },
                            onOpenQr = viewModel::openQrLabel,
                            onEditItem = viewModel::openEditItem,
                            onDeleteItem = viewModel::requestDeleteItem
                        )
                    }

                    state.activeTab == BunkerTab.SETTINGS -> {
                        SettingsScreen(
                            settings = state.settings,
                            onLanguageChange = viewModel::setLanguage,
                            onThemeModeChange = viewModel::setThemeMode,
                            onWaterConfigChange = viewModel::setWaterConfig,
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
}
