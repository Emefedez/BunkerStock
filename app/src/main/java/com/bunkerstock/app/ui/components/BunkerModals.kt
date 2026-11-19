package com.bunkerstock.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import com.bunkerstock.app.data.ProductImageStore
import kotlinx.coroutines.launch
import com.bunkerstock.app.data.AppUpdateInfo
import com.bunkerstock.app.data.InventoryItem
import com.bunkerstock.app.data.ItemCategory
import com.bunkerstock.app.data.ItemPrefill
import com.bunkerstock.app.data.ItemUnit
import com.bunkerstock.app.domain.InventoryLogic
import com.bunkerstock.app.scanner.QrCodeUtils
import com.bunkerstock.app.ui.PendingConfirm
import com.bunkerstock.app.ui.theme.BunkerRadius
import com.bunkerstock.app.ui.theme.LocalAppStrings
import com.bunkerstock.app.ui.theme.LocalBunkerColors
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

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
    val isEdit = editingItem != null

    var name by remember(editingItem, prefill) {
        mutableStateOf(editingItem?.name ?: prefill?.name.orEmpty())
    }
    var barcode by remember(editingItem, prefill) {
        mutableStateOf(editingItem?.barcode ?: prefill?.barcode.orEmpty())
    }
    var category by remember(editingItem, prefill) {
        mutableStateOf(
            ItemCategory.fromKey(editingItem?.category ?: prefill?.category)
        )
    }
    var location by remember(editingItem) {
        mutableStateOf(editingItem?.location.orEmpty())
    }
    var qtyText by remember(editingItem) {
        mutableStateOf(
            editingItem?.let { InventoryLogic.formatQty(it.qty) } ?: "1"
        )
    }
    var unit by remember(editingItem, prefill) {
        mutableStateOf(
            ItemUnit.fromKey(editingItem?.unit ?: prefill?.unit)
        )
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
        mutableStateOf(editingItem?.imageUri ?: prefill?.imageUri ?: editingItem?.nutrition?.imageUrl ?: prefill?.nutrition?.imageUrl)
    }
    var nutrition by remember(editingItem, prefill) {
        mutableStateOf(editingItem?.nutrition ?: prefill?.nutrition)
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val saved = ProductImageStore.saveCustomImage(
                    context = context,
                    uri = uri,
                    itemId = editingItem?.id ?: "new"
                )
                if (saved != null) imageUri = saved
            }
        }
    }

    // Apply prefill updates if scanner returned while sheet is open
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
    var unitExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                            onClick = { photoPicker.launch("image/*") },
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

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(strings.fieldName) },
                placeholder = { Text(strings.fieldNamePlaceholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Barcode + Inline Scan Button
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

            // Category Dropdown (with smart defaults on new item)
            ExposedDropdownMenuBox(
                expanded = catExpanded,
                onExpandedChange = { catExpanded = !catExpanded }
            ) {
                OutlinedTextField(
                    value = strings.categoryLongLabel(category),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(strings.fieldCategory) },
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
                        DropdownMenuItem(
                            text = { Text(strings.categoryLongLabel(cat)) },
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
                                        ItemCategory.MATERIALS -> Unit
                                    }
                                }
                                catExpanded = false
                            }
                        )
                    }
                }
            }

            // Quantity & Unit Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text(strings.fieldQuantity) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.weight(0.42f)
                )

                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = !unitExpanded },
                    modifier = Modifier.weight(0.58f)
                ) {
                    OutlinedTextField(
                        value = strings.unitLabel(unit),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings.fieldUnit) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false }
                    ) {
                        ItemUnit.entries.forEach { u ->
                            DropdownMenuItem(
                                text = { Text(strings.unitLabel(u)) },
                                onClick = {
                                    unit = u
                                    unitExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // No Expiry Switch
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

            // Expiry Date Input + Native DatePicker button
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

            // Location
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text(strings.fieldLocation) },
                placeholder = { Text(strings.fieldLocationPlaceholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(strings.fieldNotes) },
                placeholder = { Text(strings.fieldNotesPlaceholder) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            // Actions
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
                            id = editingItem?.id ?: "item-${System.currentTimeMillis()}",
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
                            nutrition = nutrition
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
                        val localDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        expiryDate = localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
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
    val unit = ItemUnit.fromKey(item.unit)
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

            // Telemetry box
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
                                "${InventoryLogic.formatQty(item.qty)} ${strings.unitLabel(unit)}",
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

            // Quick add controls
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
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val colors = LocalBunkerColors.current
    val info = InventoryLogic.getExpiryInfo(item, notApplicableLabel = strings.notApplicable)
    val meta = strings.qrMetaFormat(item.barcode, info.formattedDate)

    val qrBitmap = remember(item.barcode) {
        QrCodeUtils.generateQrBitmap(item.barcode, 420)
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

                // Printable white label preview
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
                                bitmap = qrBitmap.asImageBitmap(),
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
                            val printable = QrCodeUtils.generatePrintableLabelBitmap(
                                header = strings.qrHeaderBrand,
                                title = item.name,
                                meta = meta,
                                qrContent = item.barcode
                            )
                            if (printable != null) {
                                QrCodeUtils.printLabel(context, "BunkerStock-${item.barcode}", printable)
                            }
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
                            val printable = QrCodeUtils.generatePrintableLabelBitmap(
                                header = strings.qrHeaderBrand,
                                title = item.name,
                                meta = meta,
                                qrContent = item.barcode
                            )
                            if (printable != null && QrCodeUtils.shareLabelPng(context, "qr_${item.barcode}", printable)) {
                                onMessage(strings.toastQrShared)
                            } else {
                                onMessage(strings.toastExportError)
                            }
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
