package com.bunkerstock.app.ui.components

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bunkerstock.app.data.InventoryItem
import com.bunkerstock.app.data.ItemCategory
import com.bunkerstock.app.data.ShoppingScanEntry
import com.bunkerstock.app.domain.InventoryLogic
import com.bunkerstock.app.platform.BunkerDate
import com.bunkerstock.app.platform.PlatformBackHandler
import com.bunkerstock.app.ui.theme.BunkerRadius
import com.bunkerstock.app.ui.theme.LocalAppStrings
import com.bunkerstock.app.ui.theme.LocalBunkerColors

/**
 * Stacked Shopping Haul Review & Bulk-Grouping Overlay:
 * Lets the user quickly select subsets of scanned haul items (e.g. half the list -> Fridge,
 * another group -> expires in 30 days) or tap any individual item to edit expiry/location/details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingHaulReviewOverlay(
    entries: List<ShoppingScanEntry>,
    inventory: List<InventoryItem>,
    expiredDays: Int,
    criticalDays: Int,
    warningDays: Int,
    onAdjustEntry: (String, Double) -> Unit,
    onUndoEntry: (String) -> Unit,
    onOpenItemDetail: (String) -> Unit,
    onBatchUpdate: (Set<String>, String?, String?, Boolean?, ItemCategory?) -> Unit,
    onKeepScanning: () -> Unit,
    onFinishHaul: () -> Unit
) {
    val strings = LocalAppStrings.current
    val colors = LocalBunkerColors.current

    PlatformBackHandler(enabled = true) { onKeepScanning() }

    val inventoryById = remember(inventory) { inventory.associateBy { it.id } }
    val allIds = remember(entries) { entries.map { it.itemId }.toSet() }
    var selectedIds by remember(entries) {
        mutableStateOf(allIds)
    }
    val effectiveSelected = selectedIds.intersect(allIds)

    var customLocation by remember { mutableStateOf("") }
    var batchDatePickerTargetIds by remember { mutableStateOf<Set<String>?>(null) }

    val locationPresets = listOf(
        strings.shoppingPresetFridge,
        strings.shoppingPresetFreezer,
        strings.shoppingPresetPantry,
        strings.shoppingPresetBunker
    )
    val expiryPresets = listOf(
        "+7d" to 7L,
        "+15d" to 15L,
        "+30d" to 30L,
        "+90d" to 90L,
        "+1a" to 365L
    )

    val totalUnits = entries.sumOf { it.addedQty }.toInt()

    Surface(
        color = colors.canvas,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top bar
            Surface(color = colors.cardSurface, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = onKeepScanning, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = strings.shoppingKeepScanning)
                        }
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = colors.emerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.shoppingReviewTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                            )
                            Text(
                                text = strings.shoppingReviewSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.mutedText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.7f))
                }
            }

            // Selection toolbar: count + All / None / Invert
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.cardSurface.copy(alpha = 0.65f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.shoppingSelectedCount(effectiveSelected.size, entries.size),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (effectiveSelected.isNotEmpty()) colors.emerald else colors.mutedText
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallChipButton(
                        label = if (effectiveSelected.size == allIds.size) strings.batchQrDeselectAll else strings.batchQrSelectAll,
                        accent = colors.emerald,
                        onClick = {
                            selectedIds = if (effectiveSelected.size == allIds.size) emptySet() else allIds
                        }
                    )
                    SmallChipButton(
                        label = strings.shoppingInvertSelection,
                        accent = colors.amber,
                        onClick = {
                            selectedIds = allIds - effectiveSelected
                        }
                    )
                }
            }

            // Bulk-Grouping Action Panel (applies Location / Expiry / Category to all selected items at once!)
            if (effectiveSelected.isNotEmpty()) {
                Surface(
                    color = colors.cardSurface,
                    border = BorderStroke(1.dp, colors.emerald.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(BunkerRadius.Card),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Bulk Location presets + custom input
                        Text(
                            text = strings.shoppingBatchLocationTitle,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = colors.amber
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            locationPresets.forEach { preset ->
                                SmallChipButton(
                                    label = preset,
                                    accent = colors.amber,
                                    onClick = {
                                        onBatchUpdate(effectiveSelected, preset, null, null, null)
                                    }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customLocation,
                                onValueChange = { customLocation = it },
                                placeholder = {
                                    Text(
                                        strings.shoppingCustomLocationPlaceholder,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                                shape = RoundedCornerShape(BunkerRadius.Control),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = colors.canvas,
                                    unfocusedContainerColor = colors.canvas
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            )
                            Button(
                                onClick = {
                                    val loc = customLocation.trim()
                                    if (loc.isNotEmpty()) {
                                        onBatchUpdate(effectiveSelected, loc, null, null, null)
                                        customLocation = ""
                                    }
                                },
                                enabled = customLocation.isNotBlank(),
                                shape = RoundedCornerShape(BunkerRadius.Control),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(46.dp)
                            ) {
                                Text(strings.shoppingApplyBtn, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        // 2. Bulk Expiry presets + exact DatePicker
                        Text(
                            text = strings.shoppingBatchExpiryTitle,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = colors.emerald
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            expiryPresets.forEach { (label, days) ->
                                SmallChipButton(
                                    label = label,
                                    accent = colors.emerald,
                                    onClick = {
                                        val iso = InventoryLogic.relativeDateString(days)
                                        onBatchUpdate(effectiveSelected, null, iso, false, null)
                                    }
                                )
                            }
                            SmallChipButton(
                                label = strings.statusNoExpiry,
                                accent = colors.infoBlue,
                                onClick = {
                                    onBatchUpdate(effectiveSelected, null, "", true, null)
                                }
                            )
                            SmallChipButton(
                                label = strings.shoppingPickDateBtn,
                                accent = colors.cyan,
                                onClick = {
                                    batchDatePickerTargetIds = effectiveSelected
                                }
                            )
                        }

                        // 3. Bulk Category icons
                        Text(
                            text = strings.shoppingBatchCategoryTitle,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = colors.cyan
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ItemCategory.entries.forEach { cat ->
                                val (cIcon, cFg, cBg) = categoryStyle(cat)
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(BunkerRadius.Control))
                                        .background(cBg)
                                        .border(1.dp, cFg.copy(alpha = 0.5f), RoundedCornerShape(BunkerRadius.Control))
                                        .clickable {
                                            onBatchUpdate(effectiveSelected, null, null, null, cat)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(cIcon, contentDescription = null, tint = cFg, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = strings.categoryLabel(cat),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = cFg
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Stacked Haul List with instant per-item editing
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(entries, key = { it.itemId }) { entry ->
                    val item = inventoryById[entry.itemId]
                    val checked = effectiveSelected.contains(entry.itemId)
                    val category = ItemCategory.fromKey(item?.category)
                    val info = item?.let {
                        InventoryLogic.getExpiryInfo(
                            item = it,
                            notApplicableLabel = strings.notApplicable,
                            expiredDays = expiredDays,
                            criticalDays = criticalDays,
                            warningDays = warningDays
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(BunkerRadius.Card),
                        color = if (checked) colors.cardElevated else colors.cardSurface,
                        border = BorderStroke(
                            if (checked) 1.5.dp else 1.dp,
                            if (checked) colors.emerald.copy(alpha = 0.6f) else colors.borderSubtle
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        selectedIds = if (isChecked) selectedIds + entry.itemId else selectedIds - entry.itemId
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = colors.emerald),
                                    modifier = Modifier.size(28.dp)
                                )
                                ProductThumbnail(
                                    imageSource = item?.imageUri ?: item?.nutrition?.imageUrl,
                                    category = category,
                                    barcode = item?.barcode ?: entry.barcode,
                                    productName = item?.name ?: entry.name,
                                    size = 40.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (entry.isEnriching) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(13.dp),
                                                strokeWidth = 2.dp,
                                                color = colors.emerald
                                            )
                                        }
                                        Text(
                                            text = item?.name ?: entry.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        item?.nutrition?.energyKcal?.let { kcal ->
                                            Text(
                                                text = "${kcal.toInt()} kcal",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                ),
                                                color = colors.cyan
                                            )
                                        }
                                        item?.nutrition?.nutriScoreGrade?.let { grade ->
                                            Text(
                                                text = "NS $grade",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                ),
                                                color = colors.emerald
                                            )
                                        }
                                        item?.nutrition?.healthScore?.let { hs ->
                                            Text(
                                                text = "Yuka $hs",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp
                                                ),
                                                color = colors.amber
                                            )
                                        }
                                    }
                                }
                                // Qty stepper + undo
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onAdjustEntry(entry.itemId, -1.0) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(15.dp))
                                    }
                                    Text(
                                        text = "+${InventoryLogic.formatQty(entry.addedQty)}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = colors.emerald
                                    )
                                    IconButton(
                                        onClick = { onAdjustEntry(entry.itemId, 1.0) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = colors.emerald, modifier = Modifier.size(15.dp))
                                    }
                                    IconButton(
                                        onClick = { onUndoEntry(entry.itemId) },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = colors.danger, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            // Quick inline Expiry + Location + Full Detail row per stacked item
                            if (item != null && info != null) {
                                val (expFg, expBg) = expiryColors(info.status)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(BunkerRadius.Badge))
                                            .background(expBg)
                                            .border(1.dp, expFg.copy(alpha = 0.45f), RoundedCornerShape(BunkerRadius.Badge))
                                            .clickable { batchDatePickerTargetIds = setOf(item.id) }
                                            .padding(horizontal = 7.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = expFg, modifier = Modifier.size(12.dp))
                                        Text(
                                            text = "${strings.expiryCompactLabel(info)} · ${info.formattedDate}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                            color = expFg,
                                            maxLines = 1
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(BunkerRadius.Badge))
                                            .background(colors.canvas)
                                            .border(1.dp, colors.borderSubtle, RoundedCornerShape(BunkerRadius.Badge))
                                            .clickable { onOpenItemDetail(item.id) }
                                            .padding(horizontal = 7.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = colors.amber, modifier = Modifier.size(12.dp))
                                        Text(
                                            text = item.location.ifBlank { strings.detailLocationEmpty },
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = if (item.location.isBlank()) colors.mutedText else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = { onOpenItemDetail(item.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = strings.editAction, tint = colors.mutedText, modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom action bar: Keep scanning | Save & Finish
            Surface(color = colors.cardSurface, modifier = Modifier.fillMaxWidth()) {
                Column {
                    HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.7f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onKeepScanning,
                            shape = RoundedCornerShape(BunkerRadius.Control),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Text(strings.shoppingKeepScanning, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Button(
                            onClick = onFinishHaul,
                            shape = RoundedCornerShape(BunkerRadius.Control),
                            modifier = Modifier.weight(1.2f).height(46.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.shoppingDoneBtn(totalUnits), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }

    val pickerTargets = batchDatePickerTargetIds
    if (pickerTargets != null) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { batchDatePickerTargetIds = null },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val iso = BunkerDate.fromEpochMillisUtc(millis).toIso()
                        onBatchUpdate(pickerTargets, null, iso, false, null)
                    }
                    batchDatePickerTargetIds = null
                }) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { batchDatePickerTargetIds = null }) {
                    Text(strings.cancel)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun SmallChipButton(
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(BunkerRadius.Control))
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(BunkerRadius.Control))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = accent,
            maxLines = 1
        )
    }
}
