package com.bunkerstock.app.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bunkerstock.app.data.AppLanguage
import com.bunkerstock.app.data.ExpiryBatch
import com.bunkerstock.app.data.ExpiryStatus
import com.bunkerstock.app.data.InventoryItem
import com.bunkerstock.app.data.ItemCategory
import com.bunkerstock.app.data.ItemUnit
import com.bunkerstock.app.data.ProductImageStore
import com.bunkerstock.app.domain.InventoryLogic
import com.bunkerstock.app.scanner.QrCodeUtils
import com.bunkerstock.app.ui.theme.BunkerRadius
import com.bunkerstock.app.ui.theme.LocalAppStrings
import com.bunkerstock.app.ui.theme.LocalBunkerColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private val BatchPalette = listOf(
    Color(0xFF4ADE80), // Geiger green
    Color(0xFFFACC15), // Radiation amber
    Color(0xFF38BDF8), // Cyan
    Color(0xFFFB7185), // Rose
    Color(0xFFC084FC)  // Violet
)

@Composable
fun ProductDetailOverlay(
    item: InventoryItem?,
    expiredDays: Int,
    criticalDays: Int,
    warningDays: Int,
    onUpdateItem: (InventoryItem) -> Unit,
    onOpenFullEditor: (InventoryItem) -> Unit,
    onMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Keep last non-null item briefly so exit animation renders smoothly
    var lastItem by remember { mutableStateOf(item) }
    if (item != null) lastItem = item

    BackHandler(enabled = item != null) {
        onDismiss()
    }

    AnimatedVisibility(
        visible = item != null,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(150))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xB8090B0A))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            val active = item ?: lastItem ?: return@Box
            AnimatedVisibility(
                visible = item != null,
                enter = scaleIn(initialScale = 0.92f, animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                    slideInVertically(initialOffsetY = { it / 14 }, animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                    fadeIn(tween(180)),
                exit = scaleOut(targetScale = 0.94f, animationSpec = tween(150)) +
                    slideOutVertically(targetOffsetY = { it / 18 }, animationSpec = tween(150)) +
                    fadeOut(tween(140))
            ) {
                ProductDetailCard(
                    item = active,
                    expiredDays = expiredDays,
                    criticalDays = criticalDays,
                    warningDays = warningDays,
                    onUpdateItem = onUpdateItem,
                    onOpenFullEditor = { onOpenFullEditor(active) },
                    onMessage = onMessage,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun ProductDetailCard(
    item: InventoryItem,
    expiredDays: Int,
    criticalDays: Int,
    warningDays: Int,
    onUpdateItem: (InventoryItem) -> Unit,
    onOpenFullEditor: () -> Unit,
    onMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val colors = LocalBunkerColors.current
    val today = remember { LocalDate.now() }
    val isoFmt = remember { DateTimeFormatter.ISO_LOCAL_DATE }
    val ddMmFmt = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    val category = ItemCategory.fromKey(item.category)
    val unit = ItemUnit.fromKey(item.unit)
    val (catIcon, catFg, catBg) = categoryStyle(category)

    val info = InventoryLogic.getExpiryInfo(
        item = item,
        today = today,
        notApplicableLabel = strings.notApplicable,
        expiredDays = expiredDays,
        criticalDays = criticalDays,
        warningDays = warningDays
    )

    // Load high-res product photo
    var heroBitmap by remember(item.id, item.imageUri, item.nutrition?.imageUrl, item.barcode, item.name) {
        mutableStateOf<Bitmap?>(null)
    }
    LaunchedEffect(item.id, item.imageUri, item.nutrition?.imageUrl, item.barcode, item.name) {
        val src = item.imageUri ?: item.nutrition?.imageUrl
        heroBitmap = when {
            !src.isNullOrBlank() -> ProductImageStore.loadBitmap(context, src)
                ?: ProductImageStore.resolveForProduct(context, item.barcode, item.name)
            item.barcode.isNotBlank() || item.name.isNotBlank() ->
                ProductImageStore.resolveForProduct(context, item.barcode, item.name)
            else -> null
        }
    }

    // Effective batches list (synthesizes single primary batch if item.batches is empty)
    val effectiveBatches = remember(item.batches, item.qty, item.expiryDate) {
        if (item.batches.isNotEmpty()) {
            item.batches
        } else {
            listOf(
                ExpiryBatch(
                    id = "primary",
                    qty = item.qty,
                    expiryDate = item.expiryDate
                )
            )
        }
    }

    // Which batch is actively edited when tapping a calendar day, and which batches are visible on the map
    var activeBatchId by remember(item.id) { mutableStateOf(effectiveBatches.firstOrNull()?.id ?: "primary") }
    var visibleBatchIds by remember(item.id) { mutableStateOf(effectiveBatches.map { it.id }.toSet()) }

    LaunchedEffect(effectiveBatches) {
        val allIds = effectiveBatches.map { it.id }.toSet()
        if (activeBatchId !in allIds) activeBatchId = effectiveBatches.firstOrNull()?.id ?: "primary"
        val cleaned = visibleBatchIds.intersect(allIds)
        if (cleaned.isEmpty()) visibleBatchIds = allIds
    }

    val activeBatch = effectiveBatches.firstOrNull { it.id == activeBatchId } ?: effectiveBatches.first()
    val initialMonth = remember(item.id) {
        runCatching { LocalDate.parse(activeBatch.expiryDate.trim(), isoFmt) }
            .getOrNull()
            ?.let { YearMonth.from(it) }
            ?: YearMonth.from(today)
    }
    var displayedMonth by remember(item.id) { mutableStateOf(initialMonth) }

    var catMenuExpanded by remember { mutableStateOf(false) }
    var unitMenuExpanded by remember { mutableStateOf(false) }
    var notesDraft by remember(item.id, item.notes) { mutableStateOf(item.notes) }
    var locationDraft by remember(item.id, item.location) { mutableStateOf(item.location) }

    val qrCodeContent = item.barcode.ifBlank { item.id }
    val qrBitmap = remember(qrCodeContent) { QrCodeUtils.generateQrBitmap(qrCodeContent, 360) }

    Surface(
        shape = RoundedCornerShape(BunkerRadius.Sheet),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        shadowElevation = 18.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 22.dp)
            .widthIn(max = 560.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {} // swallow clicks inside card so scrim doesn't close
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // 0. TOP HERO PHOTO BANNER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
                    .background(if (heroBitmap != null) Color(0xFF151817) else catBg)
            ) {
                val bmp = heroBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x55000000),
                                        Color.Transparent,
                                        Color(0xCC111312)
                                    )
                                )
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = catIcon,
                            contentDescription = null,
                            tint = catFg.copy(alpha = 0.85f),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                // Top-left Edit full sheet button, Top-right Close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(BunkerRadius.Control),
                        color = Color(0xBB111312),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                    ) {
                        IconButton(onClick = onOpenFullEditor, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = strings.editAction,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(BunkerRadius.Control),
                        color = Color(0xBB111312),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                    ) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = strings.close,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // MAIN VERTICAL CONTENT (top-to-bottom per user spec)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. PRODUCT NAME
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 2. EXPIRY DATE + PRODUCT TYPE & BUNKER PLACEMENT BESIDE IT
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(BunkerRadius.Control))
                            .background(colors.canvas)
                            .border(1.dp, colors.borderSubtle, RoundedCornerShape(BunkerRadius.Control))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StatusBadge(info = info)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = colors.mutedText,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = info.formattedDate,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(BunkerRadius.Control))
                            .background(colors.canvas)
                            .border(1.dp, colors.borderSubtle, RoundedCornerShape(BunkerRadius.Control))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = catIcon,
                                contentDescription = null,
                                tint = catFg,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = strings.categoryLabel(category),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = catFg,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = colors.amber,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = item.location.ifBlank { strings.detailLocationEmpty },
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.5.sp),
                                color = colors.mutedText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // 3. NUTRI-SCORE + YUKA BIOSHOCK SEMICIRCLE GAUGE WITH CARROT BESIDE IT
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Nutri-Score 5-grade strip
                    NutriScoreDetailCard(
                        grade = item.nutrition?.nutriScoreGrade,
                        label = strings.detailNutriScoreLabel,
                        emptyLabel = strings.detailNoScore,
                        modifier = Modifier.weight(1f)
                    )

                    // Right: Yuka Score BioShock-style semicircular gauge with vector carrot
                    YukaBioShockGaugeCard(
                        score = item.nutrition?.healthScore,
                        label = strings.detailYukaLabel,
                        emptyLabel = strings.detailNoScore,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Extra relevant nutritional details if present (kcal, flags, NOVA)
                item.nutrition?.let { nut ->
                    if (nut.energyKcal != null || nut.safetyFlags.isNotEmpty() || nut.novaGroup != null) {
                        NutritionBadgesRow(nutrition = nut)
                    }
                }

                HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.6f))

                // 4. CATEGORY ICON DROPDOWN + QUANTITY + UNIT BESIDE IT
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category icon-only button that opens Icon + Name dropdown (incl. OTHER "?")
                    Box {
                        Row(
                            modifier = Modifier
                                .height(42.dp)
                                .clip(RoundedCornerShape(BunkerRadius.Control))
                                .background(catBg)
                                .border(1.dp, catFg.copy(alpha = 0.5f), RoundedCornerShape(BunkerRadius.Control))
                                .clickable(role = Role.Button) { catMenuExpanded = true }
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = catIcon,
                                contentDescription = strings.categoryLabel(category),
                                tint = catFg,
                                modifier = Modifier.size(20.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = catFg,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = catMenuExpanded,
                            onDismissRequest = { catMenuExpanded = false }
                        ) {
                            ItemCategory.entries.forEach { catOption ->
                                val (optIcon, optFg, _) = categoryStyle(catOption)
                                val selected = catOption == category
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = optIcon,
                                                contentDescription = null,
                                                tint = optFg,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = strings.categoryLabel(catOption),
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selected) optFg else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    },
                                    onClick = {
                                        catMenuExpanded = false
                                        onUpdateItem(item.copy(category = catOption.storageKey))
                                    }
                                )
                            }
                        }
                    }

                    // Quantity stepper
                    Row(
                        modifier = Modifier
                            .height(42.dp)
                            .clip(RoundedCornerShape(BunkerRadius.Control))
                            .background(colors.canvas)
                            .border(1.dp, colors.borderSubtle, RoundedCornerShape(BunkerRadius.Control)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (item.batches.isNotEmpty()) {
                                    val updatedBatches = item.batches.map { b ->
                                        if (b.id == activeBatch.id) b.copy(qty = max(0.0, b.qty - 1.0)) else b
                                    }
                                    onUpdateItem(InventoryLogic.normalizeWithBatches(item.copy(batches = updatedBatches)))
                                } else {
                                    onUpdateItem(item.copy(qty = max(0.0, item.qty - 1.0)))
                                }
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = strings.subtractOne, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = InventoryLogic.formatQty(item.qty),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = if (item.qty <= 0.0) colors.danger else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        IconButton(
                            onClick = {
                                if (item.batches.isNotEmpty()) {
                                    val updatedBatches = item.batches.map { b ->
                                        if (b.id == activeBatch.id) b.copy(qty = b.qty + 1.0) else b
                                    }
                                    onUpdateItem(InventoryLogic.normalizeWithBatches(item.copy(batches = updatedBatches)))
                                } else {
                                    onUpdateItem(item.copy(qty = item.qty + 1.0))
                                }
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = strings.addOne, tint = colors.emerald, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Unit dropdown right beside quantity
                    Box(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(RoundedCornerShape(BunkerRadius.Control))
                                .background(colors.canvas)
                                .border(1.dp, colors.borderSubtle, RoundedCornerShape(BunkerRadius.Control))
                                .clickable(role = Role.Button) { unitMenuExpanded = true }
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = strings.unitLabel(unit),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = colors.mutedText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = unitMenuExpanded,
                            onDismissRequest = { unitMenuExpanded = false }
                        ) {
                            ItemUnit.entries.forEach { u ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = strings.unitLabel(u),
                                            fontWeight = if (u == unit) FontWeight.Bold else FontWeight.Normal,
                                            color = if (u == unit) colors.emerald else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        unitMenuExpanded = false
                                        onUpdateItem(item.copy(unit = u.storageKey))
                                    }
                                )
                            }
                        }
                    }
                }

                // 5. SMALL "NO EXPIRY DATE" TOGGLE RIGHT UNDERNEATH
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = strings.fieldNoExpiry,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = colors.mutedText
                    )
                    Switch(
                        checked = item.noExpiry,
                        onCheckedChange = { checked ->
                            val fallbackDate = if (!checked && item.expiryDate.isBlank()) {
                                InventoryLogic.relativeDateString(180, today)
                            } else {
                                item.expiryDate
                            }
                            onUpdateItem(item.copy(noExpiry = checked, expiryDate = if (checked) "" else fallbackDate))
                        },
                        modifier = Modifier.scale(0.8f)
                    )
                }

                // 6. MULTI-BATCH ("TANDADAS") SELECTOR + INTERACTIVE EXPIRY CALENDAR
                if (!item.noExpiry) {
                    Surface(
                        shape = RoundedCornerShape(BunkerRadius.Card),
                        color = colors.canvas,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Batches header + Add Batch button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.detailBatchesTitle,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colors.mutedText
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (effectiveBatches.size > 1) {
                                        val allShown = visibleBatchIds.size == effectiveBatches.size
                                        Surface(
                                            shape = RoundedCornerShape(BunkerRadius.Badge),
                                            color = if (allShown) colors.emeraldContainer else colors.cardElevated,
                                            border = BorderStroke(
                                                1.dp,
                                                if (allShown) colors.emerald.copy(alpha = 0.55f) else colors.borderSubtle
                                            ),
                                            modifier = Modifier.clickable {
                                                visibleBatchIds = if (allShown) setOf(activeBatch.id) else effectiveBatches.map { it.id }.toSet()
                                            }
                                        ) {
                                            Text(
                                                text = strings.detailAllBatches,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (allShown) colors.emerald else colors.mutedText,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(BunkerRadius.Badge),
                                        color = colors.amberContainer,
                                        border = BorderStroke(1.dp, colors.amber.copy(alpha = 0.5f)),
                                        modifier = Modifier.clickable {
                                            val baseBatches = if (item.batches.isNotEmpty()) {
                                                item.batches
                                            } else {
                                                listOf(
                                                    ExpiryBatch(
                                                        id = "b-${System.currentTimeMillis()}-1",
                                                        qty = item.qty.coerceAtLeast(1.0),
                                                        expiryDate = item.expiryDate.ifBlank { InventoryLogic.relativeDateString(90, today) }
                                                    )
                                                )
                                            }
                                            val newBatch = ExpiryBatch(
                                                id = "b-${System.currentTimeMillis()}-${baseBatches.size + 1}",
                                                qty = 1.0,
                                                expiryDate = InventoryLogic.relativeDateString(120, today)
                                            )
                                            val nextList = baseBatches + newBatch
                                            activeBatchId = newBatch.id
                                            visibleBatchIds = nextList.map { it.id }.toSet()
                                            displayedMonth = YearMonth.from(today.plusDays(120))
                                            onUpdateItem(InventoryLogic.normalizeWithBatches(item.copy(batches = nextList)))
                                        }
                                    ) {
                                        Text(
                                            text = strings.detailAddBatch,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = colors.amber,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            // Batch selector list (pick which batches appear on the calendar map)
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                effectiveBatches.forEachIndexed { idx, batch ->
                                    val batchColor = BatchPalette[idx % BatchPalette.size]
                                    val isVisibleOnMap = visibleBatchIds.contains(batch.id)
                                    val isActive = activeBatch.id == batch.id
                                    val parsedDate = runCatching { LocalDate.parse(batch.expiryDate.trim(), isoFmt) }.getOrNull()
                                    val dateText = parsedDate?.format(ddMmFmt) ?: strings.notApplicable

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(BunkerRadius.Control))
                                            .background(if (isActive) colors.cardElevated else colors.cardSurface.copy(alpha = 0.6f))
                                            .border(
                                                if (isActive) 1.5.dp else 1.dp,
                                                if (isActive) batchColor else colors.borderSubtle.copy(alpha = 0.6f),
                                                RoundedCornerShape(BunkerRadius.Control)
                                            )
                                            .clickable {
                                                activeBatchId = batch.id
                                                visibleBatchIds = if (isVisibleOnMap && visibleBatchIds.size > 1 && isActive) {
                                                    visibleBatchIds - batch.id
                                                } else {
                                                    visibleBatchIds + batch.id
                                                }
                                                if (parsedDate != null) {
                                                    displayedMonth = YearMonth.from(parsedDate)
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Visibility dot on map
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(if (isVisibleOnMap) batchColor else Color.Transparent)
                                                .border(1.5.dp, batchColor, CircleShape)
                                                .clickable {
                                                    visibleBatchIds = if (isVisibleOnMap && visibleBatchIds.size > 1) {
                                                        visibleBatchIds - batch.id
                                                    } else {
                                                        visibleBatchIds + batch.id
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isVisibleOnMap) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color(0xFF111312),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${strings.detailBatchNumber(idx + 1)} · $dateText",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (batch.note.isNotBlank()) {
                                                Text(
                                                    text = batch.note,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colors.mutedText,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        if (effectiveBatches.size > 1) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(BunkerRadius.Badge))
                                                    .background(colors.canvas)
                                                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(BunkerRadius.Badge))
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        val updated = item.batches.map { b ->
                                                            if (b.id == batch.id) b.copy(qty = max(0.0, b.qty - 1.0)) else b
                                                        }
                                                        onUpdateItem(InventoryLogic.normalizeWithBatches(item.copy(batches = updated)))
                                                    },
                                                    modifier = Modifier.size(26.dp)
                                                ) {
                                                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(13.dp))
                                                }
                                                Text(
                                                    text = InventoryLogic.formatQty(batch.qty),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 4.dp)
                                                )
                                                IconButton(
                                                    onClick = {
                                                        val updated = item.batches.map { b ->
                                                            if (b.id == batch.id) b.copy(qty = b.qty + 1.0) else b
                                                        }
                                                        onUpdateItem(InventoryLogic.normalizeWithBatches(item.copy(batches = updated)))
                                                    },
                                                    modifier = Modifier.size(26.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = null, tint = colors.emerald, modifier = Modifier.size(13.dp))
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    val remaining = item.batches.filterNot { it.id == batch.id }
                                                    if (remaining.size == 1) {
                                                        val sole = remaining.first()
                                                        onUpdateItem(
                                                            item.copy(
                                                                qty = sole.qty,
                                                                expiryDate = sole.expiryDate,
                                                                batches = emptyList()
                                                            )
                                                        )
                                                    } else if (remaining.isNotEmpty()) {
                                                        onUpdateItem(InventoryLogic.normalizeWithBatches(item.copy(batches = remaining)))
                                                    }
                                                },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = strings.deleteAction,
                                                    tint = colors.danger,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Visual Countdown Approach Bar for active batch
                            val activeDate = runCatching { LocalDate.parse(activeBatch.expiryDate.trim(), isoFmt) }.getOrNull()
                            if (activeDate != null) {
                                ExpiryApproachBar(
                                    today = today,
                                    expiryDate = activeDate,
                                    expiredDays = expiredDays,
                                    criticalDays = criticalDays,
                                    warningDays = warningDays
                                )
                            }

                            // Interactive Monthly Calendar showing TODAY -> EXPIRY approach trajectory
                            val visibleMarkers = effectiveBatches.mapIndexedNotNull { idx, b ->
                                if (!visibleBatchIds.contains(b.id)) null
                                else {
                                    val d = runCatching { LocalDate.parse(b.expiryDate.trim(), isoFmt) }.getOrNull()
                                    if (d == null) null else BatchCalendarMarker(
                                        date = d,
                                        color = BatchPalette[idx % BatchPalette.size],
                                        isActive = b.id == activeBatch.id
                                    )
                                }
                            }

                            InteractiveExpiryCalendar(
                                month = displayedMonth,
                                today = today,
                                markers = visibleMarkers,
                                language = strings.language,
                                todayLabel = strings.detailCalendarToday,
                                onPrevMonth = { displayedMonth = displayedMonth.minusMonths(1) },
                                onNextMonth = { displayedMonth = displayedMonth.plusMonths(1) },
                                onJumpToday = { displayedMonth = YearMonth.from(today) },
                                onSelectDate = { picked ->
                                    val iso = picked.format(isoFmt)
                                    if (item.batches.isNotEmpty()) {
                                        val updated = item.batches.map { b ->
                                            if (b.id == activeBatch.id) b.copy(expiryDate = iso) else b
                                        }
                                        onUpdateItem(InventoryLogic.normalizeWithBatches(item.copy(batches = updated)))
                                    } else {
                                        onUpdateItem(item.copy(expiryDate = iso))
                                    }
                                }
                            )

                            Text(
                                text = strings.detailCalendarTapHint,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = colors.mutedText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 7. QR LABEL SECTION
                Surface(
                    shape = RoundedCornerShape(BunkerRadius.Card),
                    color = colors.canvas,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(82.dp)
                                .clip(RoundedCornerShape(BunkerRadius.Control))
                                .background(Color.White)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = qrCodeContent,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color(0xFF111312))
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = strings.detailQrSection,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.mutedText
                            )
                            Text(
                                text = qrCodeContent,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val meta = strings.qrMetaFormat(qrCodeContent, info.formattedDate)
                                OutlinedButton(
                                    onClick = {
                                        val printable = QrCodeUtils.generatePrintableLabelBitmap(
                                            header = strings.qrHeaderBrand,
                                            title = item.name,
                                            meta = meta,
                                            qrContent = qrCodeContent
                                        )
                                        if (printable != null) {
                                            QrCodeUtils.printLabel(context, "BunkerStock-$qrCodeContent", printable)
                                        }
                                    },
                                    shape = RoundedCornerShape(BunkerRadius.Control),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.qrPrint, style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = {
                                        val printable = QrCodeUtils.generatePrintableLabelBitmap(
                                            header = strings.qrHeaderBrand,
                                            title = item.name,
                                            meta = meta,
                                            qrContent = qrCodeContent
                                        )
                                        if (printable != null && QrCodeUtils.shareLabelPng(context, "qr_$qrCodeContent", printable)) {
                                            onMessage(strings.toastQrShared)
                                        } else {
                                            onMessage(strings.toastExportError)
                                        }
                                    },
                                    shape = RoundedCornerShape(BunkerRadius.Control),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("PNG", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                // 8. NOTES & PLACEMENT (INLINE EDITABLE)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = strings.detailNotesSection,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.mutedText
                    )
                    OutlinedTextField(
                        value = locationDraft,
                        onValueChange = {
                            locationDraft = it
                            onUpdateItem(item.copy(location = it))
                        },
                        label = { Text(strings.fieldLocation, style = MaterialTheme.typography.labelSmall) },
                        singleLine = true,
                        shape = RoundedCornerShape(BunkerRadius.Control),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.canvas,
                            unfocusedContainerColor = colors.canvas,
                            unfocusedBorderColor = colors.borderSubtle
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notesDraft,
                        onValueChange = {
                            notesDraft = it
                            onUpdateItem(item.copy(notes = it))
                        },
                        label = { Text(strings.fieldNotes, style = MaterialTheme.typography.labelSmall) },
                        placeholder = { Text(strings.fieldNotesPlaceholder, style = MaterialTheme.typography.bodyMedium) },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(BunkerRadius.Control),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.canvas,
                            unfocusedContainerColor = colors.canvas,
                            unfocusedBorderColor = colors.borderSubtle
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun NutriScoreDetailCard(
    grade: String?,
    label: String,
    emptyLabel: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalBunkerColors.current
    val upper = grade?.uppercase()
    val grades = listOf(
        "A" to Color(0xFF17823B),
        "B" to Color(0xFF73A92D),
        "C" to Color(0xFFD0A514),
        "D" to Color(0xFFD16D16),
        "E" to Color(0xFFBD2B24)
    )

    Column(
        modifier = modifier
            .height(98.dp)
            .clip(RoundedCornerShape(BunkerRadius.Card))
            .background(colors.canvas)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(BunkerRadius.Card))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
            color = colors.mutedText
        )
        if (upper != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                grades.forEach { (letter, bg) ->
                    val active = letter == upper
                    Box(
                        modifier = Modifier
                            .size(width = if (active) 32.dp else 22.dp, height = if (active) 40.dp else 28.dp)
                            .clip(RoundedCornerShape(if (active) 6.dp else 3.dp))
                            .background(if (active) bg else bg.copy(alpha = 0.35f))
                            .then(
                                if (active) Modifier.border(2.dp, Color.White, RoundedCornerShape(6.dp))
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letter,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = if (active) 18.sp else 11.sp
                            ),
                            color = if (active) Color.White else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            Text(
                text = "Nutri-Score $upper",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = emptyLabel,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = colors.mutedText
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

/**
 * BioShock-inspired semicircular speedometer gauge with custom vector carrot icon
 * that smoothly fills around the carrot according to the 0..100 Yuka health score.
 */
@Composable
private fun YukaBioShockGaugeCard(
    score: Int?,
    label: String,
    emptyLabel: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalBunkerColors.current
    val targetFraction = (score?.coerceIn(0, 100) ?: 0) / 100f
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "yukaGauge"
    )
    val gaugeColor = when {
        score == null -> colors.mutedText
        score >= 75 -> Color(0xFF22C55E)
        score >= 50 -> Color(0xFF84CC16)
        score >= 25 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    Column(
        modifier = modifier
            .height(98.dp)
            .clip(RoundedCornerShape(BunkerRadius.Card))
            .background(colors.canvas)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(BunkerRadius.Card))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
            color = colors.mutedText
        )

        Box(
            modifier = Modifier
                .width(108.dp)
                .height(64.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 7.5.dp.toPx()
                val pad = strokeW / 2f + 2.dp.toPx()
                val arcSize = Size(size.width - pad * 2f, (size.width - pad * 2f))
                val topLeft = Offset(pad, 4.dp.toPx())

                // Metallic BioShock outer bezel track (200-degree semicircular sweep)
                drawArc(
                    color = Color(0xFF2B302C),
                    startAngle = 170f,
                    sweepAngle = 200f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeW + 2.dp.toPx(), cap = StrokeCap.Butt)
                )

                // Subtle brass/steel tick marks around gauge arc
                val cx = topLeft.x + arcSize.width / 2f
                val cy = topLeft.y + arcSize.height / 2f
                val rOuter = arcSize.width / 2f + strokeW * 0.52f
                val rInner = arcSize.width / 2f - strokeW * 0.52f
                for (step in 0..8) {
                    val deg = Math.toRadians((170.0 + step * 25.0))
                    drawLine(
                        color = Color(0x66090B0A),
                        start = Offset(cx + (rInner * cos(deg)).toFloat(), cy + (rInner * sin(deg)).toFloat()),
                        end = Offset(cx + (rOuter * cos(deg)).toFloat(), cy + (rOuter * sin(deg)).toFloat()),
                        strokeWidth = 1.4.dp.toPx()
                    )
                }

                // Active animated health arc (fills clockwise around the carrot like BioShock enemy gauge)
                if (score != null && animatedFraction > 0f) {
                    drawArc(
                        color = gaugeColor,
                        startAngle = 170f,
                        sweepAngle = 200f * animatedFraction,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                    )
                }

                // Custom vector Carrot Icon centered inside the semicircular gauge!
                val carrotCx = size.width / 2f
                val carrotCy = size.height * 0.56f
                val s = size.height * 0.25f

                // Carrot leafy greens (top-right stem)
                val leafColor = Color(0xFF4ADE80)
                drawLine(
                    color = leafColor,
                    start = Offset(carrotCx + s * 0.32f, carrotCy - s * 0.35f),
                    end = Offset(carrotCx + s * 0.72f, carrotCy - s * 0.85f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = leafColor,
                    start = Offset(carrotCx + s * 0.32f, carrotCy - s * 0.35f),
                    end = Offset(carrotCx + s * 0.92f, carrotCy - s * 0.45f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = leafColor,
                    start = Offset(carrotCx + s * 0.32f, carrotCy - s * 0.35f),
                    end = Offset(carrotCx + s * 0.32f, carrotCy - s * 0.92f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Carrot tapered body (diagonal tactile silhouette)
                val body = Path().apply {
                    moveTo(carrotCx - s * 0.78f, carrotCy + s * 0.72f) // pointed tip
                    lineTo(carrotCx + s * 0.05f, carrotCy - s * 0.58f) // upper shoulder
                    quadraticTo(
                        carrotCx + s * 0.52f, carrotCy - s * 0.62f,
                        carrotCx + s * 0.58f, carrotCy - s * 0.12f
                    ) // rounded top
                    close()
                }
                drawPath(path = body, color = Color(0xFFF97316))
                // Subtle highlight lines on carrot
                drawLine(
                    color = Color(0x88FFEDD5),
                    start = Offset(carrotCx - s * 0.12f, carrotCy - s * 0.08f),
                    end = Offset(carrotCx + s * 0.12f, carrotCy + s * 0.14f),
                    strokeWidth = 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            Text(
                text = score?.let { "$it/100" } ?: emptyLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                ),
                color = if (score != null) gaugeColor else colors.mutedText,
                modifier = Modifier.padding(bottom = 0.dp)
            )
        }
    }
}

private data class BatchCalendarMarker(
    val date: LocalDate,
    val color: Color,
    val isActive: Boolean
)

@Composable
private fun ExpiryApproachBar(
    today: LocalDate,
    expiryDate: LocalDate,
    expiredDays: Int,
    criticalDays: Int,
    warningDays: Int
) {
    val colors = LocalBunkerColors.current
    val daysLeft = ChronoUnit.DAYS.between(today, expiryDate)
    val horizon = max(warningDays.toLong() * 2L, 180L).toFloat()
    val closeness = when {
        daysLeft <= expiredDays -> 1f
        else -> (1f - (daysLeft.toFloat() / horizon).coerceIn(0f, 1f)).coerceIn(0.06f, 1f)
    }
    val barColor = when {
        daysLeft < expiredDays -> colors.danger
        daysLeft <= criticalDays -> colors.amber
        daysLeft <= warningDays -> colors.warning
        else -> colors.emerald
    }

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "HOY",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.5.sp),
                color = colors.emerald
            )
            Text(
                text = if (daysLeft < 0) "${daysLeft}d" else "+${daysLeft}d",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = barColor
            )
            Text(
                text = expiryDate.format(DateTimeFormatter.ofPattern("dd/MM/yy")),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.5.sp),
                color = barColor
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.cardElevated)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(closeness)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun InteractiveExpiryCalendar(
    month: YearMonth,
    today: LocalDate,
    markers: List<BatchCalendarMarker>,
    language: AppLanguage,
    todayLabel: String,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToday: () -> Unit,
    onSelectDate: (LocalDate) -> Unit
) {
    val colors = LocalBunkerColors.current
    val locale = if (language == AppLanguage.ES) Locale("es", "ES") else Locale.US
    val monthTitle = remember(month, locale) {
        month.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)).replaceFirstChar { it.uppercase(locale) }
    }
    val dayHeaders = if (language == AppLanguage.ES) {
        listOf("L", "M", "X", "J", "V", "S", "D")
    } else {
        listOf("M", "T", "W", "T", "F", "S", "S")
    }

    val firstOfMonth = month.atDay(1)
    val leadingBlanks = (firstOfMonth.dayOfWeek.value - 1) // Monday = 0
    val daysInMonth = month.lengthOfMonth()
    val totalCells = ((leadingBlanks + daysInMonth + 6) / 7) * 7
    val rows = totalCells / 7

    val activeMarkerDate = markers.firstOrNull { it.isActive }?.date ?: markers.firstOrNull()?.date

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Month navigation row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevMonth, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ChevronLeft, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Surface(
                    shape = RoundedCornerShape(BunkerRadius.Badge),
                    color = colors.cardElevated,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.clickable(onClick = onJumpToday)
                ) {
                    Text(
                        text = todayLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = colors.emerald,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            IconButton(onClick = onNextMonth, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }

        // Weekday headers
        Row(modifier = Modifier.fillMaxWidth()) {
            dayHeaders.forEach { d ->
                Text(
                    text = d,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = colors.mutedText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Calendar grid
        for (r in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (c in 0 until 7) {
                    val cellIdx = r * 7 + c
                    val dayNum = cellIdx - leadingBlanks + 1
                    if (dayNum in 1..daysInMonth) {
                        val date = month.atDay(dayNum)
                        val isToday = date == today
                        val dayMarkers = markers.filter { it.date == date }
                        val inApproachPath = activeMarkerDate != null &&
                            ((!date.isBefore(today) && !date.isAfter(activeMarkerDate)) ||
                                (!date.isBefore(activeMarkerDate) && !date.isAfter(today)))

                        val cellBg = when {
                            dayMarkers.isNotEmpty() -> dayMarkers.first().color.copy(alpha = 0.28f)
                            inApproachPath -> colors.amber.copy(alpha = 0.11f)
                            else -> Color.Transparent
                        }
                        val cellBorder = when {
                            dayMarkers.isNotEmpty() -> dayMarkers.first().color
                            isToday -> colors.emerald
                            else -> Color.Transparent
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(cellBg)
                                .then(
                                    if (cellBorder != Color.Transparent) {
                                        Modifier.border(1.2.dp, cellBorder, RoundedCornerShape(4.dp))
                                    } else Modifier
                                )
                                .clickable { onSelectDate(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayNum.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = if (isToday || dayMarkers.isNotEmpty()) FontWeight.ExtraBold else FontWeight.Normal
                                    ),
                                    color = when {
                                        dayMarkers.isNotEmpty() -> dayMarkers.first().color
                                        isToday -> colors.emerald
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (dayMarkers.isNotEmpty()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        dayMarkers.take(3).forEach { m ->
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(m.color)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f).height(32.dp))
                    }
                }
            }
        }
    }
}
