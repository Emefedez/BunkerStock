package com.bunkerstock.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.bunkerstock.app.data.AppLanguage
import com.bunkerstock.app.data.ExpiryInfo
import com.bunkerstock.app.data.ExpiryStatus
import com.bunkerstock.app.data.ItemCategory
import com.bunkerstock.app.data.ItemUnit
import com.bunkerstock.app.data.SortOption
import com.bunkerstock.app.data.StatusFilter
import kotlin.math.abs

@Immutable
interface AppStrings {
    val language: AppLanguage
    // Brand & Navigation
    val brandBunker: String
    val brandStock: String
    val brandSubtitle: String
    val navWarehouse: String
    val navScanner: String
    val navSettings: String
    // Header & Actions
    val scanCode: String
    val addItem: String
    val dataMenu: String
    // Alert Banner
    val alertRotationPrefix: String
    val alertExpiredSingle: String
    val alertExpiredPlural: (Int) -> String
    val alertCriticalSingle: String
    val alertCriticalPlural: (Int) -> String
    val alertJoinAnd: String
    val alertDescExpired: String
    val alertDescCritical: String
    val reviewAffected: String
    val dismissAlert: String
    // KPIs
    val kpiReferences: String
    val kpiTotalUnits: (String) -> String
    val kpiExpired: String
    val kpiExpiredSub: String
    val kpiCritical30d: String
    val kpiCriticalSub: String
    val kpiWaterReserve: String
    val kpiWaterDays: (Int, Int, Int) -> String
    val kpiFood: String
    val kpiFoodSub: String
    // Health Bar
    val healthTitle: String
    val healthNoData: String
    val healthValidPct: (Int) -> String
    val legendOptimal: String
    val legendWarning: String
    val legendCritical: String
    val legendExpired: String
    // Filters & Sort
    val searchPlaceholder: String
    val clearSearch: String
    val allCategories: String
    val allStatuses: String
    val alertAllStatus: String
    val sortLabel: String
    val showingCount: (Int) -> String
    val resetFilters: String
    val quickAdjustHint: String
    // Empty State
    val emptyTitle: String
    val emptySubtitle: String
    // Item Card & Actions
    val expiryLabel: String
    val notApplicable: String
    val subtractOne: String
    val addOne: String
    val qrLabelAction: String
    val editAction: String
    val deleteAction: String
    // Expiry Statuses
    val statusNoExpiry: String
    val statusNoExpirySub: String
    val statusExpired: String
    val statusExpiredOneDay: String
    val statusExpiredDays: (Long) -> String
    val statusExpiresToday: String
    val statusExpiresTodaySub: String
    val statusCritical: String
    val statusWarning: String
    val statusOptimal: String
    val statusInOneDay: String
    val statusInDays: (Long) -> String
    // Add/Edit Modal
    val modalAddTitle: String
    val modalEditTitle: String
    val fieldName: String
    val fieldNamePlaceholder: String
    val fieldBarcode: String
    val fieldBarcodePlaceholder: String
    val fieldCategory: String
    val fieldLocation: String
    val fieldLocationPlaceholder: String
    val fieldQuantity: String
    val fieldUnit: String
    val fieldNoExpiry: String
    val fieldNoExpirySub: String
    val fieldExpiryDate: String
    val fieldNotes: String
    val fieldNotesPlaceholder: String
    val cancel: String
    val save: String
    val confirm: String
    val close: String
    val requiredFieldsError: String
    // Scanner
    val scannerTitle: String
    val scannerFormats: String
    val scannerGuideHint: String
    val scannerSwitchCamera: String
    val scannerTorch: String
    val scannerManualLabel: String
    val scannerManualPlaceholder: String
    val scannerManualSubmit: String
    val scannerLookingUp: String
    val scannerPermissionTitle: String
    val scannerPermissionDesc: String
    val scannerGrantPermission: String
    val scannerCameraUnavailable: String
    // Quick Scan Sheet
    val qsBadgeFound: String
    val qsCurrentStock: String
    val qsExpiry: String
    val qsCode: String
    val qsAddStockLabel: String
    val qsAddOneUnit: String
    val qsAddCustom: String
    val qsEditItem: String
    val qsScanAnother: String
    // QR Label Modal
    val qrModalTitle: String
    val qrHeaderBrand: String
    val qrMetaFormat: (String, String) -> String
    val qrPrint: String
    val qrSharePng: String
    // Settings & Backup
    val settingsTitle: String
    val settingsSectionGeneral: String
    val settingsLanguage: String
    val settingsLanguageSub: String
    val languageSpanish: String
    val languageEnglish: String
    val settingsTheme: String
    val themeDark: String
    val themeLight: String
    val themeSystem: String
    val settingsSectionAutonomy: String
    val settingsWaterPersons: String
    val settingsWaterLiters: String
    val settingsWaterFormula: (Int, Int, Int) -> String
    val settingsSectionData: String
    val exportJson: String
    val exportJsonSub: String
    val exportCsv: String
    val exportCsvSub: String
    val importJson: String
    val importJsonSub: String
    val restoreDemo: String
    val restoreDemoSub: String
    val clearAll: String
    val clearAllSub: String
    val settingsSectionAbout: String
    val aboutDesc: String
    // Confirm Dialogs
    val confirmDeleteTitle: String
    val confirmDeleteMsg: (String) -> String
    val confirmDemoTitle: String
    val confirmDemoMsg: String
    val confirmClearTitle: String
    val confirmClearMsg: String
    // Toasts / Snackbars
    val toastUpdated: String
    val toastAdded: String
    val toastDeleted: String
    val toastJsonExported: String
    val toastCsvExported: String
    val toastJsonImported: (Int) -> String
    val toastJsonInvalid: String
    val toastDemoLoaded: String
    val toastCleared: String
    val toastQrApplied: String
    val toastCodeAssigned: (String) -> String
    val toastStructuredQr: String
    val toastProductIdentified: String
    val toastNewCodeDetected: (String) -> String
    val toastQuickAdded: (String, String, String) -> String
    val toastExportError: String
    val toastQrShared: String
    val noExpiryCsv: String
    // v2.5: Non-blocking scan popup, image scan, nutrition/health, updates
    val scanFromImage: String
    val scanImageNoCode: String
    val popupDetectedNew: String
    val popupOkRegister: String
    val popupOkOpen: String
    val healthScoreLabel: (Int) -> String
    val flagHighSugar: String
    val flagHighSalt: String
    val flagHighSatFat: String
    val flagUltraProcessed: String
    val flagHighProtein: String
    val flagHighFiber: String
    val expiryEstimatedNote: (Long) -> String
    val expiryGs1Note: String
    val settingsAutoUpdate: String
    val settingsAutoUpdateSub: String
    val settingsCheckUpdatesNow: String
    val settingsCheckUpdatesSub: String
    val updateDialogTitle: (String) -> String
    val updateInstallNow: String
    val updateDownloading: String
    val updateUpToDate: String
    val updateFailed: String
    val creditsTitle: String
    val creditsBody: String
    val productPhotoLabel: String
    val replacePhoto: String
    val removePhoto: String
    // v3.0: Family Groups & Expiry Notifications
    val settingsSectionFamily: String
    val familyCreateOrJoin: String
    val familyCreateOrJoinSub: String
    val familyConnectedTo: (String) -> String
    val familyLastSync: (String) -> String
    val familySyncNow: String
    val familyInviteQrLink: String
    val familyLeave: String
    val familyDialogTitle: String
    val familyGroupNameLabel: String
    val familyGroupNamePlaceholder: String
    val familyMemberNameLabel: String
    val familyMemberNamePlaceholder: String
    val familyCreateBtn: String
    val familyJoinSection: String
    val familyJoinPlaceholder: String
    val familyJoinBtn: String
    val familyScanInviteQr: String
    val familyShareLink: String
    val familyE2eNote: String
    val toastFamilyCreated: (String) -> String
    val toastFamilyJoined: (String) -> String
    val toastFamilySynced: String
    val toastFamilySyncError: String
    val toastFamilyLeft: String
    val toastInviteInvalid: String
    val settingsExpiryNotif: String
    val settingsExpiryNotifSub: String
    val settingsSectionThresholds: String
    val thresholdExpiredDays: String
    val thresholdCriticalDays: String
    val thresholdWarningDays: String
    val filterButtonLabel: String
    val filterCategoriesHeader: String
    val filterStatusHeader: String
    val batchQrMenu: String
    val batchQrMenuSub: String
    val batchQrTitle: String
    val batchQrSubtitle: String
    val batchQrSelectAll: String
    val batchQrDeselectAll: String
    val batchQrSummary: (Int, Int) -> String
    val batchQrSavePdf: String
    val batchQrSharePdf: String
    val toastBatchPdfSaved: String
    val toastBatchPdfShared: String
    val setupWelcomeSubtitle: String
    val setupContinue: String
    val detailBatchesTitle: String
    val detailAddBatch: String
    val detailAllBatches: String
    val detailBatchNumber: (Int) -> String
    val detailCalendarToday: String
    val detailCalendarTapHint: String
    val detailQrSection: String
    val detailNotesSection: String
    val detailLocationEmpty: String
    val detailYukaLabel: String
    val detailNutriScoreLabel: String
    val detailNoScore: String
    val detailBatchesBadge: (Int) -> String
    val detailAddedBy: (String) -> String
    val detailPer100g: String
    val detailChartScale: String
    val detailTapPhotoHint: String
    val familyAutoSync: String
    val familyAutoSyncSub: String
    val familySyncInterval: String
    val familyYourUser: (String) -> String
    val familyYourUserEmpty: String
    val familyEditUserTitle: String
    val toastFamilyUserSaved: String
    val thresholdResetDefault: String
    val shoppingModeLabel: String
    val shoppingModeActiveHint: String
    val shoppingDoneBtn: (Int) -> String
    val shoppingResolving: String

