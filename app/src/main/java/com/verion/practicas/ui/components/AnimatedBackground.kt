package com.verion.practicas.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.verion.practicas.ui.theme.*

@Composable
fun AnimatedBackground(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "bg")

    val t1 by inf.animateFloat(0f, 1f,
        infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), "t1")
    val t2 by inf.animateFloat(0f, 1f,
        infiniteRepeatable(tween(13000, easing = LinearEasing), RepeatMode.Reverse), "t2")
    val t3 by inf.animateFloat(1f, 0f,
        infiniteRepeatable(tween(7500, easing = LinearEasing), RepeatMode.Reverse), "t3")
    val t4 by inf.animateFloat(0f, 1f,
        infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Reverse), "t4")
    val t5 by inf.animateFloat(0f, 1f,
        infiniteRepeatable(tween(11000, easing = LinearEasing), RepeatMode.Reverse), "t5")

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(BgDeep, BgMid, BgDark)
            )
        )

        val d = size.minDimension

        val c1 = Offset(size.width * 0.22f, size.height * (0.08f + 0.38f * t1))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(OrbPurple.copy(alpha = 0.55f), Color.Transparent),
                center = c1, radius = d * 0.58f
            ),
            radius = d * 0.58f, center = c1
        )

        val c2 = Offset(size.width * 0.80f, size.height * (0.15f + 0.42f * t2))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(OrbBlue.copy(alpha = 0.48f), Color.Transparent),
                center = c2, radius = d * 0.52f
            ),
            radius = d * 0.52f, center = c2
        )

        val c3 = Offset(size.width * (0.28f + 0.44f * t3), size.height * 0.72f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(OrbIndigo.copy(alpha = 0.42f), Color.Transparent),
                center = c3, radius = d * 0.46f
            ),
            radius = d * 0.46f, center = c3
        )

        val c4 = Offset(size.width * (0.50f + 0.28f * t4), size.height * 0.22f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(OrbTeal.copy(alpha = 0.35f), Color.Transparent),
                center = c4, radius = d * 0.40f
            ),
            radius = d * 0.40f, center = c4
        )

        val c5 = Offset(size.width * (0.60f + 0.20f * t5), size.height * (0.55f + 0.20f * t3))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(OrbPurple.copy(alpha = 0.30f), Color.Transparent),
                center = c5, radius = d * 0.32f
            ),
            radius = d * 0.32f, center = c5
        )
    }
}
