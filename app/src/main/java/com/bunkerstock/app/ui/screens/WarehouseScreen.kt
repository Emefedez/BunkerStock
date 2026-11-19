package com.bunkerstock.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bunkerstock.app.data.InventoryItem
import com.bunkerstock.app.data.ItemCategory
import com.bunkerstock.app.data.SortOption
import com.bunkerstock.app.data.StatusFilter
import com.bunkerstock.app.domain.InventoryLogic
import com.bunkerstock.app.ui.BunkerUiState
import com.bunkerstock.app.ui.components.CriticalAlertBanner
import com.bunkerstock.app.ui.components.InventoryItemCard
import com.bunkerstock.app.ui.components.KpiCard
import com.bunkerstock.app.ui.components.WarehouseHealthBar
import com.bunkerstock.app.ui.components.kpiIconFor
import com.bunkerstock.app.ui.theme.BunkerRadius
import com.bunkerstock.app.ui.theme.LocalAppStrings
import com.bunkerstock.app.ui.theme.LocalBunkerColors

@Composable
fun WarehouseScreen(
    state: BunkerUiState,
    isExpandedWidth: Boolean,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onStatusSelect: (StatusFilter) -> Unit,
    onSortSelect: (SortOption) -> Unit,
    onQuickFilterAlerts: () -> Unit,
    onResetFilters: () -> Unit,
    onDismissBanner: () -> Unit,
    onAdjustQty: (String, Double) -> Unit,
    onOpenQr: (String) -> Unit,
    onEditItem: (InventoryItem) -> Unit,
    onDeleteItem: (InventoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val colors = LocalBunkerColors.current
    val columns = if (isExpandedWidth) 2 else 1
    var sortMenuExpanded by remember { mutableStateOf(false) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // 1. Critical Alert Banner (flat strip)
        if ((state.metrics.expiredCount > 0 || state.metrics.criticalCount > 0) && !state.bannerDismissed) {
            item(span = { GridItemSpan(columns) }) {
                CriticalAlertBanner(
                    expiredCount = state.metrics.expiredCount,
                    criticalCount = state.metrics.criticalCount,
                    onReviewClick = onQuickFilterAlerts,
                    onDismiss = onDismissBanner
                )
            }
        }

        // 2. Military Telemetry Monitor Strip (KPI Cards)
        item(span = { GridItemSpan(columns) }) {
            val kpiModifier = Modifier.width(if (isExpandedWidth) 172.dp else 148.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    title = strings.kpiReferences,
                    value = state.metrics.totalRefs.toString(),
                    subtitle = strings.kpiTotalUnits(InventoryLogic.formatQty(state.metrics.totalUnits)),
                    accentColor = MaterialTheme.colorScheme.onSurface,
                    iconBgColor = colors.cardElevated,
                    icon = kpiIconFor("refs"),
                    onClick = onResetFilters,
                    modifier = kpiModifier
                )
                KpiCard(
                    title = strings.kpiExpired,
                    value = state.metrics.expiredCount.toString(),
                    subtitle = strings.kpiExpiredSub,
                    accentColor = colors.danger,
                    iconBgColor = colors.dangerContainer,
                    icon = kpiIconFor("expired"),
                    onClick = { onStatusSelect(StatusFilter.EXPIRED) },
                    modifier = kpiModifier
                )
                KpiCard(
                    title = strings.kpiCritical30d,
                    value = state.metrics.criticalCount.toString(),
                    subtitle = strings.kpiCriticalSub,
                    accentColor = colors.amber,
                    iconBgColor = colors.amberContainer,
                    icon = kpiIconFor("critical"),
                    onClick = { onStatusSelect(StatusFilter.CRITICAL) },
                    modifier = kpiModifier
                )
                KpiCard(
                    title = strings.kpiWaterReserve,
                    value = InventoryLogic.formatQty(state.metrics.waterLiters),
                    unitSuffix = "L",
                    subtitle = strings.kpiWaterDays(
                        state.metrics.waterDays,
                        state.settings.waterPersons,
                        state.settings.waterLitersPerPersonDay
                    ),
                    accentColor = colors.cyan,
                    iconBgColor = colors.cyanContainer,
                    icon = kpiIconFor("water"),
                    onClick = { onCategorySelect(ItemCategory.WATER.storageKey) },
                    modifier = kpiModifier
                )
                KpiCard(
                    title = strings.kpiFood,
                    value = InventoryLogic.formatQty(state.metrics.foodUnits),
                    subtitle = strings.kpiFoodSub,
                    accentColor = colors.emerald,
                    iconBgColor = colors.emeraldContainer,
                    icon = kpiIconFor("food"),
                    onClick = { onCategorySelect(ItemCategory.FOOD.storageKey) },
                    modifier = kpiModifier
                )
            }
        }

        // 3. Military Status Monitor Card (Health Bar)
        item(span = { GridItemSpan(columns) }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                WarehouseHealthBar(metrics = state.metrics)
                Spacer(Modifier.height(10.dp))
                androidx.compose.material3.HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.7f))
            }
        }

        // 4. Open Search, Category Chips, Status Chips & Sort (no card box)
        item(span = { GridItemSpan(columns) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                    // Search + Sort
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearchChange,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = {
                                Text(
                                    text = strings.searchPlaceholder,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.mutedText
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = colors.mutedText
                                )
                            },
                            trailingIcon = {
                                if (state.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchChange("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = strings.clearSearch,
                                            tint = colors.mutedText
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(BunkerRadius.Control),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colors.canvas,
                                unfocusedContainerColor = colors.canvas,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = colors.borderSubtle
                            )
                        )

                        Box {
                            OutlinedButton(
                                onClick = { sortMenuExpanded = true },
                                shape = RoundedCornerShape(BunkerRadius.Control),
                                border = BorderStroke(1.dp, colors.borderSubtle),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = strings.sortLabel,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false }
                            ) {
                                SortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = strings.sortOptionLabel(option),
                                                fontWeight = if (option == state.sortOption) FontWeight.Bold else FontWeight.Normal,
                                                color = if (option == state.sortOption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            onSortSelect(option)
                                            sortMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Category Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.selectedCategory == null,
                            onClick = { onCategorySelect(null) },
                            label = { Text(strings.allCategories) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.emeraldContainer,
                                selectedLabelColor = colors.emerald
                            )
                        )
                        ItemCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = state.selectedCategory.equals(cat.storageKey, ignoreCase = true),
                                onClick = {
                                    onCategorySelect(
                                        if (state.selectedCategory.equals(cat.storageKey, ignoreCase = true)) null
                                        else cat.storageKey
                                    )
                                },
                                label = { Text(strings.categoryLabel(cat)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.emeraldContainer,
                                    selectedLabelColor = colors.emerald
                                )
                            )
                        }
                    }

                    // Status Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            StatusFilter.ALL,
                            StatusFilter.ALERT_ALL,
                            StatusFilter.EXPIRED,
                            StatusFilter.CRITICAL,
                            StatusFilter.WARNING,
                            StatusFilter.OPTIMAL,
                            StatusFilter.NONE
                        ).forEach { st ->
                            FilterChip(
                                selected = state.selectedStatus == st,
                                onClick = { onStatusSelect(st) },
                                label = { Text(strings.statusFilterLabel(st)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.amberContainer,
                                    selectedLabelColor = colors.amber
                                )
                            )
                        }
                    }

                    // Count & Reset Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.showingCount(state.filteredItems.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.mutedText
                        )
                        if (state.hasActiveFilters) {
                            TextButton(onClick = onResetFilters) {
                                Icon(
                                    imageVector = Icons.Default.FilterAltOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(strings.resetFilters)
                            }
                        } else {
                            Text(
                                text = strings.sortOptionLabel(state.sortOption),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.mutedText
                            )
                        }
                    }
                }
                androidx.compose.material3.HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.7f))
        }

        // 5. Inventory Items or Empty State
        if (state.filteredItems.isEmpty()) {
            item(span = { GridItemSpan(columns) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = colors.mutedText,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = strings.emptyTitle,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = strings.emptySubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.mutedText,
                        textAlign = TextAlign.Center
                    )
                    if (state.hasActiveFilters) {
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(onClick = onResetFilters) {
                            Text(strings.resetFilters)
                        }
                    }
                }
            }
        } else {
            items(
                items = state.filteredItems,
                key = { it.id }
            ) { item ->
                InventoryItemCard(
                    item = item,
                    onDecrement = { onAdjustQty(item.id, -1.0) },
                    onIncrement = { onAdjustQty(item.id, 1.0) },
                    onQrClick = { onOpenQr(item.id) },
                    onEditClick = { onEditItem(item) },
                    onDeleteClick = { onDeleteItem(item) }
                )
            }
        }
    }
}
