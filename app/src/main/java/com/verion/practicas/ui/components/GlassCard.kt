package com.verion.practicas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.verion.practicas.ui.theme.BrandBlue
import com.verion.practicas.ui.theme.BrandIndigo
import com.verion.practicas.ui.theme.BrandPurple
import com.verion.practicas.ui.theme.GlassBg
import com.verion.practicas.ui.theme.GlassBorder

private val gradientBorder = Brush.linearGradient(
    listOf(BrandBlue, BrandPurple, BrandIndigo)
)

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(GlassBg)
            .border(1.dp, GlassBorder, shape),
        content = content
    )
}

@Composable
fun GlassCardPadded(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    innerPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    GlassCard(modifier = modifier, cornerRadius = cornerRadius) {
        Box(modifier = Modifier.padding(innerPadding), content = content)
    }
}

@Composable
fun GlassCardGradient(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(GlassBg)
            .border(1.dp, gradientBorder, shape),
        content = content
    )
}

@Composable
fun GlassCardGradientPadded(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    innerPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    GlassCardGradient(modifier = modifier, cornerRadius = cornerRadius) {
        Box(modifier = Modifier.padding(innerPadding), content = content)
    }
}
