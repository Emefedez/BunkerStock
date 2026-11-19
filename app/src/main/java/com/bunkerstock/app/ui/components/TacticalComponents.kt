package com.bunkerstock.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.bunkerstock.app.data.DashboardMetrics
import com.bunkerstock.app.data.ExpiryInfo
import com.bunkerstock.app.data.ExpiryStatus
import com.bunkerstock.app.data.InventoryItem
import com.bunkerstock.app.data.ItemCategory
import com.bunkerstock.app.data.ItemUnit
import com.bunkerstock.app.data.ProductImageStore
import com.bunkerstock.app.data.ProductNutrition
import com.bunkerstock.app.domain.InventoryLogic
import com.bunkerstock.app.ui.theme.BunkerRadius
import com.bunkerstock.app.ui.theme.LocalAppStrings
import com.bunkerstock.app.ui.theme.LocalBunkerColors

@Composable
fun categoryStyle(category: ItemCategory): Triple<ImageVector, Color, Color> {
    val c = LocalBunkerColors.current
    return when (category) {
        ItemCategory.FOOD -> Triple(Icons.Default.Restaurant, c.emerald, c.emeraldContainer)
        ItemCategory.WATER -> Triple(Icons.Default.WaterDrop, c.cyan, c.cyanContainer)
        ItemCategory.MATERIALS -> Triple(Icons.Default.MedicalServices, c.rose, c.roseContainer)
        ItemCategory.TOOLS -> Triple(Icons.Default.Bolt, c.amber, c.amberContainer)
    }
}

@Composable
fun expiryColors(status: ExpiryStatus): Pair<Color, Color> {
    val c = LocalBunkerColors.current
    return when (status) {
        ExpiryStatus.EXPIRED -> c.danger to c.dangerContainer
        ExpiryStatus.CRITICAL -> c.amber to c.amberContainer
        ExpiryStatus.WARNING -> c.warning to c.warningContainer
        ExpiryStatus.OPTIMAL -> c.emerald to c.emeraldContainer
        ExpiryStatus.NONE -> c.infoBlue to c.cardElevated
    }
}

