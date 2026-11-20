package com.bunkerstock.web

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import com.bunkerstock.app.platform.CameraOverlayControls
import com.bunkerstock.app.ui.theme.LocalAppStrings
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.resume
import kotlin.js.JsString
import kotlin.js.Promise

private fun jsStartCamera(useBack: Boolean, onCode: (JsString) -> Unit): Promise<JsString> =
    js("window.BunkerWeb.startCamera(useBack, onCode)")

private fun jsStopCamera(): Unit = js("window.BunkerWeb.stopCamera()")

private fun jsSetVideoBounds(left: Double, top: Double, width: Double, height: Double): Unit =
    js("window.BunkerWeb.setVideoBounds(left, top, width, height)")

private fun jsSetTorch(enabled: Boolean): Promise<JsString?> =
    js("window.BunkerWeb.setTorch(enabled).then(function(r){ return String(r); })")

private fun jsSetZoom(zoomed: Boolean): Promise<JsString?> =
    js("window.BunkerWeb.setZoom(zoomed).then(function(r){ return String(r); })")

private fun jsResetScanDebounce(): Unit = js("window.BunkerWeb.resetScanDebounce()")

private fun jsPickAndDecodeImage(): Promise<JsString> =
    js("window.BunkerWeb.pickAndDecodeImage()")

internal suspend fun Promise<JsString>.awaitString(): String = suspendCancellableCoroutine { cont ->
    then(
        onFulfilled = { value ->
            cont.resume(value.toString())
            null
        },
        onRejected = {
            cont.resume("")
            null
        }
    )
}

@Composable
fun WebCameraViewfinder(
    unlockTrigger: Int,
    onCodeScanned: (String) -> Unit,
    onImageScanError: (String) -> Unit,
    modifier: Modifier,
    overlayContent: @Composable (CameraOverlayControls) -> Unit
) {
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember { mutableStateOf(true) }
    var cameraError by remember { mutableStateOf(false) }
    var useBackCamera by remember { mutableStateOf(true) }
    var hasFlash by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    var supportsZoom by remember { mutableStateOf(false) }
    var zoomActive by remember { mutableStateOf(false) }
    var retryCounter by remember { mutableStateOf(0) }

    LaunchedEffect(unlockTrigger) {
        jsResetScanDebounce()
    }

    LaunchedEffect(useBackCamera, retryCounter) {
        val raw = jsStartCamera(useBackCamera) { codeJs ->
            val decoded = codeJs.toString()
            if (decoded.isNotBlank()) onCodeScanned(decoded)
        }.awaitString()

        val parsed = runCatching { Json.parseToJsonElement(raw).jsonObject }.getOrNull()
        val ok = parsed?.get("ok")?.jsonPrimitive?.booleanOrNull == true
        val denied = parsed?.get("denied")?.jsonPrimitive?.booleanOrNull == true
        if (ok) {
            hasPermission = true
            cameraError = false
            hasFlash = parsed?.get("hasFlash")?.jsonPrimitive?.booleanOrNull == true
            supportsZoom = parsed?.get("supportsZoom")?.jsonPrimitive?.booleanOrNull == true
            torchEnabled = false
            zoomActive = false
        } else if (denied) {
            hasPermission = false
            cameraError = false
        } else {
            hasPermission = true
            cameraError = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            jsStopCamera()
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                val b = coords.boundsInWindow()
                jsSetVideoBounds(b.left.toDouble(), b.top.toDouble(), b.width.toDouble(), b.height.toDouble())
            }
            .drawBehind {
                if (hasPermission && !cameraError) {
                    // Punch a crisp transparent hole through the Compose Skiko canvas so the live <video> shines through at 60fps
                    drawRect(color = Color.Transparent, blendMode = BlendMode.Clear)
                }
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            overlayContent(
                CameraOverlayControls(
                    hasPermission = hasPermission,
                    cameraError = cameraError,
                    hasFlash = hasFlash,
                    torchEnabled = torchEnabled,
                    zoomActive = zoomActive,
                    supportsZoom = supportsZoom,
                    onRequestPermission = { retryCounter++ },
                    onToggleTorch = {
                        if (hasFlash) {
                            val next = !torchEnabled
                            torchEnabled = next
                            scope.launch { jsSetTorch(next) }
                        }
                    },
                    onSwitchCamera = {
                        useBackCamera = !useBackCamera
                        torchEnabled = false
                        zoomActive = false
                    },
                    onToggleZoom = {
                        if (supportsZoom) {
                            val next = !zoomActive
                            zoomActive = next
                            scope.launch { jsSetZoom(next) }
                        }
                    },
                    onPickImageToScan = {
                        scope.launch {
                            val code = jsPickAndDecodeImage().awaitString().trim()
                            if (code.isNotEmpty()) {
                                onCodeScanned(code)
                            } else {
                                onImageScanError(strings.scanImageNoCode)
                            }
                        }
                    },
                    onResetDebounce = { jsResetScanDebounce() }
                )
            )
        }
    }
}
