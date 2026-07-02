package com.stargaze.ai.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * StarGaze AI palette, ported from the validated prototype's CSS custom properties so the native
 * app matches the approved visual language exactly.
 *
 * Enhanced with additional translucent, glow, and surface variants for modern glassmorphism and
 * depth effects used across the polished UI.
 */
object StarColors {
    // ── Core palette ──
    val Bg = Color(0xFF05060F)
    val Bg2 = Color(0xFF0A0E1F)
    val Ink = Color(0xFFEEF1FF)
    val Muted = Color(0xFF9AA3C7)
    val Faint = Color(0xFF5B6488)

    // ── Borders & Surfaces ──
    val Line = Color(0x14FFFFFF)        // rgba(255,255,255,.08)
    val Card = Color(0x0DFFFFFF)        // rgba(255,255,255,.05)
    val Card2 = Color(0x14FFFFFF)       // rgba(255,255,255,.08)
    val CardHover = Color(0x1AFFFFFF)   // rgba(255,255,255,.10)

    // ── Accents ──
    val Accent = Color(0xFF7C9BFF)
    val Accent2 = Color(0xFFB388FF)
    val Gold = Color(0xFFFFCF6B)
    val Red = Color(0xFFFF7A7A)
    val Green = Color(0xFF7CE6B0)
    val Pink = Color(0xFFFF9AD5)

    // Night (red) mode primary tint
    val NightRed = Color(0xFFFF6B6B)

    // ── Glow & Light colors for effects ──
    val AccentGlow = Color(0x337C9BFF)
    val GoldGlow = Color(0x33FFCF6B)
    val GreenGlow = Color(0x337CE6B0)

    // ── Transparent variants for layering ──
    val TransparentInk = Ink.copy(alpha = 0.08f)
    val TransparentAccent = Accent.copy(alpha = 0.10f)

    // ── Brushes ──
    val BrandGradient = Brush.linearGradient(
        colors = listOf(Accent, Accent2, Pink),
    )

    /** Radial background gradient approximating the prototype's deep-space backdrop. */
    val SkyBackdrop = Brush.verticalGradient(
        colors = listOf(Color(0xFF141A35), Color(0xFF080B18), Color(0xFF04050C)),
    )

    /** Glass-card surface that works on dark bg without heavy blur. */
    val GlassSurface = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF11152D).copy(alpha = 0.95f),
            Color(0xFF0A0D1D).copy(alpha = 0.90f),
        ),
    )
}
