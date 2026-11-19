package com.bunkerstock.app.ui

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bunkerstock.app.data.AppLanguage
import com.bunkerstock.app.data.AppSettings
import com.bunkerstock.app.data.AppThemeMode
import com.bunkerstock.app.data.AppUpdateInfo
import com.bunkerstock.app.data.BunkerRepository
import com.bunkerstock.app.data.DashboardMetrics
import com.bunkerstock.app.data.DetectedScanCandidate
import com.bunkerstock.app.data.InventoryItem
import com.bunkerstock.app.data.ItemPrefill
import com.bunkerstock.app.data.OpenFoodFactsClient
import com.bunkerstock.app.data.SortOption
import com.bunkerstock.app.data.StatusFilter
import com.bunkerstock.app.data.UpdateChecker
import com.bunkerstock.app.domain.InventoryLogic
import com.bunkerstock.app.ui.theme.stringsFor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

enum class BunkerTab {
    WAREHOUSE,
    SCANNER,
    SETTINGS
}

sealed interface PendingConfirm {
    data class DeleteItem(val item: InventoryItem) : PendingConfirm
    data object RestoreDemo : PendingConfirm
    data object ClearAll : PendingConfirm
}

data class BunkerUiState(
    val inventory: List<InventoryItem> = emptyList(),
    val filteredItems: List<InventoryItem> = emptyList(),
    val metrics: DashboardMetrics = DashboardMetrics(),
    val settings: AppSettings = AppSettings(),
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val selectedStatus: StatusFilter = StatusFilter.ALL,
    val sortOption: SortOption = SortOption.EXPIRY_ASC,
    val bannerDismissed: Boolean = false,
    val activeTab: BunkerTab = BunkerTab.WAREHOUSE,
    val showItemSheet: Boolean = false,
    val editingItem: InventoryItem? = null,
    val itemPrefill: ItemPrefill? = null,
    val scannerForForm: Boolean = false,
    val isLookingUpProduct: Boolean = false,
    val detectedCandidate: DetectedScanCandidate? = null,
    val quickScanItemId: String? = null,
    val qrLabelItemId: String? = null,
    val pendingConfirm: PendingConfirm? = null,
    val availableUpdate: AppUpdateInfo? = null,
    val isDownloadingUpdate: Boolean = false
) {
    val hasActiveFilters: Boolean
        get() = searchQuery.isNotBlank() || selectedCategory != null || selectedStatus != StatusFilter.ALL

    val quickScanItem: InventoryItem?
        get() = quickScanItemId?.let { id -> inventory.firstOrNull { it.id == id } }

    val detectedExistingItem: InventoryItem?
        get() = detectedCandidate?.existingItemId?.let { id -> inventory.firstOrNull { it.id == id } }

    val qrLabelItem: InventoryItem?
        get() = qrLabelItemId?.let { id -> inventory.firstOrNull { it.id == id } }
}

private data class LocalUiFilters(
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val selectedStatus: StatusFilter = StatusFilter.ALL,
    val sortOption: SortOption = SortOption.EXPIRY_ASC,
    val bannerDismissed: Boolean = false,
    val activeTab: BunkerTab = BunkerTab.WAREHOUSE
)

private data class ModalState(
    val showItemSheet: Boolean = false,
    val editingItem: InventoryItem? = null,
    val itemPrefill: ItemPrefill? = null,
    val scannerForForm: Boolean = false,
    val isLookingUpProduct: Boolean = false,
    val detectedCandidate: DetectedScanCandidate? = null,
    val quickScanItemId: String? = null,
    val qrLabelItemId: String? = null,
    val pendingConfirm: PendingConfirm? = null,
    val availableUpdate: AppUpdateInfo? = null,
    val isDownloadingUpdate: Boolean = false
)

class BunkerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BunkerRepository(application.applicationContext)
    private val filtersFlow = MutableStateFlow(LocalUiFilters())
    private val modalFlow = MutableStateFlow(ModalState())

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents = _toastEvents.asSharedFlow()

    val uiState: StateFlow<BunkerUiState> = combine(
        repository.inventoryFlow,
        repository.settingsFlow,
        filtersFlow,
        modalFlow
    ) { inventory, settings, filters, modals ->
        val metrics = InventoryLogic.computeDashboardMetrics(
            items = inventory,
            persons = settings.waterPersons,
            litersPerPersonDay = settings.waterLitersPerPersonDay
        )
        val filtered = InventoryLogic.filterAndSort(
            items = inventory,
            query = filters.searchQuery,
            categoryFilter = filters.selectedCategory,
            statusFilter = filters.selectedStatus,
            sortOption = filters.sortOption
        )
        BunkerUiState(
            inventory = inventory,
            filteredItems = filtered,
            metrics = metrics,
            settings = settings,
            searchQuery = filters.searchQuery,
            selectedCategory = filters.selectedCategory,
            selectedStatus = filters.selectedStatus,
            sortOption = filters.sortOption,
            bannerDismissed = filters.bannerDismissed,
            activeTab = filters.activeTab,
            showItemSheet = modals.showItemSheet,
            editingItem = modals.editingItem,
            itemPrefill = modals.itemPrefill,
            scannerForForm = modals.scannerForForm,
            isLookingUpProduct = modals.isLookingUpProduct,
            detectedCandidate = modals.detectedCandidate,
            quickScanItemId = modals.quickScanItemId,
            qrLabelItemId = modals.qrLabelItemId,
            pendingConfirm = modals.pendingConfirm,
            availableUpdate = modals.availableUpdate,
            isDownloadingUpdate = modals.isDownloadingUpdate
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BunkerUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
            val settings = uiState.value.settings
            if (settings.autoCheckUpdates) {
                checkForUpdates(silent = true)
            }
        }
    }

    private fun strings() = stringsFor(uiState.value.settings.language)

    private fun emitToast(msg: String) {
        _toastEvents.tryEmit(msg)
    }

    // Navigation
    fun selectTab(tab: BunkerTab) {
        filtersFlow.update { it.copy(activeTab = tab) }
        if (tab != BunkerTab.SCANNER) {
            modalFlow.update { it.copy(scannerForForm = false) }
        }
    }

    // Filters & Search
    fun updateSearch(query: String) {
        filtersFlow.update { it.copy(searchQuery = query) }
    }

    fun selectCategory(category: String?) {
        filtersFlow.update { it.copy(selectedCategory = category) }
    }

    fun selectStatus(status: StatusFilter) {
        filtersFlow.update { it.copy(selectedStatus = status) }
    }

    fun selectSort(sort: SortOption) {
        filtersFlow.update { it.copy(sortOption = sort) }
    }

    fun quickFilterAlerts() {
        filtersFlow.update {
            it.copy(
                activeTab = BunkerTab.WAREHOUSE,
                selectedCategory = null,
                selectedStatus = StatusFilter.ALERT_ALL,
                sortOption = SortOption.EXPIRY_ASC
            )
        }
    }

    fun resetFilters() {
        filtersFlow.update {
            it.copy(
                searchQuery = "",
                selectedCategory = null,
                selectedStatus = StatusFilter.ALL,
                sortOption = SortOption.EXPIRY_ASC
            )
        }
    }

    fun dismissBanner() {
        filtersFlow.update { it.copy(bannerDismissed = true) }
    }

    // CRUD
    fun openAddItem(prefill: ItemPrefill? = null) {
        modalFlow.update {
            it.copy(
                showItemSheet = true,
                editingItem = null,
                itemPrefill = prefill,
                scannerForForm = false
            )
        }
    }

    fun openEditItem(item: InventoryItem) {
        modalFlow.update {
            it.copy(
                showItemSheet = true,
                editingItem = item,
                itemPrefill = null,
                scannerForForm = false
            )
        }
    }

    fun closeItemSheet() {
        modalFlow.update {
            it.copy(
                showItemSheet = false,
                editingItem = null,
                itemPrefill = null,
                scannerForForm = false
            )
        }
    }

    fun launchScannerForForm() {
        modalFlow.update { it.copy(scannerForForm = true) }
    }

    fun closeFormScanner() {
        modalFlow.update { it.copy(scannerForForm = false) }
    }

    fun saveItem(item: InventoryItem, isEdit: Boolean) {
        viewModelScope.launch {
            val current = uiState.value.inventory.toMutableList()
            if (isEdit) {
                val idx = current.indexOfFirst { it.id == item.id }
                if (idx >= 0) {
                    current[idx] = item
                }
                emitToast(strings().toastUpdated)
            } else {
                current.add(0, item)
                emitToast(strings().toastAdded)
            }
            repository.saveInventory(current)
            filtersFlow.update { it.copy(bannerDismissed = false) }
            closeItemSheet()
        }
    }

    fun adjustQuantity(itemId: String, delta: Double, notifyQuickAdd: Boolean = false) {
        viewModelScope.launch {
            val current = uiState.value.inventory.toMutableList()
            val idx = current.indexOfFirst { it.id == itemId }
            if (idx < 0) return@launch
            val existing = current[idx]
            val newQty = max(0.0, ((existing.qty + delta) * 100.0).roundToInt() / 100.0)
            current[idx] = existing.copy(qty = newQty)
            repository.saveInventory(current)
            if (notifyQuickAdd) {
                emitToast(
                    strings().toastQuickAdded(
                        InventoryLogic.formatQty(delta),
                        existing.name,
                        InventoryLogic.formatQty(newQty)
                    )
                )
            }
        }
    }

    fun requestDeleteItem(item: InventoryItem) {
        modalFlow.update { it.copy(pendingConfirm = PendingConfirm.DeleteItem(item)) }
    }

    fun requestRestoreDemo() {
        modalFlow.update { it.copy(pendingConfirm = PendingConfirm.RestoreDemo) }
    }

    fun requestClearAll() {
        modalFlow.update { it.copy(pendingConfirm = PendingConfirm.ClearAll) }
    }

    fun dismissConfirm() {
        modalFlow.update { it.copy(pendingConfirm = null) }
    }

    fun executePendingConfirm() {
        val action = uiState.value.pendingConfirm ?: return
        dismissConfirm()
        viewModelScope.launch {
            when (action) {
                is PendingConfirm.DeleteItem -> {
                    val updated = uiState.value.inventory.filterNot { it.id == action.item.id }
                    repository.saveInventory(updated)
                    emitToast(strings().toastDeleted)
                }
                PendingConfirm.RestoreDemo -> {
                    repository.saveInventory(InventoryLogic.getDefaultDemoData())
                    filtersFlow.update { it.copy(bannerDismissed = false) }
                    resetFilters()
                    emitToast(strings().toastDemoLoaded)
                }
                PendingConfirm.ClearAll -> {
                    repository.saveInventory(emptyList())
                    emitToast(strings().toastCleared)
                }
            }
        }
    }

    // Optical Scan Handling (non-blocking bottom popup keeps camera live!)
    fun onCodeScanned(rawCode: String) {
        val code = rawCode.trim()
        if (code.isEmpty()) return

        playBeepAndHaptic()

        val state = uiState.value
        val (gs1Expiry, gs1Batch) = InventoryLogic.extractGs1ExpiryAndBatch(code)

        if (state.scannerForForm) {
            val structured = InventoryLogic.parseStructuredQr(code)
            val prefill = structured?.copy(
                expiryDate = structured.expiryDate ?: gs1Expiry,
                notes = buildString {
                    append(structured.notes)
                    if (!gs1Batch.isNullOrBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append("Lote: $gs1Batch")
                    }
                }
            ) ?: (state.itemPrefill?.copy(
                barcode = code,
                expiryDate = gs1Expiry ?: state.itemPrefill.expiryDate
            ) ?: ItemPrefill(
                barcode = code,
                expiryDate = gs1Expiry,
                notes = buildString {
                    if (gs1Expiry != null) append(strings().expiryGs1Note)
                    if (!gs1Batch.isNullOrBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append("Lote: $gs1Batch")
                    }
                }
            ))
            modalFlow.update {
                it.copy(
                    scannerForForm = false,
                    itemPrefill = prefill
                )
            }
            emitToast(
                if (structured != null) strings().toastQrApplied
                else strings().toastCodeAssigned(code)
            )
            return
        }

        // Global scan: DO NOT close camera — show non-blocking bottom popup!
        val existing = state.inventory.firstOrNull {
            it.barcode.isNotBlank() && it.barcode.equals(code, ignoreCase = true)
        }
        if (existing != null) {
            modalFlow.update {
                it.copy(
                    detectedCandidate = DetectedScanCandidate(
                        code = code,
                        existingItemId = existing.id,
                        isLookingUp = false
                    )
                )
            }
            return
        }

        val structured = InventoryLogic.parseStructuredQr(code)
        if (structured != null) {
            val withGs1 = structured.copy(
                expiryDate = structured.expiryDate ?: gs1Expiry,
                notes = buildString {
                    append(structured.notes)
                    if (!gs1Batch.isNullOrBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append("Lote: $gs1Batch")
                    }
                }
            )
            modalFlow.update {
                it.copy(
                    detectedCandidate = DetectedScanCandidate(
                        code = code,
                        prefill = withGs1,
                        isLookingUp = false
                    )
                )
            }
            return
        }

        // Show immediate bottom popup for new code while enriching via Open Food Facts
        val initialPrefill = ItemPrefill(
            barcode = code,
            expiryDate = gs1Expiry,
            notes = buildString {
                if (gs1Expiry != null) append(strings().expiryGs1Note)
                if (!gs1Batch.isNullOrBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append("Lote: $gs1Batch")
                }
            }
        )
        modalFlow.update {
            it.copy(
                isLookingUpProduct = true,
                detectedCandidate = DetectedScanCandidate(
                    code = code,
                    prefill = initialPrefill,
                    isLookingUp = true
                )
            )
        }

        viewModelScope.launch {
            val off = OpenFoodFactsClient.lookup(code)
            modalFlow.update { current ->
                // Only update popup if user hasn't dismissed or scanned another code
                if (current.detectedCandidate?.code != code) {
                    current.copy(isLookingUpProduct = false)
                } else if (off != null) {
                    val estDays = InventoryLogic.estimateShelfLifeDays(
                        off.name,
                        off.category,
                        off.nutrition?.safetyFlags.orEmpty()
                    )
                    val resolvedExpiry = gs1Expiry ?: InventoryLogic.relativeDateString(estDays)
                    val noteText = buildString {
                        if (gs1Expiry != null) {
                            append(strings().expiryGs1Note)
                        } else {
                            append(strings().expiryEstimatedNote(estDays))
                        }
                        if (!gs1Batch.isNullOrBlank()) append(" · Lote: $gs1Batch")
                    }
                    current.copy(
                        isLookingUpProduct = false,
                        detectedCandidate = DetectedScanCandidate(
                            code = code,
                            prefill = ItemPrefill(
                                barcode = code,
                                name = off.name,
                                category = off.category,
                                unit = off.unit,
                                expiryDate = resolvedExpiry,
                                notes = noteText,
                                nutrition = off.nutrition
                            ),
                            isLookingUp = false
                        )
                    )
                } else {
                    current.copy(
                        isLookingUpProduct = false,
                        detectedCandidate = current.detectedCandidate.copy(isLookingUp = false)
                    )
                }
            }
        }
    }

    fun dismissDetectedCandidate() {
        modalFlow.update { it.copy(detectedCandidate = null, isLookingUpProduct = false) }
    }

    fun confirmDetectedCandidate() {
        val candidate = uiState.value.detectedCandidate ?: return
        modalFlow.update { it.copy(detectedCandidate = null) }
        val existing = candidate.existingItemId?.let { id ->
            uiState.value.inventory.firstOrNull { it.id == id }
        }
        if (existing != null) {
            openEditItem(existing)
        } else {
            openAddItem(candidate.prefill ?: ItemPrefill(barcode = candidate.code))
        }
    }

    fun closeQuickScanSheet() {
        modalFlow.update { it.copy(quickScanItemId = null) }
    }

    fun editFromQuickScan() {
        val item = uiState.value.quickScanItem ?: return
        modalFlow.update { it.copy(quickScanItemId = null) }
        openEditItem(item)
    }

    // QR Label Modal
    fun openQrLabel(itemId: String) {
        viewModelScope.launch {
            val current = uiState.value.inventory.toMutableList()
            val idx = current.indexOfFirst { it.id == itemId }
            if (idx < 0) return@launch
            val item = current[idx]
            if (item.barcode.isBlank()) {
                val generated = "BNK-" + System.currentTimeMillis().toString().takeLast(6)
                current[idx] = item.copy(barcode = generated)
                repository.saveInventory(current)
            }
            modalFlow.update { it.copy(qrLabelItemId = itemId) }
        }
    }

    fun closeQrLabel() {
        modalFlow.update { it.copy(qrLabelItemId = null) }
    }

    // Export / Import
    fun buildJsonExport(): String = InventoryLogic.exportToJson(uiState.value.inventory)

    fun buildCsvExport(): String {
        val s = strings()
        return InventoryLogic.exportToCsv(
            items = uiState.value.inventory,
            noExpiryText = s.noExpiryCsv,
            statusLabelProvider = { info -> s.expiryLabel(info) }
        )
    }

    fun importJsonText(raw: String) {
        viewModelScope.launch {
            val result = InventoryLogic.importFromJson(raw)
            result.onSuccess { items ->
                repository.saveInventory(items)
                filtersFlow.update { it.copy(bannerDismissed = false) }
                emitToast(strings().toastJsonImported(items.size))
            }.onFailure {
                emitToast(strings().toastJsonInvalid)
            }
        }
    }

    fun notifyMessage(msg: String) = emitToast(msg)

    // Settings
    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { repository.updateLanguage(language) }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { repository.updateThemeMode(mode) }
    }

    fun setWaterConfig(persons: Int, litersPerDay: Int) {
        viewModelScope.launch { repository.updateWaterConfig(persons, litersPerDay) }
    }

    fun setAutoCheckUpdates(enabled: Boolean) {
        viewModelScope.launch { repository.updateAutoCheckUpdates(enabled) }
    }

    fun checkForUpdates(silent: Boolean = false) {
        viewModelScope.launch {
            val update = UpdateChecker.checkForUpdate()
            if (update != null) {
                modalFlow.update { it.copy(availableUpdate = update) }
            } else if (!silent) {
                emitToast(strings().updateUpToDate)
            }
        }
    }

    fun dismissUpdateDialog() {
        modalFlow.update { it.copy(availableUpdate = null, isDownloadingUpdate = false) }
    }

    fun downloadAndInstallUpdate() {
        val update = uiState.value.availableUpdate ?: return
        viewModelScope.launch {
            modalFlow.update { it.copy(isDownloadingUpdate = true) }
            emitToast(strings().updateDownloading)
            val ok = UpdateChecker.downloadAndInstallApk(getApplication(), update)
            modalFlow.update { it.copy(isDownloadingUpdate = false) }
            if (ok) {
                dismissUpdateDialog()
            } else {
                emitToast(strings().updateFailed)
            }
        }
    }

    private fun playBeepAndHaptic() {
        runCatching {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 130)
        }
        runCatching {
            val ctx = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = ctx.getSystemService(VibratorManager::class.java)
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(90L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = ctx.getSystemService(Vibrator::class.java)
                v?.vibrate(VibrationEffect.createOneShot(90L, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }
}
