package com.bunkerstock.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bunkerstock.app.domain.InventoryLogic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString

private val Context.bunkerDataStore: DataStore<Preferences> by preferencesDataStore(name = "bunkerstock_prefs")

class BunkerRepository(private val context: Context) {

    private object Keys {
        val INVENTORY_JSON = stringPreferencesKey("bunkerstock_inventory_v1")
        val DELETED_IDS_JSON = stringPreferencesKey("bunkerstock_deleted_ids_v1")
        val FAMILY_GROUP_JSON = stringPreferencesKey("bunkerstock_family_group_v1")
        val NOTIFIED_EXPIRY_KEYS = stringSetPreferencesKey("notified_expiry_keys_v1")
        val LANGUAGE = stringPreferencesKey("app_language")
        val THEME_MODE = stringPreferencesKey("app_theme_mode")
        val WATER_PERSONS = intPreferencesKey("water_persons")
        val WATER_LITERS_PER_DAY = intPreferencesKey("water_liters_per_day")
        val AUTO_CHECK_UPDATES = booleanPreferencesKey("auto_check_updates")
        val EXPIRY_NOTIFICATIONS = booleanPreferencesKey("expiry_notifications_enabled")
        val EXPIRED_THRESHOLD_DAYS = intPreferencesKey("expired_threshold_days")
        val CRITICAL_THRESHOLD_DAYS = intPreferencesKey("critical_threshold_days")
        val WARNING_THRESHOLD_DAYS = intPreferencesKey("warning_threshold_days")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed_v1")
    }

    val inventoryFlow: Flow<List<InventoryItem>> = context.bunkerDataStore.data.map { prefs ->
        val stored = prefs[Keys.INVENTORY_JSON]
        if (stored == null) {
            InventoryLogic.getDefaultDemoData()
        } else {
            InventoryLogic.importFromJson(stored).getOrElse { InventoryLogic.getDefaultDemoData() }
        }
    }

    val deletedIdsFlow: Flow<Map<String, Long>> = context.bunkerDataStore.data.map { prefs ->
        val stored = prefs[Keys.DELETED_IDS_JSON] ?: return@map emptyMap()
        runCatching { InventoryLogic.json.decodeFromString<Map<String, Long>>(stored) }.getOrDefault(emptyMap())
    }

    val familyGroupFlow: Flow<FamilyGroupConfig> = context.bunkerDataStore.data.map { prefs ->
        val stored = prefs[Keys.FAMILY_GROUP_JSON] ?: return@map FamilyGroupConfig()
        runCatching { InventoryLogic.json.decodeFromString<FamilyGroupConfig>(stored) }.getOrDefault(FamilyGroupConfig())
    }

    val notifiedExpiryKeysFlow: Flow<Set<String>> = context.bunkerDataStore.data.map { prefs ->
        prefs[Keys.NOTIFIED_EXPIRY_KEYS] ?: emptySet()
    }

    val settingsFlow: Flow<AppSettings> = context.bunkerDataStore.data.map { prefs ->
        AppSettings(
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
            themeMode = AppThemeMode.fromCode(prefs[Keys.THEME_MODE]),
            waterPersons = (prefs[Keys.WATER_PERSONS] ?: 2).coerceIn(1, 20),
            waterLitersPerPersonDay = (prefs[Keys.WATER_LITERS_PER_DAY] ?: 3).coerceIn(1, 15),
            autoCheckUpdates = prefs[Keys.AUTO_CHECK_UPDATES] ?: true,
            expiryNotificationsEnabled = prefs[Keys.EXPIRY_NOTIFICATIONS] ?: true,
            expiredThresholdDays = (prefs[Keys.EXPIRED_THRESHOLD_DAYS] ?: 0).coerceIn(0, 30),
            criticalThresholdDays = (prefs[Keys.CRITICAL_THRESHOLD_DAYS] ?: 30).coerceIn(1, 365),
            warningThresholdDays = (prefs[Keys.WARNING_THRESHOLD_DAYS] ?: 90).coerceIn(2, 730),
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false
        )
    }

    suspend fun ensureInitialized() {
        val prefs = context.bunkerDataStore.data.first()
        if (prefs[Keys.INVENTORY_JSON] == null) {
            saveInventory(InventoryLogic.getDefaultDemoData())
        }
    }

    suspend fun saveInventory(items: List<InventoryItem>) {
        val encoded = InventoryLogic.exportToJson(items)
        context.bunkerDataStore.edit { prefs ->
            prefs[Keys.INVENTORY_JSON] = encoded
        }
    }

    suspend fun saveDeletedIds(deleted: Map<String, Long>) {
        val encoded = InventoryLogic.json.encodeToString(deleted)
        context.bunkerDataStore.edit { prefs ->
            prefs[Keys.DELETED_IDS_JSON] = encoded
        }
    }

    suspend fun saveFamilyGroup(config: FamilyGroupConfig) {
        val encoded = InventoryLogic.json.encodeToString(config)
        context.bunkerDataStore.edit { prefs ->
            prefs[Keys.FAMILY_GROUP_JSON] = encoded
        }
    }

    suspend fun clearFamilyGroup() {
        context.bunkerDataStore.edit { prefs ->
            prefs.remove(Keys.FAMILY_GROUP_JSON)
        }
    }

    suspend fun saveNotifiedExpiryKeys(keys: Set<String>) {
        context.bunkerDataStore.edit { prefs ->
            prefs[Keys.NOTIFIED_EXPIRY_KEYS] = keys
        }
    }

    suspend fun updateLanguage(language: AppLanguage) {
        context.bunkerDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.code
        }
    }

    suspend fun updateThemeMode(mode: AppThemeMode) {
        context.bunkerDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.code
        }
    }

    suspend fun updateWaterConfig(persons: Int, litersPerDay: Int) {
        context.bunkerDataStore.edit { prefs ->
            prefs[Keys.WATER_PERSONS] = persons.coerceIn(1, 20)
            prefs[Keys.WATER_LITERS_PER_DAY] = litersPerDay.coerceIn(1, 15)
        }
    }

    suspend fun updateAutoCheckUpdates(enabled: Boolean) {
        context.bunkerDataStore.edit { prefs ->
            prefs[Keys.AUTO_CHECK_UPDATES] = enabled
        }
    }

    suspend fun updateExpiryNotifications(enabled: Boolean) {
        context.bunkerDataStore.edit { prefs ->
            prefs[Keys.EXPIRY_NOTIFICATIONS] = enabled
        }
    }

    suspend fun updateExpiryThresholds(expiredDays: Int, criticalDays: Int, warningDays: Int) {
        val safeExpired = expiredDays.coerceIn(0, 30)
        val safeCritical = criticalDays.coerceIn(safeExpired + 1, 365)
        val safeWarning = warningDays.coerceIn(safeCritical + 1, 730)
        context.bunkerDataStore.edit { prefs ->
            prefs[Keys.EXPIRED_THRESHOLD_DAYS] = safeExpired
            prefs[Keys.CRITICAL_THRESHOLD_DAYS] = safeCritical
            prefs[Keys.WARNING_THRESHOLD_DAYS] = safeWarning
        }
    }

    suspend fun markOnboardingCompleted() {
        context.bunkerDataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = true
        }
    }
}