    fun kpiCriticalTitle(criticalDays: Int): String = when (language) {
        AppLanguage.ES -> "PRÓX. $criticalDays DÍAS"
        AppLanguage.EN -> "NEXT $criticalDays DAYS"
    }

    fun legendWarningDynamic(criticalDays: Int, warningDays: Int): String = when (language) {
        AppLanguage.ES -> "Precaución (${criticalDays + 1}-${warningDays}d)"
        AppLanguage.EN -> "Caution (${criticalDays + 1}-${warningDays}d)"
    }

    fun legendCriticalDynamic(criticalDays: Int): String = when (language) {
        AppLanguage.ES -> "Crítico (≤${criticalDays}d)"
        AppLanguage.EN -> "Critical (≤${criticalDays}d)"
    }

    fun statusFilterLabelDynamic(filter: StatusFilter, criticalDays: Int, warningDays: Int): String = when (language) {
        AppLanguage.ES -> when (filter) {
            StatusFilter.ALL -> "Todos"
            StatusFilter.ALERT_ALL -> "Alertas (Cad. + Crít.)"
            StatusFilter.EXPIRED -> "Caducados"
            StatusFilter.CRITICAL -> "Críticos (≤ ${criticalDays}d)"
            StatusFilter.WARNING -> "Precaución (${criticalDays + 1}-${warningDays}d)"
            StatusFilter.OPTIMAL -> "Óptimos (> ${warningDays}d)"
            StatusFilter.NONE -> "Sin caducidad"
        }
        AppLanguage.EN -> when (filter) {
            StatusFilter.ALL -> "All"
            StatusFilter.ALERT_ALL -> "Alerts (Exp. + Crit.)"
            StatusFilter.EXPIRED -> "Expired"
            StatusFilter.CRITICAL -> "Critical (≤ ${criticalDays}d)"
            StatusFilter.WARNING -> "Caution (${criticalDays + 1}-${warningDays}d)"
            StatusFilter.OPTIMAL -> "Optimal (> ${warningDays}d)"
            StatusFilter.NONE -> "No expiry"
        }
    }
    fun safetyFlagLabel(flag: String): String = when (flag) {
        "HIGH_SUGAR" -> flagHighSugar
        "HIGH_SALT" -> flagHighSalt
        "HIGH_SAT_FAT" -> flagHighSatFat
        "ULTRA_PROCESSED" -> flagUltraProcessed
        "HIGH_PROTEIN" -> flagHighProtein
        "HIGH_FIBER" -> flagHighFiber
        else -> flag
    }
    fun categoryLabel(category: ItemCategory): String = when (language) {
        AppLanguage.ES -> when (category) {
            ItemCategory.FOOD -> "Alimentos"
            ItemCategory.WATER -> "Agua"
            ItemCategory.MATERIALS -> "Materiales"
            ItemCategory.TOOLS -> "Herramientas"
            ItemCategory.OTHER -> "Otro"
        }
        AppLanguage.EN -> when (category) {
            ItemCategory.FOOD -> "Food"
            ItemCategory.WATER -> "Water"
            ItemCategory.MATERIALS -> "Medical / Gear"
            ItemCategory.TOOLS -> "Tools / Power"
            ItemCategory.OTHER -> "Other"
        }
    }

