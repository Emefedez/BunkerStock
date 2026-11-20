package com.bunkerstock.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bunkerstock.app.data.AppLanguage
import com.bunkerstock.app.data.ExpiryBatch
import com.bunkerstock.app.data.InventoryItem
import com.bunkerstock.app.data.ItemCategory
import com.bunkerstock.app.data.ProductNutrition
import com.bunkerstock.app.domain.InventoryLogic
import com.bunkerstock.app.platform.BunkerDate
import com.bunkerstock.app.platform.BunkerYearMonth
import com.bunkerstock.app.platform.LocalBunkerPlatform
import com.bunkerstock.app.platform.PlatformBackHandler
import com.bunkerstock.app.platform.currentTimeMillis
import com.bunkerstock.app.platform.formatOneDecimal
import com.bunkerstock.app.platform.todayBunkerDate
import com.bunkerstock.app.ui.theme.BunkerGrainOverlay
import com.bunkerstock.app.ui.theme.BunkerRadius
import com.bunkerstock.app.ui.theme.LocalAppStrings
import com.bunkerstock.app.ui.theme.LocalBunkerColors
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private val BatchPalette = listOf(
    Color(0xFF4ADE80),
    Color(0xFFFACC15),
    Color(0xFF38BDF8),
    Color(0xFFFB7185),
    Color(0xFFC084FC)
)

