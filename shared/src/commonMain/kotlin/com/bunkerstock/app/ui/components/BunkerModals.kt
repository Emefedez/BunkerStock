package com.bunkerstock.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bunkerstock.app.data.AppLanguage
import com.bunkerstock.app.data.AppUpdateInfo
import com.bunkerstock.app.data.FamilyGroupConfig
import com.bunkerstock.app.data.FamilySyncClient
import com.bunkerstock.app.data.InventoryItem
import com.bunkerstock.app.data.ItemCategory
import com.bunkerstock.app.data.ItemPrefill
import com.bunkerstock.app.data.ItemUnit
import com.bunkerstock.app.domain.InventoryLogic
import com.bunkerstock.app.platform.BunkerDate
import com.bunkerstock.app.platform.LocalBunkerPlatform
import com.bunkerstock.app.platform.currentTimeMillis
import com.bunkerstock.app.platform.isWebPlatform
import com.bunkerstock.app.ui.PendingConfirm
import com.bunkerstock.app.ui.theme.BunkerRadius
import com.bunkerstock.app.ui.theme.BunkerShieldLogo
import com.bunkerstock.app.ui.theme.LocalAppStrings
import com.bunkerstock.app.ui.theme.LocalBunkerColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditorSheet(
    editingItem: InventoryItem?,
    prefill: ItemPrefill?,
    onDismiss: () -> Unit,
    onScanBarcodeClick: () -> Unit,
    onSave: (InventoryItem, Boolean) -> Unit,
    onValidationError: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val colors = LocalBunkerColors.current
    val platform = LocalBunkerPlatform.current
    val isEdit = editingItem != null

    var name by remember(editingItem, prefill) {
        mutableStateOf(editingItem?.name ?: prefill?.name.orEmpty())
    }
    var barcode by remember(editingItem, prefill) {
        mutableStateOf(editingItem?.barcode ?: prefill?.barcode.orEmpty())
    }
    var category by remember(editingItem, prefill) {
        mutableStateOf(ItemCategory.fromKey(editingItem?.category ?: prefill?.category))
    }
    var location by remember(editingItem, prefill) {
        mutableStateOf(editingItem?.location.orEmpty())
    }
    var qtyText by remember(editingItem, prefill) {
        mutableStateOf(editingItem?.let { InventoryLogic.formatQty(it.qty) } ?: "1")
    }
    var unit by remember(editingItem, prefill) {
        mutableStateOf(ItemUnit.fromKey(editingItem?.unit ?: prefill?.unit))
    }
    var noExpiry by remember(editingItem, prefill) {
        mutableStateOf(editingItem?.noExpiry ?: prefill?.noExpiry ?: false)
    }
    var expiryDate by remember(editingItem, prefill) {
        mutableStateOf(
            editingItem?.expiryDate?.ifBlank { InventoryLogic.relativeDateString(180) }
                ?: prefill?.expiryDate?.ifBlank { InventoryLogic.relativeDateString(180) }
                ?: InventoryLogic.relativeDateString(180)
        )
    }
    var notes by remember(editingItem, prefill) {
        mutableStateOf(editingItem?.notes ?: prefill?.notes.orEmpty())
    }
    var imageUri by remember(editingItem, prefill) {
        mutableStateOf(
            editingItem?.imageUri ?: prefill?.imageUri ?: editingItem?.nutrition?.imageUrl ?: prefill?.nutrition?.imageUrl
        )
    }
    var nutrition by remember(editingItem, prefill) {
        mutableStateOf(editingItem?.nutrition ?: prefill?.nutrition)
    }

    LaunchedEffect(prefill) {
        if (prefill != null) {
            if (prefill.barcode.isNotBlank()) barcode = prefill.barcode
            if (prefill.name.isNotBlank() && name.isBlank()) name = prefill.name
            if (prefill.category != null) category = ItemCategory.fromKey(prefill.category)
            if (prefill.unit != null) unit = ItemUnit.fromKey(prefill.unit)
            if (!prefill.expiryDate.isNullOrBlank()) expiryDate = prefill.expiryDate
            if (prefill.noExpiry != null) noExpiry = prefill.noExpiry
            if (prefill.notes.isNotBlank() && notes.isBlank()) notes = prefill.notes
            if (prefill.imageUri != null) imageUri = prefill.imageUri
            if (prefill.nutrition != null) nutrition = prefill.nutrition
        }
    }

    var catExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val (selectedCatIcon, selectedCatFg, _) = categoryStyle(category)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.cardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isEdit) strings.modalEditTitle else strings.modalAddTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Product Photo Preview + Replace / Remove controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProductThumbnail(
                    imageSource = imageUri,
                    category = category,
                    size = 64.dp
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(strings.productPhotoLabel, style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                platform.pickCustomProductPhoto(editingItem?.id ?: "new") { saved ->
                                    if (saved != null) imageUri = saved
                                }
                            },
                            shape = RoundedCornerShape(BunkerRadius.Control)
                        ) {
                            Text(strings.replacePhoto)
                        }
                        if (!imageUri.isNullOrBlank()) {
                            TextButton(onClick = { imageUri = null }) {
                                Text(strings.removePhoto, color = colors.danger)
                            }
                        }
                    }
                }
            }

            nutrition?.let { nut ->
                NutritionBadgesRow(nutrition = nut)
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(strings.fieldName) },
                placeholder = { Text(strings.fieldNamePlaceholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text(strings.fieldBarcode) },
                    placeholder = { Text(strings.fieldBarcodePlaceholder) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onScanBarcodeClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.amber,
                        contentColor = Color(0xFF090D16)
                    ),
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .height(54.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = strings.scanCode)
                }
            }

            // Category Dropdown WITH category icons on both field and dropdown items!
            ExposedDropdownMenuBox(
                expanded = catExpanded,
                onExpandedChange = { catExpanded = !catExpanded }
            ) {
                OutlinedTextField(
                    value = strings.categoryLongLabel(category),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(strings.fieldCategory) },
                    leadingIcon = {
                        Icon(
                            imageVector = selectedCatIcon,
                            contentDescription = null,
                            tint = selectedCatFg,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = catExpanded,
                    onDismissRequest = { catExpanded = false }
                ) {
                    ItemCategory.entries.forEach { cat ->
                        val (optIcon, optFg, _) = categoryStyle(cat)
                        val isSelected = cat == category
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = optIcon,
                                    contentDescription = null,
                                    tint = optFg,
                                    modifier = Modifier.size(19.dp)
                                )
                            },
                            text = {
                                Text(
                                    text = strings.categoryLongLabel(cat),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) optFg else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                category = cat
                                if (!isEdit) {
                                    when (cat) {
                                        ItemCategory.WATER -> {
                                            unit = ItemUnit.LITERS
                                            noExpiry = false
                                        }
                                        ItemCategory.TOOLS -> {
                                            unit = ItemUnit.UNITS
                                            noExpiry = true
                                        }
                                        ItemCategory.FOOD -> {
                                            unit = ItemUnit.UNITS
                                            noExpiry = false
                                        }
                                        ItemCategory.MATERIALS, ItemCategory.OTHER -> Unit
                                    }
                                }
                                catExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = qtyText,
                onValueChange = { qtyText = it },
                label = { Text(strings.fieldQuantity) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.fieldNoExpiry, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        strings.fieldNoExpirySub,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.mutedText
                    )
                }
                Switch(
                    checked = noExpiry,
                    onCheckedChange = { noExpiry = it }
                )
            }

            if (!noExpiry) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = expiryDate,
                        onValueChange = { expiryDate = it },
                        label = { Text(strings.fieldExpiryDate) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = strings.fieldExpiryDate,
                            tint = colors.emerald
                        )
                    }
                }
            }

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text(strings.fieldLocation) },
                placeholder = { Text(strings.fieldLocationPlaceholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(strings.fieldNotes) },
                placeholder = { Text(strings.fieldNotesPlaceholder) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(strings.cancel)
                }
                Button(
                    onClick = {
                        val parsedQty = qtyText.replace(",", ".").toDoubleOrNull()
                        if (name.isBlank() || parsedQty == null || parsedQty < 0.0) {
                            onValidationError(strings.requiredFieldsError)
                            return@Button
                        }
                        val item = InventoryItem(
                            id = editingItem?.id ?: "item-${currentTimeMillis()}",
                            barcode = barcode.trim(),
                            name = name.trim(),
                            category = category.storageKey,
                            qty = parsedQty,
                            unit = unit.storageKey,
                            noExpiry = noExpiry,
                            expiryDate = if (noExpiry) "" else expiryDate.trim(),
                            location = location.trim(),
                            notes = notes.trim(),
                            imageUri = imageUri,
                            nutrition = nutrition,
                            batches = editingItem?.batches.orEmpty(),
                            addedBy = editingItem?.addedBy.orEmpty()
                        )
                        onSave(item, isEdit)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(strings.save, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        expiryDate = BunkerDate.fromEpochMillisUtc(millis).toIso()
                    }
                    showDatePicker = false
                }) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(strings.cancel)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickScanActionSheet(
    item: InventoryItem,
    onAddStock: (Double) -> Unit,
    onEditItem: () -> Unit,
    onScanAnother: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val colors = LocalBunkerColors.current
    val info = InventoryLogic.getExpiryInfo(item, notApplicableLabel = strings.notApplicable)
    var customQty by remember { mutableStateOf("1") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.cardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.emeraldContainer)
                        .border(1.dp, colors.emerald.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colors.emerald)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.qsBadgeFound,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.emerald
                    )
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.canvas,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.qsCurrentStock, style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
                            Text(
                                InventoryLogic.formatQty(item.qty),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.qsExpiry, style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
                            Text(
                                info.formattedDate,
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${strings.qsCode}: ${item.barcode.ifBlank { "N/A" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.mutedText
                        )
                        StatusBadge(info = info)
                    }
                }
            }

            Text(strings.qsAddStockLabel, style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onAddStock(1.0) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(strings.qsAddOneUnit, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onAddStock(6.0) },
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("+6", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customQty,
                    onValueChange = { customQty = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val v = customQty.replace(",", ".").toDoubleOrNull()
                        if (v != null && v > 0.0) onAddStock(v)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(54.dp)
                ) {
                    Text(strings.qsAddCustom)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEditItem,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.qsEditItem)
                }
                Button(
                    onClick = onScanAnother,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.amber,
                        contentColor = Color(0xFF090D16)
                    )
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.qsScanAnother, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun QrLabelDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onMessage: (String) -> Unit
) {
    val platform = LocalBunkerPlatform.current
    val strings = LocalAppStrings.current
    val colors = LocalBunkerColors.current
    val info = InventoryLogic.getExpiryInfo(item, notApplicableLabel = strings.notApplicable)
    val meta = strings.qrMetaFormat(item.barcode, info.formattedDate)

    val qrBitmap = remember(item.barcode) {
        platform.generateQrImage(item.barcode, 420)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.cardSurface,
            border = BorderStroke(1.dp, colors.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = null, tint = colors.amber)
                    Text(strings.qrModalTitle, style = MaterialTheme.typography.titleMedium)
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(2.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = strings.qrHeaderBrand,
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF475569)
                        )
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap,
                                contentDescription = item.barcode,
                                modifier = Modifier.size(180.dp)
                            )
                        }
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF090D16),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF334155),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            platform.printSingleQrLabel(
                                header = strings.qrHeaderBrand,
                                title = item.name,
                                meta = meta,
                                qrContent = item.barcode
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.qrPrint)
                    }

                    Button(
                        onClick = {
                            val ok = platform.shareSingleQrLabel(
                                header = strings.qrHeaderBrand,
                                title = item.name,
                                meta = meta,
                                qrContent = item.barcode
                            )
                            onMessage(if (ok) strings.toastQrShared else strings.toastExportError)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.qrSharePng)
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text(strings.close)
                }
            }
        }
    }
}

