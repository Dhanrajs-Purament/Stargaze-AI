package com.stargaze.ai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val OnAccentInk = Color(0xFF0A0C1A)

/** Extended typography scale with display and overline styles. */
val StarGazeTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.2).sp,
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.15).sp,
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)

private val StarGazeColorScheme = darkColorScheme(
    primary = StarColors.Accent,
    secondary = StarColors.Accent2,
    tertiary = StarColors.Gold,
    background = StarColors.Bg,
    surface = StarColors.Bg2,
    onPrimary = OnAccentInk,
    onBackground = StarColors.Ink,
    onSurface = StarColors.Ink,
    onSurfaceVariant = StarColors.Muted,
    outline = StarColors.Line,
    error = StarColors.Red,
)

private val HighContrastColorScheme = darkColorScheme(
    primary = Color(0xFF78D9FF),
    secondary = Color(0xFFB48CFF),
    tertiary = Color(0xFFFFE082),
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    onPrimary = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFFCCCCCC),
    outline = Color(0xFFFFFFFF),
    error = Color(0xFFFF4D4D),
)

@Composable
fun StarGazeTheme(highContrast: Boolean = false, content: @Composable () -> Unit) {
    // The app is intentionally always dark (it is a stargazing tool); we reference the system
    // setting so a future light-mode has a hook, but always apply the dark scheme.
    @Suppress("UNUSED_VARIABLE")
    val systemDark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (highContrast) HighContrastColorScheme else StarGazeColorScheme,
        typography = StarGazeTypography,
        content = content,
    )
}
