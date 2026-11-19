package com.bunkerstock.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bunkerstock.app.domain.InventoryLogic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.bunkerDataStore: DataStore<Preferences> by preferencesDataStore(name = "bunkerstock_prefs")

class BunkerRepository(private val context: Context) {

    private object Keys {
        val INVENTORY_JSON = stringPreferencesKey("bunkerstock_inventory_v1")
        val LANGUAGE = stringPreferencesKey("app_language")
        val THEME_MODE = stringPreferencesKey("app_theme_mode")
        val WATER_PERSONS = intPreferencesKey("water_persons")
        val WATER_LITERS_PER_DAY = intPreferencesKey("water_liters_per_day")
        val AUTO_CHECK_UPDATES = booleanPreferencesKey("auto_check_updates")
    }

    val inventoryFlow: Flow<List<InventoryItem>> = context.bunkerDataStore.data.map { prefs ->
        val stored = prefs[Keys.INVENTORY_JSON]
        if (stored == null) {
            InventoryLogic.getDefaultDemoData()
        } else {
            InventoryLogic.importFromJson(stored).getOrElse { InventoryLogic.getDefaultDemoData() }
        }
    }

    val settingsFlow: Flow<AppSettings> = context.bunkerDataStore.data.map { prefs ->
        AppSettings(
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
            themeMode = AppThemeMode.fromCode(prefs[Keys.THEME_MODE]),
            waterPersons = (prefs[Keys.WATER_PERSONS] ?: 2).coerceIn(1, 20),
            waterLitersPerPersonDay = (prefs[Keys.WATER_LITERS_PER_DAY] ?: 3).coerceIn(1, 15),
            autoCheckUpdates = prefs[Keys.AUTO_CHECK_UPDATES] ?: true
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
}
