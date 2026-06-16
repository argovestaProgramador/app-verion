package com.verion.practicas.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.verion.practicas.ApiClient
import com.verion.practicas.ui.theme.BgDeep
import com.verion.practicas.ui.theme.BrandBlue
import com.verion.practicas.ui.theme.BrandPurple
import com.verion.practicas.ui.theme.SurfaceContainerHigh
import com.verion.practicas.ui.theme.TextPrimary
import com.verion.practicas.ui.theme.TextSecondary
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

@Composable
fun MapSearchScreen(
    results: List<JSONObject>,
    centerLat: Double,
    centerLng: Double,
    isConvocatoriasMode: Boolean,
    isTecnicosMode: Boolean,
    onBack: () -> Unit,
    onTecnicoClick: (String) -> Unit,
    onEmpresaClick: (String) -> Unit,
    onConvocatoriaClick: (JSONObject) -> Unit
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<JSONObject?>(null) }
    var mapView  by remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(Unit) { onDispose { mapView?.onDetach() } }

    Box(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        AndroidView(
            factory = { ctx ->
                Configuration.getInstance().apply {
                    load(ctx, ctx.getSharedPreferences("osm_prefs", 0))
                    userAgentValue    = ctx.packageName
                    osmdroidBasePath  = File(ctx.cacheDir, "osmdroid")
                    osmdroidTileCache = File(ctx.cacheDir, "osmdroid/tiles")
                }
                MapView(ctx).also { mv ->
                    mapView = mv
                    mv.setTileSource(TileSourceFactory.MAPNIK)
                    mv.setMultiTouchControls(true)
                    mv.controller.setZoom(13.0)
                    mv.controller.setCenter(GeoPoint(centerLat, centerLng))
                    mv.isHorizontalMapRepetitionEnabled = false
                    mv.isVerticalMapRepetitionEnabled   = false
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { mv ->
                mv.overlays.clear()

                mv.overlays.add(Marker(mv).apply {
                    position = GeoPoint(centerLat, centerLng)
                    icon     = createUserMarker(context)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title    = "Tu ubicación"
                })

                results.forEach { obj ->
                    val lat = obj.optDouble("lat").takeIf { !it.isNaN() && it != 0.0 }
                            ?: obj.optDouble("empresa_lat").takeIf { !it.isNaN() && it != 0.0 }
                            ?: return@forEach
                    val lng = obj.optDouble("lng").takeIf { !it.isNaN() && it != 0.0 }
                            ?: obj.optDouble("empresa_lng").takeIf { !it.isNaN() && it != 0.0 }
                            ?: return@forEach

                    val label = when {
                        isConvocatoriasMode -> obj.optString("titulo", "?")
                        isTecnicosMode      -> obj.optString("nombre_completo", "?")
                        else                -> obj.optString("razon_social", "?")
                    }.take(1).uppercase().ifEmpty { "?" }

                    val color = when {
                        isConvocatoriasMode -> Color(0xFF34D399)
                        isTecnicosMode      -> BrandPurple
                        else                -> BrandBlue
                    }

                    mv.overlays.add(Marker(mv).apply {
                        position = GeoPoint(lat, lng)
                        icon     = createProfileMarker(context, label, color)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        setOnMarkerClickListener { _, _ -> selected = obj; true }
                    })
                }
                mv.invalidate()
            }
        )

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(BgDeep.copy(alpha = 0.88f))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Volver",
                    tint     = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(BgDeep.copy(alpha = 0.88f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = when {
                        isConvocatoriasMode -> "Convocatorias cercanas · ${results.size}"
                        isTecnicosMode      -> "Técnicos cercanos · ${results.size}"
                        else                -> "Empresas cercanas · ${results.size}"
                    },
                    color = TextPrimary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        AnimatedVisibility(
            visible  = selected != null,
            enter    = slideInVertically { it } + fadeIn(),
            exit     = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selected?.let { obj ->
                MapResultCard(
                    obj                 = obj,
                    isConvocatoriasMode = isConvocatoriasMode,
                    isTecnicosMode      = isTecnicosMode,
                    onDismiss           = { selected = null },
                    onOpen              = {
                        when {
                            isConvocatoriasMode -> onConvocatoriaClick(obj)
                            isTecnicosMode      -> onTecnicoClick(obj.optString("usuario_id", ""))
                            else                -> onEmpresaClick(obj.optString("usuario_id", ""))
                        }
                        selected = null
                    }
                )
            }
        }
    }
}

@Composable
private fun MapResultCard(
    obj: JSONObject,
    isConvocatoriasMode: Boolean,
    isTecnicosMode: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit
) {
    val context  = LocalContext.current
    val title    = when {
        isConvocatoriasMode -> obj.optString("titulo", "Convocatoria")
        isTecnicosMode      -> obj.optString("nombre_completo", "Técnico")
        else                -> obj.optString("razon_social", "Empresa")
    }
    val subtitle = when {
        isConvocatoriasMode -> obj.optString("razon_social", "")
        isTecnicosMode      -> when (obj.optString("nivel")) {
            "PRACTICANTE" -> "Practicante"
            "EGRESADO"    -> "Egresado"
            "CERTIFICADO" -> "Certificado"
            else          -> ""
        }
        else -> obj.optString("sector", "")
    }
    val rawKey = when {
        isConvocatoriasMode -> obj.optString("logo_key", "")
        isTecnicosMode      -> obj.optString("foto_key", "")
        else                -> obj.optString("logo_key", "")
    }
    val imageKey    = if (rawKey == "null" || rawKey.isBlank()) "" else rawKey
    val accentColor = when {
        isConvocatoriasMode -> Color(0xFF34D399)
        isTecnicosMode      -> BrandPurple
        else                -> BrandBlue
    }
    val avatarShape = if (isTecnicosMode) CircleShape else RoundedCornerShape(12.dp)

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(avatarShape)
                    .background(Brush.linearGradient(listOf(accentColor.copy(alpha = 0.45f), accentColor))),
                contentAlignment = Alignment.Center
            ) {
                if (imageKey.isNotBlank()) {
                    AsyncImage(
                        model              = ImageRequest.Builder(context)
                            .data("${ApiClient.BASE_URL}/api/uploads/$imageKey")
                            .crossfade(true).build(),
                        contentDescription = null,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize().clip(avatarShape)
                    )
                } else {
                    Text(
                        title.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        color      = Color.White,
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onOpen,
                shape   = RoundedCornerShape(50.dp),
                colors  = ButtonDefaults.filledTonalButtonColors(
                    containerColor = accentColor.copy(alpha = 0.18f),
                    contentColor   = accentColor
                )
            ) {
                Text("Ver", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private fun createProfileMarker(context: Context, initial: String, color: Color): BitmapDrawable {
    val dp     = context.resources.displayMetrics.density
    val sizePx = (56 * dp).toInt()
    val bm     = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val cv     = Canvas(bm)

    cv.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 3f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toArgb(); style = Paint.Style.FILL })

    cv.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 3f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color  = android.graphics.Color.WHITE
            style       = Paint.Style.STROKE
            strokeWidth = 4 * dp
        })

    cv.drawText(initial, sizePx / 2f, sizePx / 2f + 10 * dp,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = android.graphics.Color.WHITE
            textSize   = 22 * dp
            textAlign  = Paint.Align.CENTER
            typeface   = Typeface.DEFAULT_BOLD
        })

    return BitmapDrawable(context.resources, bm)
}

private fun createUserMarker(context: Context): BitmapDrawable {
    val dp     = context.resources.displayMetrics.density
    val sizePx = (22 * dp).toInt()
    val bm     = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val cv     = Canvas(bm)

    cv.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; style = Paint.Style.FILL })

    cv.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 4 * dp,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color(0xFF2563EB).toArgb(); style = Paint.Style.FILL })

    return BitmapDrawable(context.resources, bm)
}
