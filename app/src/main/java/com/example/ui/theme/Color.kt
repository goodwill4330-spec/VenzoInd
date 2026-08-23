package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// =======================================================================
// BHARAT CHAT: CYBER-GREEN & TRICOLOR COLOR PALETTE
// =======================================================================

// --- Cyber-Green Core Tokens ---
val CyberGreenNeon = Color(0xFF00FF87)         // Ultra vibrant neon cyber-green
val CyberGreenPrimary = Color(0xFF10E078)      // Primary glowing emerald
val CyberGreenLight = Color(0xFF4EFA9D)        // Lighter cyber-green tint
val CyberGreenDark = Color(0xFF078A4A)         // Deep saturated cyber-green
val CyberGreenDeep = Color(0xFF034A26)         // Ambient dark cyber-green
val CyberGreenGlow = Color(0x6600FF87)         // Radial / halo glow
val CyberGreenPill = Color(0x2E10E078)         // Glass pill active background

// --- Authentic Tricolor Tokens ---
val BharatSaffron = Color(0xFFFF7A00)          // Radiant Saffron / Kesari
val BharatSaffronLight = Color(0xFFFF9E40)     // Golden Saffron tint
val BharatSaffronDark = Color(0xFFCC5A00)      // Deep amber saffron
val BharatSaffronGlow = Color(0x66FF7A00)      // Saffron halo / highlight

val BharatWhite = Color(0xFFFFFFFF)            // Pure high-contrast frost white
val BharatWhiteTranslucent = Color(0xCCFFFFFF) // Frosted white overlay

val BharatGreen = Color(0xFF0FA958)            // India Green / Vibrant emerald
val BharatGreenLight = Color(0xFF22C55E)       // Bright emerald highlight
val BharatGreenDark = Color(0xFF065F38)        // Deep sovereign green
val BharatGreenGlow = Color(0x6622C55E)        // Emerald accent glow

val BharatNavy = Color(0xFF0A1128)             // Deep Ashoka Chakra Navy
val BharatNavyLight = Color(0xFF1C2C5E)        // Navy card surface
val BharatNavyDeep = Color(0xFF060919)         // Ultra dark OLED navy background

val BharatElectricCyan = Color(0xFF00E5FF)     // Ashoka Chakra electric cyan accent
val BharatCyanGlow = Color(0x6600E5FF)         // Electric cyan glow

// --- Glassmorphic Surface & Border Tokens ---
// Dark Glass Surfaces (OLED Deep Cyber + Glass Frost)
val DarkBackground = Color(0xFF060913)
val DarkSurface = Color(0xCC0D1527)            // Frosted translucent surface
val DarkSurfaceElevated = Color(0xE6131F3B)
val DarkSurfaceGlass = Color(0x80101C38)       // Translucent glass backdrop
val DarkCardBg = Color(0x99111E38)            // Glassmorphic card surface
val DarkBorder = Color(0x3300FF87)             // Cyber-green glass hairline border
val DarkBorderTricolor = Color(0x33FF7A00)     // Saffron glass hairline border
val DarkBorderCyan = Color(0x3300E5FF)         // Cyan glass hairline border
val DarkGlassBorder = Color(0x2600FF87)

// Light Glass Surfaces (Crisp White Frost + Cyber Accent)
val LightBackground = Color(0xFFF4F8F6)
val LightSurface = Color(0xE6FFFFFF)
val LightSurfaceElevated = Color(0xF2EDF5F0)
val LightSurfaceGlass = Color(0xB3FFFFFF)
val LightCardBg = Color(0xD9FFFFFF)
val LightBorder = Color(0x1F10E078)
val LightGlassBorder = Color(0x260FA958)

// --- Text Tokens ---
val TextPrimaryDark = Color(0xFFF1F5F9)
val TextSecondaryDark = Color(0xFF94A3B8)
val TextMutedDark = Color(0xFF64748B)

val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)
val TextMutedLight = Color(0xFF94A3B8)

// --- Functional & Status Tokens ---
val GoldAccent = Color(0xFFFFD700)
val NeonPurple = Color(0xFFA855F7)
val RoseError = Color(0xFFFF3366)
val OnlineGreen = Color(0xFF00FF87)
val SecretChatPink = Color(0xFFFF2E93)
val UpiPurple = Color(0xFF7C3AED)

// --- Legacy WhatsApp Compatibility Tokens ---
val WhatsAppGreen = CyberGreenPrimary
val WhatsAppGreenDark = CyberGreenDark
val WhatsAppGreenDeep = CyberGreenDeep
val WhatsAppGreenLight = CyberGreenLight
val WhatsAppGreenPill = CyberGreenPill
val WhatsAppIconMuted = Color(0xFF8696A0)
val WhatsAppActivePill = Color(0x3300FF87)
val BharatFrost = Color(0x1A00FF87)
val BharatFrostLight = Color(0x2600FF87)
val BharatGlassBorderDark = DarkBorder

// --- Sovereign & Cyber Gradients ---
val BharatTricolorBrush = Brush.horizontalGradient(
    colors = listOf(BharatSaffron, BharatWhite, CyberGreenNeon)
)

val CyberGreenGradient = Brush.linearGradient(
    colors = listOf(CyberGreenNeon, CyberGreenPrimary, CyberGreenDark)
)

val CyberGlassDarkGradient = Brush.verticalGradient(
    colors = listOf(Color(0xCC131F3B), Color(0x990A1128))
)

val CyberGlassLightGradient = Brush.verticalGradient(
    colors = listOf(Color(0xE6FFFFFF), Color(0xB3E8F5EE))
)

val BharatSaffronGradient = Brush.linearGradient(
    colors = listOf(BharatSaffron, BharatSaffronLight)
)

val BharatGreenGradient = Brush.linearGradient(
    colors = listOf(CyberGreenNeon, BharatGreenLight)
)

val BharatCyberGradient = Brush.linearGradient(
    colors = listOf(BharatSaffron, BharatElectricCyan, CyberGreenNeon)
)

val BharatDarkGlassGradient = CyberGlassDarkGradient
val BharatLightGlassGradient = CyberGlassLightGradient

val UpiGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFD946EF))
)