    fun categoryLongLabel(category: ItemCategory): String = when (language) {
        AppLanguage.ES -> when (category) {
            ItemCategory.FOOD -> "Alimentos"
            ItemCategory.WATER -> "Agua"
            ItemCategory.MATERIALS -> "Supervivencia / Botiquín"
            ItemCategory.TOOLS -> "Herramientas / Energía"
            ItemCategory.OTHER -> "Otro"
        }
        AppLanguage.EN -> when (category) {
            ItemCategory.FOOD -> "Food"
            ItemCategory.WATER -> "Water"
            ItemCategory.MATERIALS -> "Survival / First Aid"
            ItemCategory.TOOLS -> "Tools / Power"
            ItemCategory.OTHER -> "Other"
        }
    }

    fun unitLabel(unit: ItemUnit): String = when (language) {
        AppLanguage.ES -> when (unit) {
            ItemUnit.UNITS -> "unidades"
            ItemUnit.LITERS -> "litros"
            ItemUnit.JUGS_5L -> "garrafas 5L"
            ItemUnit.KG -> "kg"
            ItemUnit.CANS -> "latas"
            ItemUnit.PACKS -> "paquetes"
            ItemUnit.BOXES -> "cajas"
            ItemUnit.RATIONS -> "raciones"
        }
        AppLanguage.EN -> when (unit) {
            ItemUnit.UNITS -> "units"
            ItemUnit.LITERS -> "liters"
            ItemUnit.JUGS_5L -> "5L jugs"
            ItemUnit.KG -> "kg"
            ItemUnit.CANS -> "cans"
            ItemUnit.PACKS -> "packs"
            ItemUnit.BOXES -> "boxes"
            ItemUnit.RATIONS -> "rations"
        }
    }

    fun statusFilterLabel(filter: StatusFilter): String = when (language) {
        AppLanguage.ES -> when (filter) {
            StatusFilter.ALL -> "Todos"
            StatusFilter.ALERT_ALL -> "Alertas (Cad. + Crít.)"
            StatusFilter.EXPIRED -> "Caducados"
            StatusFilter.CRITICAL -> "Críticos (≤ 30d)"
            StatusFilter.WARNING -> "Precaución (31-90d)"
            StatusFilter.OPTIMAL -> "Óptimos (> 90d)"
            StatusFilter.NONE -> "Sin caducidad"
        }
        AppLanguage.EN -> when (filter) {
            StatusFilter.ALL -> "All"
            StatusFilter.ALERT_ALL -> "Alerts (Exp. + Crit.)"
            StatusFilter.EXPIRED -> "Expired"
            StatusFilter.CRITICAL -> "Critical (≤ 30d)"
            StatusFilter.WARNING -> "Caution (31-90d)"
            StatusFilter.OPTIMAL -> "Optimal (> 90d)"
            StatusFilter.NONE -> "No expiry"
        }
    }

    fun sortOptionLabel(option: SortOption): String = when (language) {
        AppLanguage.ES -> when (option) {
            SortOption.EXPIRY_ASC -> "Caducidad (Próxima)"
            SortOption.EXPIRY_DESC -> "Caducidad (Lejana)"
            SortOption.NAME_ASC -> "Nombre (A-Z)"
            SortOption.QTY_ASC -> "Cantidad (Menor)"
            SortOption.QTY_DESC -> "Cantidad (Mayor)"
        }
        AppLanguage.EN -> when (option) {
            SortOption.EXPIRY_ASC -> "Expiry (Soonest)"
            SortOption.EXPIRY_DESC -> "Expiry (Farthest)"
            SortOption.NAME_ASC -> "Name (A-Z)"
            SortOption.QTY_ASC -> "Quantity (Lowest)"
            SortOption.QTY_DESC -> "Quantity (Highest)"
        }
    }

    fun expiryLabel(info: ExpiryInfo): String = when (info.status) {
        ExpiryStatus.NONE -> statusNoExpiry
        ExpiryStatus.EXPIRED -> statusExpired
        ExpiryStatus.CRITICAL -> if (info.isToday) statusExpiresToday else statusCritical
        ExpiryStatus.WARNING -> statusWarning
        ExpiryStatus.OPTIMAL -> statusOptimal
    }

    fun expirySublabel(info: ExpiryInfo): String = when (info.status) {
        ExpiryStatus.NONE -> statusNoExpirySub
        ExpiryStatus.EXPIRED -> {
            val d = abs(info.daysLeft)
            if (d == 1L) statusExpiredOneDay else statusExpiredDays(d)
        }
        ExpiryStatus.CRITICAL -> when {
            info.isToday -> statusExpiresTodaySub
            info.daysLeft == 1L -> statusInOneDay
            else -> statusInDays(info.daysLeft)
        }
        ExpiryStatus.WARNING, ExpiryStatus.OPTIMAL -> statusInDays(info.daysLeft)
    }
}

