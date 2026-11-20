package com.bunkerstock.app.ui

import com.bunkerstock.app.data.AppLanguage
import com.bunkerstock.app.data.AppSettings
import com.bunkerstock.app.data.AppThemeMode
import com.bunkerstock.app.data.AppUpdateInfo
import com.bunkerstock.app.data.DashboardMetrics
import com.bunkerstock.app.data.DetectedScanCandidate
import com.bunkerstock.app.data.FamilyGroupConfig
import com.bunkerstock.app.data.FamilySyncClient
import com.bunkerstock.app.data.InventoryItem
import com.bunkerstock.app.data.ItemCategory
import com.bunkerstock.app.data.ItemPrefill
import com.bunkerstock.app.data.OpenFoodFactsClient
import com.bunkerstock.app.data.OpenFoodFactsResult
import com.bunkerstock.app.data.ShoppingScanEntry
import com.bunkerstock.app.data.SortOption
import com.bunkerstock.app.data.StatusFilter
import com.bunkerstock.app.domain.InventoryLogic
import com.bunkerstock.app.platform.BunkerPlatform
import com.bunkerstock.app.platform.BunkerStorage
import com.bunkerstock.app.platform.currentTimeMillis
import com.bunkerstock.app.platform.isWebPlatform
import com.bunkerstock.app.ui.theme.stringsFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    val selectedCategories: Set<String> = emptySet(),
    val selectedStatuses: Set<StatusFilter> = emptySet(),
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
    val detailItemId: String? = null,
    val detailOriginX: Float = 0.5f,
    val detailOriginY: Float = 0.5f,
    val showBatchQrDialog: Boolean = false,
    val shoppingModeActive: Boolean = false,
    val shoppingEntries: List<ShoppingScanEntry> = emptyList(),
    val showShoppingReview: Boolean = false,
    val scannerSearchQuery: String = "",
    val scannerRemoteResults: List<OpenFoodFactsResult> = emptyList(),
    val isSearchingRemoteProducts: Boolean = false,
    val pendingConfirm: PendingConfirm? = null,
    val availableUpdate: AppUpdateInfo? = null,
    val isDownloadingUpdate: Boolean = false,
    val familyGroup: FamilyGroupConfig = FamilyGroupConfig(),
    val isSyncingFamily: Boolean = false,
    val showFamilyDialog: Boolean = false
) {
    val hasActiveFilters: Boolean
        get() = searchQuery.isNotBlank() || selectedCategories.isNotEmpty() || selectedStatuses.isNotEmpty()

    val activeFilterCount: Int
        get() = selectedCategories.size + selectedStatuses.size

    val quickScanItem: InventoryItem?
        get() = quickScanItemId?.let { id -> inventory.firstOrNull { it.id == id } }

    val detectedExistingItem: InventoryItem?
        get() = detectedCandidate?.existingItemId?.let { id -> inventory.firstOrNull { it.id == id } }

    val qrLabelItem: InventoryItem?
        get() = qrLabelItemId?.let { id -> inventory.firstOrNull { it.id == id } }

    val detailItem: InventoryItem?
        get() = detailItemId?.let { id -> inventory.firstOrNull { it.id == id } }

    val scannerLocalMatches: List<InventoryItem>
        get() {
            val q = scannerSearchQuery.trim().lowercase()
            if (q.length < 2) return emptyList()
            return inventory.filter { item ->
                item.name.lowercase().contains(q) ||
                    item.barcode.lowercase().contains(q) ||
                    item.location.lowercase().contains(q)
            }.take(6)
        }
}

private data class LocalUiFilters(
    val searchQuery: String = "",
    val selectedCategories: Set<String> = emptySet(),
    val selectedStatuses: Set<StatusFilter> = emptySet(),
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
    val detailItemId: String? = null,
    val detailOriginX: Float = 0.5f,
    val detailOriginY: Float = 0.5f,
    val showBatchQrDialog: Boolean = false,
    val shoppingModeActive: Boolean = false,
    val shoppingEntries: List<ShoppingScanEntry> = emptyList(),
    val showShoppingReview: Boolean = false,
    val scannerSearchQuery: String = "",
    val scannerRemoteResults: List<OpenFoodFactsResult> = emptyList(),
    val isSearchingRemoteProducts: Boolean = false,
    val pendingConfirm: PendingConfirm? = null,
    val availableUpdate: AppUpdateInfo? = null,
    val isDownloadingUpdate: Boolean = false,
    val isSyncingFamily: Boolean = false,
    val showFamilyDialog: Boolean = false
)

