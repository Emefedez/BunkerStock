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
data class AppStrings(
    val language: AppLanguage,
    // Brand & Navigation
    val brandBunker: String,
    val brandStock: String,
    val brandSubtitle: String,
    val navWarehouse: String,
    val navScanner: String,
    val navSettings: String,
    // Header & Actions
    val scanCode: String,
    val addItem: String,
    val dataMenu: String,
    // Alert Banner
    val alertRotationPrefix: String,
    val alertExpiredSingle: String,
    val alertExpiredPlural: (Int) -> String,
    val alertCriticalSingle: String,
    val alertCriticalPlural: (Int) -> String,
    val alertJoinAnd: String,
    val alertDescExpired: String,
    val alertDescCritical: String,
    val reviewAffected: String,
    val dismissAlert: String,
    // KPIs
    val kpiReferences: String,
    val kpiTotalUnits: (String) -> String,
    val kpiExpired: String,
    val kpiExpiredSub: String,
    val kpiCritical30d: String,
    val kpiCriticalSub: String,
    val kpiWaterReserve: String,
    val kpiWaterDays: (Int, Int, Int) -> String,
    val kpiFood: String,
    val kpiFoodSub: String,
    // Health Bar
    val healthTitle: String,
    val healthNoData: String,
    val healthValidPct: (Int) -> String,
    val legendOptimal: String,
    val legendWarning: String,
    val legendCritical: String,
    val legendExpired: String,
    // Filters & Sort
    val searchPlaceholder: String,
    val clearSearch: String,
    val allCategories: String,
    val allStatuses: String,
    val alertAllStatus: String,
    val sortLabel: String,
    val showingCount: (Int) -> String,
    val resetFilters: String,
    val quickAdjustHint: String,
    // Empty State
    val emptyTitle: String,
    val emptySubtitle: String,
    // Item Card & Actions
    val expiryLabel: String,
    val notApplicable: String,
    val subtractOne: String,
    val addOne: String,
    val qrLabelAction: String,
    val editAction: String,
    val deleteAction: String,
    // Expiry Statuses
    val statusNoExpiry: String,
    val statusNoExpirySub: String,
    val statusExpired: String,
    val statusExpiredOneDay: String,
    val statusExpiredDays: (Long) -> String,
    val statusExpiresToday: String,
    val statusExpiresTodaySub: String,
    val statusCritical: String,
    val statusWarning: String,
    val statusOptimal: String,
    val statusInOneDay: String,
    val statusInDays: (Long) -> String,
    // Add/Edit Modal
    val modalAddTitle: String,
    val modalEditTitle: String,
    val fieldName: String,
    val fieldNamePlaceholder: String,
    val fieldBarcode: String,
    val fieldBarcodePlaceholder: String,
    val fieldCategory: String,
    val fieldLocation: String,
    val fieldLocationPlaceholder: String,
    val fieldQuantity: String,
    val fieldUnit: String,
    val fieldNoExpiry: String,
    val fieldNoExpirySub: String,
    val fieldExpiryDate: String,
    val fieldNotes: String,
    val fieldNotesPlaceholder: String,
    val cancel: String,
    val save: String,
    val confirm: String,
    val close: String,
    val requiredFieldsError: String,
    // Scanner
    val scannerTitle: String,
    val scannerFormats: String,
    val scannerGuideHint: String,
    val scannerSwitchCamera: String,
    val scannerTorch: String,
    val scannerManualLabel: String,
    val scannerManualPlaceholder: String,
    val scannerManualSubmit: String,
    val scannerLookingUp: String,
    val scannerPermissionTitle: String,
    val scannerPermissionDesc: String,
    val scannerGrantPermission: String,
    val scannerCameraUnavailable: String,
    // Quick Scan Sheet
    val qsBadgeFound: String,
    val qsCurrentStock: String,
    val qsExpiry: String,
    val qsCode: String,
    val qsAddStockLabel: String,
    val qsAddOneUnit: String,
    val qsAddCustom: String,
    val qsEditItem: String,
    val qsScanAnother: String,
    // QR Label Modal
    val qrModalTitle: String,
    val qrHeaderBrand: String,
    val qrMetaFormat: (String, String) -> String,
    val qrPrint: String,
    val qrSharePng: String,
    // Settings & Backup
    val settingsTitle: String,
    val settingsSectionGeneral: String,
    val settingsLanguage: String,
    val settingsLanguageSub: String,
    val languageSpanish: String,
    val languageEnglish: String,
    val settingsTheme: String,
    val themeDark: String,
    val themeLight: String,
    val themeSystem: String,
    val settingsSectionAutonomy: String,
    val settingsWaterPersons: String,
    val settingsWaterLiters: String,
    val settingsWaterFormula: (Int, Int, Int) -> String,
    val settingsSectionData: String,
    val exportJson: String,
    val exportJsonSub: String,
    val exportCsv: String,
    val exportCsvSub: String,
    val importJson: String,
    val importJsonSub: String,
    val restoreDemo: String,
    val restoreDemoSub: String,
    val clearAll: String,
    val clearAllSub: String,
    val settingsSectionAbout: String,
    val aboutDesc: String,
    // Confirm Dialogs
    val confirmDeleteTitle: String,
    val confirmDeleteMsg: (String) -> String,
    val confirmDemoTitle: String,
    val confirmDemoMsg: String,
    val confirmClearTitle: String,
    val confirmClearMsg: String,
    // Toasts / Snackbars
    val toastUpdated: String,
    val toastAdded: String,
    val toastDeleted: String,
    val toastJsonExported: String,
    val toastCsvExported: String,
    val toastJsonImported: (Int) -> String,
    val toastJsonInvalid: String,
    val toastDemoLoaded: String,
    val toastCleared: String,
    val toastQrApplied: String,
    val toastCodeAssigned: (String) -> String,
    val toastStructuredQr: String,
    val toastProductIdentified: String,
    val toastNewCodeDetected: (String) -> String,
    val toastQuickAdded: (String, String, String) -> String,
    val toastExportError: String,
    val toastQrShared: String,
    val noExpiryCsv: String,
    // v2.5: Non-blocking scan popup, image scan, nutrition/health, updates
    val scanFromImage: String,
    val scanImageNoCode: String,
    val popupDetectedNew: String,
    val popupOkRegister: String,
    val popupOkOpen: String,
    val healthScoreLabel: (Int) -> String,
    val flagHighSugar: String,
    val flagHighSalt: String,
    val flagHighSatFat: String,
    val flagUltraProcessed: String,
    val flagHighProtein: String,
    val flagHighFiber: String,
    val expiryEstimatedNote: (Long) -> String,
    val expiryGs1Note: String,
    val settingsAutoUpdate: String,
    val settingsAutoUpdateSub: String,
    val settingsCheckUpdatesNow: String,
    val settingsCheckUpdatesSub: String,
    val updateDialogTitle: (String) -> String,
    val updateInstallNow: String,
    val updateDownloading: String,
    val updateUpToDate: String,
    val updateFailed: String,
    val creditsTitle: String,
    val creditsBody: String,
    val productPhotoLabel: String,
    val replacePhoto: String,
    val removePhoto: String
) {
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
        }
        AppLanguage.EN -> when (category) {
            ItemCategory.FOOD -> "Food"
            ItemCategory.WATER -> "Water"
            ItemCategory.MATERIALS -> "Medical / Gear"
            ItemCategory.TOOLS -> "Tools / Power"
        }
    }

    fun categoryLongLabel(category: ItemCategory): String = when (language) {
        AppLanguage.ES -> when (category) {
            ItemCategory.FOOD -> "Alimentos"
            ItemCategory.WATER -> "Agua"
            ItemCategory.MATERIALS -> "Supervivencia / Botiquín"
            ItemCategory.TOOLS -> "Herramientas / Energía"
        }
        AppLanguage.EN -> when (category) {
            ItemCategory.FOOD -> "Food"
            ItemCategory.WATER -> "Water"
            ItemCategory.MATERIALS -> "Survival / First Aid"
            ItemCategory.TOOLS -> "Tools / Power"
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

val SpanishStrings = AppStrings(
    language = AppLanguage.ES,
    brandBunker = "BUNKER",
    brandStock = "STOCK",
    brandSubtitle = "Control Logístico & Caducidad",
    navWarehouse = "Almacén",
    navScanner = "Escáner",
    navSettings = "Ajustes",
    scanCode = "Escanear Código",
    addItem = "Añadir Artículo",
    dataMenu = "Datos",
    alertRotationPrefix = "Alerta de Rotación:",
    alertExpiredSingle = "1 artículo caducado",
    alertExpiredPlural = { "$it artículos caducados" },
    alertCriticalSingle = "1 próximo a caducar (<30 días)",
    alertCriticalPlural = { "$it próximos a caducar (<30 días)" },
    alertJoinAnd = " y ",
    alertDescExpired = "Retira los productos caducados y prioriza el consumo de los próximos a vencer.",
    alertDescCritical = "Consume o rota pronto los artículos marcados en ámbar antes de su vencimiento.",
    reviewAffected = "Revisar Afectados",
    dismissAlert = "Ocultar alerta",
    kpiReferences = "REFERENCIAS",
    kpiTotalUnits = { "$it unidades en total" },
    kpiExpired = "CADUCADOS",
    kpiExpiredSub = "Retirar o reemplazar",
    kpiCritical30d = "PRÓX. 30 DÍAS",
    kpiCriticalSub = "Prioridad de consumo",
    kpiWaterReserve = "RESERVA AGUA",
    kpiWaterDays = { days, p, l -> "~$days días ($p pers. / ${l}L)" },
    kpiFood = "ALIMENTOS",
    kpiFoodSub = "Unidades / envases activos",
    healthTitle = "ESTADO DE VIGENCIA DEL ALMACÉN",
    healthNoData = "Sin datos",
    healthValidPct = { "$it% Vigente" },
    legendOptimal = "Óptimo / Sin cad.",
    legendWarning = "Precaución (31-90d)",
    legendCritical = "Crítico (≤30d)",
    legendExpired = "Caducado",
    searchPlaceholder = "Buscar nombre, código, ubicación...",
    clearSearch = "Limpiar búsqueda",
    allCategories = "Todas",
    allStatuses = "Todos",
    alertAllStatus = "Alertas",
    sortLabel = "Ordenar",
    showingCount = { "Mostrando $it artículos" },
    resetFilters = "Limpiar filtros",
    quickAdjustHint = "Usa + / - para ajustar existencias al instante",
    emptyTitle = "No hay artículos que mostrar",
    emptySubtitle = "Prueba ajustando los filtros o añade un nuevo artículo al almacén.",
    expiryLabel = "Caducidad:",
    notApplicable = "No aplica",
    subtractOne = "Restar 1",
    addOne = "Sumar 1",
    qrLabelAction = "Etiqueta QR",
    editAction = "Editar",
    deleteAction = "Eliminar",
    statusNoExpiry = "Sin caducidad",
    statusNoExpirySub = "Material duradero",
    statusExpired = "Caducado",
    statusExpiredOneDay = "Hace 1 día",
    statusExpiredDays = { "Hace $it días" },
    statusExpiresToday = "Caduca HOY",
    statusExpiresTodaySub = "Consumo inmediato",
    statusCritical = "Crítico",
    statusWarning = "Precaución",
    statusOptimal = "Óptimo",
    statusInOneDay = "En 1 día",
    statusInDays = { "En $it días" },
    modalAddTitle = "Añadir al Inventario",
    modalEditTitle = "Editar Artículo",
    fieldName = "Nombre del artículo *",
    fieldNamePlaceholder = "Ej. Garrafas Agua Mineral 5L",
    fieldBarcode = "Código de barras / QR",
    fieldBarcodePlaceholder = "EAN-13, UPC o código BNK",
    fieldCategory = "Categoría",
    fieldLocation = "Ubicación en el bunker",
    fieldLocationPlaceholder = "Ej. Estantería 1 - Balda B",
    fieldQuantity = "Cantidad *",
    fieldUnit = "Unidad",
    fieldNoExpiry = "Sin fecha de caducidad",
    fieldNoExpirySub = "Material duradero o herramientas",
    fieldExpiryDate = "Fecha de caducidad (AAAA-MM-DD)",
    fieldNotes = "Notas tácticas / Lote",
    fieldNotesPlaceholder = "Observaciones de rotación o almacenamiento",
    cancel = "Cancelar",
    save = "Guardar",
    confirm = "Confirmar",
    close = "Cerrar",
    requiredFieldsError = "Revisa los campos obligatorios",
    scannerTitle = "Escáner Óptico / QR",
    scannerFormats = "EAN-13, EAN-8, UPC, Code-128, QR",
    scannerGuideHint = "Centra el código en el recuadro",
    scannerSwitchCamera = "Cambiar cámara",
    scannerTorch = "Linterna",
    scannerManualLabel = "Entrada manual de código",
    scannerManualPlaceholder = "Escribe EAN, código BNK o QR...",
    scannerManualSubmit = "Buscar",
    scannerLookingUp = "Consultando Open Food Facts...",
    scannerPermissionTitle = "Acceso a cámara requerido",
    scannerPermissionDesc = "Concede permiso de cámara para escanear códigos de barras y etiquetas QR, o usa la entrada manual inferior.",
    scannerGrantPermission = "Activar cámara",
    scannerCameraUnavailable = "Cámara no disponible en este dispositivo. Usa la entrada manual.",
    qsBadgeFound = "ARTÍCULO LOCALIZADO",
    qsCurrentStock = "Stock Actual",
    qsExpiry = "Caducidad",
    qsCode = "Código",
    qsAddStockLabel = "Añadir unidades al stock:",
    qsAddOneUnit = "+1 Unidad",
    qsAddCustom = "Sumar",
    qsEditItem = "Editar ficha",
    qsScanAnother = "Escanear otro",
    qrModalTitle = "Etiqueta QR de Almacén",
    qrHeaderBrand = "BUNKERSTOCK LOGÍSTICA",
    qrMetaFormat = { code, date -> "COD: $code | CAD: $date" },
    qrPrint = "Imprimir",
    qrSharePng = "Compartir PNG",
    settingsTitle = "Ajustes",
    settingsSectionGeneral = "GENERAL",
    settingsLanguage = "Idioma / Language",
    settingsLanguageSub = "Cambia el idioma de toda la aplicación",
    languageSpanish = "Español",
    languageEnglish = "English",
    settingsTheme = "Tema visual",
    themeDark = "Oscuro",
    themeLight = "Claro",
    themeSystem = "Sistema",
    settingsSectionAutonomy = "AUTONOMÍA HÍDRICA",
    settingsWaterPersons = "Personas en el bunker",
    settingsWaterLiters = "Litros por persona / día",
    settingsWaterFormula = { p, l, total -> "$p pers. × ${l}L = ${total}L / día" },
    settingsSectionData = "DATOS Y COPIAS DE SEGURIDAD",
    exportJson = "Exportar copia JSON",
    exportJsonSub = "Copia completa restaurable",
    exportCsv = "Exportar CSV (Excel)",
    exportCsvSub = "Tabla separada por punto y coma con UTF-8",
    importJson = "Importar copia JSON",
    importJsonSub = "Reemplaza el inventario actual desde archivo",
    restoreDemo = "Restaurar datos de ejemplo",
    restoreDemoSub = "Carga los 9 artículos tácticos iniciales",
    clearAll = "Vaciar todo el almacén",
    clearAllSub = "Elimina permanentemente todos los registros",
    settingsSectionAbout = "ACERCA DE",
    aboutDesc = "BunkerStock v2.8 · Control logístico de inventario, salud nutricional y caducidad diseñado para operación offline.",
    confirmDeleteTitle = "Eliminar Artículo",
    confirmDeleteMsg = { "¿Seguro que deseas eliminar \"$it\" del inventario?" },
    confirmDemoTitle = "Restaurar Datos de Ejemplo",
    confirmDemoMsg = "Esto reemplazará el inventario actual por los artículos de demostración iniciales. ¿Continuar?",
    confirmClearTitle = "Vaciar Todo el Almacén",
    confirmClearMsg = "Se eliminarán todos los registros permanentemente. Te recomendamos exportar una copia JSON antes.",
    toastUpdated = "Artículo actualizado correctamente",
    toastAdded = "Artículo añadido al almacén",
    toastDeleted = "Artículo eliminado",
    toastJsonExported = "Copia JSON guardada",
    toastCsvExported = "Archivo CSV exportado para Excel",
    toastJsonImported = { "Importados $it artículos correctamente" },
    toastJsonInvalid = "Error: El archivo JSON no es válido",
    toastDemoLoaded = "Datos de ejemplo cargados",
    toastCleared = "Almacén vaciado por completo",
    toastQrApplied = "Datos QR aplicados al formulario",
    toastCodeAssigned = { "Código asignado: $it" },
    toastStructuredQr = "QR estructurado reconocido",
    toastProductIdentified = "Producto identificado automáticamente",
    toastNewCodeDetected = { "Nuevo código detectado: $it" },
    toastQuickAdded = { amt, name, total -> "+$amt añadido a \"$name\" (Total: $total)" },
    toastExportError = "No se pudo completar la operación de archivo",
    toastQrShared = "Etiqueta QR lista para compartir",
    noExpiryCsv = "No caduca",
    scanFromImage = "Imagen QR",
    scanImageNoCode = "No se detectó ningún código en la imagen",
    popupDetectedNew = "PRODUCTO DETECTADO",
    popupOkRegister = "OK · Registrar",
    popupOkOpen = "OK · Editar",
    healthScoreLabel = { "Salud $it/100" },
    flagHighSugar = "Alto azúcar",
    flagHighSalt = "Alta sal",
    flagHighSatFat = "Grasa sat.",
    flagUltraProcessed = "NOVA 4",
    flagHighProtein = "Alta proteína",
    flagHighFiber = "Alta fibra",
    expiryEstimatedNote = { "Caducidad estimada (~$it d) — verifica el envase" },
    expiryGs1Note = "Caducidad extraída del código GS1",
    settingsAutoUpdate = "Actualizaciones automáticas",
    settingsAutoUpdateSub = "Comprueba releases en GitHub (solo lectura, tus datos nunca salen del dispositivo)",
    settingsCheckUpdatesNow = "Buscar actualizaciones",
    settingsCheckUpdatesSub = "Comprueba si hay una nueva versión del APK",
    updateDialogTitle = { "Actualización v$it disponible" },
    updateInstallNow = "Descargar e instalar",
    updateDownloading = "Descargando actualización...",
    updateUpToDate = "BunkerStock ya está actualizado",
    updateFailed = "No se pudo comprobar/descargar la actualización",
    creditsTitle = "Créditos · by Emefedez",
    creditsBody = "Gracias a Android / Jetpack Compose, Material 3, CameraX, Google ML Kit, ZXing, Open Food Facts, GitHub, android-design-skills y al equipo de Google DeepMind.",
    productPhotoLabel = "Foto del producto",
    replacePhoto = "Cambiar foto",
    removePhoto = "Quitar"
)

val EnglishStrings = AppStrings(
    language = AppLanguage.EN,
    brandBunker = "BUNKER",
    brandStock = "STOCK",
    brandSubtitle = "Logistics & Expiry Control",
    navWarehouse = "Stock",
    navScanner = "Scanner",
    navSettings = "Settings",
    scanCode = "Scan Code",
    addItem = "Add Item",
    dataMenu = "Data",
    alertRotationPrefix = "Rotation Alert:",
    alertExpiredSingle = "1 expired item",
    alertExpiredPlural = { "$it expired items" },
    alertCriticalSingle = "1 expiring soon (<30 days)",
    alertCriticalPlural = { "$it expiring soon (<30 days)" },
    alertJoinAnd = " and ",
    alertDescExpired = "Remove expired goods and prioritize consuming items close to expiry.",
    alertDescCritical = "Consume or rotate amber-marked items soon before expiration.",
    reviewAffected = "Review Affected",
    dismissAlert = "Dismiss alert",
    kpiReferences = "REFERENCES",
    kpiTotalUnits = { "$it total units" },
    kpiExpired = "EXPIRED",
    kpiExpiredSub = "Remove or replace",
    kpiCritical30d = "NEXT 30 DAYS",
    kpiCriticalSub = "Priority consumption",
    kpiWaterReserve = "WATER RESERVE",
    kpiWaterDays = { days, p, l -> "~$days days ($p ppl / ${l}L)" },
    kpiFood = "FOOD",
    kpiFoodSub = "Active units / packs",
    healthTitle = "WAREHOUSE FRESHNESS STATUS",
    healthNoData = "No data",
    healthValidPct = { "$it% Valid" },
    legendOptimal = "Optimal / No exp.",
    legendWarning = "Caution (31-90d)",
    legendCritical = "Critical (≤30d)",
    legendExpired = "Expired",
    searchPlaceholder = "Search name, barcode, location...",
    clearSearch = "Clear search",
    allCategories = "All",
    allStatuses = "All",
    alertAllStatus = "Alerts",
    sortLabel = "Sort",
    showingCount = { "Showing $it items" },
    resetFilters = "Reset filters",
    quickAdjustHint = "Use + / - to adjust stock instantly",
    emptyTitle = "No items to display",
    emptySubtitle = "Try adjusting your filters or add a new item to the bunker.",
    expiryLabel = "Expiry:",
    notApplicable = "N/A",
    subtractOne = "Subtract 1",
    addOne = "Add 1",
    qrLabelAction = "QR Label",
    editAction = "Edit",
    deleteAction = "Delete",
    statusNoExpiry = "No expiry",
    statusNoExpirySub = "Durable gear",
    statusExpired = "Expired",
    statusExpiredOneDay = "1 day ago",
    statusExpiredDays = { "$it days ago" },
    statusExpiresToday = "Expires TODAY",
    statusExpiresTodaySub = "Use immediately",
    statusCritical = "Critical",
    statusWarning = "Caution",
    statusOptimal = "Optimal",
    statusInOneDay = "In 1 day",
    statusInDays = { "In $it days" },
    modalAddTitle = "Add to Inventory",
    modalEditTitle = "Edit Item",
    fieldName = "Item name *",
    fieldNamePlaceholder = "e.g. Mineral Water Jugs 5L",
    fieldBarcode = "Barcode / QR",
    fieldBarcodePlaceholder = "EAN-13, UPC or BNK code",
    fieldCategory = "Category",
    fieldLocation = "Bunker location",
    fieldLocationPlaceholder = "e.g. Shelf 1 - Rack B",
    fieldQuantity = "Quantity *",
    fieldUnit = "Unit",
    fieldNoExpiry = "No expiration date",
    fieldNoExpirySub = "Durable material or tools",
    fieldExpiryDate = "Expiration date (YYYY-MM-DD)",
    fieldNotes = "Tactical notes / Batch",
    fieldNotesPlaceholder = "Rotation or storage notes",
    cancel = "Cancel",
    save = "Save",
    confirm = "Confirm",
    close = "Close",
    requiredFieldsError = "Please check the required fields",
    scannerTitle = "Optical / QR Scanner",
    scannerFormats = "EAN-13, EAN-8, UPC, Code-128, QR",
    scannerGuideHint = "Center code inside the frame",
    scannerSwitchCamera = "Switch camera",
    scannerTorch = "Flashlight",
    scannerManualLabel = "Manual code entry",
    scannerManualPlaceholder = "Enter EAN, BNK code or QR...",
    scannerManualSubmit = "Lookup",
    scannerLookingUp = "Querying Open Food Facts...",
    scannerPermissionTitle = "Camera permission needed",
    scannerPermissionDesc = "Grant camera access to scan barcodes and QR labels, or use manual entry below.",
    scannerGrantPermission = "Enable camera",
    scannerCameraUnavailable = "Camera unavailable on this device. Use manual entry.",
    qsBadgeFound = "ITEM LOCATED",
    qsCurrentStock = "Current Stock",
    qsExpiry = "Expiry",
    qsCode = "Code",
    qsAddStockLabel = "Add units to stock:",
    qsAddOneUnit = "+1 Unit",
    qsAddCustom = "Add",
    qsEditItem = "Edit item",
    qsScanAnother = "Scan another",
    qrModalTitle = "Warehouse QR Label",
    qrHeaderBrand = "BUNKERSTOCK LOGISTICS",
    qrMetaFormat = { code, date -> "CODE: $code | EXP: $date" },
    qrPrint = "Print",
    qrSharePng = "Share PNG",
    settingsTitle = "Settings",
    settingsSectionGeneral = "GENERAL",
    settingsLanguage = "Language / Idioma",
    settingsLanguageSub = "Switch the language across the entire app",
    languageSpanish = "Español",
    languageEnglish = "English",
    settingsTheme = "Visual theme",
    themeDark = "Dark",
    themeLight = "Light",
    themeSystem = "System",
    settingsSectionAutonomy = "WATER AUTONOMY",
    settingsWaterPersons = "People in bunker",
    settingsWaterLiters = "Liters per person / day",
    settingsWaterFormula = { p, l, total -> "$p ppl × ${l}L = ${total}L / day" },
    settingsSectionData = "DATA & BACKUPS",
    exportJson = "Export JSON backup",
    exportJsonSub = "Full restorable inventory backup",
    exportCsv = "Export CSV (Excel)",
    exportCsvSub = "Semicolon-delimited UTF-8 spreadsheet",
    importJson = "Import JSON backup",
    importJsonSub = "Replace current inventory from file",
    restoreDemo = "Restore sample data",
    restoreDemoSub = "Loads the 9 initial tactical sample items",
    clearAll = "Clear all inventory",
    clearAllSub = "Permanently deletes all records",
    settingsSectionAbout = "ABOUT",
    aboutDesc = "BunkerStock v2.8 · Tactical logistics, product health scoring & expiry control built for offline readiness.",
    confirmDeleteTitle = "Delete Item",
    confirmDeleteMsg = { "Are you sure you want to delete \"$it\" from inventory?" },
    confirmDemoTitle = "Restore Sample Data",
    confirmDemoMsg = "This will replace your current inventory with the initial sample items. Continue?",
    confirmClearTitle = "Clear All Inventory",
    confirmClearMsg = "All records will be permanently deleted. We recommend exporting a JSON backup first.",
    toastUpdated = "Item updated",
    toastAdded = "Item added to stock",
    toastDeleted = "Item deleted",
    toastJsonExported = "JSON backup saved",
    toastCsvExported = "CSV exported for Excel",
    toastJsonImported = { "Imported $it items successfully" },
    toastJsonInvalid = "Error: Invalid JSON file",
    toastDemoLoaded = "Sample data loaded",
    toastCleared = "Inventory cleared",
    toastQrApplied = "QR data applied to form",
    toastCodeAssigned = { "Code assigned: $it" },
    toastStructuredQr = "Structured QR recognized",
    toastProductIdentified = "Product identified automatically",
    toastNewCodeDetected = { "New code detected: $it" },
    toastQuickAdded = { amt, name, total -> "+$amt added to \"$name\" (Total: $total)" },
    toastExportError = "Could not complete file operation",
    toastQrShared = "QR label ready to share",
    noExpiryCsv = "No expiry",
    scanFromImage = "Image QR",
    scanImageNoCode = "No QR or barcode found in image",
    popupDetectedNew = "PRODUCT DETECTED",
    popupOkRegister = "OK · Register",
    popupOkOpen = "OK · Edit",
    healthScoreLabel = { "Health $it/100" },
    flagHighSugar = "High sugar",
    flagHighSalt = "High salt",
    flagHighSatFat = "High sat. fat",
    flagUltraProcessed = "NOVA 4",
    flagHighProtein = "High protein",
    flagHighFiber = "High fiber",
    expiryEstimatedNote = { "Estimated expiry (~$it d) — check package" },
    expiryGs1Note = "Expiry extracted from GS1 code",
    settingsAutoUpdate = "Automatic updates",
    settingsAutoUpdateSub = "Read-only GitHub Releases check (your inventory data never leaves this device)",
    settingsCheckUpdatesNow = "Check for updates",
    settingsCheckUpdatesSub = "Check if a newer APK release is available",
    updateDialogTitle = { "Update v$it available" },
    updateInstallNow = "Download & install",
    updateDownloading = "Downloading update...",
    updateUpToDate = "BunkerStock is up to date",
    updateFailed = "Could not check/download update",
    creditsTitle = "Credits · by Emefedez",
    creditsBody = "Thanks to Android / Jetpack Compose, Material 3, CameraX, Google ML Kit, ZXing, Open Food Facts, GitHub, android-design-skills, and the Google DeepMind team.",
    productPhotoLabel = "Product photo",
    replacePhoto = "Replace photo",
    removePhoto = "Remove"
)

fun stringsFor(language: AppLanguage): AppStrings = when (language) {
    AppLanguage.ES -> SpanishStrings
    AppLanguage.EN -> EnglishStrings
}

val LocalAppStrings = staticCompositionLocalOf { SpanishStrings }