object SpanishStrings : AppStrings {
    override val language = AppLanguage.ES
    override val brandBunker = "BUNKER"
    override val brandStock = "STOCK"
    override val brandSubtitle = "Control Logístico & Caducidad"
    override val navWarehouse = "Almacén"
    override val navScanner = "Escáner"
    override val navSettings = "Ajustes"
    override val scanCode = "Escanear Código"
    override val addItem = "Añadir Artículo"
    override val dataMenu = "Datos"
    override val alertRotationPrefix = "Alerta de Rotación:"
    override val alertExpiredSingle = "1 artículo caducado"
    override val alertExpiredPlural: (Int) -> String = { "$it artículos caducados" }
    override val alertCriticalSingle = "1 próximo a caducar (<30 días)"
    override val alertCriticalPlural: (Int) -> String = { "$it próximos a caducar (<30 días)" }
    override val alertJoinAnd = " y "
    override val alertDescExpired = "Retira los productos caducados y prioriza el consumo de los próximos a vencer."
    override val alertDescCritical = "Consume o rota pronto los artículos marcados en ámbar antes de su vencimiento."
    override val reviewAffected = "Revisar Afectados"
    override val dismissAlert = "Ocultar alerta"
    override val kpiReferences = "REFERENCIAS"
    override val kpiTotalUnits: (String) -> String = { "$it unidades en total" }
    override val kpiExpired = "CADUCADOS"
    override val kpiExpiredSub = "Retirar o reemplazar"
    override val kpiCritical30d = "PRÓX. 30 DÍAS"
    override val kpiCriticalSub = "Prioridad de consumo"
    override val kpiWaterReserve = "RESERVA AGUA"
    override val kpiWaterDays: (Int, Int, Int) -> String = { days, p, l -> "~$days días ($p pers. / ${l}L)" }
    override val kpiFood = "ALIMENTOS"
    override val kpiFoodSub = "Unidades / envases activos"
    override val healthTitle = "ESTADO DE VIGENCIA DEL ALMACÉN"
    override val healthNoData = "Sin datos"
    override val healthValidPct: (Int) -> String = { "$it% Vigente" }
    override val legendOptimal = "Óptimo / Sin cad."
    override val legendWarning = "Precaución (31-90d)"
    override val legendCritical = "Crítico (≤30d)"
    override val legendExpired = "Caducado"
    override val searchPlaceholder = "Buscar nombre, código, ubicación..."
    override val clearSearch = "Limpiar búsqueda"
    override val allCategories = "Todas"
    override val allStatuses = "Todos"
    override val alertAllStatus = "Alertas"
    override val sortLabel = "Ordenar"
    override val showingCount: (Int) -> String = { "Mostrando $it artículos" }
    override val resetFilters = "Limpiar filtros"
    override val quickAdjustHint = "Usa + / - para ajustar existencias al instante"
    override val emptyTitle = "No hay artículos que mostrar"
    override val emptySubtitle = "Prueba ajustando los filtros o añade un nuevo artículo al almacén."
    override val expiryLabel = "Caducidad:"
    override val notApplicable = "No aplica"
    override val subtractOne = "Restar 1"
    override val addOne = "Sumar 1"
    override val qrLabelAction = "Etiqueta QR"
    override val editAction = "Editar"
    override val deleteAction = "Eliminar"
    override val statusNoExpiry = "Sin caducidad"
    override val statusNoExpirySub = "Material duradero"
    override val statusExpired = "Caducado"
    override val statusExpiredOneDay = "Hace 1 día"
    override val statusExpiredDays: (Long) -> String = { "Hace $it días" }
    override val statusExpiresToday = "Caduca HOY"
    override val statusExpiresTodaySub = "Consumo inmediato"
    override val statusCritical = "Crítico"
    override val statusWarning = "Precaución"
    override val statusOptimal = "Óptimo"
    override val statusInOneDay = "En 1 día"
    override val statusInDays: (Long) -> String = { "En $it días" }
    override val modalAddTitle = "Añadir al Inventario"
    override val modalEditTitle = "Editar Artículo"
    override val fieldName = "Nombre del artículo *"
    override val fieldNamePlaceholder = "Ej. Garrafas Agua Mineral 5L"
    override val fieldBarcode = "Código de barras / QR"
    override val fieldBarcodePlaceholder = "EAN-13, UPC o código BNK"
    override val fieldCategory = "Categoría"
    override val fieldLocation = "Ubicación en el bunker"
    override val fieldLocationPlaceholder = "Ej. Estantería 1 - Balda B"
    override val fieldQuantity = "Cantidad *"
    override val fieldUnit = "Unidad"
    override val fieldNoExpiry = "Sin fecha de caducidad"
    override val fieldNoExpirySub = "Material duradero o herramientas"
    override val fieldExpiryDate = "Fecha de caducidad (AAAA-MM-DD)"
    override val fieldNotes = "Notas tácticas / Lote"
    override val fieldNotesPlaceholder = "Observaciones de rotación o almacenamiento"
    override val cancel = "Cancelar"
    override val save = "Guardar"
    override val confirm = "Confirmar"
    override val close = "Cerrar"
    override val requiredFieldsError = "Revisa los campos obligatorios"
    override val scannerTitle = "Escáner Óptico / QR"
    override val scannerFormats = "EAN-13, EAN-8, UPC, Code-128, QR"
    override val scannerGuideHint = "Centra el código en el recuadro"
    override val scannerSwitchCamera = "Cambiar cámara"
    override val scannerTorch = "Linterna"
    override val scannerManualLabel = "Entrada manual de código"
    override val scannerManualPlaceholder = "Escribe EAN, código BNK o QR..."
    override val scannerManualSubmit = "Buscar"
    override val scannerLookingUp = "Consultando Open Food Facts..."
    override val scannerPermissionTitle = "Acceso a cámara requerido"
    override val scannerPermissionDesc = "Concede permiso de cámara para escanear códigos de barras y etiquetas QR, o usa la entrada manual inferior."
    override val scannerGrantPermission = "Activar cámara"
    override val scannerCameraUnavailable = "Cámara no disponible en este dispositivo. Usa la entrada manual."
    override val qsBadgeFound = "ARTÍCULO LOCALIZADO"
    override val qsCurrentStock = "Stock Actual"
    override val qsExpiry = "Caducidad"
    override val qsCode = "Código"
    override val qsAddStockLabel = "Añadir unidades al stock:"
    override val qsAddOneUnit = "+1 Unidad"
    override val qsAddCustom = "Sumar"
    override val qsEditItem = "Editar ficha"
    override val qsScanAnother = "Escanear otro"
    override val qrModalTitle = "Etiqueta QR de Almacén"
    override val qrHeaderBrand = "BUNKERSTOCK LOGÍSTICA"
    override val qrMetaFormat: (String, String) -> String = { code, date -> "COD: $code | CAD: $date" }
    override val qrPrint = "Imprimir"
    override val qrSharePng = "Compartir PNG"
    override val settingsTitle = "Ajustes"
    override val settingsSectionGeneral = "GENERAL"
    override val settingsLanguage = "Idioma / Language"
    override val settingsLanguageSub = "Cambia el idioma de toda la aplicación"
    override val languageSpanish = "Español"
    override val languageEnglish = "English"
    override val settingsTheme = "Tema visual"
    override val themeDark = "Oscuro"
    override val themeLight = "Claro"
    override val themeSystem = "Sistema"
    override val settingsSectionAutonomy = "AUTONOMÍA HÍDRICA"
    override val settingsWaterPersons = "Personas en el bunker"
    override val settingsWaterLiters = "Litros por persona / día"
    override val settingsWaterFormula: (Int, Int, Int) -> String = { p, l, total -> "$p pers. × ${l}L = ${total}L / día" }
    override val settingsSectionData = "DATOS Y COPIAS DE SEGURIDAD"
    override val exportJson = "Exportar copia JSON"
    override val exportJsonSub = "Copia completa restaurable"
    override val exportCsv = "Exportar CSV (Excel)"
    override val exportCsvSub = "Tabla separada por punto y coma con UTF-8"
    override val importJson = "Importar copia JSON"
    override val importJsonSub = "Reemplaza el inventario actual desde archivo"
    override val restoreDemo = "Restaurar datos de ejemplo"
    override val restoreDemoSub = "Carga los 9 artículos tácticos iniciales"
    override val clearAll = "Vaciar todo el almacén"
    override val clearAllSub = "Elimina permanentemente todos los registros"
    override val settingsSectionAbout = "ACERCA DE"
    override val aboutDesc = "BunkerStock v3.3 · Control logístico familiar de inventario, salud nutricional y caducidad con sincronización cifrada."
    override val confirmDeleteTitle = "Eliminar Artículo"
    override val confirmDeleteMsg: (String) -> String = { "¿Seguro que deseas eliminar \"$it\" del inventario?" }
    override val confirmDemoTitle = "Restaurar Datos de Ejemplo"
    override val confirmDemoMsg = "Esto reemplazará el inventario actual por los artículos de demostración iniciales. ¿Continuar?"
    override val confirmClearTitle = "Vaciar Todo el Almacén"
    override val confirmClearMsg = "Se eliminarán todos los registros permanentemente. Te recomendamos exportar una copia JSON antes."
    override val toastUpdated = "Artículo actualizado correctamente"
    override val toastAdded = "Artículo añadido al almacén"
    override val toastDeleted = "Artículo eliminado"
    override val toastJsonExported = "Copia JSON guardada"
    override val toastCsvExported = "Archivo CSV exportado para Excel"
    override val toastJsonImported: (Int) -> String = { "Importados $it artículos correctamente" }
    override val toastJsonInvalid = "Error: El archivo JSON no es válido"
    override val toastDemoLoaded = "Datos de ejemplo cargados"
    override val toastCleared = "Almacén vaciado por completo"
    override val toastQrApplied = "Datos QR aplicados al formulario"
    override val toastCodeAssigned: (String) -> String = { "Código asignado: $it" }
    override val toastStructuredQr = "QR estructurado reconocido"
    override val toastProductIdentified = "Producto identificado automáticamente"
    override val toastNewCodeDetected: (String) -> String = { "Nuevo código detectado: $it" }
    override val toastQuickAdded: (String, String, String) -> String = { amt, name, total -> "+$amt añadido a \"$name\" (Total: $total)" }
    override val toastExportError = "No se pudo completar la operación de archivo"
    override val toastQrShared = "Etiqueta QR lista para compartir"
    override val noExpiryCsv = "No caduca"
    override val scanFromImage = "Imagen QR"
    override val scanImageNoCode = "No se detectó ningún código en la imagen"
    override val popupDetectedNew = "PRODUCTO DETECTADO"
    override val popupOkRegister = "OK · Registrar"
    override val popupOkOpen = "OK · Editar"
    override val healthScoreLabel: (Int) -> String = { "Salud $it/100" }
    override val flagHighSugar = "Alto azúcar"
    override val flagHighSalt = "Alta sal"
    override val flagHighSatFat = "Grasa sat."
    override val flagUltraProcessed = "NOVA 4"
    override val flagHighProtein = "Alta proteína"
    override val flagHighFiber = "Alta fibra"
    override val expiryEstimatedNote: (Long) -> String = { "Caducidad estimada (~$it d) — verifica el envase" }
    override val expiryGs1Note = "Caducidad extraída del código GS1"
    override val settingsAutoUpdate = "Actualizaciones automáticas"
    override val settingsAutoUpdateSub = "Comprueba releases en GitHub (solo lectura, tus datos nunca salen del dispositivo)"
    override val settingsCheckUpdatesNow = "Buscar actualizaciones"
    override val settingsCheckUpdatesSub = "Comprueba si hay una nueva versión del APK"
    override val updateDialogTitle: (String) -> String = { "Actualización v$it disponible" }
    override val updateInstallNow = "Descargar e instalar"
    override val updateDownloading = "Descargando actualización..."
    override val updateUpToDate = "BunkerStock ya está actualizado"
    override val updateFailed = "No se pudo comprobar/descargar la actualización"
    override val creditsTitle = "Créditos · by Emefedez"
    override val creditsBody = "Gracias a Android / Jetpack Compose, Material 3, CameraX, Google ML Kit, ZXing, Open Food Facts, GitHub, android-design-skills y al equipo de Google DeepMind."
    override val productPhotoLabel = "Foto del producto"
    override val replacePhoto = "Cambiar foto"
    override val removePhoto = "Quitar"
    override val settingsSectionFamily = "GRUPO FAMILIAR (SINCRONIZACIÓN)"
    override val familyCreateOrJoin = "Crear o unirse a grupo familiar"
    override val familyCreateOrJoinSub = "Comparte el inventario sin cuentas ni contraseñas (por QR o enlace)"
    override val familyConnectedTo: (String) -> String = { "Conectado a: $it" }
    override val familyLastSync: (String) -> String = { "Última sincro: $it" }
    override val familySyncNow = "Sincronizar ahora"
    override val familyInviteQrLink = "Invitar (QR / Enlace)"
    override val familyLeave = "Salir del grupo"
    override val familyDialogTitle = "Grupo Familiar"
    override val familyGroupNameLabel = "Nombre del grupo"
    override val familyGroupNamePlaceholder = "Ej. Casa Familia"
    override val familyMemberNameLabel = "Tu nombre (opcional)"
    override val familyMemberNamePlaceholder = "Ej. Papá"
    override val familyCreateBtn = "Crear grupo nuevo"
    override val familyJoinSection = "O únete a un grupo existente"
    override val familyJoinPlaceholder = "Pega el enlace bunkerstock://join..."
    override val familyJoinBtn = "Unirse al grupo"
    override val familyScanInviteQr = "Escanear QR familiar"
    override val familyShareLink = "Compartir enlace"
    override val familyE2eNote = "Cifrado extremo a extremo (AES-256). No requiere registro ni claves y no sube fotos personales."
    override val toastFamilyCreated: (String) -> String = { "Grupo \"$it\" creado y sincronizado" }
    override val toastFamilyJoined: (String) -> String = { "Unido al grupo \"$it\"" }
    override val toastFamilySynced = "Inventario familiar sincronizado"
    override val toastFamilySyncError = "Sin conexión o error al sincronizar"
    override val toastFamilyLeft = "Has salido del grupo familiar"
    override val toastInviteInvalid = "Enlace o QR de grupo no válido"
    override val settingsExpiryNotif = "Notificaciones de caducidad"
    override val settingsExpiryNotifSub = "Avisa cuando un producto confirmado entra en rango peligroso"
    override val settingsSectionThresholds = "UMBRALES DE CADUCIDAD (DÍAS)"
    override val thresholdExpiredDays = "Caducado (días antes)"
    override val thresholdCriticalDays = "Crítico (≤ días)"
    override val thresholdWarningDays = "Precaución (≤ días)"
    override val filterButtonLabel = "Filtrar"
    override val filterCategoriesHeader = "CATEGORÍAS"
    override val filterStatusHeader = "ESTADO DE CADUCIDAD"
    override val batchQrMenu = "Batch QR (PDF A4)"
    override val batchQrMenuSub = "Selecciona productos y genera un PDF A4 listo para imprimir (24 QRs/hoja)"
    override val batchQrTitle = "Batch QR · PDF A4"
    override val batchQrSubtitle = "Elige los artículos a incluir. Caben 24 etiquetas escaneables por hoja A4 y se añaden páginas automáticamente."
    override val batchQrSelectAll = "Seleccionar todos"
    override val batchQrDeselectAll = "Quitar todos"
    override val batchQrSummary: (Int, Int) -> String = { count, pages -> "$count selec. · $pages pág. A4" }
    override val batchQrSavePdf = "Guardar PDF"
    override val batchQrSharePdf = "Compartir PDF"
    override val toastBatchPdfSaved = "PDF de etiquetas QR guardado"
    override val toastBatchPdfShared = "PDF de etiquetas QR listo para compartir"
    override val setupWelcomeSubtitle = "Elige tu idioma preferido. Podrás cambiarlo en cualquier momento desde Ajustes."
    override val setupContinue = "Comenzar"
    override val detailBatchesTitle = "TANDADAS / LOTES"
    override val detailAddBatch = "+ Tandada"
    override val detailAllBatches = "Todas"
    override val detailBatchNumber: (Int) -> String = { "Tandada $it" }
    override val detailCalendarToday = "Hoy"
    override val detailCalendarTapHint = "Toca un día para cambiar la caducidad de la tandada activa"
    override val detailQrSection = "ETIQUETA QR"
    override val detailNotesSection = "NOTAS Y UBICACIÓN"
    override val detailLocationEmpty = "Sin ubicación asignada"
    override val detailYukaLabel = "SCORE YUKA"
    override val detailNutriScoreLabel = "NUTRI-SCORE"
    override val detailNoScore = "Sin datos"
    override val detailBatchesBadge: (Int) -> String = { "$it tandadas" }
    override val detailAddedBy: (String) -> String = { "Añadido por $it" }
    override val detailPer100g = "POR 100 G"
    override val detailChartScale = "Barras en escala común de 0–100 g"
    override val detailTapPhotoHint = "Toca la foto para ampliar"
    override val familyAutoSync = "Sincronización automática (diffs)"
    override val familyAutoSyncSub = "Comprueba cambios en el grupo mientras la app está abierta"
    override val familySyncInterval = "Comprobar cada (seg.)"
    override val familyYourUser: (String) -> String = { "Tu usuario: $it" }
    override val familyYourUserEmpty = "Configura tu nombre de usuario"
    override val familyEditUserTitle = "Cambiar nombre de usuario"
    override val toastFamilyUserSaved = "Nombre de usuario guardado"
    override val thresholdResetDefault = "Restablecer (0 / 30 / 90)"
    override val shoppingModeLabel = "Modo Compra"
    override val shoppingModeActiveHint = "Escanea productos seguidos: se añaden solos al búnker"
    override val shoppingDoneBtn: (Int) -> String = { "Terminar compra ($it)" }
    override val shoppingResolving = "Identificando..."
}

