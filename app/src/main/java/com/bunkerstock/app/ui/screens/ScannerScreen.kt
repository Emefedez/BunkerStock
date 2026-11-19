package com.bunkerstock.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bunkerstock.app.data.DetectedScanCandidate
import com.bunkerstock.app.data.InventoryItem
import com.bunkerstock.app.data.ItemUnit
import com.bunkerstock.app.domain.InventoryLogic
import com.bunkerstock.app.scanner.BarcodeAnalyzer
import com.bunkerstock.app.ui.components.NutritionBadgesRow
import com.bunkerstock.app.ui.components.StatusBadge
import com.bunkerstock.app.ui.theme.BunkerRadius
import com.bunkerstock.app.ui.theme.LocalAppStrings
import com.bunkerstock.app.ui.theme.LocalBunkerColors
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
    detectedCandidate: DetectedScanCandidate?,
    detectedExistingItem: InventoryItem?,
    unlockTrigger: Int,
    onCodeScanned: (String) -> Unit,
    onDismissCandidate: () -> Unit,
    onConfirmCandidate: () -> Unit,
    onQuickAddExisting: (String, Double) -> Unit,
    onImageScanError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val strings = LocalAppStrings.current
    val colors = LocalBunkerColors.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }.getOrNull()?.let { bmp ->
                BarcodeAnalyzer.decodeBitmap(bmp) { decoded ->
                    ContextCompat.getMainExecutor(context).execute {
                        if (decoded != null) {
                            onCodeScanned(decoded)
                        } else {
                            onImageScanError(strings.scanImageNoCode)
                        }
                    }
                }
            } ?: onImageScanError(strings.scanImageNoCode)
        }
    }

    var useBackCamera by remember { mutableStateOf(true) }
    var torchEnabled by remember { mutableStateOf(false) }
    var hasFlashUnit by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var manualCode by remember { mutableStateOf("") }

    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val analyzer = remember {
        BarcodeAnalyzer { code ->
            ContextCompat.getMainExecutor(context).execute {
                onCodeScanned(code)
            }
        }
    }

    LaunchedEffect(unlockTrigger) {
        analyzer.resetLastEmitted()
    }

    LaunchedEffect(torchEnabled, boundCamera) {
        boundCamera?.cameraControl?.enableTorch(torchEnabled)
    }

    // Bind CameraX cleanly only when camera selection or permission changes (never on recomposition!)
    LaunchedEffect(previewViewRef, hasCameraPermission, useBackCamera) {
        val pv = previewViewRef ?: return@LaunchedEffect
        if (!hasCameraPermission) return@LaunchedEffect
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = pv.surfaceProvider
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(analyzerExecutor, analyzer)
                    }
                val selector = if (useBackCamera) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }
                cameraProvider.unbindAll()
                val cam = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageAnalysis
                )
                boundCamera = cam
                hasFlashUnit = cam.cameraInfo.hasFlashUnit()
                cameraError = false
            } catch (_: Exception) {
                cameraError = true
            }
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(Unit) {
        onDispose {
            analyzer.close()
            analyzerExecutor.shutdown()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 180.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Card
            Surface(
                shape = RoundedCornerShape(BunkerRadius.Card),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(BunkerRadius.Control))
                            .background(colors.amberContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = colors.amber
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.scannerTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = strings.scannerFormats,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.mutedText
                        )
                    }
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(BunkerRadius.Control),
                        border = BorderStroke(1.dp, colors.amber.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            Icons.Default.ImageSearch,
                            contentDescription = strings.scanFromImage,
                            tint = colors.amber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(strings.scanFromImage, color = colors.amber)
                    }
                }
            }

            // Camera Controls
            if (hasCameraPermission && !cameraError) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            useBackCamera = !useBackCamera
                            torchEnabled = false
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(BunkerRadius.Control),
                        border = BorderStroke(1.dp, colors.borderSubtle)
                    ) {
                        Icon(Icons.Default.Cameraswitch, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.scannerSwitchCamera)
                    }

                    if (hasFlashUnit) {
                        Button(
                            onClick = { torchEnabled = !torchEnabled },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (torchEnabled) colors.amber else colors.cardElevated,
                                contentColor = if (torchEnabled) Color(0xFF090D16) else colors.amber
                            ),
                            shape = RoundedCornerShape(BunkerRadius.Control)
                        ) {
                            Icon(
                                imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(strings.scannerTorch)
                        }
                    }
                }
            }

            // Camera Preview / Permission State
            Surface(
                shape = RoundedCornerShape(BunkerRadius.Card),
                color = Color(0xFF020617),
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                when {
                    !hasCameraPermission -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideocamOff,
                                contentDescription = null,
                                tint = colors.amber,
                                modifier = Modifier.size(40.dp)
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
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.amber,
                                    contentColor = Color(0xFF090D16)
                                ),
                                shape = RoundedCornerShape(BunkerRadius.Control)
                            ) {
                                Text(strings.scannerGrantPermission, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    cameraError -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.scannerCameraUnavailable,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    else -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { ctx ->
                                    PreviewView(ctx).apply {
                                        scaleType = PreviewView.ScaleType.FILL_CENTER
                                    }.also { previewViewRef = it }
                                }
                            )

                            // Tactical Viewfinder Overlay
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(236.dp)
                                        .height(140.dp)
                                        .border(2.dp, colors.amber.copy(alpha = 0.85f), RoundedCornerShape(10.dp)),
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
                                    color = Color(0xCC090D16),
                                    border = BorderStroke(1.dp, colors.amber.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = strings.scannerGuideHint,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.amber,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Manual Barcode Entry Fallback
            Surface(
                shape = RoundedCornerShape(BunkerRadius.Card),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = strings.scannerManualLabel,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualCode,
                            onValueChange = { manualCode = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = {
                                Text(
                                    text = strings.scannerManualPlaceholder,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.mutedText
                                )
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (manualCode.isNotBlank()) {
                                        onCodeScanned(manualCode.trim())
                                        manualCode = ""
                                    }
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
                                if (manualCode.isNotBlank()) {
                                    onCodeScanned(manualCode.trim())
                                    manualCode = ""
                                }
                            },
                            shape = RoundedCornerShape(BunkerRadius.Control),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.height(54.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = strings.scannerManualSubmit)
                        }
                    }
                }
            }
        }

        // Non-blocking Bottom Detection Popup (keeps camera open until dismissed or confirmed!)
        AnimatedVisibility(
            visible = detectedCandidate != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp)
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
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header with X dismiss button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                                analyzer.resetLastEmitted()
                                onDismissCandidate()
                            },
                            modifier = Modifier.size(44.dp)
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

                    // Show nutrition & health score if enriched
                    val nut = detectedExistingItem?.nutrition ?: candidate.prefill?.nutrition
                    if (nut != null) {
                        NutritionBadgesRow(nutrition = nut)
                    }

                    // Existing item telemetry or estimated expiry preview
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
                                    contentColor = Color(0xFF090D16)
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
                            modifier = Modifier.fillMaxWidth().height(48.dp)
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