@Composable
fun ProductDetailOverlay(
    item: InventoryItem?,
    originX: Float = 0.2f,
    originY: Float = 0.5f,
    expiredDays: Int,
    criticalDays: Int,
    warningDays: Int,
    onUpdateItem: (InventoryItem) -> Unit,
    onOpenFullEditor: (InventoryItem) -> Unit,
    onMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var lastItem by remember { mutableStateOf(item) }
    if (item != null) lastItem = item

    var showFullscreenPhoto by remember(item?.id) { mutableStateOf(false) }
    var activeHeroBitmap by remember(item?.id) { mutableStateOf<ImageBitmap?>(null) }

    PlatformBackHandler(enabled = item != null) {
        if (showFullscreenPhoto) {
            showFullscreenPhoto = false
        } else {
            onDismiss()
        }
    }

    val zoomOrigin = TransformOrigin(
        pivotFractionX = originX.coerceIn(0.05f, 0.95f),
        pivotFractionY = originY.coerceIn(0.08f, 0.92f)
    )

    AnimatedVisibility(
        visible = item != null,
        enter = scaleIn(
            initialScale = 0.16f,
            transformOrigin = zoomOrigin,
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
        ) + slideInVertically(
            initialOffsetY = { (it * 0.08f).toInt() },
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
        ) + fadeIn(tween(220)),
        exit = scaleOut(
            targetScale = 0.20f,
            transformOrigin = zoomOrigin,
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
        ) + slideOutVertically(
            targetOffsetY = { (it * 0.06f).toInt() },
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(180))
    ) {
        val active = item ?: lastItem ?: return@AnimatedVisibility
        Box(modifier = Modifier.fillMaxSize()) {
            ProductDetailFullPage(
                item = active,
                expiredDays = expiredDays,
                criticalDays = criticalDays,
                warningDays = warningDays,
                onBitmapLoaded = { activeHeroBitmap = it },
                onPhotoClick = {
                    if (activeHeroBitmap != null) showFullscreenPhoto = true
                },
                onUpdateItem = onUpdateItem,
                onOpenFullEditor = { onOpenFullEditor(active) },
                onMessage = onMessage,
                onBack = onDismiss
            )

            AnimatedVisibility(
                visible = showFullscreenPhoto && activeHeroBitmap != null,
                enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.92f, animationSpec = tween(240)),
                exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.94f, animationSpec = tween(180))
            ) {
                activeHeroBitmap?.let { bmp ->
                    FullscreenZoomablePhotoViewer(
                        bitmap = bmp,
                        title = active.name,
                        onBack = { showFullscreenPhoto = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductDetailFullPage(
    item: InventoryItem,
    expiredDays: Int,
    criticalDays: Int,
    warningDays: Int,
    onBitmapLoaded: (ImageBitmap?) -> Unit,
    onPhotoClick: () -> Unit,
    onUpdateItem: (InventoryItem) -> Unit,
    onOpenFullEditor: () -> Unit,
    onMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    val platform = LocalBunkerPlatform.current
    val strings = LocalAppStrings.current
    val colors = LocalBunkerColors.current
    val today = remember { todayBunkerDate() }

    val category = ItemCategory.fromKey(item.category)
    val (catIcon, catFg, catBg) = categoryStyle(category)

    val info = InventoryLogic.getExpiryInfo(
        item = item,
        today = today,
        notApplicableLabel = strings.notApplicable,
        expiredDays = expiredDays,
        criticalDays = criticalDays,
        warningDays = warningDays
    )
    val (expFg, expBg) = expiryColors(info.status)

    var animStage1 by remember(item.id) { mutableStateOf(false) }
    var animStage2 by remember(item.id) { mutableStateOf(false) }
    var animStage3 by remember(item.id) { mutableStateOf(false) }
    LaunchedEffect(item.id) {
        animStage1 = false
        animStage2 = false
        animStage3 = false
        delay(90)
        animStage1 = true
        delay(110)
        animStage2 = true
        delay(100)
        animStage3 = true
    }

    val stage1Alpha by animateFloatAsState(if (animStage1) 1f else 0f, tween(340, easing = FastOutSlowInEasing), label = "s1a")
    val stage1Offset by animateFloatAsState(if (animStage1) 0f else 18f, tween(340, easing = FastOutSlowInEasing), label = "s1y")
    val stage2Alpha by animateFloatAsState(if (animStage2) 1f else 0f, tween(380, easing = FastOutSlowInEasing), label = "s2a")
    val stage2Offset by animateFloatAsState(if (animStage2) 0f else 22f, tween(380, easing = FastOutSlowInEasing), label = "s2y")
    val stage3Alpha by animateFloatAsState(if (animStage3) 1f else 0f, tween(380, easing = FastOutSlowInEasing), label = "s3a")

    var heroBitmap by remember(item.id, item.imageUri, item.nutrition?.imageUrl, item.barcode, item.name) {
        mutableStateOf<ImageBitmap?>(null)
    }
    var isPhotoLoading by remember(item.id, item.imageUri, item.nutrition?.imageUrl, item.barcode, item.name) {
        mutableStateOf(true)
    }
    LaunchedEffect(item.id, item.imageUri, item.nutrition?.imageUrl, item.barcode, item.name) {
        val src = item.imageUri ?: item.nutrition?.imageUrl
        val shouldLoad = !src.isNullOrBlank() || item.barcode.isNotBlank() || item.name.isNotBlank()
        isPhotoLoading = shouldLoad
        val loaded = when {
            !src.isNullOrBlank() -> platform.loadProductImage(src)
                ?: platform.resolveProductImage(item.barcode, item.name)
            item.barcode.isNotBlank() || item.name.isNotBlank() ->
                platform.resolveProductImage(item.barcode, item.name)
            else -> null
        }
        heroBitmap = loaded
        onBitmapLoaded(loaded)
        isPhotoLoading = false
    }

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
        BunkerDate.parseIso(activeBatch.expiryDate.trim())
            ?.let { BunkerYearMonth.from(it) }
            ?: BunkerYearMonth.from(today)
    }
    var displayedMonth by remember(item.id) { mutableStateOf(initialMonth) }

    var catMenuExpanded by remember { mutableStateOf(false) }
    var notesDraft by remember(item.id, item.notes) { mutableStateOf(item.notes) }
    var locationDraft by remember(item.id, item.location) { mutableStateOf(item.location) }

    val qrCodeContent = item.barcode.ifBlank { item.id }
    val qrBitmap = remember(qrCodeContent) { platform.generateQrImage(qrCodeContent, 360) }
    val authorName = item.addedBy.ifBlank { item.updatedBy }.trim()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) {
        BunkerGrainOverlay(isDark = colors.isDark)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(236.dp)
                    .background(if (heroBitmap != null) Color(0xFF141716) else catBg)
                    .clickable(enabled = heroBitmap != null, onClick = onPhotoClick)
            ) {
                Crossfade(
                    targetState = heroBitmap,
                    animationSpec = tween(320),
                    label = "heroPhotoFade"
                ) { bmp ->
                    if (bmp != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                bitmap = bmp,
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
                                                Color(0x88000000),
                                                Color.Transparent,
                                                Color(0xD8111312)
                                            )
                                        )
                                    )
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = catIcon,
                                contentDescription = null,
                                tint = catFg.copy(alpha = if (isPhotoLoading) 0.42f else 0.85f),
                                modifier = Modifier.size(68.dp)
                            )
                            if (isPhotoLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(86.dp),
                                    strokeWidth = 3.dp,
                                    color = catFg
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(BunkerRadius.Control),
                        color = Color(0xBB111312),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = strings.close,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(BunkerRadius.Control),
                        color = Color(0xBB111312),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        IconButton(onClick = onOpenFullEditor, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = strings.editAction,
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (authorName.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(BunkerRadius.Badge),
                            color = Color(0xCC111312),
                            border = BorderStroke(1.dp, colors.emerald.copy(alpha = 0.55f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = colors.emerald,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = strings.detailAddedBy(authorName),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    if (heroBitmap != null) {
                        Surface(
                            shape = RoundedCornerShape(BunkerRadius.Badge),
                            color = Color(0xBB111312),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = strings.detailTapPhotoHint,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = strings.detailTapPhotoHint,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. PRODUCT NAME + EQUAL-HEIGHT FULL-COLOR EXPIRY & CATEGORY/PLACEMENT BOXES
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = stage1Alpha
                            translationY = stage1Offset
                        },
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Entire box tinted with expiry status color; concise "Hace 6 días" + date beneath
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(BunkerRadius.Control))
                                .background(expBg)
                                .border(1.dp, expFg.copy(alpha = 0.55f), RoundedCornerShape(BunkerRadius.Control))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(expFg)
                                )
                                Text(
                                    text = strings.expiryCompactLabel(info),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = expFg,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = expFg.copy(alpha = 0.85f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = info.formattedDate,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Category & Placement box — matched exact height via IntrinsicSize.Min + fillMaxHeight
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(BunkerRadius.Control))
                                .background(colors.cardSurface)
                                .border(1.dp, colors.borderSubtle, RoundedCornerShape(BunkerRadius.Control))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
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
                }

                // 2. NUTRI-SCORE + YUKA BIOSHOCK GAUGE + CONCRETE CALORIES/MACROS
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = stage2Alpha
                            translationY = stage2Offset
                        },
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        YsiFoodsNutriScoreCard(
                            grade = item.nutrition?.nutriScoreGrade,
                            label = strings.detailNutriScoreLabel,
                            emptyLabel = strings.detailNoScore,
                            animateIn = animStage2,
                            modifier = Modifier.weight(1f)
                        )

                        YukaBioShockGaugeCard(
                            score = item.nutrition?.healthScore,
                            label = strings.detailYukaLabel,
                            emptyLabel = strings.detailNoScore,
                            animateIn = animStage2,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    item.nutrition?.let { nut ->
                        YsiFoodsNutritionPanel(
                            nutrition = nut,
                            language = strings.language,
                            per100Label = strings.detailPer100g,
                            chartScaleLabel = strings.detailChartScale
                        )
                    }
                }

                HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.6f))

                // 3. CATEGORY ICON DROPDOWN + QUANTITY + NO EXPIRY + BATCHES/CALENDAR + QR + NOTES
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(stage3Alpha),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            Row(
                                modifier = Modifier
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(BunkerRadius.Control))
                                    .background(catBg)
                                    .border(1.dp, catFg.copy(alpha = 0.5f), RoundedCornerShape(BunkerRadius.Control))
                                    .clickable(role = Role.Button) { catMenuExpanded = true }
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
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

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(BunkerRadius.Control))
                                .background(colors.cardSurface)
                                .border(1.dp, colors.borderSubtle, RoundedCornerShape(BunkerRadius.Control)),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
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
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = strings.subtractOne, modifier = Modifier.size(18.dp))
                            }
                            Text(
                                text = InventoryLogic.formatQty(item.qty),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp
                                ),
                                color = if (item.qty <= 0.0) colors.danger else MaterialTheme.colorScheme.onSurface
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
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = strings.addOne, tint = colors.emerald, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

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

                    if (!item.noExpiry) {
                        Surface(
                            shape = RoundedCornerShape(BunkerRadius.Card),
                            color = colors.cardSurface,
                            border = BorderStroke(1.dp, colors.borderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
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
                                                            id = "b-${currentTimeMillis()}-1",
                                                            qty = item.qty.coerceAtLeast(1.0),
                                                            expiryDate = item.expiryDate.ifBlank { InventoryLogic.relativeDateString(90, today) }
                                                        )
                                                    )
                                                }
                                                val newBatch = ExpiryBatch(
                                                    id = "b-${currentTimeMillis()}-${baseBatches.size + 1}",
                                                    qty = 1.0,
                                                    expiryDate = InventoryLogic.relativeDateString(120, today)
                                                )
                                                val nextList = baseBatches + newBatch
                                                activeBatchId = newBatch.id
                                                visibleBatchIds = nextList.map { it.id }.toSet()
                                                displayedMonth = BunkerYearMonth.from(today.plusDays(120))
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

                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    effectiveBatches.forEachIndexed { idx, batch ->
                                        val batchColor = BatchPalette[idx % BatchPalette.size]
                                        val isVisibleOnMap = visibleBatchIds.contains(batch.id)
                                        val isActive = activeBatch.id == batch.id
                                        val parsedDate = BunkerDate.parseIso(batch.expiryDate.trim())
                                        val dateText = parsedDate?.toDisplayDdMmYyyy() ?: strings.notApplicable

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(BunkerRadius.Control))
                                                .background(if (isActive) colors.cardElevated else colors.canvas)
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
                                                        displayedMonth = BunkerYearMonth.from(parsedDate)
                                                    }
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                                        ) {
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
                                                    text = "${idx + 1}. $dateText",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                                        fontSize = 13.sp
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

                                val activeDate = BunkerDate.parseIso(activeBatch.expiryDate.trim())
                                if (activeDate != null) {
                                    ExpiryApproachBar(
                                        today = today,
                                        todayLabel = strings.detailCalendarToday.uppercase(),
                                        expiryDate = activeDate,
                                        expiredDays = expiredDays,
                                        criticalDays = criticalDays,
                                        warningDays = warningDays
                                    )
                                }

                                val visibleMarkers = effectiveBatches.mapIndexedNotNull { idx, b ->
                                    if (!visibleBatchIds.contains(b.id)) null
                                    else {
                                        val d = BunkerDate.parseIso(b.expiryDate.trim())
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
                                    onJumpToday = { displayedMonth = BunkerYearMonth.from(today) },
                                    onSelectDate = { picked ->
                                        val iso = picked.toIso()
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

                    // QR LABEL SECTION
                    Surface(
                        shape = RoundedCornerShape(BunkerRadius.Card),
                        color = colors.cardSurface,
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
                                        bitmap = qrBitmap,
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
                                            platform.printSingleQrLabel(
                                                header = strings.qrHeaderBrand,
                                                title = item.name,
                                                meta = meta,
                                                qrContent = qrCodeContent
                                            )
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
                                            val ok = platform.shareSingleQrLabel(
                                                header = strings.qrHeaderBrand,
                                                title = item.name,
                                                meta = meta,
                                                qrContent = qrCodeContent
                                            )
                                            onMessage(if (ok) strings.toastQrShared else strings.toastExportError)
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

                    // NOTES & PLACEMENT (INLINE EDITABLE)
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
                                focusedContainerColor = colors.cardSurface,
                                unfocusedContainerColor = colors.cardSurface,
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
                                focusedContainerColor = colors.cardSurface,
                                unfocusedContainerColor = colors.cardSurface,
                                unfocusedBorderColor = colors.borderSubtle
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun FullscreenZoomablePhotoViewer(
    bitmap: ImageBitmap,
    title: String,
    onBack: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF5080A09))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val nextScale = (scale * zoom).coerceIn(1f, 4.5f)
                    scale = nextScale
                    offset = if (nextScale <= 1.02f) Offset.Zero else offset + pan
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.2f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(BunkerRadius.Control),
                color = Color(0xBB111312),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun YsiFoodsNutriScoreCard(
    grade: String?,
    label: String,
    emptyLabel: String,
    animateIn: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalBunkerColors.current
    val upper = grade?.uppercase()
    val pop by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0.7f,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "nutriPop"
    )
    val grades = listOf(
        "A" to Color(0xFF17823B),
        "B" to Color(0xFF73A92D),
        "C" to Color(0xFFD0A514),
        "D" to Color(0xFFD16D16),
        "E" to Color(0xFFBD2B24)
    )

    Column(
        modifier = modifier
            .height(108.dp)
            .clip(RoundedCornerShape(BunkerRadius.Card))
            .background(colors.cardSurface)
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
                            .scale(if (active) pop else 1f)
                            .size(width = if (active) 32.dp else 21.dp, height = if (active) 40.dp else 28.dp)
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
            Text(text = emptyLabel, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = colors.mutedText)
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun YukaBioShockGaugeCard(
    score: Int?,
    label: String,
    emptyLabel: String,
    animateIn: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalBunkerColors.current
    val targetFraction = if (animateIn) (score?.coerceIn(0, 100) ?: 0) / 100f else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 820, easing = FastOutSlowInEasing),
        label = "yukaGauge"
    )
    val carrotAlpha by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "carrotAlpha"
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
            .height(108.dp)
            .clip(RoundedCornerShape(BunkerRadius.Card))
            .background(colors.cardSurface)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(BunkerRadius.Card))
            .padding(horizontal = 10.dp, vertical = 10.dp),
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
                .width(114.dp)
                .height(68.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 8.dp.toPx()
                val pad = strokeW / 2f + 2.dp.toPx()
                val arcSize = Size(size.width - pad * 2f, size.width - pad * 2f)
                val topLeft = Offset(pad, 3.dp.toPx())

                drawArc(
                    color = Color(0xFF2B302C),
                    startAngle = 170f,
                    sweepAngle = 200f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeW + 2.dp.toPx(), cap = StrokeCap.Butt)
                )

                val cx = topLeft.x + arcSize.width / 2f
                val cy = topLeft.y + arcSize.height / 2f
                val rOuter = arcSize.width / 2f + strokeW * 0.54f
                val rInner = arcSize.width / 2f - strokeW * 0.54f
                for (step in 0..8) {
                    val deg = (170.0 + step * 25.0) * PI / 180.0
                    drawLine(
                        color = Color(0x77090B0A),
                        start = Offset(cx + (rInner * cos(deg)).toFloat(), cy + (rInner * sin(deg)).toFloat()),
                        end = Offset(cx + (rOuter * cos(deg)).toFloat(), cy + (rOuter * sin(deg)).toFloat()),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }

                if (score != null && animatedFraction > 0.005f) {
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

                val carrotCx = size.width / 2f
                val carrotCy = size.height * 0.55f
                val s = size.height * 0.24f

                val leafColor = Color(0xFF4ADE80).copy(alpha = carrotAlpha)
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

                val body = Path().apply {
                    moveTo(carrotCx - s * 0.78f, carrotCy + s * 0.72f)
                    lineTo(carrotCx + s * 0.05f, carrotCy - s * 0.58f)
                    quadraticTo(
                        carrotCx + s * 0.52f, carrotCy - s * 0.62f,
                        carrotCx + s * 0.58f, carrotCy - s * 0.12f
                    )
                    close()
                }
                drawPath(path = body, color = Color(0xFFF97316).copy(alpha = carrotAlpha))
                drawLine(
                    color = Color(0x88FFEDD5).copy(alpha = carrotAlpha),
                    start = Offset(carrotCx - s * 0.12f, carrotCy - s * 0.08f),
                    end = Offset(carrotCx + s * 0.12f, carrotCy + s * 0.14f),
                    strokeWidth = 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            val displayedScore = score?.let { (it * animatedFraction / targetFraction.coerceAtLeast(0.01f)).toInt().coerceIn(0, it) }
            Text(
                text = if (score != null) "${displayedScore ?: score}/100" else emptyLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.5.sp
                ),
                color = if (score != null) gaugeColor else colors.mutedText
            )
        }
    }
}

private data class YsiMetric(
    val label: String,
    val value: Double?,
    val unit: String,
    val color: Color
)

@Composable
private fun YsiFoodsNutritionPanel(
    nutrition: ProductNutrition,
    language: AppLanguage,
    per100Label: String,
    chartScaleLabel: String
) {
    val colors = LocalBunkerColors.current
    val hasAnyConcrete = listOf(
        nutrition.energyKcal,
        nutrition.proteinGrams,
        nutrition.carbsGrams,
        nutrition.sugarsGrams,
        nutrition.fatGrams,
        nutrition.satFatGrams,
        nutrition.fiberGrams,
        nutrition.saltGrams
    ).any { it != null }
    if (!hasAnyConcrete) return

    var chartOpen by remember { mutableStateOf(false) }

    val labels = if (language == AppLanguage.ES) {
        listOf("Calorías", "Proteína", "Carbs", "Azúcares", "Grasa", "Saturadas", "Fibra", "Sal")
    } else {
        listOf("Calories", "Protein", "Carbs", "Sugars", "Fat", "Saturates", "Fibre", "Salt")
    }
    val metrics = listOf(
        YsiMetric(labels[0], nutrition.energyKcal, "kcal", Color(0xFF4ADE80)),
        YsiMetric(labels[1], nutrition.proteinGrams, "g", Color(0xFF60A5FA)),
        YsiMetric(labels[2], nutrition.carbsGrams, "g", Color(0xFFFACC15)),
        YsiMetric(labels[3], nutrition.sugarsGrams, "g", Color(0xFFFB923C)),
        YsiMetric(labels[4], nutrition.fatGrams, "g", Color(0xFF84CC16)),
        YsiMetric(labels[5], nutrition.satFatGrams, "g", Color(0xFFF87171)),
        YsiMetric(labels[6], nutrition.fiberGrams, "g", Color(0xFF2DD4BF)),
        YsiMetric(labels[7], nutrition.saltGrams, "g", Color(0xFFC084FC))
    )

    fun fmtVal(v: Double?, unit: String): String {
        if (v == null) return "—"
        val num = if (unit == "kcal" || v % 1.0 == 0.0) v.toInt().toString()
        else formatOneDecimal(v)
        return "$num $unit"
    }

    Surface(
        shape = RoundedCornerShape(BunkerRadius.Card),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = per100Label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    ),
                    color = colors.mutedText
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (chartOpen) colors.emerald else colors.canvas)
                        .border(1.dp, if (chartOpen) colors.emerald else colors.borderSubtle, CircleShape)
                        .clickable(role = Role.Button) { chartOpen = !chartOpen },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = if (chartOpen) Color(0xFF111312) else colors.emerald,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                metrics.chunked(4).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowItems.forEach { m ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(BunkerRadius.Badge))
                                    .background(colors.canvas)
                                    .padding(horizontal = 6.dp, vertical = 5.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = m.label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                    color = colors.mutedText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = fmtVal(m.value, m.unit),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    ),
                                    color = if (m.value != null) m.color else colors.mutedText,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = chartOpen,
                enter = expandVertically(tween(240)) + fadeIn(tween(220)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.6f))
                    metrics.drop(1).forEach { m ->
                        val frac by animateFloatAsState(
                            targetValue = ((m.value ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f),
                            animationSpec = tween(550, easing = FastOutSlowInEasing),
                            label = "bar_${m.label}"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = m.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = colors.mutedText,
                                maxLines = 1,
                                modifier = Modifier.width(64.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colors.canvas)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(frac)
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(m.color)
                                )
                            }
                            Text(
                                text = fmtVal(m.value, m.unit),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(44.dp)
                            )
                        }
                    }
                    Text(
                        text = chartScaleLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = colors.mutedText,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (nutrition.safetyFlags.isNotEmpty()) {
                NutritionBadgesRow(
                    nutrition = nutrition.copy(nutriScoreGrade = null, healthScore = null, energyKcal = null)
                )
            }
        }
    }
}

