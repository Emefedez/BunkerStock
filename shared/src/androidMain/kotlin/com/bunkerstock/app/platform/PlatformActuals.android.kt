package com.bunkerstock.app.platform

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.random.Random

actual fun createGrainImageBitmap(isDark: Boolean): ImageBitmap? {
    val size = 160
    val bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val rng = Random(20261119)
    val pixels = IntArray(size * size)
    for (i in pixels.indices) {
        val v = if (isDark) (14 + rng.nextInt(26)) else (160 + rng.nextInt(45))
        val a = if (isDark) (4 + rng.nextInt(9)) else (4 + rng.nextInt(7))
        pixels[i] = android.graphics.Color.argb(a, v, v, v)
    }
    bmp.setPixels(pixels, 0, size, 0, 0, size, size)
    return bmp.asImageBitmap()
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
