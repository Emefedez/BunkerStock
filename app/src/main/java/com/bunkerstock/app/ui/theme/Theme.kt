package com.bunkerstock.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bunkerstock.app.data.AppThemeMode

// Core Bunker Palette
val Slate950 = Color(0xFF090D16)
val Slate900 = Color(0xFF0F172A)
val Slate850 = Color(0xFF161F30)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val Slate500 = Color(0xFF64748B)
val Slate400 = Color(0xFF94A3B8)
val Slate300 = Color(0xFFCBD5E1)
val Slate100 = Color(0xFFF1F5F9)

val Emerald500 = Color(0xFF10B981)
val Emerald400 = Color(0xFF34D399)
val Emerald700 = Color(0xFF047857)

val Amber500 = Color(0xFFF59E0B)
val Amber400 = Color(0xFFFBBF24)
val Amber700 = Color(0xFFB45309)

val Yellow400 = Color(0xFFFACC15)
val Yellow700 = Color(0xFFA16207)

val Red500 = Color(0xFFEF4444)
val Red400 = Color(0xFFF87171)
val Red700 = Color(0xFFB91C1C)

val Cyan400 = Color(0xFF22D3EE)
val Cyan700 = Color(0xFF0E7490)

val Rose400 = Color(0xFFFB7185)
val Rose700 = Color(0xFFBE123C)

val Blue400 = Color(0xFF60A5FA)

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
    canvas = Slate950,
    cardSurface = Slate900,
    cardElevated = Slate850,
    borderSubtle = Slate800,
    emerald = Emerald400,
    emeraldContainer = Color(0xFF0B2B23),
    amber = Amber400,
    amberContainer = Color(0xFF2E220D),
    warning = Yellow400,
    warningContainer = Color(0xFF2B260F),
    danger = Red400,
    dangerContainer = Color(0xFF331418),
    dangerBannerBg = Color(0xFF3B1214),
    cyan = Cyan400,
    cyanContainer = Color(0xFF0C2632),
    rose = Rose400,
    roseContainer = Color(0xFF331420),
    infoBlue = Blue400,
    mutedText = Slate400
)

private val LightBunkerExtended = BunkerExtendedColors(
    isDark = false,
    canvas = Color(0xFFF1F5F9),
    cardSurface = Color(0xFFFFFFFF),
    cardElevated = Color(0xFFF8FAFC),
    borderSubtle = Color(0xFFCBD5E1),
    emerald = Emerald700,
    emeraldContainer = Color(0xFFD1FAE5),
    amber = Amber700,
    amberContainer = Color(0xFFFEF3C7),
    warning = Yellow700,
    warningContainer = Color(0xFFFEF9C3),
    danger = Red700,
    dangerContainer = Color(0xFFFEE2E2),
    dangerBannerBg = Color(0xFFFEE2E2),
    cyan = Cyan700,
    cyanContainer = Color(0xFFCFFAFE),
    rose = Rose700,
    roseContainer = Color(0xFFFFE4E6),
    infoBlue = Color(0xFF1D4ED8),
    mutedText = Color(0xFF475569)
)

val LocalBunkerColors = staticCompositionLocalOf { DarkBunkerExtended }

private val BunkerDarkColorScheme = darkColorScheme(
    primary = Emerald500,
    onPrimary = Color(0xFF032117),
    primaryContainer = Color(0xFF0B3529),
    onPrimaryContainer = Color(0xFF6EE7B7),
    secondary = Amber500,
    onSecondary = Color(0xFF1F1402),
    secondaryContainer = Color(0xFF38270A),
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = Cyan400,
    onTertiary = Color(0xFF04222A),
    background = Slate950,
    onBackground = Slate100,
    surface = Slate900,
    onSurface = Slate100,
    surfaceVariant = Slate850,
    onSurfaceVariant = Slate300,
    outline = Slate700,
    outlineVariant = Slate800,
    error = Red400,
    onError = Color(0xFF2D0606),
    errorContainer = Color(0xFF3B1214),
    onErrorContainer = Color(0xFFFECACA)
)

private val BunkerLightColorScheme = lightColorScheme(
    primary = Emerald700,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Amber700,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Cyan700,
    onTertiary = Color.White,
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1),
    error = Red700,
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