object EnglishStrings : AppStrings {
    override val language = AppLanguage.EN
    override val brandBunker = "BUNKER"
    override val brandStock = "STOCK"
    override val brandSubtitle = "Logistics & Expiry Control"
    override val navWarehouse = "Stock"
    override val navScanner = "Scanner"
    override val navSettings = "Settings"
    override val scanCode = "Scan Code"
    override val addItem = "Add Item"
    override val dataMenu = "Data"
    override val alertRotationPrefix = "Rotation Alert:"
    override val alertExpiredSingle = "1 expired item"
    override val alertExpiredPlural: (Int) -> String = { "$it expired items" }
    override val alertCriticalSingle = "1 expiring soon (<30 days)"
    override val alertCriticalPlural: (Int) -> String = { "$it expiring soon (<30 days)" }
    override val alertJoinAnd = " and "
    override val alertDescExpired = "Remove expired goods and prioritize consuming items close to expiry."
    override val alertDescCritical = "Consume or rotate amber-marked items soon before expiration."
    override val reviewAffected = "Review Affected"
    override val dismissAlert = "Dismiss alert"
    override val kpiReferences = "REFERENCES"
    override val kpiTotalUnits: (String) -> String = { "$it total units" }
    override val kpiExpired = "EXPIRED"
    override val kpiExpiredSub = "Remove or replace"
    override val kpiCritical30d = "NEXT 30 DAYS"
    override val kpiCriticalSub = "Priority consumption"
    override val kpiWaterReserve = "WATER RESERVE"
    override val kpiWaterDays: (Int, Int, Int) -> String = { days, p, l -> "~$days days ($p ppl / ${l}L)" }
    override val kpiFood = "FOOD"
    override val kpiFoodSub = "Active units / packs"
    override val healthTitle = "WAREHOUSE FRESHNESS STATUS"
    override val healthNoData = "No data"
    override val healthValidPct: (Int) -> String = { "$it% Valid" }
    override val legendOptimal = "Optimal / No exp."
    override val legendWarning = "Caution (31-90d)"
    override val legendCritical = "Critical (≤30d)"
    override val legendExpired = "Expired"
    override val searchPlaceholder = "Search name, barcode, location..."
    override val clearSearch = "Clear search"
    override val allCategories = "All"
    override val allStatuses = "All"
    override val alertAllStatus = "Alerts"
    override val sortLabel = "Sort"
    override val showingCount: (Int) -> String = { "Showing $it items" }
    override val resetFilters = "Reset filters"
    override val quickAdjustHint = "Use + / - to adjust stock instantly"
    override val emptyTitle = "No items to display"
    override val emptySubtitle = "Try adjusting your filters or add a new item to the bunker."
    override val expiryLabel = "Expiry:"
    override val notApplicable = "N/A"
    override val subtractOne = "Subtract 1"
    override val addOne = "Add 1"
    override val qrLabelAction = "QR Label"
    override val editAction = "Edit"
    override val deleteAction = "Delete"
    override val statusNoExpiry = "No expiry"
    override val statusNoExpirySub = "Durable gear"
    override val statusExpired = "Expired"
    override val statusExpiredOneDay = "1 day ago"
    override val statusExpiredDays: (Long) -> String = { "$it days ago" }
    override val statusExpiresToday = "Expires TODAY"
    override val statusExpiresTodaySub = "Use immediately"
    override val statusCritical = "Critical"
    override val statusWarning = "Caution"
    override val statusOptimal = "Optimal"
    override val statusInOneDay = "In 1 day"
    override val statusInDays: (Long) -> String = { "In $it days" }
    override val modalAddTitle = "Add to Inventory"
    override val modalEditTitle = "Edit Item"
    override val fieldName = "Item name *"
    override val fieldNamePlaceholder = "e.g. Mineral Water Jugs 5L"
    override val fieldBarcode = "Barcode / QR"
    override val fieldBarcodePlaceholder = "EAN-13, UPC or BNK code"
    override val fieldCategory = "Category"
    override val fieldLocation = "Bunker location"
    override val fieldLocationPlaceholder = "e.g. Shelf 1 - Rack B"
    override val fieldQuantity = "Quantity *"
    override val fieldUnit = "Unit"
    override val fieldNoExpiry = "No expiration date"
    override val fieldNoExpirySub = "Durable material or tools"
    override val fieldExpiryDate = "Expiration date (YYYY-MM-DD)"
    override val fieldNotes = "Tactical notes / Batch"
    override val fieldNotesPlaceholder = "Rotation or storage notes"
    override val cancel = "Cancel"
    override val save = "Save"
    override val confirm = "Confirm"
    override val close = "Close"
    override val requiredFieldsError = "Please check the required fields"
    override val scannerTitle = "Optical / QR Scanner"
    override val scannerFormats = "EAN-13, EAN-8, UPC, Code-128, QR"
    override val scannerGuideHint = "Center code inside the frame"
    override val scannerSwitchCamera = "Switch camera"
    override val scannerTorch = "Flashlight"
    override val scannerManualLabel = "Manual code entry"
    override val scannerManualPlaceholder = "Enter EAN, BNK code or QR..."
    override val scannerManualSubmit = "Lookup"
    override val scannerLookingUp = "Querying Open Food Facts..."
    override val scannerPermissionTitle = "Camera permission needed"
    override val scannerPermissionDesc = "Grant camera access to scan barcodes and QR labels, or use manual entry below."
    override val scannerGrantPermission = "Enable camera"
    override val scannerCameraUnavailable = "Camera unavailable on this device. Use manual entry."
    override val qsBadgeFound = "ITEM LOCATED"
    override val qsCurrentStock = "Current Stock"
    override val qsExpiry = "Expiry"
    override val qsCode = "Code"
    override val qsAddStockLabel = "Add units to stock:"
    override val qsAddOneUnit = "+1 Unit"
    override val qsAddCustom = "Add"
    override val qsEditItem = "Edit item"
    override val qsScanAnother = "Scan another"
    override val qrModalTitle = "Warehouse QR Label"
    override val qrHeaderBrand = "BUNKERSTOCK LOGISTICS"
    override val qrMetaFormat: (String, String) -> String = { code, date -> "CODE: $code | EXP: $date" }
    override val qrPrint = "Print"
    override val qrSharePng = "Share PNG"
    override val settingsTitle = "Settings"
    override val settingsSectionGeneral = "GENERAL"
    override val settingsLanguage = "Language / Idioma"
    override val settingsLanguageSub = "Switch the language across the entire app"
    override val languageSpanish = "Español"
    override val languageEnglish = "English"
    override val settingsTheme = "Visual theme"
    override val themeDark = "Dark"
    override val themeLight = "Light"
    override val themeSystem = "System"
    override val settingsSectionAutonomy = "WATER AUTONOMY"
    override val settingsWaterPersons = "People in bunker"
    override val settingsWaterLiters = "Liters per person / day"
    override val settingsWaterFormula: (Int, Int, Int) -> String = { p, l, total -> "$p ppl × ${l}L = ${total}L / day" }
    override val settingsSectionData = "DATA & BACKUPS"
    override val exportJson = "Export JSON backup"
    override val exportJsonSub = "Full restorable inventory backup"
    override val exportCsv = "Export CSV (Excel)"
    override val exportCsvSub = "Semicolon-delimited UTF-8 spreadsheet"
    override val importJson = "Import JSON backup"
    override val importJsonSub = "Replace current inventory from file"
    override val restoreDemo = "Restore sample data"
    override val restoreDemoSub = "Loads the 9 initial tactical sample items"
    override val clearAll = "Clear all inventory"
    override val clearAllSub = "Permanently deletes all records"
    override val settingsSectionAbout = "ABOUT"
    override val aboutDesc = "BunkerStock v3.3 · Family logistics, product health scoring & expiry control with E2E encrypted sync."
    override val confirmDeleteTitle = "Delete Item"
    override val confirmDeleteMsg: (String) -> String = { "Are you sure you want to delete \"$it\" from inventory?" }
    override val confirmDemoTitle = "Restore Sample Data"
    override val confirmDemoMsg = "This will replace your current inventory with the initial sample items. Continue?"
    override val confirmClearTitle = "Clear All Inventory"
    override val confirmClearMsg = "All records will be permanently deleted. We recommend exporting a JSON backup first."
    override val toastUpdated = "Item updated"
    override val toastAdded = "Item added to stock"
    override val toastDeleted = "Item deleted"
    override val toastJsonExported = "JSON backup saved"
    override val toastCsvExported = "CSV exported for Excel"
    override val toastJsonImported: (Int) -> String = { "Imported $it items successfully" }
    override val toastJsonInvalid = "Error: Invalid JSON file"
    override val toastDemoLoaded = "Sample data loaded"
    override val toastCleared = "Inventory cleared"
    override val toastQrApplied = "QR data applied to form"
    override val toastCodeAssigned: (String) -> String = { "Code assigned: $it" }
    override val toastStructuredQr = "Structured QR recognized"
    override val toastProductIdentified = "Product identified automatically"
    override val toastNewCodeDetected: (String) -> String = { "New code detected: $it" }
    override val toastQuickAdded: (String, String, String) -> String = { amt, name, total -> "+$amt added to \"$name\" (Total: $total)" }
    override val toastExportError = "Could not complete file operation"
    override val toastQrShared = "QR label ready to share"
    override val noExpiryCsv = "No expiry"
    override val scanFromImage = "Image QR"
    override val scanImageNoCode = "No QR or barcode found in image"
    override val popupDetectedNew = "PRODUCT DETECTED"
    override val popupOkRegister = "OK · Register"
    override val popupOkOpen = "OK · Edit"
    override val healthScoreLabel: (Int) -> String = { "Health $it/100" }
    override val flagHighSugar = "High sugar"
    override val flagHighSalt = "High salt"
    override val flagHighSatFat = "High sat. fat"
    override val flagUltraProcessed = "NOVA 4"
    override val flagHighProtein = "High protein"
    override val flagHighFiber = "High fiber"
    override val expiryEstimatedNote: (Long) -> String = { "Estimated expiry (~$it d) — check package" }
    override val expiryGs1Note = "Expiry extracted from GS1 code"
    override val settingsAutoUpdate = "Automatic updates"
    override val settingsAutoUpdateSub = "Read-only GitHub Releases check (your inventory data never leaves this device)"
    override val settingsCheckUpdatesNow = "Check for updates"
    override val settingsCheckUpdatesSub = "Check if a newer APK release is available"
    override val updateDialogTitle: (String) -> String = { "Update v$it available" }
    override val updateInstallNow = "Download & install"
    override val updateDownloading = "Downloading update..."
    override val updateUpToDate = "BunkerStock is up to date"
    override val updateFailed = "Could not check/download update"
    override val creditsTitle = "Credits · by Emefedez"
    override val creditsBody = "Thanks to Android / Jetpack Compose, Material 3, CameraX, Google ML Kit, ZXing, Open Food Facts, GitHub, android-design-skills, and the Google DeepMind team."
    override val productPhotoLabel = "Product photo"
    override val replacePhoto = "Replace photo"
    override val removePhoto = "Remove"
    override val settingsSectionFamily = "FAMILY GROUP (SYNC)"
    override val familyCreateOrJoin = "Create or join a family group"
    override val familyCreateOrJoinSub = "Share stock with zero accounts or API keys (via QR or link)"
    override val familyConnectedTo: (String) -> String = { "Connected to: $it" }
    override val familyLastSync: (String) -> String = { "Last sync: $it" }
    override val familySyncNow = "Sync now"
    override val familyInviteQrLink = "Invite (QR / Link)"
    override val familyLeave = "Leave group"
    override val familyDialogTitle = "Family Group"
    override val familyGroupNameLabel = "Group name"
    override val familyGroupNamePlaceholder = "e.g. Family Home"
    override val familyMemberNameLabel = "Your name (optional)"
    override val familyMemberNamePlaceholder = "e.g. Dad"
    override val familyCreateBtn = "Create new group"
    override val familyJoinSection = "Or join an existing group"
    override val familyJoinPlaceholder = "Paste bunkerstock://join... link"
    override val familyJoinBtn = "Join group"
    override val familyScanInviteQr = "Scan family QR"
    override val familyShareLink = "Share link"
    override val familyE2eNote = "End-to-end encrypted (AES-256). No accounts or keys needed; personal photos stay on-device."
    override val toastFamilyCreated: (String) -> String = { "Group \"$it\" created and synced" }
    override val toastFamilyJoined: (String) -> String = { "Joined group \"$it\"" }
    override val toastFamilySynced = "Family inventory synced"
    override val toastFamilySyncError = "Offline or sync failed"
    override val toastFamilyLeft = "Left family group"
    override val toastInviteInvalid = "Invalid family invite link or QR"
    override val settingsExpiryNotif = "Expiry notifications"
    override val settingsExpiryNotifSub = "Alerts when a confirmed item enters the dangerous range"
    override val settingsSectionThresholds = "EXPIRY THRESHOLDS (DAYS)"
    override val thresholdExpiredDays = "Expired margin (days before)"
    override val thresholdCriticalDays = "Critical (≤ days)"
    override val thresholdWarningDays = "Caution (≤ days)"
    override val filterButtonLabel = "Filter"
    override val filterCategoriesHeader = "CATEGORIES"
    override val filterStatusHeader = "EXPIRY STATUS"
    override val batchQrMenu = "Batch QR (A4 PDF)"
    override val batchQrMenuSub = "Pick products and generate a print-ready A4 PDF (24 QRs/page)"
    override val batchQrTitle = "Batch QR · A4 PDF"
    override val batchQrSubtitle = "Select items to print. Fits 24 scannable QR labels per A4 sheet and flows to extra pages automatically."
    override val batchQrSelectAll = "Select all"
    override val batchQrDeselectAll = "Clear all"
    override val batchQrSummary: (Int, Int) -> String = { count, pages -> "$count selected · $pages A4 pg." }
    override val batchQrSavePdf = "Save PDF"
    override val batchQrSharePdf = "Share PDF"
    override val toastBatchPdfSaved = "Batch QR PDF saved"
    override val toastBatchPdfShared = "Batch QR PDF ready to share"
    override val setupWelcomeSubtitle = "Choose your preferred language. You can change this anytime in Settings."
    override val setupContinue = "Continue"
    override val detailBatchesTitle = "BATCHES / LOTS"
    override val detailAddBatch = "+ Batch"
    override val detailAllBatches = "All"
    override val detailBatchNumber: (Int) -> String = { "Batch $it" }
    override val detailCalendarToday = "Today"
    override val detailCalendarTapHint = "Tap any day to update the active batch's expiry date"
    override val detailQrSection = "QR LABEL"
    override val detailNotesSection = "NOTES & PLACEMENT"
    override val detailLocationEmpty = "No placement assigned"
    override val detailYukaLabel = "YUKA SCORE"
    override val detailNutriScoreLabel = "NUTRI-SCORE"
    override val detailNoScore = "No data"
    override val detailBatchesBadge: (Int) -> String = { "$it batches" }
    override val detailAddedBy: (String) -> String = { "Added by $it" }
    override val detailPer100g = "PER 100 G"
    override val detailChartScale = "Bars share a 0–100 g scale"
    override val detailTapPhotoHint = "Tap photo to zoom"
    override val familyAutoSync = "Auto-sync diffs"
    override val familyAutoSyncSub = "Checks for group changes while the app is open"
    override val familySyncInterval = "Check every (sec.)"
    override val familyYourUser: (String) -> String = { "Your user: $it" }
    override val familyYourUserEmpty = "Set your display name"
    override val familyEditUserTitle = "Edit your username"
    override val toastFamilyUserSaved = "Username updated"
    override val thresholdResetDefault = "Reset (0 / 30 / 90)"
    override val shoppingModeLabel = "Shopping Mode"
    override val shoppingModeActiveHint = "Scan items back-to-back — auto-added to your stock"
    override val shoppingDoneBtn: (Int) -> String = { "Finish haul ($it)" }
    override val shoppingResolving = "Identifying..."
}

fun stringsFor(language: AppLanguage): AppStrings = when (language) {
    AppLanguage.ES -> SpanishStrings
    AppLanguage.EN -> EnglishStrings
}

val LocalAppStrings = staticCompositionLocalOf<AppStrings> { SpanishStrings }