class BunkerController(
    private val scope: CoroutineScope,
    private val storage: BunkerStorage,
    private val platform: BunkerPlatform
) {
    private val filtersFlow = MutableStateFlow(LocalUiFilters())
    private val modalFlow = MutableStateFlow(ModalState())
    private val appForegroundFlow = MutableStateFlow(true)

    // Serializes all read-modify-write inventory updates so concurrent Shopping Mode
    // Open Food Facts enrichments NEVER overwrite each other's calories/macros!
    private val inventoryMutex = Mutex()
    private val syncMutex = Mutex()
    private var lastRemoteCipherHash: String = ""
    private var lastLocalStateHash: String = ""
    private val shoppingCooldownByCode = mutableMapOf<String, Long>()
    private var idSeqCounter: Long = 0L
    private var scannerSearchJob: Job? = null

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents = _toastEvents.asSharedFlow()

    val uiState: StateFlow<BunkerUiState> = combine(
        storage.inventoryFlow,
        storage.settingsFlow,
        storage.familyGroupFlow,
        filtersFlow,
        modalFlow
    ) { inventory, settings, familyGroup, filters, modals ->
        val metrics = InventoryLogic.computeDashboardMetrics(
            items = inventory,
            persons = settings.waterPersons,
            litersPerPersonDay = settings.waterLitersPerPersonDay,
            expiredDays = settings.expiredThresholdDays,
            criticalDays = settings.criticalThresholdDays,
            warningDays = settings.warningThresholdDays
        )
        val filtered = InventoryLogic.filterAndSortMulti(
            items = inventory,
            query = filters.searchQuery,
            categories = filters.selectedCategories,
            statuses = filters.selectedStatuses,
            sortOption = filters.sortOption,
            expiredDays = settings.expiredThresholdDays,
            criticalDays = settings.criticalThresholdDays,
            warningDays = settings.warningThresholdDays
        )
        BunkerUiState(
            inventory = inventory,
            filteredItems = filtered,
            metrics = metrics,
            settings = settings,
            searchQuery = filters.searchQuery,
            selectedCategories = filters.selectedCategories,
            selectedStatuses = filters.selectedStatuses,
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
            detailItemId = modals.detailItemId,
            detailOriginX = modals.detailOriginX,
            detailOriginY = modals.detailOriginY,
            showBatchQrDialog = modals.showBatchQrDialog,
            shoppingModeActive = modals.shoppingModeActive,
            shoppingEntries = modals.shoppingEntries,
            showShoppingReview = modals.showShoppingReview,
            scannerSearchQuery = modals.scannerSearchQuery,
            scannerRemoteResults = modals.scannerRemoteResults,
            isSearchingRemoteProducts = modals.isSearchingRemoteProducts,
            pendingConfirm = modals.pendingConfirm,
            availableUpdate = modals.availableUpdate,
            isDownloadingUpdate = modals.isDownloadingUpdate,
            familyGroup = familyGroup,
            isSyncingFamily = modals.isSyncingFamily,
            showFamilyDialog = modals.showFamilyDialog
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BunkerUiState()
    )

    init {
        scope.launch {
            storage.ensureInitialized()
            platform.schedulePeriodicExpiryCheck()
            platform.evaluateExpiryNotifications()
            val family = storage.familyGroupFlow.first()
            if (family.isJoined) {
                syncFamilyNow(silent = true)
            }
            val settings = storage.settingsFlow.first()
            if (settings.autoCheckUpdates && platform.supportsApkSelfUpdate) {
                checkForUpdates(silent = true)
            }
        }

        // Foreground diff-polling loop: checks for remote family diffs every N seconds while open
        scope.launch {
            combine(
                appForegroundFlow,
                storage.familyGroupFlow,
                storage.settingsFlow
            ) { fg, family, settings ->
                Triple(fg && family.isJoined && settings.familyAutoSyncEnabled, settings.familySyncIntervalSec, family.roomId)
            }
                .distinctUntilChanged()
                .collectLatest { (shouldPoll, intervalSec, _) ->
                    if (!shouldPoll) return@collectLatest
                    val delayMs = intervalSec.coerceIn(5, 300) * 1000L
                    while (true) {
                        delay(delayMs)
                        syncFamilyNow(silent = true, isBackgroundPoll = true)
                    }
                }
        }
    }

    private fun nextUniqueId(prefix: String = "item"): String {
        idSeqCounter++
        return "$prefix-${currentTimeMillis()}-$idSeqCounter"
    }

    private suspend fun mutateInventory(
        transform: (List<InventoryItem>) -> List<InventoryItem>
    ): List<InventoryItem> = inventoryMutex.withLock {
        val current = storage.inventoryFlow.first()
        val updated = transform(current)
        storage.saveInventory(updated)
        updated
    }

    fun setAppForeground(inForeground: Boolean) {
        val wasForeground = appForegroundFlow.value
        appForegroundFlow.value = inForeground
        if (inForeground && !wasForeground) {
            scope.launch {
                val family = storage.familyGroupFlow.first()
                if (family.isJoined) {
                    syncFamilyNow(silent = true, isBackgroundPoll = true)
                }
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
            modalFlow.update { it.copy(scannerForForm = false, showShoppingReview = false) }
        }
    }

    // Filters & Search
    fun updateSearch(query: String) {
        filtersFlow.update { it.copy(searchQuery = query) }
    }

    fun selectCategory(category: String?) {
        filtersFlow.update { current ->
            val next = if (category == null) {
                emptySet()
            } else if (current.selectedCategories.size == 1 &&
                current.selectedCategories.first().equals(category, ignoreCase = true)
            ) {
                emptySet()
            } else {
                setOf(category)
            }
            current.copy(selectedCategories = next)
        }
    }

    fun toggleCategory(category: String) {
        filtersFlow.update { current ->
            val next = current.selectedCategories.toMutableSet()
            val existing = next.firstOrNull { it.equals(category, ignoreCase = true) }
            if (existing != null) next.remove(existing) else next.add(category)
            current.copy(selectedCategories = next)
        }
    }

    fun selectStatus(status: StatusFilter) {
        filtersFlow.update { current ->
            val next = if (status == StatusFilter.ALL) {
                emptySet()
            } else if (current.selectedStatuses.size == 1 && current.selectedStatuses.contains(status)) {
                emptySet()
            } else {
                setOf(status)
            }
            current.copy(selectedStatuses = next)
        }
    }

    fun toggleStatus(status: StatusFilter) {
        if (status == StatusFilter.ALL) {
            filtersFlow.update { it.copy(selectedStatuses = emptySet()) }
            return
        }
        filtersFlow.update { current ->
            val next = current.selectedStatuses.toMutableSet()
            if (next.contains(status)) next.remove(status) else next.add(status)
            current.copy(selectedStatuses = next)
        }
    }

    fun selectSort(sort: SortOption) {
        filtersFlow.update { it.copy(sortOption = sort) }
    }

    fun quickFilterAlerts() {
        filtersFlow.update {
            it.copy(
                activeTab = BunkerTab.WAREHOUSE,
                selectedCategories = emptySet(),
                selectedStatuses = setOf(StatusFilter.ALERT_ALL),
                sortOption = SortOption.EXPIRY_ASC
            )
        }
    }

    fun resetFilters() {
        filtersFlow.update {
            it.copy(
                searchQuery = "",
                selectedCategories = emptySet(),
                selectedStatuses = emptySet(),
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
        scope.launch {
            val now = currentTimeMillis()
            val member = uiState.value.familyGroup.memberName.trim()
            mutateInventory { existingList ->
                val current = existingList.toMutableList()
                val existingAuthor = if (isEdit) {
                    current.firstOrNull { it.id == item.id }?.addedBy.orEmpty()
                } else ""
                val stamped = item.copy(
                    addedBy = item.addedBy.ifBlank { existingAuthor.ifBlank { member } },
                    updatedAt = now,
                    updatedBy = member
                )
                if (isEdit) {
                    val idx = current.indexOfFirst { it.id == stamped.id }
                    if (idx >= 0) current[idx] = stamped
                } else {
                    current.add(0, stamped)
                }
                current
            }
            emitToast(if (isEdit) strings().toastUpdated else strings().toastAdded)
            filtersFlow.update { it.copy(bannerDismissed = false) }
            closeItemSheet()
            platform.evaluateExpiryNotifications()
            if (uiState.value.familyGroup.isJoined) syncFamilyNow(silent = true)
        }
    }

    fun adjustQuantity(itemId: String, delta: Double, notifyQuickAdd: Boolean = false) {
        scope.launch {
            var changedName: String? = null
            var changedQty = 0.0
            val member = uiState.value.familyGroup.memberName
            mutateInventory { existingList ->
                val current = existingList.toMutableList()
                val idx = current.indexOfFirst { it.id == itemId }
                if (idx < 0) return@mutateInventory current
                val existing = current[idx]
                val updatedItem = if (existing.batches.isNotEmpty()) {
                    val sortedBatches = existing.batches.sortedBy { it.expiryDate.ifBlank { "9999-12-31" } }.toMutableList()
                    val targetIdx = if (delta < 0) {
                        sortedBatches.indexOfFirst { it.qty > 0.0 }.takeIf { it >= 0 } ?: 0
                    } else {
                        sortedBatches.lastIndex
                    }
                    val b = sortedBatches[targetIdx]
                    sortedBatches[targetIdx] = b.copy(qty = max(0.0, ((b.qty + delta) * 100.0).roundToInt() / 100.0))
                    InventoryLogic.normalizeWithBatches(existing.copy(batches = sortedBatches)).copy(
                        updatedAt = currentTimeMillis(),
                        updatedBy = member
                    )
                } else {
                    val newQty = max(0.0, ((existing.qty + delta) * 100.0).roundToInt() / 100.0)
                    existing.copy(
                        qty = newQty,
                        updatedAt = currentTimeMillis(),
                        updatedBy = member
                    )
                }
                changedName = existing.name
                changedQty = updatedItem.qty
                current[idx] = updatedItem
                current
            }
            if (notifyQuickAdd && changedName != null) {
                emitToast(
                    strings().toastQuickAdded(
                        InventoryLogic.formatQty(delta),
                        changedName!!,
                        InventoryLogic.formatQty(changedQty)
                    )
                )
            }
            if (uiState.value.familyGroup.isJoined) syncFamilyNow(silent = true)
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
        scope.launch {
            when (action) {
                is PendingConfirm.DeleteItem -> {
                    mutateInventory { list -> list.filterNot { it.id == action.item.id } }
                    val deleted = storage.deletedIdsFlow.first().toMutableMap()
                    deleted[action.item.id] = currentTimeMillis()
                    storage.saveDeletedIds(deleted)
                    emitToast(strings().toastDeleted)
                    if (uiState.value.familyGroup.isJoined) syncFamilyNow(silent = true)
                }
                PendingConfirm.RestoreDemo -> {
                    mutateInventory { InventoryLogic.getDefaultDemoData() }
                    filtersFlow.update { it.copy(bannerDismissed = false) }
                    resetFilters()
                    emitToast(strings().toastDemoLoaded)
                }
                PendingConfirm.ClearAll -> {
                    mutateInventory { emptyList() }
                    emitToast(strings().toastCleared)
                }
            }
        }
    }

    // YsiFoods-style Product Name & Code Search in Scanner Bar
    fun updateScannerSearchQuery(query: String) {
        modalFlow.update { it.copy(scannerSearchQuery = query) }
        scannerSearchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2 || InventoryLogic.isLikelyRawCode(trimmed)) {
            modalFlow.update { it.copy(scannerRemoteResults = emptyList(), isSearchingRemoteProducts = false) }
            return
        }
        scannerSearchJob = scope.launch {
            modalFlow.update { it.copy(isSearchingRemoteProducts = true) }
            delay(if (isWebPlatform) 650L else 360L)
            val lang = uiState.value.settings.language
            val hits = OpenFoodFactsClient.searchByName(trimmed, language = lang, limit = 12)
            modalFlow.update { cur ->
                if (cur.scannerSearchQuery.trim() == trimmed) {
                    cur.copy(scannerRemoteResults = hits, isSearchingRemoteProducts = false)
                } else {
                    cur
                }
            }
        }
    }

    fun submitScannerSearchBar(rawInput: String) {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return
        if (InventoryLogic.isLikelyRawCode(trimmed)) {
            clearScannerSearch()
            onCodeScanned(trimmed)
        } else {
            // Force immediate remote search if user pressed Search on a product name
            scannerSearchJob?.cancel()
            modalFlow.update { it.copy(scannerSearchQuery = trimmed, isSearchingRemoteProducts = true) }
            scannerSearchJob = scope.launch {
                val hits = OpenFoodFactsClient.searchByName(trimmed, language = uiState.value.settings.language, limit = 12)
                modalFlow.update { cur ->
                    if (cur.scannerSearchQuery.trim() == trimmed) {
                        cur.copy(scannerRemoteResults = hits, isSearchingRemoteProducts = false)
                    } else {
                        cur
                    }
                }
            }
        }
    }

    fun clearScannerSearch() {
        scannerSearchJob?.cancel()
        modalFlow.update {
            it.copy(
                scannerSearchQuery = "",
                scannerRemoteResults = emptyList(),
                isSearchingRemoteProducts = false
            )
        }
    }

    fun selectLocalSearchResult(item: InventoryItem) {
        clearScannerSearch()
        platform.playScanFeedback()
        val state = uiState.value
        if (state.scannerForForm) {
            modalFlow.update {
                it.copy(
                    scannerForForm = false,
                    itemPrefill = ItemPrefill(
                        barcode = item.barcode,
                        name = item.name,
                        category = item.category,
                        unit = item.unit,
                        expiryDate = item.expiryDate.ifBlank { null },
                        noExpiry = item.noExpiry,
                        notes = item.notes,
                        imageUri = item.imageUri,
                        nutrition = item.nutrition
                    )
                )
            }
            return
        }
        if (state.shoppingModeActive) {
            adjustQuantity(item.id, 1.0, notifyQuickAdd = false)
            addOrIncrementShoppingEntry(item.id, item.barcode, item.name, isNew = false, isEnriching = false)
            return
        }
        modalFlow.update {
            it.copy(
                detectedCandidate = DetectedScanCandidate(
                    code = item.barcode.ifBlank { item.id },
                    existingItemId = item.id,
                    isLookingUp = false
                )
            )
        }
    }

    fun selectRemoteSearchResult(result: OpenFoodFactsResult) {
        clearScannerSearch()
        platform.playScanFeedback()
        val state = uiState.value
        val code = result.barcode.trim()
        val estDays = InventoryLogic.estimateShelfLifeDays(
            result.name,
            result.category,
            result.nutrition?.safetyFlags.orEmpty()
        )
        val estExpiry = InventoryLogic.relativeDateString(estDays)
        val estNote = strings().expiryEstimatedNote(estDays)

        val prefill = ItemPrefill(
            barcode = code,
            name = result.name,
            category = result.category,
            unit = result.unit,
            expiryDate = estExpiry,
            notes = estNote,
            imageUri = result.nutrition?.imageUrl,
            nutrition = result.nutrition
        )

        if (state.scannerForForm) {
            modalFlow.update { it.copy(scannerForForm = false, itemPrefill = prefill) }
            emitToast(strings().toastProductIdentified)
            return
        }

        // Check if product already exists by barcode
        val existing = if (code.isNotBlank()) {
            state.inventory.firstOrNull { it.barcode.isNotBlank() && it.barcode.equals(code, ignoreCase = true) }
        } else null

        if (state.shoppingModeActive) {
            if (existing != null) {
                scope.launch {
                    val member = uiState.value.familyGroup.memberName.trim()
                    mutateInventory { list ->
                        list.map { it ->
                            if (it.id != existing.id) it
                            else it.copy(
                                qty = ((it.qty + 1.0) * 100.0).roundToInt() / 100.0,
                                nutrition = it.nutrition?.takeIf { n -> n.energyKcal != null } ?: result.nutrition ?: it.nutrition,
                                imageUri = it.imageUri ?: result.nutrition?.imageUrl,
                                updatedAt = currentTimeMillis(),
                                updatedBy = member
                            )
                        }
                    }
                    if (uiState.value.familyGroup.isJoined) syncFamilyNow(silent = true)
                }
                addOrIncrementShoppingEntry(existing.id, existing.barcode, existing.name, isNew = false, isEnriching = false)
            } else {
                val now = currentTimeMillis()
                val newId = nextUniqueId()
                val member = state.familyGroup.memberName.trim()
                val created = InventoryItem(
                    id = newId,
                    barcode = code,
                    name = result.name,
                    category = result.category,
                    qty = 1.0,
                    unit = result.unit,
                    noExpiry = false,
                    expiryDate = estExpiry,
                    notes = estNote,
                    imageUri = result.nutrition?.imageUrl,
                    nutrition = result.nutrition,
                    addedBy = member,
                    updatedAt = now,
                    updatedBy = member
                )
                scope.launch {
                    mutateInventory { listOf(created) + it }
                    platform.evaluateExpiryNotifications()
                    if (uiState.value.familyGroup.isJoined) syncFamilyNow(silent = true)
                }
                addOrIncrementShoppingEntry(newId, code, result.name, isNew = true, isEnriching = false)
            }
            return
        }

        if (existing != null) {
            modalFlow.update {
                it.copy(
                    detectedCandidate = DetectedScanCandidate(
                        code = code,
                        existingItemId = existing.id,
                        prefill = prefill,
                        isLookingUp = false
                    )
                )
            }
        } else {
            modalFlow.update {
                it.copy(
                    detectedCandidate = DetectedScanCandidate(
                        code = code.ifBlank { result.name },
                        prefill = prefill,
                        isLookingUp = false
                    )
                )
            }
        }
    }

    private fun addOrIncrementShoppingEntry(
        itemId: String,
        barcode: String,
        name: String,
        isNew: Boolean,
        isEnriching: Boolean
    ) {
        modalFlow.update { m ->
            val list = m.shoppingEntries.toMutableList()
            val idx = list.indexOfFirst { it.itemId == itemId }
            if (idx >= 0) {
                val prev = list.removeAt(idx)
                list.add(0, prev.copy(addedQty = prev.addedQty + 1.0))
            } else {
                list.add(0, ShoppingScanEntry(itemId, barcode, name, 1.0, isNew = isNew, isEnriching = isEnriching))
            }
            m.copy(shoppingEntries = list)
        }
    }

    // Optical Scan Handling (non-blocking bottom popup keeps camera live!)
    fun onCodeScanned(rawCode: String) {
        val trimmedRaw = rawCode.trim()
        if (trimmedRaw.isEmpty()) return

        platform.playScanFeedback()

        if (FamilySyncClient.isInviteLink(trimmedRaw)) {
            joinFamilyByInvite(trimmedRaw, uiState.value.familyGroup.memberName)
            return
        }

        val state = uiState.value
        val gs1 = InventoryLogic.parseGs1Code(trimmedRaw)
        val code = gs1.barcode
        val gs1Expiry = gs1.expiryIso
        val gs1Batch = gs1.batch

        if (!state.scannerForForm && state.shoppingModeActive) {
            handleShoppingModeScan(code, gs1Expiry, gs1Batch)
            return
        }

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
            modalFlow.update { it.copy(scannerForForm = false, itemPrefill = prefill) }
            emitToast(
                if (structured != null) strings().toastQrApplied
                else strings().toastCodeAssigned(code)
            )
            return
        }

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
            // Opportunistically enrich missing calories on existing item too
            if (existing.nutrition?.energyKcal == null && code.matches(Regex("^\\d{8,14}$"))) {
                scope.launch { enrichExistingItemNutrition(existing.id, code) }
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

        scope.launch {
            val off = OpenFoodFactsClient.lookup(code)
            modalFlow.update { current ->
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
                        if (gs1Expiry != null) append(strings().expiryGs1Note)
                        else append(strings().expiryEstimatedNote(estDays))
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
                                imageUri = off.nutrition?.imageUrl,
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

    private suspend fun enrichExistingItemNutrition(itemId: String, barcode: String) {
        val off = OpenFoodFactsClient.lookup(barcode) ?: return
        val nut = off.nutrition ?: return
        mutateInventory { list ->
            list.map { item ->
                if (item.id != itemId) item
                else item.copy(
                    nutrition = nut,
                    imageUri = item.imageUri ?: nut.imageUrl,
                    updatedAt = currentTimeMillis()
                )
            }
        }
        if (uiState.value.familyGroup.isJoined) syncFamilyNow(silent = true)
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

    // Shopping / Rapid Back-to-Back Intake Mode + Batch Review
    fun toggleShoppingMode() {
        modalFlow.update {
            it.copy(
                shoppingModeActive = !it.shoppingModeActive,
                detectedCandidate = null
            )
        }
    }

    fun openShoppingReview() {
        modalFlow.update { it.copy(showShoppingReview = true) }
    }

    fun closeShoppingReview() {
        modalFlow.update { it.copy(showShoppingReview = false) }
    }

    private fun handleShoppingModeScan(code: String, gs1Expiry: String?, gs1Batch: String?) {
        val now = currentTimeMillis()
        val lastAt = shoppingCooldownByCode[code] ?: 0L
        if (now - lastAt < 2200L) return
        shoppingCooldownByCode[code] = now

        val state = uiState.value
        val member = state.familyGroup.memberName.trim()
        val existing = state.inventory.firstOrNull {
            it.barcode.isNotBlank() && it.barcode.equals(code, ignoreCase = true)
        }

        if (existing != null) {
            adjustQuantity(existing.id, 1.0, notifyQuickAdd = false)
            val needEnrich = existing.nutrition?.energyKcal == null && code.matches(Regex("^\\d{8,14}$"))
            addOrIncrementShoppingEntry(existing.id, code, existing.name, isNew = false, isEnriching = needEnrich)
            if (needEnrich) {
                scope.launch {
                    enrichExistingItemNutrition(existing.id, code)
                    modalFlow.update { m ->
                        m.copy(
                            shoppingEntries = m.shoppingEntries.map { e ->
                                if (e.itemId == existing.id) e.copy(isEnriching = false) else e
                            }
                        )
                    }
                }
            }
            return
        }

        val structured = InventoryLogic.parseStructuredQr(code)
        val newId = nextUniqueId()
        val initialName = structured?.name?.ifBlank { code } ?: code
        val initialExpiry = structured?.expiryDate ?: gs1Expiry ?: InventoryLogic.relativeDateString(180)
        val initialNotes = buildString {
            if (structured != null) append(structured.notes)
            if (gs1Expiry != null) {
                if (isNotEmpty()) append(" · ")
                append(strings().expiryGs1Note)
            }
            if (!gs1Batch.isNullOrBlank()) {
                if (isNotEmpty()) append(" · ")
                append("Lote: $gs1Batch")
            }
        }
        val created = InventoryItem(
            id = newId,
            barcode = code,
            name = initialName,
            category = structured?.category ?: ItemCategory.FOOD.storageKey,
            qty = 1.0,
            noExpiry = structured?.noExpiry ?: false,
            expiryDate = initialExpiry,
            notes = initialNotes,
            addedBy = member,
            updatedAt = now,
            updatedBy = member
        )

        val needLookup = structured == null && code.matches(Regex("^\\d{8,14}$"))
        addOrIncrementShoppingEntry(
            itemId = newId,
            barcode = code,
            name = if (needLookup) strings().shoppingResolving else initialName,
            isNew = true,
            isEnriching = needLookup
        )

        scope.launch {
            // Serialized insert via inventoryMutex so concurrent scans NEVER clobber each other!
            mutateInventory { listOf(created) + it }
            platform.evaluateExpiryNotifications()

            if (needLookup) {
                val off = OpenFoodFactsClient.lookup(code)
                if (off != null) {
                    val estDays = InventoryLogic.estimateShelfLifeDays(
                        off.name,
                        off.category,
                        off.nutrition?.safetyFlags.orEmpty()
                    )
                    val resolvedExpiry = gs1Expiry ?: InventoryLogic.relativeDateString(estDays)
                    val noteText = buildString {
                        if (gs1Expiry != null) append(strings().expiryGs1Note)
                        else append(strings().expiryEstimatedNote(estDays))
                        if (!gs1Batch.isNullOrBlank()) append(" · Lote: $gs1Batch")
                    }
                    // Serialized patch via inventoryMutex: preserves any user edits made in the meantime
                    // (like location/qty/expiry) while ALWAYS persisting calories & nutrition!
                    mutateInventory { latest ->
                        latest.map { item ->
                            if (item.id != newId) item
                            else {
                                val userChangedExpiry = item.expiryDate != initialExpiry || item.noExpiry
                                item.copy(
                                    name = if (item.name == initialName) off.name else item.name,
                                    category = off.category,
                                    expiryDate = if (userChangedExpiry) item.expiryDate else resolvedExpiry,
                                    notes = if (item.notes == initialNotes) noteText else item.notes,
                                    imageUri = item.imageUri ?: off.nutrition?.imageUrl,
                                    nutrition = off.nutrition,
                                    updatedAt = currentTimeMillis()
                                )
                            }
                        }
                    }
                    modalFlow.update { m ->
                        m.copy(
                            shoppingEntries = m.shoppingEntries.map { e ->
                                if (e.itemId == newId) e.copy(name = off.name, isEnriching = false) else e
                            }
                        )
                    }
                } else {
                    modalFlow.update { m ->
                        m.copy(
                            shoppingEntries = m.shoppingEntries.map { e ->
                                if (e.itemId == newId) e.copy(name = code, isEnriching = false) else e
                            }
                        )
                    }
                }
            }
            if (uiState.value.familyGroup.isJoined) syncFamilyNow(silent = true)
        }
    }

    /**
     * Batch-edit a selected subset of items (e.g. half the haul -> "Nevera", or expiry in 30 days, or category).
     */
    fun batchUpdateItems(
        itemIds: Set<String>,
        location: String? = null,
        expiryDate: String? = null,
        noExpiry: Boolean? = null,
        category: ItemCategory? = null
    ) {
        if (itemIds.isEmpty()) return
        scope.launch {
            val now = currentTimeMillis()
            val member = uiState.value.familyGroup.memberName.trim()
            var count = 0
            mutateInventory { list ->
                list.map { item ->
                    if (!itemIds.contains(item.id)) item
                    else {
                        count++
                        val nextNoExp = noExpiry ?: if (expiryDate != null) false else item.noExpiry
                        val nextExp = when {
                            nextNoExp -> ""
                            expiryDate != null -> expiryDate
                            else -> item.expiryDate
                        }
                        item.copy(
                            location = location ?: item.location,
                            noExpiry = nextNoExp,
                            expiryDate = nextExp,
                            category = category?.storageKey ?: item.category,
                            updatedAt = now,
                            updatedBy = member
                        )
                    }
                }
            }
            if (count > 0) {
                emitToast(strings().toastBatchUpdated(count))
                platform.evaluateExpiryNotifications()
                if (uiState.value.familyGroup.isJoined) syncFamilyNow(silent = true)
            }
        }
    }

    fun adjustShoppingEntry(itemId: String, delta: Double) {
        val entry = uiState.value.shoppingEntries.firstOrNull { it.itemId == itemId } ?: return
        val nextAdded = entry.addedQty + delta
        if (nextAdded <= 0.0) {
            undoShoppingEntry(itemId)
            return
        }
        adjustQuantity(itemId, delta, notifyQuickAdd = false)
        modalFlow.update { m ->
            m.copy(
                shoppingEntries = m.shoppingEntries.map { e ->
                    if (e.itemId == itemId) e.copy(addedQty = nextAdded) else e
                }
            )
        }
    }

    fun undoShoppingEntry(itemId: String) {
        val entry = uiState.value.shoppingEntries.firstOrNull { it.itemId == itemId } ?: return
        modalFlow.update { m ->
            val remaining = m.shoppingEntries.filterNot { it.itemId == itemId }
            m.copy(
                shoppingEntries = remaining,
                showShoppingReview = m.showShoppingReview && remaining.isNotEmpty()
            )
        }
        scope.launch {
            mutateInventory { list ->
                val current = list.toMutableList()
                val idx = current.indexOfFirst { it.id == itemId }
                if (idx >= 0) {
                    val item = current[idx]
                    if (entry.isNew && item.qty <= entry.addedQty) {
                        current.removeAt(idx)
                    } else {
                        current[idx] = item.copy(
                            qty = max(0.0, item.qty - entry.addedQty),
                            updatedAt = currentTimeMillis()
                        )
                    }
                }
                current
            }
            if (uiState.value.familyGroup.isJoined) syncFamilyNow(silent = true)
        }
    }

    fun finishShoppingSession() {
        shoppingCooldownByCode.clear()
        modalFlow.update {
            it.copy(
                shoppingModeActive = false,
                shoppingEntries = emptyList(),
                showShoppingReview = false
            )
        }
        selectTab(BunkerTab.WAREHOUSE)
        if (uiState.value.familyGroup.isJoined) syncFamilyNow(silent = true)
    }

    // QR Label Modal
    fun openQrLabel(itemId: String) {
        scope.launch {
            mutateInventory { list ->
                val current = list.toMutableList()
                val idx = current.indexOfFirst { it.id == itemId }
                if (idx >= 0 && current[idx].barcode.isBlank()) {
                    val generated = "BNK-" + currentTimeMillis().toString().takeLast(6)
                    current[idx] = current[idx].copy(barcode = generated)
                }
                current
            }
            modalFlow.update { it.copy(qrLabelItemId = itemId) }
        }
    }

    fun closeQrLabel() {
        modalFlow.update { it.copy(qrLabelItemId = null) }
    }

    fun openItemDetail(itemId: String, originX: Float = 0.5f, originY: Float = 0.5f) {
        modalFlow.update {
            it.copy(
                detailItemId = itemId,
                detailOriginX = originX.coerceIn(0.05f, 0.95f),
                detailOriginY = originY.coerceIn(0.08f, 0.92f)
            )
        }
        scope.launch {
            var targetBarcode = ""
            var needNutrition = false
            mutateInventory { list ->
                val current = list.toMutableList()
                val idx = current.indexOfFirst { it.id == itemId }
                if (idx >= 0) {
                    var item = current[idx]
                    if (item.barcode.isBlank()) {
                        val generated = "BNK-" + currentTimeMillis().toString().takeLast(6)
                        item = item.copy(barcode = generated)
                        current[idx] = item
                    }
                    targetBarcode = item.barcode
                    needNutrition = item.nutrition?.energyKcal == null && targetBarcode.matches(Regex("^\\d{8,14}$"))
                }
                current
            }
            if (needNutrition) {
                enrichExistingItemNutrition(itemId, targetBarcode)
            }
        }
    }

    fun closeItemDetail() {
        modalFlow.update { it.copy(detailItemId = null) }
    }

    fun updateItemLive(updated: InventoryItem) {
        scope.launch {
            val normalized = InventoryLogic.normalizeWithBatches(updated).copy(
                updatedAt = currentTimeMillis(),
                updatedBy = uiState.value.familyGroup.memberName
            )
            mutateInventory { list ->
                list.map { if (it.id == normalized.id) normalized else it }
            }
            platform.evaluateExpiryNotifications()
            if (uiState.value.familyGroup.isJoined) syncFamilyNow(silent = true)
        }
    }

    fun openBatchQrDialog() {
        modalFlow.update { it.copy(showBatchQrDialog = true) }
    }

    fun closeBatchQrDialog() {
        modalFlow.update { it.copy(showBatchQrDialog = false) }
    }

    suspend fun prepareItemsForBatchQr(selectedIds: Set<String>): List<InventoryItem> {
        val resolved = mutableListOf<InventoryItem>()
        var mutated = false
        mutateInventory { list ->
            val current = list.toMutableList()
            val baseTs = currentTimeMillis()
            var seq = 0
            for (i in current.indices) {
                val item = current[i]
                if (selectedIds.contains(item.id)) {
                    if (item.barcode.isBlank()) {
                        val code = "BNK-" + ((baseTs + seq) % 1_000_000L).toString().padStart(6, '0')
                        seq++
                        val updated = item.copy(barcode = code, updatedAt = baseTs)
                        current[i] = updated
                        resolved.add(updated)
                        mutated = true
                    } else {
                        resolved.add(item)
                    }
                }
            }
            current
        }
        if (mutated && uiState.value.familyGroup.isJoined) {
            syncFamilyNow(silent = true)
        }
        return resolved
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
        scope.launch {
            val result = InventoryLogic.importFromJson(raw)
            result.onSuccess { items ->
                mutateInventory { items }
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
        scope.launch { storage.updateLanguage(language) }
    }

    fun completeOnboarding() {
        scope.launch { storage.markOnboardingCompleted() }
    }

    fun setThemeMode(mode: AppThemeMode) {
        scope.launch { storage.updateThemeMode(mode) }
    }

    fun setWaterConfig(persons: Int, litersPerDay: Int) {
        scope.launch { storage.updateWaterConfig(persons, litersPerDay) }
    }

    fun setExpiryThresholds(expiredDays: Int, criticalDays: Int, warningDays: Int) {
        scope.launch {
            storage.updateExpiryThresholds(expiredDays, criticalDays, warningDays)
            platform.evaluateExpiryNotifications()
        }
    }

    fun setAutoCheckUpdates(enabled: Boolean) {
        scope.launch { storage.updateAutoCheckUpdates(enabled) }
    }

    fun setExpiryNotifications(enabled: Boolean) {
        scope.launch {
            storage.updateExpiryNotifications(enabled)
            if (enabled) platform.evaluateExpiryNotifications()
        }
    }

    fun setFamilySyncPrefs(enabled: Boolean, intervalSec: Int) {
        scope.launch { storage.updateFamilySyncPrefs(enabled, intervalSec) }
    }

    fun updateFamilyMemberName(memberName: String) {
        scope.launch {
            val current = storage.familyGroupFlow.first()
            if (!current.isJoined) return@launch
            storage.saveFamilyGroup(current.copy(memberName = memberName.trim()))
            emitToast(strings().toastFamilyUserSaved)
        }
    }

    // Family Group Sync (E2E Encrypted, Zero-Auth)
    fun openFamilyDialog() {
        modalFlow.update { it.copy(showFamilyDialog = true) }
    }

    fun closeFamilyDialog() {
        modalFlow.update { it.copy(showFamilyDialog = false) }
    }

    fun createFamilyGroup(groupName: String, memberName: String) {
        scope.launch {
            modalFlow.update { it.copy(isSyncingFamily = true) }
            val localItems = storage.inventoryFlow.first()
            val localDeleted = storage.deletedIdsFlow.first()
            val result = FamilySyncClient.createGroup(groupName, memberName, localItems, localDeleted)
            modalFlow.update { it.copy(isSyncingFamily = false) }
            result.onSuccess { cfg ->
                storage.saveFamilyGroup(cfg)
                emitToast(strings().toastFamilyCreated(cfg.groupName))
            }.onFailure {
                emitToast(strings().toastFamilySyncError)
            }
        }
    }

    fun joinFamilyByInvite(rawInvite: String, memberName: String = "") {
        val parsed = FamilySyncClient.parseInviteLink(rawInvite)
        if (parsed == null) {
            emitToast(strings().toastInviteInvalid)
            return
        }
        val cfg = parsed.copy(memberName = memberName.trim())
        scope.launch {
            modalFlow.update { it.copy(isSyncingFamily = true) }
            val localItems = storage.inventoryFlow.first()
            val localDeleted = storage.deletedIdsFlow.first()
            val result = FamilySyncClient.syncGroup(cfg, localItems, localDeleted, isInitialJoin = true)
            modalFlow.update { it.copy(isSyncingFamily = false) }
            result.onSuccess { (items, deleted, syncedCfg) ->
                mutateInventory { items }
                storage.saveDeletedIds(deleted)
                storage.saveFamilyGroup(syncedCfg)
                emitToast(strings().toastFamilyJoined(syncedCfg.groupName))
                platform.evaluateExpiryNotifications()
                selectTab(BunkerTab.WAREHOUSE)
                closeFamilyDialog()
            }.onFailure {
                emitToast(strings().toastFamilySyncError)
            }
        }
    }

    fun syncFamilyNow(silent: Boolean = false, isBackgroundPoll: Boolean = false) {
        scope.launch {
            val cfg = storage.familyGroupFlow.first()
            if (!cfg.isJoined) return@launch
            if (!syncMutex.tryLock()) return@launch
            try {
                if (!isBackgroundPoll) {
                    modalFlow.update { it.copy(isSyncingFamily = true) }
                }
                val localItems = storage.inventoryFlow.first()
                val localDeleted = storage.deletedIdsFlow.first()
                val result = FamilySyncClient.syncGroupDiff(
                    config = cfg,
                    localItems = localItems,
                    localDeleted = localDeleted,
                    lastRemoteHash = if (isBackgroundPoll) lastRemoteCipherHash else "",
                    lastLocalHash = if (isBackgroundPoll) lastLocalStateHash else ""
                )
                result.onSuccess { outcome ->
                    lastRemoteCipherHash = outcome.remoteCipherHash
                    lastLocalStateHash = outcome.localStateHash
                    if (outcome.localChanged) {
                        mutateInventory { outcome.items }
                        storage.saveDeletedIds(outcome.deletedIds)
                        platform.evaluateExpiryNotifications()
                    }
                    if (outcome.localChanged || outcome.remoteUploaded || outcome.config.lastSyncedAt != cfg.lastSyncedAt) {
                        storage.saveFamilyGroup(outcome.config)
                    }
                    if (!silent) emitToast(strings().toastFamilySynced)
                }.onFailure {
                    if (!silent) emitToast(strings().toastFamilySyncError)
                }
            } finally {
                if (!isBackgroundPoll) {
                    modalFlow.update { it.copy(isSyncingFamily = false) }
                }
                syncMutex.unlock()
            }
        }
    }

    fun leaveFamilyGroup() {
        scope.launch {
            lastRemoteCipherHash = ""
            lastLocalStateHash = ""
            storage.clearFamilyGroup()
            closeFamilyDialog()
            emitToast(strings().toastFamilyLeft)
        }
    }

    fun checkForUpdates(silent: Boolean = false) {
        if (!platform.supportsApkSelfUpdate) return
        scope.launch {
            val update = platform.checkForAppUpdate()
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
        scope.launch {
            modalFlow.update { it.copy(isDownloadingUpdate = true) }
            emitToast(strings().updateDownloading)
            val ok = platform.downloadAndInstallAppUpdate(update)
            modalFlow.update { it.copy(isDownloadingUpdate = false) }
            if (ok) dismissUpdateDialog() else emitToast(strings().updateFailed)
        }
    }
}
