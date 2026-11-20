package com.bunkerstock.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

actual fun createGrainImageBitmap(isDark: Boolean): ImageBitmap? = null

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op on desktop JVM test target
}
