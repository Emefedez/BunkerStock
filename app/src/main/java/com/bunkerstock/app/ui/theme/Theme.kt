package com.bunkerstock.app.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bunkerstock.app.data.AppThemeMode
import kotlin.random.Random

/** Machined bunker corner radii: sharp industrial steel feel */
object BunkerRadius {
    val Card = 6.dp
    val Sheet = 8.dp
    val Control = 4.dp
    val Badge = 3.dp
}

@Composable
fun BunkerGrainOverlay(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val grainBrush = remember(isDark) {
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
        val imageBitmap: ImageBitmap = bmp.asImageBitmap()
        ShaderBrush(ImageShader(imageBitmap, TileMode.Repeated, TileMode.Repeated))
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(brush = grainBrush)
    }
}

// Nuclear Concrete & Gunmetal Bunker Palette (zero navy blue)
val BunkerCharcoal950 = Color(0xFF111312) // Deep bunker concrete
val BunkerConcrete900 = Color(0xFF191C1A) // Armored panel surface
val BunkerSteel850    = Color(0xFF222623) // Elevated gunmetal
val BunkerSteel800    = Color(0xFF2D332E) // Border / divider
val BunkerSteel700    = Color(0xFF3F4741) // Stronger outline
val BunkerAsh400      = Color(0xFF9AA39C) // Muted technical text
val BunkerAsh300      = Color(0xFFC5CDC7) // Secondary light text
val BunkerAsh100      = Color(0xFFEAEFEA) // Primary crisp phosphor-white text

val GeigerGreen500    = Color(0xFF22C55E) // Phosphor tactical green
val GeigerGreen400    = Color(0xFF4ADE80)
val GeigerGreen700    = Color(0xFF15803D)

val RadAmber500       = Color(0xFFEAB308) // Radiation hazard yellow-amber
val RadAmber400       = Color(0xFFFACC15)
val RadAmber700       = Color(0xFFA16207)

val CautionYellow400  = Color(0xFFFDE047)
val CautionYellow700  = Color(0xFF854D0E)

val HazardRed500      = Color(0xFFEF4444)
val HazardRed400      = Color(0xFFF87171)
val HazardRed700      = Color(0xFFB91C1C)

val CoolantCyan400    = Color(0xFF38BDF8)
val CoolantCyan700    = Color(0xFF0369A1)

val MedRose400        = Color(0xFFFB7185)
val MedRose700        = Color(0xFFBE123C)

val SteelBlue400      = Color(0xFF93C5FD)

@Immutable
data class BunkerExtendedColors(
    val isDark: Boolean,
    val canvas: Color,
    val cardSurface: Color,
    val cardElevated: Color,
    val borderSubtle: Color,
    val emerald: Color,
    val emeraldContainer: Color,
    val amber: Color,
    val amberContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val dangerBannerBg: Color,
    val cyan: Color,
    val cyanContainer: Color,
    val rose: Color,
    val roseContainer: Color,
    val infoBlue: Color,
    val mutedText: Color
)

private val DarkBunkerExtended = BunkerExtendedColors(
    isDark = true,
    canvas = BunkerCharcoal950,
    cardSurface = BunkerConcrete900,
    cardElevated = BunkerSteel850,
    borderSubtle = BunkerSteel800,
    emerald = GeigerGreen400,
    emeraldContainer = Color(0xFF132619),
    amber = RadAmber400,
    amberContainer = Color(0xFF2B2610),
    warning = CautionYellow400,
    warningContainer = Color(0xFF292712),
    danger = HazardRed400,
    dangerContainer = Color(0xFF2E1616),
    dangerBannerBg = Color(0xFF331515),
    cyan = CoolantCyan400,
    cyanContainer = Color(0xFF14242C),
    rose = MedRose400,
    roseContainer = Color(0xFF2D161E),
    infoBlue = SteelBlue400,
    mutedText = BunkerAsh400
)

private val LightBunkerExtended = BunkerExtendedColors(
    isDark = false,
    canvas = Color(0xFFE7EAE6),
    cardSurface = Color(0xFFF6F7F5),
    cardElevated = Color(0xFFECEFE9),
    borderSubtle = Color(0xFFC2C8C2),
    emerald = GeigerGreen700,
    emeraldContainer = Color(0xFFDCFCE7),
    amber = RadAmber700,
    amberContainer = Color(0xFFFEF9C3),
    warning = CautionYellow700,
    warningContainer = Color(0xFFFEF3C7),
    danger = HazardRed700,
    dangerContainer = Color(0xFFFEE2E2),
    dangerBannerBg = Color(0xFFFEE2E2),
    cyan = CoolantCyan700,
    cyanContainer = Color(0xFFE0F2FE),
    rose = MedRose700,
    roseContainer = Color(0xFFFFE4E6),
    infoBlue = Color(0xFF1D4ED8),
    mutedText = Color(0xFF525B54)
)

val LocalBunkerColors = staticCompositionLocalOf { DarkBunkerExtended }

private val BunkerDarkColorScheme = darkColorScheme(
    primary = GeigerGreen500,
    onPrimary = Color(0xFF071A0D),
    primaryContainer = Color(0xFF132E1C),
    onPrimaryContainer = Color(0xFF86EFAC),
    secondary = RadAmber500,
    onSecondary = Color(0xFF1C1604),
    secondaryContainer = Color(0xFF2E2710),
    onSecondaryContainer = Color(0xFFFEF08A),
    tertiary = CoolantCyan400,
    onTertiary = Color(0xFF08202B),
    background = BunkerCharcoal950,
    onBackground = BunkerAsh100,
    surface = BunkerConcrete900,
    onSurface = BunkerAsh100,
    surfaceVariant = BunkerSteel850,
    onSurfaceVariant = BunkerAsh300,
    outline = BunkerSteel700,
    outlineVariant = BunkerSteel800,
    error = HazardRed400,
    onError = Color(0xFF2B0707),
    errorContainer = Color(0xFF331515),
    onErrorContainer = Color(0xFFFECACA)
)

private val BunkerLightColorScheme = lightColorScheme(
    primary = GeigerGreen700,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCFCE7),
    onPrimaryContainer = Color(0xFF14532D),
    secondary = RadAmber700,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF9C3),
    onSecondaryContainer = Color(0xFF713F12),
    tertiary = CoolantCyan700,
    onTertiary = Color.White,
    background = Color(0xFFE7EAE6),
    onBackground = Color(0xFF141715),
    surface = Color(0xFFF6F7F5),
    onSurface = Color(0xFF141715),
    surfaceVariant = Color(0xFFDCE1DC),
    onSurfaceVariant = Color(0xFF374039),
    outline = Color(0xFF8C968E),
    outlineVariant = Color(0xFFC2C8C2),
    error = HazardRed700,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

val BunkerTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

@Composable
fun BunkerStockTheme(
    themeMode: AppThemeMode,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (useDark) BunkerDarkColorScheme else BunkerLightColorScheme
    val extended = if (useDark) DarkBunkerExtended else LightBunkerExtended

    CompositionLocalProvider(LocalBunkerColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = BunkerTypography,
            content = content
        )
    }
}