@Composable
fun ConfirmActionDialog(
    pending: PendingConfirm,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val colors = LocalBunkerColors.current

    val (title, msg, isDestructive) = when (pending) {
        is PendingConfirm.DeleteItem -> Triple(
            strings.confirmDeleteTitle,
            strings.confirmDeleteMsg(pending.item.name),
            true
        )
        PendingConfirm.RestoreDemo -> Triple(
            strings.confirmDemoTitle,
            strings.confirmDemoMsg,
            false
        )
        PendingConfirm.ClearAll -> Triple(
            strings.confirmClearTitle,
            strings.confirmClearMsg,
            true
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(msg) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (isDestructive) {
                    ButtonDefaults.buttonColors(containerColor = colors.danger, contentColor = Color.White)
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(strings.confirm)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
fun AppUpdateDialog(
    update: AppUpdateInfo,
    isDownloading: Boolean,
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        title = { Text(strings.updateDialogTitle(update.versionName), fontWeight = FontWeight.Bold) },
        text = {
            Text(
                if (isDownloading) strings.updateDownloading
                else update.releaseNotes.ifBlank { strings.settingsCheckUpdatesSub }
            )
        },
        confirmButton = {
            Button(
                onClick = onInstall,
                enabled = !isDownloading,
                shape = RoundedCornerShape(BunkerRadius.Control)
            ) {
                Text(strings.updateInstallNow, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDownloading) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
fun FamilyGroupDialog(
    familyGroup: FamilyGroupConfig,
    isSyncing: Boolean,
    onCreateGroup: (String, String) -> Unit,
    onJoinGroup: (String, String) -> Unit,
    onScanInviteQr: () -> Unit,
    onSyncNow: () -> Unit,
    onLeaveGroup: () -> Unit,
    onDismiss: () -> Unit
) {
    val platform = LocalBunkerPlatform.current
    val strings = LocalAppStrings.current
    val colors = LocalBunkerColors.current

    var groupName by remember { mutableStateOf(familyGroup.groupName.ifBlank { "Familia" }) }
    var memberName by remember { mutableStateOf(familyGroup.memberName) }
    var joinInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(BunkerRadius.Sheet),
            color = colors.cardSurface,
            border = BorderStroke(1.dp, colors.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Group, contentDescription = null, tint = colors.emerald)
                    Text(strings.familyDialogTitle, style = MaterialTheme.typography.titleLarge)
                }

                Text(
                    text = strings.familyE2eNote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.mutedText
                )

                if (familyGroup.isJoined) {
                    val inviteLink = remember(familyGroup, platform.webInviteBaseUrl) {
                        FamilySyncClient.buildInviteLink(familyGroup, webBaseUrl = platform.webInviteBaseUrl)
                    }
                    val qrBmp = remember(inviteLink) { platform.generateQrImage(inviteLink, 420) }

                    Surface(
                        shape = RoundedCornerShape(BunkerRadius.Card),
                        color = Color.White,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = familyGroup.groupName.uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF111312)
                            )
                            if (qrBmp != null) {
                                Image(
                                    bitmap = qrBmp,
                                    contentDescription = inviteLink,
                                    modifier = Modifier.size(180.dp)
                                )
                            }
                            Text(
                                text = inviteLink.take(44) + "…",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF475569),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                platform.shareText(
                                    title = strings.familyShareLink,
                                    text = "Únete al inventario familiar \"${familyGroup.groupName}\" en BunkerStock:\n$inviteLink"
                                )
                            },
                            shape = RoundedCornerShape(BunkerRadius.Control),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.familyShareLink, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onSyncNow,
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(BunkerRadius.Control)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = strings.familySyncNow, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onLeaveGroup) {
                            Text(strings.familyLeave, color = colors.danger)
                        }
                        TextButton(onClick = onDismiss) {
                            Text(strings.close)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text(strings.familyGroupNameLabel) },
                        placeholder = { Text(strings.familyGroupNamePlaceholder) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = memberName,
                        onValueChange = { memberName = it },
                        label = { Text(strings.familyMemberNameLabel) },
                        placeholder = { Text(strings.familyMemberNamePlaceholder) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onCreateGroup(groupName, memberName) },
                        enabled = !isSyncing,
                        shape = RoundedCornerShape(BunkerRadius.Control),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(strings.familyCreateBtn, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.7f))

                    Text(strings.familyJoinSection, style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = joinInput,
                        onValueChange = { joinInput = it },
                        placeholder = { Text(strings.familyJoinPlaceholder) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onScanInviteQr,
                            shape = RoundedCornerShape(BunkerRadius.Control),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(strings.familyScanInviteQr, maxLines = 1)
                        }
                        Button(
                            onClick = { onJoinGroup(joinInput, memberName) },
                            enabled = !isSyncing && joinInput.isNotBlank(),
                            shape = RoundedCornerShape(BunkerRadius.Control),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(strings.familyJoinBtn, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(strings.close)
                    }
                }
            }
        }
    }
}

@Composable
fun BatchQrPdfDialog(
    items: List<InventoryItem>,
    isBusy: Boolean,
    onSavePdf: (Set<String>) -> Unit,
    onSharePdf: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val colors = LocalBunkerColors.current
    var selectedIds by remember(items) {
        mutableStateOf(items.map { it.id }.toSet())
    }
    val allSelected = items.isNotEmpty() && selectedIds.size == items.size
    val pageCount = InventoryLogic.batchQrPageCount(selectedIds.size)

    Dialog(onDismissRequest = { if (!isBusy) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(BunkerRadius.Sheet),
            color = colors.cardSurface,
            border = BorderStroke(1.dp, colors.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = colors.amber)
                    Text(strings.batchQrTitle, style = MaterialTheme.typography.titleLarge)
                }

                Text(
                    text = strings.batchQrSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.mutedText
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            selectedIds = if (allSelected) emptySet() else items.map { it.id }.toSet()
                        },
                        enabled = !isBusy && items.isNotEmpty()
                    ) {
                        Text(
                            text = if (allSelected) strings.batchQrDeselectAll else strings.batchQrSelectAll,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(BunkerRadius.Badge),
                        color = colors.emeraldContainer,
                        border = BorderStroke(1.dp, colors.emerald.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = strings.batchQrSummary(selectedIds.size, pageCount),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = colors.emerald,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.7f))

                if (items.isEmpty()) {
                    Text(
                        text = strings.emptyTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.mutedText,
                        modifier = Modifier.padding(vertical = 18.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(items, key = { it.id }) { item ->
                            val checked = selectedIds.contains(item.id)
                            val cat = ItemCategory.fromKey(item.category)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(BunkerRadius.Control))
                                    .background(if (checked) colors.cardElevated else colors.canvas)
                                    .border(
                                        1.dp,
                                        if (checked) colors.emerald.copy(alpha = 0.45f) else colors.borderSubtle.copy(alpha = 0.5f),
                                        RoundedCornerShape(BunkerRadius.Control)
                                    )
                                    .clickable(enabled = !isBusy) {
                                        selectedIds = if (checked) selectedIds - item.id else selectedIds + item.id
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(checkedColor = colors.emerald)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${strings.categoryLabel(cat)} · ${item.barcode.ifBlank { "BNK-AUTO" }}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp
                                        ),
                                        color = colors.mutedText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.7f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSavePdf(selectedIds) },
                        enabled = !isBusy && selectedIds.isNotEmpty(),
                        shape = RoundedCornerShape(BunkerRadius.Control),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(strings.batchQrSavePdf, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Button(
                        onClick = { onSharePdf(selectedIds) },
                        enabled = !isBusy && selectedIds.isNotEmpty(),
                        shape = RoundedCornerShape(BunkerRadius.Control),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(strings.batchQrSharePdf, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    enabled = !isBusy,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(strings.close)
                }
            }
        }
    }
}

@Composable
fun FirstTimeSetupDialog(
    selectedLanguage: AppLanguage,
    onLanguageSelect: (AppLanguage) -> Unit,
    onComplete: () -> Unit
) {
    val strings = LocalAppStrings.current
    val colors = LocalBunkerColors.current
    val platform = LocalBunkerPlatform.current
    var pwaInstalled by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(BunkerRadius.Sheet),
            color = colors.cardSurface,
            border = BorderStroke(1.dp, colors.emerald.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BunkerShieldLogo(modifier = Modifier.size(56.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = strings.brandBunker,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Text(
                            text = strings.brandStock,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = colors.emerald
                        )
                    }
                    Text(
                        text = "Idioma / Language",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.amber
                    )
                    Text(
                        text = strings.setupWelcomeSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.mutedText,
                        textAlign = TextAlign.Center
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val options = listOf(
                        Triple(AppLanguage.ES, "Español", "Interfaz completa en español"),
                        Triple(AppLanguage.EN, "English", "Full interface in English")
                    )
                    options.forEach { (lang, title, sub) ->
                        val active = selectedLanguage == lang
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(BunkerRadius.Control))
                                .background(if (active) colors.emeraldContainer else colors.canvas)
                                .border(
                                    if (active) 1.5.dp else 1.dp,
                                    if (active) colors.emerald else colors.borderSubtle,
                                    RoundedCornerShape(BunkerRadius.Control)
                                )
                                .clickable(role = Role.RadioButton) { onLanguageSelect(lang) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (active) Icons.Default.CheckCircle else Icons.Default.Language,
                                contentDescription = null,
                                tint = if (active) colors.emerald else colors.mutedText,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (active) colors.emerald else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = sub,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                    color = colors.mutedText
                                )
                            }
                        }
                    }
                }

                // Optional Web App (PWA) install tip/button — Web only, hidden when already installed
                if (isWebPlatform && !platform.isRunningAsInstalledPwa) {
                    Surface(
                        shape = RoundedCornerShape(BunkerRadius.Control),
                        color = colors.canvas,
                        border = BorderStroke(1.dp, colors.cyan.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (pwaInstalled) Icons.Default.CheckCircle else Icons.Default.Download,
                                    contentDescription = null,
                                    tint = if (pwaInstalled) colors.emerald else colors.cyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (pwaInstalled) strings.setupPwaInstalled else strings.setupPwaTitle,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (pwaInstalled) colors.emerald else colors.cyan
                                )
                            }
                            if (!pwaInstalled) {
                                Text(
                                    text = if (platform.isIosBrowser) strings.setupPwaIosHint else strings.setupPwaGenericHint,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                    color = colors.mutedText
                                )
                                if (platform.canPromptPwaInstall) {
                                    OutlinedButton(
                                        onClick = {
                                            platform.promptPwaInstall { accepted ->
                                                if (accepted) pwaInstalled = true
                                            }
                                        },
                                        shape = RoundedCornerShape(BunkerRadius.Control),
                                        border = BorderStroke(1.dp, colors.cyan),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            Icons.Default.Download,
                                            contentDescription = null,
                                            tint = colors.cyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(strings.setupPwaInstallBtn, color = colors.cyan, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onComplete,
                    shape = RoundedCornerShape(BunkerRadius.Control),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text(strings.setupContinue, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
