package com.bunkerstock.app

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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bunkerstock.app.platform.CameraOverlayControls
import com.bunkerstock.app.scanner.BarcodeAnalyzer
import com.bunkerstock.app.ui.theme.LocalAppStrings
import java.util.concurrent.Executors

/**
 * 100% Native Android CameraX + bundled offline Google ML Kit Barcode Scanner viewfinder.
 */
@Composable
fun AndroidCameraViewfinder(
    unlockTrigger: Int,
    onCodeScanned: (String) -> Unit,
    onImageScanError: (String) -> Unit,
    modifier: Modifier,
    overlayContent: @Composable (CameraOverlayControls) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val strings = LocalAppStrings.current

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

    val currentOnCodeScanned by rememberUpdatedState(onCodeScanned)
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val analyzer = remember {
        BarcodeAnalyzer { code ->
            ContextCompat.getMainExecutor(context).execute {
                currentOnCodeScanned(code)
            }
        }
    }

    LaunchedEffect(unlockTrigger) {
        analyzer.resetLastEmitted()
    }

    LaunchedEffect(torchEnabled, boundCamera) {
        boundCamera?.cameraControl?.enableTorch(torchEnabled)
    }

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

    Box(modifier = modifier.background(Color(0xFF0A0C0B))) {
        if (hasCameraPermission && !cameraError) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }.also { previewViewRef = it }
                }
            )
        }

        overlayContent(
            CameraOverlayControls(
                hasPermission = hasCameraPermission,
                cameraError = cameraError,
                hasFlash = hasFlashUnit,
                torchEnabled = torchEnabled,
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onToggleTorch = { if (hasFlashUnit) torchEnabled = !torchEnabled },
                onSwitchCamera = {
                    useBackCamera = !useBackCamera
                    torchEnabled = false
                },
                onPickImageToScan = { imagePickerLauncher.launch("image/*") },
                onResetDebounce = { analyzer.resetLastEmitted() }
            )
        )
    }
}
