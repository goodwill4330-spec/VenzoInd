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

// --- Material 3 Dark Color Scheme (Tricolor Inspired) ---
private val DarkTricolorColorScheme = darkColorScheme(
    primary = BharatSaffron,
    onPrimary = BharatWhite,
    primaryContainer = BharatSaffronDark,
    onPrimaryContainer = BharatWhite,
    inversePrimary = BharatSaffronLight,

    secondary = BharatGreenLight,
    onSecondary = BharatWhite,
    secondaryContainer = BharatGreenDark,
    onSecondaryContainer = BharatWhite,

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
    surfaceTint = BharatSaffron,

    error = RoseError,
    onError = BharatWhite,
    errorContainer = Color(0x33F43F5E),
    onErrorContainer = Color(0xFFFECDD3),

    outline = DarkBorder,
    outlineVariant = Color(0x3364748B),
    scrim = Color.Black
)

// --- Material 3 Light Color Scheme (WhatsApp Style) ---
private val LightTricolorColorScheme = lightColorScheme(
    primary = WhatsAppGreen,
    onPrimary = BharatWhite,
    primaryContainer = WhatsAppGreenPill,
    onPrimaryContainer = WhatsAppGreenDark,
    inversePrimary = WhatsAppGreenLight,

    secondary = WhatsAppGreenDark,
    onSecondary = BharatWhite,
    secondaryContainer = WhatsAppActivePill,
    onSecondaryContainer = WhatsAppGreenDeep,

    tertiary = WhatsAppGreenDark,
    onTertiary = BharatWhite,
    tertiaryContainer = WhatsAppGreenPill,
    onTertiaryContainer = WhatsAppGreenDark,

    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111B21),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111B21),
    surfaceVariant = Color(0xFFF0F2F5),
    onSurfaceVariant = Color(0xFF667781),
    surfaceTint = WhatsAppGreen,

    error = RoseError,
    onError = BharatWhite,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF9F1239),

    outline = Color(0xFFE9EDEF),
    outlineVariant = Color(0xFFE9EDEF),
    scrim = Color.Black
)

/**
 * Extended color tokens for custom glassmorphic styling,
 * sovereign gradients, and specialized chat states.
 */
@Immutable
data class BharatExtendedColors(
    val saffron: Color,
    val white: Color,
    val green: Color,
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
    val isDark: Boolean
)

val LocalBharatColors = staticCompositionLocalOf {
    BharatExtendedColors(
        saffron = BharatSaffron,
        white = BharatWhite,
        green = BharatGreenLight,
        navy = BharatNavy,
        cyan = BharatElectricCyan,
        glassBg = DarkSurfaceGlass,
        glassBorder = BharatGlassBorderDark,
        cardBg = DarkCardBg,
        textPrimary = TextPrimaryDark,
        textSecondary = TextSecondaryDark,
        textMuted = TextMutedDark,
        glassGradient = BharatDarkGlassGradient,
        tricolorGradient = BharatTricolorBrush,
        isDark = true
    )
}

/**
 * Custom MaterialTheme for Bharat Chat supporting both Light and Dark modes.
 *
 * Primary Color Palette:
 * - Saffron (Kesari): Primary brand, action items, highlights (#FF671F)
 * - White: High-contrast surfaces, readable text, glass frost layers (#FFFFFF)
 * - Green (Emerald): Secondary brand, success, UPI confirmations, online status (#046A38 / #10B981)
 * - Ashoka Navy & Electric Cyan: Tertiary brand, AI badges, encryption indicators (#06038D / #00F0FF)
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
            green = BharatGreenLight,
            navy = BharatNavy,
            cyan = BharatElectricCyan,
            glassBg = DarkSurfaceGlass,
            glassBorder = BharatGlassBorderDark,
            cardBg = DarkCardBg,
            textPrimary = TextPrimaryDark,
            textSecondary = TextSecondaryDark,
            textMuted = TextMutedDark,
            glassGradient = BharatDarkGlassGradient,
            tricolorGradient = BharatTricolorBrush,
            isDark = true
        )
    } else {
        BharatExtendedColors(
            saffron = WhatsAppGreen,
            white = BharatWhite,
            green = WhatsAppGreen,
            navy = WhatsAppGreenDark,
            cyan = WhatsAppGreenDark,
            glassBg = Color(0xFFFFFFFF),
            glassBorder = Color(0xFFE9EDEF),
            cardBg = Color(0xFFFFFFFF),
            textPrimary = Color(0xFF111B21),
            textSecondary = Color(0xFF667781),
            textMuted = Color(0xFF8696A0),
            glassGradient = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF7F8FA))),
            tricolorGradient = Brush.horizontalGradient(listOf(WhatsAppGreen, WhatsAppGreenLight, WhatsAppGreenDark)),
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

// Backwards compatibility for templates
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    BharatChatTheme(darkTheme = darkTheme, content = content)
}
