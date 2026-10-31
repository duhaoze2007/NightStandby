package com.duhaoze.standby.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class StandbyColors(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val line: Color,
    val lineStrong: Color,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val accent: Color,
    val accentSoft: Color,
    val red: Color,
    val isRed: Boolean
)

val NightColors = StandbyColors(
    background = Color.Black,
    surface = Color(0xFF060606),
    surfaceRaised = Color(0xFF0C0C0D),
    line = Color(0xFF1C1C1E),
    lineStrong = Color(0xFF303033),
    primary = Color(0xFFF3F0E8),
    secondary = Color(0xFF9B968D),
    tertiary = Color(0xFF5E5A54),
    accent = Color(0xFFE6B568),
    accentSoft = Color(0xFF2B2113),
    red = Color(0xFFFF5A4E),
    isRed = false
)

val RedColors = StandbyColors(
    background = Color.Black,
    surface = Color(0xFF050000),
    surfaceRaised = Color(0xFF0A0000),
    line = Color(0xFF2B0B08),
    lineStrong = Color(0xFF48130E),
    primary = Color(0xFFFF5A4E),
    secondary = Color(0xFFB33A31),
    tertiary = Color(0xFF6E201B),
    accent = Color(0xFFFF3B30),
    accentSoft = Color(0xFF260200),
    red = Color(0xFFFF3B30),
    isRed = true
)

val LocalStandbyColors = staticCompositionLocalOf { NightColors }

@Composable
fun StandbyTheme(redMode: Boolean, content: @Composable () -> Unit) {
    val colors = if (redMode) RedColors else NightColors
    CompositionLocalProvider(LocalStandbyColors provides colors) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = colors.accent,
                onPrimary = Color.Black,
                secondary = colors.primary,
                onSecondary = Color.Black,
                background = colors.background,
                onBackground = colors.primary,
                surface = colors.surface,
                onSurface = colors.primary,
                surfaceVariant = colors.surfaceRaised,
                onSurfaceVariant = colors.secondary,
                outline = colors.lineStrong,
                error = colors.red,
                onError = Color.Black
            ),
            content = content
        )
    }
}

object StandbyType {
    const val MONO_TABULAR = "tnum"
}
