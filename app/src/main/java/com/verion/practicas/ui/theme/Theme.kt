package com.verion.practicas.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary          = BrandBlue,
    onPrimary        = Color.White,
    secondary        = BrandPurple,
    onSecondary      = Color.White,
    background       = BgDeep,
    onBackground     = TextPrimary,
    surface          = BgMid,
    onSurface        = TextPrimary,
    surfaceVariant   = Color(0xFF1E1B4B),
    onSurfaceVariant = TextSecondary,
    outline          = GlassBorder,
    error            = Color(0xFFF87171),
)

@Composable
fun VeriOnTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