@Composable
fun CriticalAlertBanner(
    expiredCount: Int,
    criticalCount: Int,
    onReviewClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val colors = LocalBunkerColors.current

    val parts = buildList {
        if (expiredCount > 0) {
            add(if (expiredCount == 1) strings.alertExpiredSingle else strings.alertExpiredPlural(expiredCount))
        }
        if (criticalCount > 0) {
            add(if (criticalCount == 1) strings.alertCriticalSingle else strings.alertCriticalPlural(criticalCount))
        }
    }
    val title = "${strings.alertRotationPrefix} ${parts.joinToString(strings.alertJoinAnd)}"
    val desc = if (expiredCount > 0) strings.alertDescExpired else strings.alertDescCritical

    // Ultra-compact single-row horizontally scrollable hazard strip (~40dp tall)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.dangerBannerBg.copy(alpha = 0.78f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(colors.danger)
            )
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = colors.danger,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp),
                    color = if (colors.isDark) Color(0xFFFECACA) else Color(0xFF7F1D1D),
                    maxLines = 1
                )
                Text(
                    text = "· $desc",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = if (colors.isDark) Color(0xFFFCA5A5).copy(alpha = 0.8f) else Color(0xFF991B1B),
                    maxLines = 1
                )
            }
            Box(
                modifier = Modifier
                    .padding(start = 6.dp)
                    .clip(RoundedCornerShape(BunkerRadius.Badge))
                    .background(colors.danger.copy(alpha = 0.2f))
                    .border(1.dp, colors.danger.copy(alpha = 0.5f), RoundedCornerShape(BunkerRadius.Badge))
                    .clickable(role = Role.Button, onClick = onReviewClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = strings.reviewAffected,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (colors.isDark) Color(0xFFFECACA) else colors.danger,
                    maxLines = 1
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = strings.dismissAlert,
                    tint = colors.danger,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.danger.copy(alpha = 0.35f))
        )
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    iconBgColor: Color,
    icon: ImageVector,
    unitSuffix: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalBunkerColors.current
    // Military monitor telemetry module card
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(BunkerRadius.Card))
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(BunkerRadius.Card),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(BunkerRadius.Control))
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = accentColor
                )
                if (unitSuffix != null) {
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = unitSuffix,
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                        color = accentColor.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                color = colors.mutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun WarehouseHealthBar(
    metrics: DashboardMetrics,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val colors = LocalBunkerColors.current

    val badgeText = if (metrics.totalRefs == 0) {
        strings.healthNoData
    } else {
        strings.healthValidPct(metrics.healthyPercent)
    }

    val (badgeFg, badgeBg) = when {
        metrics.totalRefs == 0 -> colors.mutedText to colors.cardElevated
        metrics.healthyPercent >= 80 -> colors.emerald to colors.emeraldContainer
        metrics.healthyPercent >= 50 -> colors.amber to colors.amberContainer
        else -> colors.danger to colors.dangerContainer
    }

    // Military telemetry status monitor card
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(BunkerRadius.Card),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.healthTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .border(1.dp, badgeFg.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = badgeFg
                    )
                }
            }

            // Segmented bar
            val total = metrics.totalRefs.coerceAtLeast(1).toFloat()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.cardElevated),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (metrics.totalRefs > 0) {
                    if (metrics.optimalOrNoneCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(metrics.optimalOrNoneCount / total)
                                .fillMaxHeight()
                                .background(colors.emerald)
                        )
                    }
                    if (metrics.warningCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(metrics.warningCount / total)
                                .fillMaxHeight()
                                .background(colors.warning)
                        )
                    }
                    if (metrics.criticalCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(metrics.criticalCount / total)
                                .fillMaxHeight()
                                .background(colors.amber)
                        )
                    }
                    if (metrics.expiredCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(metrics.expiredCount / total)
                                .fillMaxHeight()
                                .background(colors.danger)
                        )
                    }
                }
            }

            // 2x2 legend grid so labels never clip on narrow screens
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    LegendDot(colors.emerald, strings.legendOptimal, Modifier.weight(1f))
                    LegendDot(colors.warning, strings.legendWarning, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    LegendDot(colors.amber, strings.legendCritical, Modifier.weight(1f))
                    LegendDot(colors.danger, strings.legendExpired, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
            color = LocalBunkerColors.current.mutedText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NutritionBadgesRow(
    nutrition: ProductNutrition,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val colors = LocalBunkerColors.current

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        nutrition.nutriScoreGrade?.let { grade ->
            val (fg, bg) = when (grade.uppercase()) {
                "A" -> colors.emerald to colors.emeraldContainer
                "B" -> Color(0xFF84CC16) to colors.emeraldContainer
                "C" -> colors.warning to colors.warningContainer
                "D" -> colors.amber to colors.amberContainer
                else -> colors.danger to colors.dangerContainer
            }
            SmallPill("Nutri-Score $grade", fg, bg)
        }
        nutrition.healthScore?.let { score ->
            val (fg, bg) = when {
                score >= 70 -> colors.emerald to colors.emeraldContainer
                score >= 45 -> colors.amber to colors.amberContainer
                else -> colors.danger to colors.dangerContainer
            }
            SmallPill(strings.healthScoreLabel(score), fg, bg)
        }
        nutrition.energyKcal?.let { kcal ->
            SmallPill("${kcal.toInt()} kcal/100g", colors.cyan, colors.cyanContainer)
        }
        nutrition.safetyFlags.forEach { flag ->
            val positive = flag == "HIGH_PROTEIN" || flag == "HIGH_FIBER"
            SmallPill(
                text = strings.safetyFlagLabel(flag),
                fg = if (positive) colors.emerald else colors.danger,
                bg = if (positive) colors.emeraldContainer else colors.dangerContainer
            )
        }
    }
}

@Composable
private fun SmallPill(text: String, fg: Color, bg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(BunkerRadius.Badge))
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.35f), RoundedCornerShape(BunkerRadius.Badge))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = fg
        )
    }
}

