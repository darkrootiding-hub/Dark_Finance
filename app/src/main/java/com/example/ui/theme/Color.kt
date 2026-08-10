package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Premium Fintech Palette (Revolut / CRED / Monzo style)
val FintechPrimary = Color(0xFF6C63FF)
val FintechSecondary = Color(0xFF4F8CFF)
val FintechAccent = Color(0xFFFF7A45)
val FintechSuccess = Color(0xFF2ECC71)
val FintechError = Color(0xFFFF5A5F)
val FintechBackground = Color(0xFFF4F7FC)
val FintechSurface = Color(0xFFFFFFFF)
val FintechTextPrimary = Color(0xFF111827)
val FintechTextSecondary = Color(0xFF6B7280)
val FintechTextTertiary = Color(0xFF9CA3AF)

// Glassmorphism tokens
val GlassWhite = Color(0xFFFFFFFF).copy(alpha = 0.25f)
val GlassWhiteLight = Color(0xFFFFFFFF).copy(alpha = 0.45f)
val GlassWhiteHeavy = Color(0xFFFFFFFF).copy(alpha = 0.75f)
val GlassBorder = Color(0xFFFFFFFF).copy(alpha = 0.65f)
val GlassBorderSubtle = Color(0xFFFFFFFF).copy(alpha = 0.35f)
val GlassShadow = Color(0xFF3B82F6).copy(alpha = 0.08f)

// Gradient & Glow accents
val GlowPurple = Color(0xFF6C63FF).copy(alpha = 0.25f)
val GlowBlue = Color(0xFF4F8CFF).copy(alpha = 0.25f)
val GlowOrange = Color(0xFFFF7A45).copy(alpha = 0.25f)
val PremiumDarkCard = Color(0xFF1E1B4B)
val PremiumGold = Color(0xFFFFB300)

// Backwards compatibility aliases
val PrimaryOrange = FintechAccent
val PrimaryPurple = FintechPrimary
val DarkCardBg = PremiumDarkCard
val LightBackground = FintechBackground
val SurfaceWhite = FintechSurface
val TextPrimary = FintechTextPrimary
val TextSecondary = FintechTextSecondary
val SuccessGreen = FintechSuccess
val ErrorRed = FintechError
val ChartGrey = Color(0xFF374151)
val LightGreyBox = Color(0xFFEFF6FF)