private data class BatchCalendarMarker(
    val date: BunkerDate,
    val color: Color,
    val isActive: Boolean
)

@Composable
private fun ExpiryApproachBar(
    today: BunkerDate,
    todayLabel: String,
    expiryDate: BunkerDate,
    expiredDays: Int,
    criticalDays: Int,
    warningDays: Int
) {
    val colors = LocalBunkerColors.current
    val daysLeft = today.daysUntil(expiryDate)
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
                text = todayLabel,
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
                text = expiryDate.toDisplayDdMmYy(),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.5.sp),
                color = barColor
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.canvas)
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
    month: BunkerYearMonth,
    today: BunkerDate,
    markers: List<BatchCalendarMarker>,
    language: AppLanguage,
    todayLabel: String,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToday: () -> Unit,
    onSelectDate: (BunkerDate) -> Unit
) {
    val colors = LocalBunkerColors.current
    val monthTitle = remember(month, language) { month.formatTitle(language) }
    val dayHeaders = if (language == AppLanguage.ES) {
        listOf("L", "M", "X", "J", "V", "S", "D")
    } else {
        listOf("M", "T", "W", "T", "F", "S", "S")
    }

    val firstOfMonth = month.atDay(1)
    val leadingBlanks = firstOfMonth.dayOfWeekIso() - 1
    val daysInMonth = month.lengthOfMonth()
    val totalCells = ((leadingBlanks + daysInMonth + 6) / 7) * 7
    val rows = totalCells / 7

    val activeMarkerDate = markers.firstOrNull { it.isActive }?.date ?: markers.firstOrNull()?.date

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    color = colors.canvas,
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