@Composable
fun StatusBadge(
    info: ExpiryInfo,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val (fg, bg) = expiryColors(info.status)
    val label = strings.expiryLabel(info)
    val sub = strings.expirySublabel(info)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(fg)
        )
        Text(
            text = "$label · $sub",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ProductThumbnail(
    imageSource: String?,
    category: ItemCategory,
    size: Dp = 52.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalBunkerColors.current
    val (catIcon, catFg, catBg) = categoryStyle(category)
    var bitmap by remember(imageSource) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(imageSource) {
        bitmap = if (!imageSource.isNullOrBlank()) {
            ProductImageStore.loadBitmap(context, imageSource)
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(BunkerRadius.Control))
            .background(if (bitmap != null) Color.White else catBg)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(BunkerRadius.Control)),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size)
            )
        } else {
            Icon(
                imageVector = catIcon,
                contentDescription = null,
                tint = catFg,
                modifier = Modifier.size(size * 0.45f)
            )
        }
    }
}

@Composable
fun InventoryItemCard(
    item: InventoryItem,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onQrClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val colors = LocalBunkerColors.current
    val info = InventoryLogic.getExpiryInfo(item, notApplicableLabel = strings.notApplicable)
    val category = ItemCategory.fromKey(item.category)
    val unit = ItemUnit.fromKey(item.unit)
    val (catIcon, catFg, catBg) = categoryStyle(category)

    val accentBar = when (info.status) {
        ExpiryStatus.EXPIRED -> colors.danger
        ExpiryStatus.CRITICAL -> colors.amber
        else -> Color.Transparent
    }

    val rowBg = when (info.status) {
        ExpiryStatus.EXPIRED -> colors.dangerContainer.copy(alpha = 0.25f)
        ExpiryStatus.CRITICAL -> colors.amberContainer.copy(alpha = 0.20f)
        else -> Color.Transparent
    }

    // Flat editorial list row with subtle left status bar & bottom hairline (zero card-box clutter)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBg)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            if (accentBar != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(140.dp)
                        .background(accentBar)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            // Top badges: Category + Expiry Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(catBg)
                        .border(1.dp, catFg.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = catIcon,
                        contentDescription = null,
                        tint = catFg,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = strings.categoryLabel(category),
                        style = MaterialTheme.typography.labelSmall,
                        color = catFg
                    )
                }
                Spacer(Modifier.width(8.dp))
                StatusBadge(info = info)
            }

            // Item thumbnail + name + metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                ProductThumbnail(
                    imageSource = item.imageUri ?: item.nutrition?.imageUrl,
                    category = category,
                    size = 54.dp
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = colors.mutedText,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${strings.expiryLabel} ${info.formattedDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (item.barcode.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = colors.amber,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = item.barcode,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.amber
                            )
                        }
                    }
                    if (item.location.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = colors.mutedText,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = item.location,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.mutedText
                            )
                        }
                    }
                    if (item.notes.isNotBlank()) {
                        Text(
                            text = "\"${item.notes}\"",
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = colors.mutedText
                        )
                    }
                    item.nutrition?.let { nut ->
                        Spacer(Modifier.height(4.dp))
                        NutritionBadgesRow(nutrition = nut)
                    }
                }
            }

            // Divider & Bottom Action Row (48dp touch targets)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.borderSubtle.copy(alpha = 0.6f))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tactile - qty + stepper
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(BunkerRadius.Control))
                        .background(colors.canvas)
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(BunkerRadius.Control))
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDecrement,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = strings.subtractOne,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .widthIn(min = 64.dp)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = InventoryLogic.formatQty(item.qty),
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                            color = if (item.qty <= 0.0) colors.danger else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.unitLabel(unit),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                            color = colors.mutedText,
                            maxLines = 1
                        )
                    }
                    IconButton(
                        onClick = onIncrement,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = strings.addOne,
                            tint = colors.emerald
                        )
                    }
                }

                // QR / Edit / Delete action buttons (48dp targets)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onQrClick,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = strings.qrLabelAction }
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = strings.qrLabelAction,
                            tint = colors.amber
                        )
                    }
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = strings.editAction,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = strings.deleteAction,
                            tint = colors.danger
                        )
                    }
                }
            }
        }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.borderSubtle.copy(alpha = 0.7f))
        )
    }
}

@Composable
fun kpiIconFor(type: String): ImageVector = when (type) {
    "refs" -> Icons.Default.Inventory2
    "expired" -> Icons.Default.Warning
    "critical" -> Icons.Default.HourglassBottom
    "water" -> Icons.Default.WaterDrop
    else -> Icons.Default.Restaurant
}
