package com.bunkerstock.app.data

import kotlinx.serialization.Serializable

@Serializable
data class ProductNutrition(
    val nutriScoreGrade: String? = null, // A..E
    val novaGroup: Int? = null,          // 1..4
    val healthScore: Int? = null,        // 0..100
    val energyKcal: Double? = null,
    val proteinGrams: Double? = null,
    val carbsGrams: Double? = null,
    val sugarsGrams: Double? = null,
    val fatGrams: Double? = null,
    val satFatGrams: Double? = null,
    val fiberGrams: Double? = null,
    val saltGrams: Double? = null,
    val additivesCount: Int? = null,
    val imageUrl: String? = null,
    val safetyFlags: List<String> = emptyList() // HIGH_SUGAR, HIGH_SALT, HIGH_SAT_FAT, ULTRA_PROCESSED, HIGH_PROTEIN, HIGH_FIBER
)

@Serializable
data class ExpiryBatch(
    val id: String,
    val qty: Double = 1.0,
    val expiryDate: String = "", // yyyy-MM-dd
    val note: String = ""
)

@Serializable
data class InventoryItem(
    val id: String,
    val barcode: String = "",
    val name: String,
    val category: String = ItemCategory.FOOD.storageKey,
    val qty: Double = 1.0,
    val unit: String = ItemUnit.UNITS.storageKey,
    val noExpiry: Boolean = false,
    val expiryDate: String = "", // yyyy-MM-dd (soonest active expiry when batches exist)
    val location: String = "",
    val notes: String = "",
    val imageUri: String? = null,
    val nutrition: ProductNutrition? = null,
    val batches: List<ExpiryBatch> = emptyList(),
    val addedBy: String = "",
    val updatedAt: Long = 0L,
    val updatedBy: String = ""
)

@Serializable
data class FamilyGroupConfig(
    val roomId: String = "",
    val secretKey: String = "",
    val groupName: String = "",
    val memberName: String = "",
    val serverBaseUrl: String = FamilySyncClient.DEFAULT_SERVER,
    val lastSyncedAt: Long = 0L
) {
    val isJoined: Boolean get() = roomId.isNotBlank() && secretKey.isNotBlank()
}

@Serializable
data class FamilySyncSnapshot(
    val groupName: String = "",
    val items: List<InventoryItem> = emptyList(),
    val deletedIds: Map<String, Long> = emptyMap(),
    val updatedAt: Long = 0L
)

enum class ItemCategory(val storageKey: String) {
    FOOD("Alimentos"),
    WATER("Agua"),
    MATERIALS("Materiales"),
    TOOLS("Herramientas"),
    OTHER("Otro");

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
    val daysLeft: Long,
    val formattedDate: String,
    val isToday: Boolean = false
)

enum class StatusFilter {
    ALL,
    ALERT_ALL,
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
    val waterLitersPerPersonDay: Int = 3,
    val autoCheckUpdates: Boolean = true,
    val expiryNotificationsEnabled: Boolean = true,
    val expiredThresholdDays: Int = 0,
    val criticalThresholdDays: Int = 30,
    val warningThresholdDays: Int = 90,
    val familyAutoSyncEnabled: Boolean = true,
    val familySyncIntervalSec: Int = 15,
    val onboardingCompleted: Boolean = true
)

data class ItemPrefill(
    val barcode: String = "",
    val name: String = "",
    val category: String? = null,
    val unit: String? = null,
    val expiryDate: String? = null,
    val noExpiry: Boolean? = null,
    val notes: String = "",
    val imageUri: String? = null,
    val nutrition: ProductNutrition? = null
)

data class OpenFoodFactsResult(
    val name: String,
    val category: String,
    val unit: String,
    val nutrition: ProductNutrition? = null
)

data class DetectedScanCandidate(
    val code: String,
    val existingItemId: String? = null,
    val prefill: ItemPrefill? = null,
    val isLookingUp: Boolean = false
)

data class ShoppingScanEntry(
    val itemId: String,
    val barcode: String,
    val name: String,
    val addedQty: Double = 1.0,
    val isNew: Boolean = false,
    val isEnriching: Boolean = false
)

data class AppUpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val htmlUrl: String
)
