package com.bunkerstock.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.bunkerstock.app.data.DashboardMetrics
import com.bunkerstock.app.data.ExpiryInfo
import com.bunkerstock.app.data.ExpiryStatus
import com.bunkerstock.app.data.InventoryItem
import com.bunkerstock.app.data.ItemCategory
import com.bunkerstock.app.data.ItemUnit
import com.bunkerstock.app.domain.InventoryLogic
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

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.dangerBannerBg,
        border = BorderStroke(1.dp, colors.danger.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.danger.copy(alpha = 0.18f))
                        .border(1.dp, colors.danger.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = colors.danger,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        color = if (colors.isDark) Color(0xFFFECACA) else Color(0xFF7F1D1D)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (colors.isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = strings.dismissAlert,
                        tint = colors.danger
                    )
                }
            }
            Button(
                onClick = onReviewClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.danger,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(strings.reviewAffected, fontWeight = FontWeight.Bold)
            }
        }
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
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = accentColor
                )
                if (unitSuffix != null) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = unitSuffix,
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                        color = accentColor.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 3.dp)
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

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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

            // Compact legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LegendDot(colors.emerald, strings.legendOptimal)
                LegendDot(colors.warning, strings.legendWarning)
                LegendDot(colors.amber, strings.legendCritical)
                LegendDot(colors.danger, strings.legendExpired)
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp),
            color = LocalBunkerColors.current.mutedText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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

    val borderColor = when (info.status) {
        ExpiryStatus.EXPIRED -> colors.danger.copy(alpha = 0.45f)
        ExpiryStatus.CRITICAL -> colors.amber.copy(alpha = 0.45f)
        else -> colors.borderSubtle
    }

    val cardBg = when (info.status) {
        ExpiryStatus.EXPIRED -> if (colors.isDark) Color(0xFF1A0F14) else Color(0xFFFFF5F5)
        ExpiryStatus.CRITICAL -> if (colors.isDark) Color(0xFF1A160E) else Color(0xFFFFFBEB)
        else -> colors.cardSurface
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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

            // Item name
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Technical metadata
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.canvas)
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
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
}

@Composable
fun kpiIconFor(type: String): ImageVector = when (type) {
    "refs" -> Icons.Default.Inventory2
    "expired" -> Icons.Default.Warning
    "critical" -> Icons.Default.HourglassBottom
    "water" -> Icons.Default.WaterDrop
    else -> Icons.Default.Restaurant
}
