package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// --- Dynamic Bharat Tricolor & Futuristic Cyber Palette ---
// WhatsApp / Modern Emerald Theme Palette
val WhatsAppGreen = Color(0xFF25D366)
val WhatsAppGreenDark = Color(0xFF128C7E)
val WhatsAppGreenDeep = Color(0xFF075E54)
val WhatsAppGreenLight = Color(0xFF20C65A)
val WhatsAppGreenPill = Color(0xFFE8F5E9)
val WhatsAppIconMuted = Color(0xFF536471)
val WhatsAppActivePill = Color(0xFFD8FDD2)

// Saffron / Accent Highlights
val BharatSaffron = Color(0xFF25D366)
val BharatSaffronLight = Color(0xFF20C65A)
val BharatSaffronDark = Color(0xFF128C7E)
val BharatSaffronGlow = Color(0xFF25D366)

// Pure White & Clean Surfaces
val BharatWhite = Color(0xFFFFFFFF)
val BharatFrost = Color(0x1F000000)
val BharatFrostLight = Color(0x33000000)
val BharatGlassBorder = Color(0x1F000000)
val BharatGlassBorderDark = Color(0x2238BDF8)

// Emerald Green - Growth, Prosperity, Harmony
val BharatGreen = Color(0xFF25D366)
val BharatGreenLight = Color(0xFF25D366)
val BharatGreenDark = Color(0xFF128C7E)
val BharatGreenGlow = Color(0xFF25D366)

// Ashoka Navy & Cyber Space
val BharatNavy = Color(0xFF06038D)
val BharatNavyLight = Color(0xFF1E3A8A)
val BharatCyberBlue = Color(0xFF0284C7)
val BharatElectricCyan = Color(0xFF00F0FF)

// Dark Theme Surfaces (Deep OLED Space & Midnight Cyber)
val DarkBackground = Color(0xFF070B14)
val DarkSurface = Color(0xFF0D1527)
val DarkSurfaceElevated = Color(0xFF131F37)
val DarkSurfaceGlass = Color(0xE60D1527)
val DarkCardBg = Color(0xFF111C33)
val DarkBorder = Color(0x2638BDF8)

// Light Theme Surfaces (Clean Frost & Pearl)
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFF1F5F9)
val LightSurfaceGlass = Color(0xF2FFFFFF)
val LightCardBg = Color(0xFFFFFFFF)
val LightBorder = Color(0x1A0F172A)

// Text Colors
val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFF94A3B8)
val TextMutedDark = Color(0xFF64748B)

val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)
val TextMutedLight = Color(0xFF94A3B8)

// Accent & Status Colors
val GoldAccent = Color(0xFFFFD700)
val NeonPurple = Color(0xFFA855F7)
val RoseError = Color(0xFFF43F5E)
val OnlineGreen = Color(0xFF10B981)
val SecretChatPink = Color(0xFFEC4899)
val UpiPurple = Color(0xFF5F259F)

// Gradient Brushes
val BharatTricolorBrush = Brush.horizontalGradient(
    colors = listOf(BharatSaffron, BharatWhite, BharatGreen)
)

val BharatSaffronGradient = Brush.linearGradient(
    colors = listOf(BharatSaffron, BharatSaffronLight)
)

val BharatGreenGradient = Brush.linearGradient(
    colors = listOf(BharatGreenLight, BharatGreen)
)

val BharatCyberGradient = Brush.linearGradient(
    colors = listOf(BharatSaffron, BharatElectricCyan, BharatGreenLight)
)

val BharatDarkGlassGradient = Brush.verticalGradient(
    colors = listOf(Color(0xD9131F37), Color(0xB30D1527))
)

val BharatLightGlassGradient = Brush.verticalGradient(
    colors = listOf(Color(0xF2FFFFFF), Color(0xDCF1F5F9))
)

val UpiGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFD946EF))
)
