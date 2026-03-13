package com.carcassonne.companion.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

// ─── Fixed accent colors (same in both themes) ───────────────────────────────
val CarcGreen      = Color(0xFF22C55E)
val CarcGreenDark  = Color(0xFF16A34A)
val CarcGreenDeep  = Color(0xFF0F7A35)
val CarcRed        = Color(0xFFEF4444)
val CarcYellow     = Color(0xFFEAB308)
val CarcBlue       = Color(0xFF3B82F6)
val CarcOrange     = Color(0xFFF97316)

// Meeple colors
val MeepleRed    = Color(0xFFEF4444)
val MeepleBlue   = Color(0xFF3B82F6)
val MeepleGreen  = Color(0xFF22C55E)
val MeepleYellow = Color(0xFFEAB308)
val MeepleBlack  = Color(0xFF6B7280)
val MeepleGray   = Color(0xFF9CA3AF)

fun meepleColor(name: String): Color = when (name) {
    "red"    -> MeepleRed
    "blue"   -> MeepleBlue
    "green"  -> MeepleGreen
    "yellow" -> MeepleYellow
    "black"  -> MeepleBlack
    "gray"   -> MeepleGray
    else     -> MeepleGreen
}

// ─── Theme color set ─────────────────────────────────────────────────────────
data class CarcColors(
    val bg: Color,
    val bg2: Color,
    val bg3: Color,
    val card: Color,
    val card2: Color,
    val border: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val isDark: Boolean
)

val DarkCarcColors = CarcColors(
    bg      = Color(0xFF071007),
    bg2     = Color(0xFF0D1F0D),
    bg3     = Color(0xFF112011),
    card    = Color(0xFF122012),
    card2   = Color(0xFF1A2E1A),
    border  = Color(0xFF1E3A1E),
    text    = Color(0xFFF0FFF0),
    text2   = Color(0xFF9CB89C),
    text3   = Color(0xFF5A7A5A),
    isDark  = true
)

val LightCarcColors = CarcColors(
    bg      = Color(0xFFF5FBF5),
    bg2     = Color(0xFFEAF5EA),
    bg3     = Color(0xFFDDEEDD),
    card    = Color(0xFFFFFFFF),
    card2   = Color(0xFFF0F8F0),
    border  = Color(0xFFBDD8BD),
    text    = Color(0xFF0D1F0D),
    text2   = Color(0xFF3A5C3A),
    text3   = Color(0xFF6B8F6B),
    isDark  = false
)

// ─── CompositionLocal ─────────────────────────────────────────────────────────
val LocalCarcColors = staticCompositionLocalOf { DarkCarcColors }

// ─── Convenience @Composable accessors ────────────────────────────────────────
// Используются везде вместо старых val — автоматически меняются с темой
val CarcBg     @Composable get() = LocalCarcColors.current.bg
val CarcBg2    @Composable get() = LocalCarcColors.current.bg2
val CarcBg3    @Composable get() = LocalCarcColors.current.bg3
val CarcCard   @Composable get() = LocalCarcColors.current.card
val CarcCard2  @Composable get() = LocalCarcColors.current.card2
val CarcBorder @Composable get() = LocalCarcColors.current.border
val CarcText   @Composable get() = LocalCarcColors.current.text
val CarcText2  @Composable get() = LocalCarcColors.current.text2
val CarcText3  @Composable get() = LocalCarcColors.current.text3

// ─── Material color schemes ───────────────────────────────────────────────────
private fun darkScheme() = darkColorScheme(
    primary          = CarcGreen,
    onPrimary        = Color(0xFF071007),
    primaryContainer = CarcGreenDeep,
    secondary        = CarcGreenDark,
    background       = Color(0xFF071007),
    surface          = Color(0xFF122012),
    onBackground     = Color(0xFFF0FFF0),
    onSurface        = Color(0xFFF0FFF0),
    outline          = Color(0xFF1E3A1E),
    error            = CarcRed
)

private fun lightScheme() = lightColorScheme(
    primary          = CarcGreenDark,
    onPrimary        = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDEEDD),
    secondary        = CarcGreen,
    background       = Color(0xFFF5FBF5),
    surface          = Color(0xFFFFFFFF),
    onBackground     = Color(0xFF0D1F0D),
    onSurface        = Color(0xFF0D1F0D),
    outline          = Color(0xFFBDD8BD),
    error            = CarcRed
)

// ─── Theme entry point ────────────────────────────────────────────────────────
@Composable
fun CarcassonneTheme(
    darkMode: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkMode) DarkCarcColors else LightCarcColors
    val scheme = if (darkMode) darkScheme() else lightScheme()
    CompositionLocalProvider(LocalCarcColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography  = Typography(),
            content     = content
        )
    }
}
