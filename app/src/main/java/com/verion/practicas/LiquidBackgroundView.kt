package com.verion.practicas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class LiquidBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Blob(
        val centerX: Float, val centerY: Float,
        val ampX: Float, val ampY: Float,
        val speedX: Float, val speedY: Float,
        val phaseX: Float, val phaseY: Float,
        val radius: Float, val color: Int, val alpha: Int
    )

    private val blobs = listOf(
        Blob(0.30f, 0.25f, 0.14f, 0.11f, 0.80f, 0.60f, 0.0f, 0.0f, 0.50f, Color.parseColor("#1E40AF"), 170),
        Blob(0.70f, 0.30f, 0.12f, 0.16f, 0.55f, 0.90f, 2.1f, 1.5f, 0.45f, Color.parseColor("#5B21B6"), 150),
        Blob(0.20f, 0.65f, 0.18f, 0.10f, 1.10f, 0.70f, 4.2f, 3.0f, 0.42f, Color.parseColor("#0D9488"), 140),
        Blob(0.78f, 0.72f, 0.13f, 0.15f, 0.70f, 1.20f, 1.0f, 5.0f, 0.48f, Color.parseColor("#1D4ED8"), 160),
        Blob(0.50f, 0.12f, 0.10f, 0.13f, 1.30f, 0.50f, 3.5f, 2.3f, 0.38f, Color.parseColor("#6D28D9"), 130),
        Blob(0.15f, 0.45f, 0.11f, 0.14f, 0.65f, 1.05f, 1.8f, 4.1f, 0.36f, Color.parseColor("#1E3A8A"), 120),
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var animTime = 0f

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#080D1A"))

        blobs.forEach { blob ->
            val cx = (blob.centerX + blob.ampX * sin((animTime * blob.speedX + blob.phaseX).toDouble())).toFloat() * width
            val cy = (blob.centerY + blob.ampY * cos((animTime * blob.speedY + blob.phaseY).toDouble())).toFloat() * height
            val r = blob.radius * min(width, height)

            val baseColor = blob.color and 0x00FFFFFF
            val fullAlpha = (blob.alpha shl 24) or baseColor
            val gradient = RadialGradient(cx, cy, r,
                intArrayOf(fullAlpha, Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP)
            paint.shader = gradient
            canvas.drawCircle(cx, cy, r, paint)
        }

        animTime += 0.004f
        postInvalidateOnAnimation()
    }
}
