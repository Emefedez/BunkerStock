package com.bunkerstock.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.bunkerstock.app.ui.components.categoryStyle
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
    onToggleCategory: (String) -> Unit,
    onStatusSelect: (StatusFilter) -> Unit,
    onToggleStatus: (StatusFilter) -> Unit,
    onSortSelect: (SortOption) -> Unit,
    onQuickFilterAlerts: () -> Unit,
    onResetFilters: () -> Unit,
    onDismissBanner: () -> Unit,
    onAdjustQty: (String, Double) -> Unit,
    onOpenDetail: (String, Float, Float) -> Unit,
    onOpenQr: (String) -> Unit,
    onEditItem: (InventoryItem) -> Unit,
    onDeleteItem: (InventoryItem) -> Unit,
    gridState: LazyGridState = rememberLazyGridState(),
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val colors = LocalBunkerColors.current
    val columns = if (isExpandedWidth) 2 else 1
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var filterMenuExpanded by remember { mutableStateOf(false) }

    val expiredDays = state.settings.expiredThresholdDays
    val criticalDays = state.settings.criticalThresholdDays
    val warningDays = state.settings.warningThresholdDays

    val filterButtonSummary = remember(
        state.selectedCategories,
        state.selectedStatuses,
        strings,
        criticalDays,
        warningDays
    ) {
        val labels = buildList {
            state.selectedCategories.forEach { key ->
                add(strings.categoryLabel(ItemCategory.fromKey(key)))
            }
            state.selectedStatuses.forEach { st ->
                add(strings.statusFilterLabelDynamic(st, criticalDays, warningDays))
            }
        }
        if (labels.isEmpty()) strings.filterButtonLabel else labels.joinToString(", ")
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // 1. Auto-scrolling single-row Critical Alert Banner
        if ((state.metrics.expiredCount > 0 || state.metrics.criticalCount > 0) && !state.bannerDismissed) {
            item(span = { GridItemSpan(columns) }) {
                CriticalAlertBanner(
                    expiredCount = state.metrics.expiredCount,
                    criticalCount = state.metrics.criticalCount,
                    criticalDays = criticalDays,
                    onReviewClick = onQuickFilterAlerts,
                    onDismiss = onDismissBanner
                )
            }
        }

        // 2. Military Telemetry Monitor Strip (KPI Cards)
        item(span = { GridItemSpan(columns) }) {
            val kpiModifier = Modifier.width(if (isExpandedWidth) 172.dp else 144.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    title = strings.kpiCriticalTitle(criticalDays),
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
                WarehouseHealthBar(
                    metrics = state.metrics,
                    criticalDays = criticalDays,
                    warningDays = warningDays
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.7f))
            }
        }

        // 4. Compact 40dp Single-Row Search + Filter Dropdown (Checkboxes) + Sort
        item(span = { GridItemSpan(columns) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Compact 40dp Search Field (same height as Filter & Sort buttons)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(BunkerRadius.Control))
                            .background(colors.cardSurface)
                            .border(
                                1.dp,
                                if (state.searchQuery.isNotEmpty()) MaterialTheme.colorScheme.primary else colors.borderSubtle,
                                RoundedCornerShape(BunkerRadius.Control)
                            )
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = colors.mutedText,
                            modifier = Modifier.size(16.dp)
                        )
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (state.searchQuery.isEmpty()) {
                                Text(
                                    text = strings.searchPlaceholder,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                                    color = colors.mutedText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = state.searchQuery,
                                onValueChange = onSearchChange,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchChange("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = strings.clearSearch,
                                    tint = colors.mutedText,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    // Compact 40dp Filter Dropdown Button with Checkboxes (overflows with "...")
                    Box {
                        val hasDropdownFilters = state.activeFilterCount > 0
                        Row(
                            modifier = Modifier
                                .height(40.dp)
                                .widthIn(min = 78.dp, max = 126.dp)
                                .clip(RoundedCornerShape(BunkerRadius.Control))
                                .background(if (hasDropdownFilters) colors.emeraldContainer else colors.cardSurface)
                                .border(
                                    1.dp,
                                    if (hasDropdownFilters) colors.emerald.copy(alpha = 0.65f) else colors.borderSubtle,
                                    RoundedCornerShape(BunkerRadius.Control)
                                )
                                .clickable(role = Role.Button) { filterMenuExpanded = true }
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = strings.filterButtonLabel,
                                tint = if (hasDropdownFilters) colors.emerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = filterButtonSummary,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = if (hasDropdownFilters) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (hasDropdownFilters) colors.emerald else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        DropdownMenu(
                            expanded = filterMenuExpanded,
                            onDismissRequest = { filterMenuExpanded = false },
                            modifier = Modifier
                                .widthIn(min = 230.dp)
                                .heightIn(max = 420.dp)
                        ) {
                            Text(
                                text = strings.filterCategoriesHeader,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.mutedText,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                            ItemCategory.entries.forEach { cat ->
                                val checked = state.selectedCategories.any {
                                    it.equals(cat.storageKey, ignoreCase = true)
                                }
                                val (catIcon, catFg, _) = categoryStyle(cat)
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Checkbox(
                                                checked = checked,
                                                onCheckedChange = null,
                                                colors = CheckboxDefaults.colors(checkedColor = colors.emerald)
                                            )
                                            Icon(
                                                imageVector = catIcon,
                                                contentDescription = null,
                                                tint = catFg,
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Text(
                                                text = strings.categoryLabel(cat),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    },
                                    onClick = { onToggleCategory(cat.storageKey) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                )
                            }

                            HorizontalDivider(
                                color = colors.borderSubtle.copy(alpha = 0.6f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            Text(
                                text = strings.filterStatusHeader,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.mutedText,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                            listOf(
                                StatusFilter.ALERT_ALL,
                                StatusFilter.EXPIRED,
                                StatusFilter.CRITICAL,
                                StatusFilter.WARNING,
                                StatusFilter.OPTIMAL,
                                StatusFilter.NONE
                            ).forEach { st ->
                                val checked = state.selectedStatuses.contains(st)
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Checkbox(
                                                checked = checked,
                                                onCheckedChange = null,
                                                colors = CheckboxDefaults.colors(checkedColor = colors.amber)
                                            )
                                            Text(
                                                text = strings.statusFilterLabelDynamic(st, criticalDays, warningDays),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    },
                                    onClick = { onToggleStatus(st) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                )
                            }

                            if (state.hasActiveFilters) {
                                HorizontalDivider(
                                    color = colors.borderSubtle.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FilterAltOff,
                                                contentDescription = null,
                                                tint = colors.danger,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = strings.resetFilters,
                                                color = colors.danger,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    },
                                    onClick = {
                                        onResetFilters()
                                        filterMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Compact 40dp Sort Button
                    Box {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(BunkerRadius.Control))
                                .background(colors.cardSurface)
                                .border(1.dp, colors.borderSubtle, RoundedCornerShape(BunkerRadius.Control))
                                .clickable(role = Role.Button) { sortMenuExpanded = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = strings.sortLabel,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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

                // Tight Count & Active Filter / Sort Summary Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.showingCount(state.filteredItems.size),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = colors.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (state.hasActiveFilters) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(BunkerRadius.Badge))
                                .clickable(role = Role.Button, onClick = onResetFilters)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterAltOff,
                                contentDescription = null,
                                tint = colors.amber,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = strings.resetFilters,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                color = colors.amber,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            text = strings.sortOptionLabel(state.sortOption),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = colors.mutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.7f))
        }

        // 5. Compact Inventory Items or Empty State
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
            item(span = { GridItemSpan(columns) }) {
                Spacer(Modifier.height(4.dp))
            }
            items(
                items = state.filteredItems,
                key = { it.id }
            ) { item ->
                InventoryItemCard(
                    item = item,
                    expiredDays = expiredDays,
                    criticalDays = criticalDays,
                    warningDays = warningDays,
                    onCardClick = { ox, oy -> onOpenDetail(item.id, ox, oy) },
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
