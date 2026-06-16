package com.verion.practicas.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
    primary                  = BrandBlue,
    onPrimary                = Color(0xFF001E3D),
    primaryContainer         = PrimaryContainer,
    onPrimaryContainer       = OnPrimaryContainer,
    secondary                = BrandPurple,
    onSecondary              = Color(0xFF1A0061),
    secondaryContainer       = SecondaryContainer,
    onSecondaryContainer     = OnSecondaryContainer,
    tertiary                 = BrandIndigo,
    onTertiary               = Color(0xFF001B3F),
    tertiaryContainer        = TertiaryContainer,
    onTertiaryContainer      = OnTertiaryContainer,
    background               = BgDeep,
    onBackground             = TextPrimary,
    surface                  = SurfaceContainer,
    onSurface                = TextPrimary,
    surfaceVariant           = SurfaceContainerHigh,
    onSurfaceVariant         = TextSecondary,
    surfaceContainer         = SurfaceContainer,
    surfaceContainerLow      = SurfaceContainerLow,
    surfaceContainerHigh     = SurfaceContainerHigh,
    surfaceContainerHighest  = SurfaceContainerHighest,
    surfaceContainerLowest   = SurfaceContainerLowest,
    outline                  = GlassBorder,
    outlineVariant           = Color(0xFF1E2050),
    error                    = ErrorColor,
    onError                  = Color(0xFF7F1D1D),
    errorContainer           = ErrorContainer,
    onErrorContainer         = Color(0xFFFFDAD6),
    scrim                    = Color(0xFF000000),
    inverseSurface           = TextPrimary,
    inverseOnSurface         = BgDeep,
    inversePrimary           = Color(0xFF1A5FBF),
)

private val VeriOnShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small      = RoundedCornerShape(10.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun VeriOnTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography  = VeriOnTypography,
        shapes      = VeriOnShapes,
        content     = content
    )
}
