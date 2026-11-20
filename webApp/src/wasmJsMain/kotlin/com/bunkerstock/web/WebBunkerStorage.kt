package com.bunkerstock.web

import com.bunkerstock.app.data.AppLanguage
import com.bunkerstock.app.data.AppSettings
import com.bunkerstock.app.data.AppThemeMode
import com.bunkerstock.app.data.FamilyGroupConfig
import com.bunkerstock.app.data.InventoryItem
import com.bunkerstock.app.domain.InventoryLogic
import com.bunkerstock.app.platform.BunkerStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.encodeToString

private fun jsStorageGet(key: String): String? = js("window.BunkerWeb.storageGet(key)")
private fun jsStorageSet(key: String, value: String): Unit = js("window.BunkerWeb.storageSet(key, value)")
private fun jsStorageRemove(key: String): Unit = js("window.BunkerWeb.storageRemove(key)")

class WebBunkerStorage : BunkerStorage {

    private companion object {
        const val KEY_INVENTORY = "bunkerstock_inventory_v1"
        const val KEY_DELETED = "bunkerstock_deleted_ids_v1"
        const val KEY_FAMILY = "bunkerstock_family_group_v1"
        const val KEY_SETTINGS = "bunkerstock_settings_v1"
    }

    private val _inventory = MutableStateFlow(loadInventory())
    private val _deleted = MutableStateFlow(loadDeleted())
    private val _family = MutableStateFlow(loadFamily())
    private val _settings = MutableStateFlow(loadSettings())

    override val inventoryFlow: Flow<List<InventoryItem>> = _inventory
    override val deletedIdsFlow: Flow<Map<String, Long>> = _deleted
    override val familyGroupFlow: Flow<FamilyGroupConfig> = _family
    override val settingsFlow: Flow<AppSettings> = _settings

    private fun loadInventory(): List<InventoryItem> {
        val raw = jsStorageGet(KEY_INVENTORY) ?: return InventoryLogic.getDefaultDemoData()
        return InventoryLogic.importFromJson(raw).getOrElse { InventoryLogic.getDefaultDemoData() }
    }

    private fun loadDeleted(): Map<String, Long> {
        val raw = jsStorageGet(KEY_DELETED) ?: return emptyMap()
        return runCatching { InventoryLogic.json.decodeFromString<Map<String, Long>>(raw) }.getOrDefault(emptyMap())
    }

    private fun loadFamily(): FamilyGroupConfig {
        val raw = jsStorageGet(KEY_FAMILY) ?: return FamilyGroupConfig()
        return runCatching { InventoryLogic.json.decodeFromString<FamilyGroupConfig>(raw) }.getOrDefault(FamilyGroupConfig())
    }

    private fun loadSettings(): AppSettings {
        val raw = jsStorageGet(KEY_SETTINGS) ?: return AppSettings(onboardingCompleted = false)
        return runCatching {
            val map = InventoryLogic.json.decodeFromString<Map<String, String>>(raw)
            AppSettings(
                language = AppLanguage.fromCode(map["lang"]),
                themeMode = AppThemeMode.fromCode(map["theme"]),
                waterPersons = (map["wp"]?.toIntOrNull() ?: 2).coerceIn(1, 20),
                waterLitersPerPersonDay = (map["wl"]?.toIntOrNull() ?: 3).coerceIn(1, 15),
                autoCheckUpdates = false,
                expiryNotificationsEnabled = false,
                expiredThresholdDays = (map["exp"]?.toIntOrNull() ?: 0).coerceIn(0, 30),
                criticalThresholdDays = (map["crit"]?.toIntOrNull() ?: 30).coerceIn(1, 365),
                warningThresholdDays = (map["warn"]?.toIntOrNull() ?: 90).coerceIn(2, 730),
                familyAutoSyncEnabled = map["fauto"]?.toBooleanStrictOrNull() ?: true,
                familySyncIntervalSec = (map["fint"]?.toIntOrNull() ?: 15).coerceIn(5, 300),
                onboardingCompleted = map["onb"]?.toBooleanStrictOrNull() ?: false
            )
        }.getOrDefault(AppSettings(onboardingCompleted = false))
    }

    private fun persistSettings(s: AppSettings) {
        _settings.value = s
        val map = mapOf(
            "lang" to s.language.code,
            "theme" to s.themeMode.code,
            "wp" to s.waterPersons.toString(),
            "wl" to s.waterLitersPerPersonDay.toString(),
            "exp" to s.expiredThresholdDays.toString(),
            "crit" to s.criticalThresholdDays.toString(),
            "warn" to s.warningThresholdDays.toString(),
            "fauto" to s.familyAutoSyncEnabled.toString(),
            "fint" to s.familySyncIntervalSec.toString(),
            "onb" to s.onboardingCompleted.toString()
        )
        jsStorageSet(KEY_SETTINGS, InventoryLogic.json.encodeToString(map))
    }

    override suspend fun ensureInitialized() {
        if (jsStorageGet(KEY_INVENTORY) == null) {
            saveInventory(InventoryLogic.getDefaultDemoData())
        }
    }

    override suspend fun saveInventory(items: List<InventoryItem>) {
        _inventory.value = items
        jsStorageSet(KEY_INVENTORY, InventoryLogic.exportToJson(items))
    }

    override suspend fun saveDeletedIds(deleted: Map<String, Long>) {
        _deleted.value = deleted
        jsStorageSet(KEY_DELETED, InventoryLogic.json.encodeToString(deleted))
    }

    override suspend fun saveFamilyGroup(config: FamilyGroupConfig) {
        _family.value = config
        jsStorageSet(KEY_FAMILY, InventoryLogic.json.encodeToString(config))
    }

    override suspend fun clearFamilyGroup() {
        _family.value = FamilyGroupConfig()
        jsStorageRemove(KEY_FAMILY)
    }

    override suspend fun updateLanguage(language: AppLanguage) {
        persistSettings(_settings.value.copy(language = language))
    }

    override suspend fun updateThemeMode(mode: AppThemeMode) {
        persistSettings(_settings.value.copy(themeMode = mode))
    }

    override suspend fun updateWaterConfig(persons: Int, litersPerDay: Int) {
        persistSettings(
            _settings.value.copy(
                waterPersons = persons.coerceIn(1, 20),
                waterLitersPerPersonDay = litersPerDay.coerceIn(1, 15)
            )
        )
    }

    override suspend fun updateAutoCheckUpdates(enabled: Boolean) {
        persistSettings(_settings.value.copy(autoCheckUpdates = enabled))
    }

    override suspend fun updateExpiryNotifications(enabled: Boolean) {
        persistSettings(_settings.value.copy(expiryNotificationsEnabled = enabled))
    }

    override suspend fun updateExpiryThresholds(expiredDays: Int, criticalDays: Int, warningDays: Int) {
        val safeExpired = expiredDays.coerceIn(0, 30)
        val safeCritical = criticalDays.coerceIn(safeExpired + 1, 365)
        val safeWarning = warningDays.coerceIn(safeCritical + 1, 730)
        persistSettings(
            _settings.value.copy(
                expiredThresholdDays = safeExpired,
                criticalThresholdDays = safeCritical,
                warningThresholdDays = safeWarning
            )
        )
    }

    override suspend fun markOnboardingCompleted() {
        persistSettings(_settings.value.copy(onboardingCompleted = true))
    }

    override suspend fun updateFamilySyncPrefs(enabled: Boolean, intervalSec: Int) {
        persistSettings(
            _settings.value.copy(
                familyAutoSyncEnabled = enabled,
                familySyncIntervalSec = intervalSec.coerceIn(5, 300)
            )
        )
    }
}
