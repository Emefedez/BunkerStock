package com.bunkerstock.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bunkerstock.app.data.AppLanguage
import com.bunkerstock.app.data.AppSettings
import com.bunkerstock.app.data.AppThemeMode
import com.bunkerstock.app.ui.theme.BunkerRadius
import com.bunkerstock.app.ui.theme.LocalAppStrings
import com.bunkerstock.app.ui.theme.LocalBunkerColors

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onLanguageChange: (AppLanguage) -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onWaterConfigChange: (Int, Int) -> Unit,
    onAutoUpdateChange: (Boolean) -> Unit,
    onCheckUpdatesNow: () -> Unit,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onImportJson: () -> Unit,
    onRestoreDemo: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val colors = LocalBunkerColors.current

    val segColors = SegmentedButtonDefaults.colors(
        activeContainerColor = colors.emeraldContainer,
        activeContentColor = colors.emerald,
        activeBorderColor = colors.emerald.copy(alpha = 0.6f),
        inactiveContainerColor = colors.canvas,
        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        inactiveBorderColor = colors.borderSubtle
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // SECTION 1: GENERAL (Language & Theme)
            SettingsSectionLabel(strings.settingsSectionGeneral)
            Surface(
                shape = RoundedCornerShape(BunkerRadius.Card),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Language Toggle (Español / English)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = colors.emerald)
                            Column {
                                Text(strings.settingsLanguage, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    strings.settingsLanguageSub,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.mutedText
                                )
                            }
                        }
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val langs = listOf(
                                AppLanguage.ES to strings.languageSpanish,
                                AppLanguage.EN to strings.languageEnglish
                            )
                            langs.forEachIndexed { idx, (lang, label) ->
                                SegmentedButton(
                                    selected = settings.language == lang,
                                    onClick = { onLanguageChange(lang) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = idx,
                                        count = langs.size,
                                        baseShape = RoundedCornerShape(BunkerRadius.Control)
                                    ),
                                    colors = segColors,
                                    icon = {}
                                ) {
                                    Text(
                                        text = label,
                                        fontWeight = if (settings.language == lang) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.6f))

                    // Theme selector
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = colors.amber)
                            Text(strings.settingsTheme, style = MaterialTheme.typography.titleMedium)
                        }
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val themes = listOf(
                                AppThemeMode.DARK to strings.themeDark,
                                AppThemeMode.LIGHT to strings.themeLight,
                                AppThemeMode.SYSTEM to strings.themeSystem
                            )
                            themes.forEachIndexed { idx, (mode, label) ->
                                SegmentedButton(
                                    selected = settings.themeMode == mode,
                                    onClick = { onThemeModeChange(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = idx,
                                        count = themes.size,
                                        baseShape = RoundedCornerShape(BunkerRadius.Control)
                                    ),
                                    colors = segColors,
                                    icon = {}
                                ) {
                                    Text(
                                        text = label,
                                        fontWeight = if (settings.themeMode == mode) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 2: WATER AUTONOMY
            SettingsSectionLabel(strings.settingsSectionAutonomy)
            Surface(
                shape = RoundedCornerShape(BunkerRadius.Card),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StepperSettingRow(
                        icon = Icons.Default.WaterDrop,
                        iconTint = colors.cyan,
                        label = strings.settingsWaterPersons,
                        value = settings.waterPersons,
                        onDecrement = { onWaterConfigChange(settings.waterPersons - 1, settings.waterLitersPerPersonDay) },
                        onIncrement = { onWaterConfigChange(settings.waterPersons + 1, settings.waterLitersPerPersonDay) }
                    )
                    HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.6f))
                    StepperSettingRow(
                        icon = Icons.Default.WaterDrop,
                        iconTint = colors.cyan,
                        label = strings.settingsWaterLiters,
                        value = settings.waterLitersPerPersonDay,
                        onDecrement = { onWaterConfigChange(settings.waterPersons, settings.waterLitersPerPersonDay - 1) },
                        onIncrement = { onWaterConfigChange(settings.waterPersons, settings.waterLitersPerPersonDay + 1) }
                    )
                    Text(
                        text = strings.settingsWaterFormula(
                            settings.waterPersons,
                            settings.waterLitersPerPersonDay,
                            settings.waterPersons * settings.waterLitersPerPersonDay
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.cyan
                    )
                }
            }

            // SECTION 3: DATA & BACKUPS
            SettingsSectionLabel(strings.settingsSectionData)
            Surface(
                shape = RoundedCornerShape(BunkerRadius.Card),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsActionRow(
                        icon = Icons.Default.Download,
                        iconTint = colors.emerald,
                        title = strings.exportJson,
                        subtitle = strings.exportJsonSub,
                        onClick = onExportJson
                    )
                    HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.6f))
                    SettingsActionRow(
                        icon = Icons.Default.TableView,
                        iconTint = colors.cyan,
                        title = strings.exportCsv,
                        subtitle = strings.exportCsvSub,
                        onClick = onExportCsv
                    )
                    HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.6f))
                    SettingsActionRow(
                        icon = Icons.Default.UploadFile,
                        iconTint = colors.amber,
                        title = strings.importJson,
                        subtitle = strings.importJsonSub,
                        onClick = onImportJson
                    )
                    HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.6f))
                    SettingsActionRow(
                        icon = Icons.Default.RestartAlt,
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                        title = strings.restoreDemo,
                        subtitle = strings.restoreDemoSub,
                        onClick = onRestoreDemo
                    )
                    HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.6f))
                    SettingsActionRow(
                        icon = Icons.Default.DeleteForever,
                        iconTint = colors.danger,
                        title = strings.clearAll,
                        subtitle = strings.clearAllSub,
                        titleColor = colors.danger,
                        onClick = onClearAll
                    )
                }
            }

            // SECTION 4: UPDATES & ABOUT
            SettingsSectionLabel(strings.settingsSectionAbout)
            Surface(
                shape = RoundedCornerShape(BunkerRadius.Card),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = colors.emerald, modifier = Modifier.size(22.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.settingsAutoUpdate, style = MaterialTheme.typography.titleMedium)
                            Text(
                                strings.settingsAutoUpdateSub,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.mutedText
                            )
                        }
                        Switch(
                            checked = settings.autoCheckUpdates,
                            onCheckedChange = onAutoUpdateChange
                        )
                    }
                    HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.6f))
                    SettingsActionRow(
                        icon = Icons.Default.Download,
                        iconTint = colors.amber,
                        title = strings.settingsCheckUpdatesNow,
                        subtitle = strings.settingsCheckUpdatesSub,
                        onClick = onCheckUpdatesNow
                    )
                    HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.6f))
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = colors.emerald)
                        Text(
                            text = strings.aboutDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.mutedText
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = LocalBunkerColors.current.mutedText,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun StepperSettingRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    val strings = LocalAppStrings.current
    val colors = LocalBunkerColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(BunkerRadius.Control))
                .background(colors.canvas)
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(BunkerRadius.Control))
        ) {
            IconButton(onClick = onDecrement, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.Remove, contentDescription = strings.subtractOne)
            }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                textAlign = TextAlign.Center,
                modifier = Modifier.width(34.dp)
            )
            IconButton(onClick = onIncrement, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.Add, contentDescription = strings.addOne, tint = colors.emerald)
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = titleColor)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalBunkerColors.current.mutedText
            )
        }
    }
}
