package com.carcassonne.companion.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Color Palette ───────────────────────────────────────────────────────────
val CarcGreen      = Color(0xFF22C55E)
val CarcGreenDark  = Color(0xFF16A34A)
val CarcGreenDeep  = Color(0xFF0F7A35)
val CarcBg         = Color(0xFF071007)
val CarcBg2        = Color(0xFF0D1F0D)
val CarcBg3        = Color(0xFF112011)
val CarcCard       = Color(0xFF122012)
val CarcCard2      = Color(0xFF1A2E1A)
val CarcBorder     = Color(0xFF1E3A1E)
val CarcText       = Color(0xFFF0FFF0)
val CarcText2      = Color(0xFF9CB89C)
val CarcText3      = Color(0xFF5A7A5A)
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

private val DarkColorScheme = darkColorScheme(
    primary          = CarcGreen,
    onPrimary        = CarcBg,
    primaryContainer = CarcGreenDeep,
    secondary        = CarcGreenDark,
    background       = CarcBg,
    surface          = CarcCard,
    onBackground     = CarcText,
    onSurface        = CarcText,
    outline          = CarcBorder,
    error            = CarcRed
)

@Composable
fun CarcassonneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography(),
        content     = content
    )
}
