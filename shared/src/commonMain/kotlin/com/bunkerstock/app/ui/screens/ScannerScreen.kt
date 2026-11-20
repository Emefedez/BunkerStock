package com.bunkerstock.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bunkerstock.app.data.DetectedScanCandidate
import com.bunkerstock.app.data.InventoryItem
import com.bunkerstock.app.data.ItemCategory
import com.bunkerstock.app.data.ItemUnit
import com.bunkerstock.app.data.OpenFoodFactsResult
import com.bunkerstock.app.data.ShoppingScanEntry
import com.bunkerstock.app.domain.InventoryLogic
import com.bunkerstock.app.platform.LocalBunkerPlatform
import com.bunkerstock.app.ui.components.NutritionBadgesRow
import com.bunkerstock.app.ui.components.ProductThumbnail
import com.bunkerstock.app.ui.components.ScoreColumn
import com.bunkerstock.app.ui.components.StatusBadge
import com.bunkerstock.app.ui.theme.BunkerRadius
import com.bunkerstock.app.ui.theme.LocalAppStrings
import com.bunkerstock.app.ui.theme.LocalBunkerColors

@Composable
fun ScannerScreen(
    detectedCandidate: DetectedScanCandidate?,
    detectedExistingItem: InventoryItem?,
    shoppingModeActive: Boolean,
    shoppingEntries: List<ShoppingScanEntry>,
    showShoppingToggle: Boolean,
    searchQuery: String,
    localSearchMatches: List<InventoryItem>,
    remoteSearchResults: List<OpenFoodFactsResult>,
    isSearchingRemote: Boolean,
    unlockTrigger: Int,
    onCodeScanned: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSelectLocalResult: (InventoryItem) -> Unit,
    onSelectRemoteResult: (OpenFoodFactsResult) -> Unit,
    onToggleShoppingMode: () -> Unit,
    onOpenShoppingReview: () -> Unit,
    onAdjustShoppingEntry: (String, Double) -> Unit,
    onUndoShoppingEntry: (String) -> Unit,
    onOpenShoppingItemDetail: (String) -> Unit,
    onFinishShoppingSession: () -> Unit,
    onDismissCandidate: () -> Unit,
    onConfirmCandidate: () -> Unit,
    onQuickAddExisting: (String, Double) -> Unit,
    onImageScanError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val platform = LocalBunkerPlatform.current
    val strings = LocalAppStrings.current
    val colors = LocalBunkerColors.current

    val showSearchTray = searchQuery.trim().length >= 2 && !InventoryLogic.isLikelyRawCode(searchQuery)

    Column(modifier = modifier.fillMaxSize()) {
        platform.CameraViewfinder(
            unlockTrigger = unlockTrigger,
            shoppingModeActive = shoppingModeActive,
            onCodeScanned = onCodeScanned,
            onImageScanError = onImageScanError,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { controls ->
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    !controls.hasPermission -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0A0C0B))
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideocamOff,
                                contentDescription = null,
                                tint = colors.amber,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = strings.scannerPermissionTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = strings.scannerPermissionDesc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.mutedText,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = controls.onRequestPermission,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.amber,
                                    contentColor = Color(0xFF111312)
                                ),
                                shape = RoundedCornerShape(BunkerRadius.Control)
                            ) {
                                Text(strings.scannerGrantPermission, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    controls.cameraError -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0A0C0B))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.scannerCameraUnavailable,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.mutedText,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    else -> {
                        // Tactical Viewfinder Frame over transparent live camera
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(240.dp)
                                    .height(150.dp)
                                    .border(2.dp, colors.amber.copy(alpha = 0.85f), RoundedCornerShape(BunkerRadius.Card)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp)
                                        .height(2.dp)
                                        .background(Color(0xCCEF4444))
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(BunkerRadius.Badge),
                                color = Color(0xCC111312),
                                border = BorderStroke(
                                    1.dp,
                                    if (shoppingModeActive) colors.emerald.copy(alpha = 0.65f) else colors.amber.copy(alpha = 0.4f)
                                )
                            ) {
                                Text(
                                    text = if (shoppingModeActive) strings.shoppingModeActiveHint else strings.scannerGuideHint,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (shoppingModeActive) colors.emerald else colors.amber,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Top-left Shopping / Rapid Intake Mode pill toggle
                if (showShoppingToggle) {
                    Surface(
                        shape = RoundedCornerShape(BunkerRadius.Control),
                        color = if (shoppingModeActive) colors.emerald else Color(0xBB111312),
                        border = BorderStroke(
                            1.dp,
                            if (shoppingModeActive) colors.emerald else Color.White.copy(alpha = 0.22f)
                        ),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .height(42.dp)
                            .clickable(onClick = onToggleShoppingMode)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = strings.shoppingModeLabel,
                                tint = if (shoppingModeActive) Color(0xFF111312) else colors.emerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = strings.shoppingModeLabel,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (shoppingModeActive) Color(0xFF111312) else Color.White
                            )
                        }
                    }
                }

                // Top-right Fixed-Position Viewfinder Controls
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ViewfinderIconButton(
                        icon = Icons.Default.ImageSearch,
                        contentDescription = strings.scanFromImage,
                        tint = colors.amber,
                        active = false,
                        onClick = controls.onPickImageToScan
                    )
                    if (controls.hasPermission && !controls.cameraError) {
                        if (controls.supportsZoom) {
                            ViewfinderIconButton(
                                icon = Icons.Default.ZoomIn,
                                contentDescription = "2x",
                                tint = if (controls.zoomActive) Color(0xFF111312) else Color.White,
                                active = controls.zoomActive,
                                activeBg = colors.emerald,
                                onClick = controls.onToggleZoom
                            )
                        }
                        ViewfinderIconButton(
                            icon = if (controls.torchEnabled && controls.hasFlash) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = strings.scannerTorch,
                            tint = when {
                                !controls.hasFlash -> Color.White.copy(alpha = 0.28f)
                                controls.torchEnabled -> Color(0xFF111312)
                                else -> colors.amber
                            },
                            active = controls.torchEnabled && controls.hasFlash,
                            activeBg = colors.amber,
                            enabled = controls.hasFlash,
                            onClick = controls.onToggleTorch
                        )
                        ViewfinderIconButton(
                            icon = Icons.Default.Cameraswitch,
                            contentDescription = strings.scannerSwitchCamera,
                            tint = Color.White,
                            active = false,
                            onClick = controls.onSwitchCamera
                        )
                    }
                }

                // YsiFoods-Style Live Product Name Search Results Tray (over bottom of viewfinder!)
                androidx.compose.animation.AnimatedVisibility(
                    visible = showSearchTray,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(BunkerRadius.Sheet),
                        color = colors.cardSurface.copy(alpha = 0.98f),
                        border = BorderStroke(1.5.dp, colors.emerald.copy(alpha = 0.65f)),
                        shadowElevation = 12.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isSearchingRemote) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = colors.emerald
                                        )
                                    }
                                    Text(
                                        text = if (isSearchingRemote) strings.scannerSearchingProducts else strings.scannerSearchOnlineHeader,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colors.emerald
                                    )
                                }
                                IconButton(onClick = onClearSearch, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = strings.close, tint = colors.mutedText, modifier = Modifier.size(16.dp))
                                }
                            }

                            if (localSearchMatches.isEmpty() && remoteSearchResults.isEmpty() && !isSearchingRemote) {
                                Text(
                                    text = strings.scannerNoSearchResults,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.mutedText,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 270.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    if (localSearchMatches.isNotEmpty()) {
                                        item {
                                            Text(
                                                text = strings.scannerSearchLocalHeader,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                                color = colors.amber
                                            )
                                        }
                                        items(localSearchMatches, key = { "loc_${it.id}" }) { item ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(BunkerRadius.Control))
                                                    .background(colors.canvas)
                                                    .border(1.dp, colors.amber.copy(alpha = 0.4f), RoundedCornerShape(BunkerRadius.Control))
                                                    .clickable { onSelectLocalResult(item) }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                ProductThumbnail(
                                                    imageSource = item.imageUri ?: item.nutrition?.imageUrl,
                                                    category = ItemCategory.fromKey(item.category),
                                                    barcode = item.barcode,
                                                    productName = item.name,
                                                    size = 38.dp
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(BunkerRadius.Badge))
                                                                .background(colors.amberContainer)
                                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                                        ) {
                                                            Text(
                                                                text = strings.scannerInStockBadge,
                                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                                                                color = colors.amber
                                                            )
                                                        }
                                                        Text(
                                                            text = "×${InventoryLogic.formatQty(item.qty)}",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                                            color = colors.emerald
                                                        )
                                                    }
                                                    Text(
                                                        text = item.name,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                ScoreColumn(nutrition = item.nutrition)
                                            }
                                        }
                                    }

                                    items(
                                        items = remoteSearchResults,
                                        key = { "off_${it.barcode}_${it.name}" }
                                    ) { hit ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(BunkerRadius.Control))
                                                .background(colors.canvas)
                                                .border(1.dp, colors.borderSubtle, RoundedCornerShape(BunkerRadius.Control))
                                                .clickable { onSelectRemoteResult(hit) }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            ProductThumbnail(
                                                imageSource = hit.nutrition?.imageUrl,
                                                category = ItemCategory.fromKey(hit.category),
                                                barcode = hit.barcode,
                                                productName = hit.name,
                                                size = 40.dp
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = hit.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (hit.barcode.isNotBlank()) {
                                                        Text(
                                                            text = hit.barcode,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.5.sp),
                                                            color = colors.mutedText
                                                        )
                                                    }
                                                    hit.nutrition?.energyKcal?.let { kcal ->
                                                        Text(
                                                            text = "${kcal.toInt()} kcal/100g",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontFamily = FontFamily.Monospace,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 10.sp
                                                            ),
                                                            color = colors.cyan
                                                        )
                                                    }
                                                }
                                            }
                                            ScoreColumn(nutrition = hit.nutrition)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Live Shopping Haul Tray over bottom of viewfinder
                androidx.compose.animation.AnimatedVisibility(
                    visible = !showSearchTray && shoppingModeActive && shoppingEntries.isNotEmpty(),
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    val totalAdded = shoppingEntries.sumOf { it.addedQty }.toInt()
                    Surface(
                        shape = RoundedCornerShape(BunkerRadius.Sheet),
                        color = colors.cardSurface.copy(alpha = 0.96f),
                        border = BorderStroke(1.5.dp, colors.emerald.copy(alpha = 0.7f)),
                        shadowElevation = 12.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                items(shoppingEntries, key = { it.itemId }) { entry ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(BunkerRadius.Control))
                                            .background(colors.canvas)
                                            .border(1.dp, colors.borderSubtle, RoundedCornerShape(BunkerRadius.Control))
                                            .clickable { onOpenShoppingItemDetail(entry.itemId) }
                                            .padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (entry.isEnriching) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp,
                                                color = colors.emerald
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = entry.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.5.sp
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = entry.barcode,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 9.5.sp
                                                ),
                                                color = colors.mutedText,
                                                maxLines = 1
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { onAdjustShoppingEntry(entry.itemId, -1.0) },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(15.dp))
                                            }
                                            Text(
                                                text = "+${InventoryLogic.formatQty(entry.addedQty)}",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.ExtraBold
                                                ),
                                                color = colors.emerald,
                                                modifier = Modifier.padding(horizontal = 3.dp)
                                            )
                                            IconButton(
                                                onClick = { onAdjustShoppingEntry(entry.itemId, 1.0) },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, tint = colors.emerald, modifier = Modifier.size(15.dp))
                                            }
                                            IconButton(
                                                onClick = { onUndoShoppingEntry(entry.itemId) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = null, tint = colors.danger, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            // Primary action: Organize haul (batch group location/expiry/category) + quick Done
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onOpenShoppingReview,
                                    shape = RoundedCornerShape(BunkerRadius.Control),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.amber,
                                        contentColor = Color(0xFF111312)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        strings.shoppingReviewBtn(totalAdded),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                OutlinedButton(
                                    onClick = onFinishShoppingSession,
                                    shape = RoundedCornerShape(BunkerRadius.Control),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    modifier = Modifier.height(42.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.emerald, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // Non-blocking Bottom Detection Popup over viewfinder
                androidx.compose.animation.AnimatedVisibility(
                    visible = !showSearchTray && !shoppingModeActive && detectedCandidate != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    val candidate = detectedCandidate ?: return@AnimatedVisibility
                    Surface(
                        shape = RoundedCornerShape(BunkerRadius.Sheet),
                        color = colors.cardSurface,
                        border = BorderStroke(1.5.dp, colors.emerald.copy(alpha = 0.65f)),
                        shadowElevation = 12.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProductThumbnail(
                                    imageSource = detectedExistingItem?.imageUri
                                        ?: detectedExistingItem?.nutrition?.imageUrl
                                        ?: candidate.prefill?.imageUri
                                        ?: candidate.prefill?.nutrition?.imageUrl,
                                    category = ItemCategory.fromKey(detectedExistingItem?.category ?: candidate.prefill?.category),
                                    size = 46.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (detectedExistingItem != null) strings.qsBadgeFound else strings.popupDetectedNew,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.emerald
                                    )
                                    Text(
                                        text = detectedExistingItem?.name
                                            ?: candidate.prefill?.name?.ifBlank { candidate.code }
                                            ?: candidate.code,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = candidate.code,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.amber
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        controls.onResetDebounce()
                                        onDismissCandidate()
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = strings.close,
                                        tint = colors.mutedText
                                    )
                                }
                            }

                            if (candidate.isLookingUp) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = colors.emerald)
                                    Text(strings.scannerLookingUp, style = MaterialTheme.typography.bodyMedium, color = colors.mutedText)
                                }
                            }

                            val nut = detectedExistingItem?.nutrition ?: candidate.prefill?.nutrition
                            if (nut != null) {
                                NutritionBadgesRow(nutrition = nut)
                            }

                            if (detectedExistingItem != null) {
                                val info = InventoryLogic.getExpiryInfo(detectedExistingItem, notApplicableLabel = strings.notApplicable)
                                val unit = ItemUnit.fromKey(detectedExistingItem.unit)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${strings.qsCurrentStock}: ${InventoryLogic.formatQty(detectedExistingItem.qty)} ${strings.unitLabel(unit)}",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    StatusBadge(info = info)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onQuickAddExisting(detectedExistingItem.id, 1.0) },
                                        shape = RoundedCornerShape(BunkerRadius.Control),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("+1")
                                    }
                                    OutlinedButton(
                                        onClick = { onQuickAddExisting(detectedExistingItem.id, 6.0) },
                                        shape = RoundedCornerShape(BunkerRadius.Control)
                                    ) {
                                        Text("+6", fontFamily = FontFamily.Monospace)
                                    }
                                    Button(
                                        onClick = onConfirmCandidate,
                                        shape = RoundedCornerShape(BunkerRadius.Control),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.amber,
                                            contentColor = Color(0xFF111312)
                                        ),
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(strings.popupOkOpen, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                val prefill = candidate.prefill
                                if (!prefill?.expiryDate.isNullOrBlank()) {
                                    Text(
                                        text = "${strings.expiryLabel} ${prefill?.expiryDate}" +
                                            if (!prefill?.notes.isNullOrBlank()) " · ${prefill?.notes}" else "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.mutedText
                                    )
                                }
                                Button(
                                    onClick = onConfirmCandidate,
                                    shape = RoundedCornerShape(BunkerRadius.Control),
                                    modifier = Modifier.fillMaxWidth().height(46.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.popupOkRegister, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Slim docked YsiFoods-style Product Name & Barcode Search bar
        HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.7f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.cardSurface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = {
                    Text(
                        text = strings.scannerManualPlaceholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = onClearSearch, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = strings.clearSearch, modifier = Modifier.size(16.dp))
                        }
                    }
                } else null,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (searchQuery.isNotBlank()) onSearchSubmit(searchQuery)
                    }
                ),
                shape = RoundedCornerShape(BunkerRadius.Control),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.canvas,
                    unfocusedContainerColor = colors.canvas
                )
            )
            Button(
                onClick = {
                    if (searchQuery.isNotBlank()) onSearchSubmit(searchQuery)
                },
                shape = RoundedCornerShape(BunkerRadius.Control),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.height(52.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = strings.scannerManualSubmit)
            }
        }
    }
}

@Composable
private fun ViewfinderIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    active: Boolean,
    activeBg: Color = Color(0xCC111312),
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(BunkerRadius.Control),
        color = when {
            active -> activeBg
            !enabled -> Color(0x77111312)
            else -> Color(0xBB111312)
        },
        border = BorderStroke(1.dp, Color.White.copy(alpha = if (enabled) 0.2f else 0.08f)),
        modifier = Modifier.size(42.dp)
    ) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
