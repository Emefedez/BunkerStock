package com.bunkerstock.app.data

import kotlinx.serialization.Serializable

@Serializable
data class InventoryItem(
    val id: String,
    val barcode: String = "",
    val name: String,
    val category: String = ItemCategory.FOOD.storageKey,
    val qty: Double = 1.0,
    val unit: String = ItemUnit.UNITS.storageKey,
    val noExpiry: Boolean = false,
    val expiryDate: String = "", // yyyy-MM-dd
    val location: String = "",
    val notes: String = ""
)

enum class ItemCategory(val storageKey: String) {
    FOOD("Alimentos"),
    WATER("Agua"),
    MATERIALS("Materiales"),
    TOOLS("Herramientas");

    companion object {
        fun fromKey(key: String?): ItemCategory =
            entries.firstOrNull { it.storageKey.equals(key, ignoreCase = true) } ?: FOOD
    }
}

enum class ItemUnit(val storageKey: String) {
    UNITS("unidades"),
    LITERS("litros"),
    JUGS_5L("garrafas 5L"),
    KG("kg"),
    CANS("latas"),
    PACKS("paquetes"),
    BOXES("cajas"),
    RATIONS("raciones");

    companion object {
        fun fromKey(key: String?): ItemUnit =
            entries.firstOrNull { it.storageKey.equals(key, ignoreCase = true) } ?: UNITS
    }
}

enum class ExpiryStatus {
    EXPIRED,
    CRITICAL,
    WARNING,
    OPTIMAL,
    NONE
}

data class ExpiryInfo(
    val status: ExpiryStatus,
    val daysLeft: Long, // Long.MAX_VALUE when NONE
    val formattedDate: String,
    val isToday: Boolean = false
)

enum class StatusFilter {
    ALL,
    ALERT_ALL, // EXPIRED + CRITICAL
    EXPIRED,
    CRITICAL,
    WARNING,
    OPTIMAL,
    NONE
}

enum class SortOption {
    EXPIRY_ASC,
    EXPIRY_DESC,
    NAME_ASC,
    QTY_ASC,
    QTY_DESC
}

data class DashboardMetrics(
    val totalRefs: Int = 0,
    val totalUnits: Double = 0.0,
    val expiredCount: Int = 0,
    val criticalCount: Int = 0,
    val warningCount: Int = 0,
    val optimalOrNoneCount: Int = 0,
    val waterLiters: Double = 0.0,
    val waterDays: Int = 0,
    val foodUnits: Double = 0.0,
    val healthyPercent: Int = 0
)

enum class AppLanguage(val code: String) {
    ES("es"),
    EN("en");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ES
    }
}

enum class AppThemeMode(val code: String) {
    DARK("dark"),
    LIGHT("light"),
    SYSTEM("system");

    companion object {
        fun fromCode(code: String?): AppThemeMode =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: DARK
    }
}

data class AppSettings(
    val language: AppLanguage = AppLanguage.ES,
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val waterPersons: Int = 2,
    val waterLitersPerPersonDay: Int = 3
)

data class ItemPrefill(
    val barcode: String = "",
    val name: String = "",
    val category: String? = null,
    val unit: String? = null,
    val expiryDate: String? = null,
    val noExpiry: Boolean? = null,
    val notes: String = ""
)

data class OpenFoodFactsResult(
    val name: String,
    val category: String,
    val unit: String
)
