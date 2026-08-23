package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// =======================================================================
// MATERIAL 3 COLOR SCHEMES: CYBER-GREEN & TRICOLOR GLASSMORPHISM
// =======================================================================

// --- Dark Mode: Cyber-Green Neon, Saffron Accent & Deep Navy OLED Glass ---
private val DarkTricolorColorScheme = darkColorScheme(
    primary = CyberGreenNeon,
    onPrimary = Color(0xFF022613),
    primaryContainer = CyberGreenDark,
    onPrimaryContainer = CyberGreenLight,
    inversePrimary = CyberGreenPrimary,

    secondary = BharatSaffron,
    onSecondary = BharatWhite,
    secondaryContainer = BharatSaffronDark,
    onSecondaryContainer = BharatSaffronLight,

    tertiary = BharatElectricCyan,
    onTertiary = DarkBackground,
    tertiaryContainer = BharatNavyLight,
    onTertiaryContainer = BharatWhite,

    background = DarkBackground,
    onBackground = TextPrimaryDark,

    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    surfaceTint = CyberGreenNeon,

    error = RoseError,
    onError = BharatWhite,
    errorContainer = Color(0x33FF3366),
    onErrorContainer = Color(0xFFFF8DA9),

    outline = DarkBorder,
    outlineVariant = Color(0x2600FF87),
    scrim = Color.Black
)

// --- Light Mode: Crisp White Frost, Cyber Emerald & Subtle Saffron Accents ---
private val LightTricolorColorScheme = lightColorScheme(
    primary = CyberGreenPrimary,
    onPrimary = BharatWhite,
    primaryContainer = CyberGreenPill,
    onPrimaryContainer = CyberGreenDark,
    inversePrimary = CyberGreenLight,

    secondary = BharatSaffron,
    onSecondary = BharatWhite,
    secondaryContainer = Color(0x26FF7A00),
    onSecondaryContainer = BharatSaffronDark,

    tertiary = BharatGreenDark,
    onTertiary = BharatWhite,
    tertiaryContainer = Color(0x2610E078),
    onTertiaryContainer = BharatGreenDark,

    background = LightBackground,
    onBackground = TextPrimaryLight,

    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    surfaceTint = CyberGreenPrimary,

    error = RoseError,
    onError = BharatWhite,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF9F1239),

    outline = LightBorder,
    outlineVariant = Color(0x1F000000),
    scrim = Color.Black
)

/**
 * Extended color tokens for specialized glassmorphic overlays,
 * glowing hairline borders, sovereign tricolor brushes, and cyber elements.
 */
@Immutable
data class BharatExtendedColors(
    val saffron: Color,
    val white: Color,
    val green: Color,
    val cyberNeon: Color,
    val navy: Color,
    val cyan: Color,
    val glassBg: Color,
    val glassBorder: Color,
    val cardBg: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val glassGradient: Brush,
    val tricolorGradient: Brush,
    val cyberGreenGradient: Brush,
    val isDark: Boolean
)

val LocalBharatColors = staticCompositionLocalOf {
    BharatExtendedColors(
        saffron = BharatSaffron,
        white = BharatWhite,
        green = BharatGreenLight,
        cyberNeon = CyberGreenNeon,
        navy = BharatNavy,
        cyan = BharatElectricCyan,
        glassBg = DarkSurfaceGlass,
        glassBorder = DarkBorder,
        cardBg = DarkCardBg,
        textPrimary = TextPrimaryDark,
        textSecondary = TextSecondaryDark,
        textMuted = TextMutedDark,
        glassGradient = CyberGlassDarkGradient,
        tricolorGradient = BharatTricolorBrush,
        cyberGreenGradient = CyberGreenGradient,
        isDark = true
    )
}

/**
 * BharatChatTheme applies both the customized MaterialTheme and
 * the custom BharatExtendedColors composition local.
 */
@Composable
fun BharatChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkTricolorColorScheme else LightTricolorColorScheme

    val extendedColors = if (darkTheme) {
        BharatExtendedColors(
            saffron = BharatSaffron,
            white = BharatWhite,
            green = CyberGreenPrimary,
            cyberNeon = CyberGreenNeon,
            navy = BharatNavy,
            cyan = BharatElectricCyan,
            glassBg = DarkSurfaceGlass,
            glassBorder = DarkBorder,
            cardBg = DarkCardBg,
            textPrimary = TextPrimaryDark,
            textSecondary = TextSecondaryDark,
            textMuted = TextMutedDark,
            glassGradient = CyberGlassDarkGradient,
            tricolorGradient = BharatTricolorBrush,
            cyberGreenGradient = CyberGreenGradient,
            isDark = true
        )
    } else {
        BharatExtendedColors(
            saffron = BharatSaffron,
            white = BharatWhite,
            green = CyberGreenPrimary,
            cyberNeon = CyberGreenNeon,
            navy = BharatNavyLight,
            cyan = BharatElectricCyan,
            glassBg = LightSurfaceGlass,
            glassBorder = LightBorder,
            cardBg = LightCardBg,
            textPrimary = TextPrimaryLight,
            textSecondary = TextSecondaryLight,
            textMuted = TextMutedLight,
            glassGradient = CyberGlassLightGradient,
            tricolorGradient = BharatTricolorBrush,
            cyberGreenGradient = CyberGreenGradient,
            isDark = false
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = colorScheme.background.toArgb()
                it.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(it, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    CompositionLocalProvider(LocalBharatColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Backwards compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    BharatChatTheme(darkTheme = darkTheme, content = content)
}
