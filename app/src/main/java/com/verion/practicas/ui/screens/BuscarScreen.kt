package com.verion.practicas.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.verion.practicas.ApiClient
import com.verion.practicas.R
import com.verion.practicas.TokenManager
import com.verion.practicas.ui.components.GlassCard
import com.verion.practicas.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private fun JSONObject.str(key: String): String =
    if (isNull(key)) "" else optString(key, "")

private fun JSONArray?.toObjectList(): List<JSONObject> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { runCatching { getJSONObject(it) }.getOrNull() }
}

@SuppressLint("MissingPermission")
private suspend fun fetchBuscarLocation(context: Context): Pair<Double, Double>? {
    val granted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!granted) return null
    return withContext(Dispatchers.IO) {
        runCatching {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
                .firstNotNullOfOrNull { p -> runCatching { lm.getLastKnownLocation(p)?.let { Pair(it.latitude, it.longitude) } }.getOrNull() }
        }.getOrNull()
    }
}

@Composable
fun BuscarScreen(
    tokenManager: TokenManager,
    rol: String,
    isGuest: Boolean,
    postuladas: Set<String> = emptySet(),
    onConvocatoriaClick: (JSONObject) -> Unit = {},
    onTecnicoClick: (String) -> Unit = {}
) {
    val isTecnico = rol == "TECNICO"
    val context   = LocalContext.current
    val scope     = rememberCoroutineScope()

    var search              by remember { mutableStateOf("") }
    var selectedCategoriaId by remember { mutableStateOf<String?>(null) }
    var categorias          by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var isLoading           by remember { mutableStateOf(true) }
    var resultList          by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var errorMsg            by remember { mutableStateOf<String?>(null) }

    var userLat by remember { mutableStateOf<Double?>(null) }
    var userLng by remember { mutableStateOf<Double?>(null) }
    var locMsg  by remember { mutableStateOf<String?>(null) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            scope.launch {
                val loc = fetchBuscarLocation(context)
                if (loc != null) { userLat = loc.first; userLng = loc.second; locMsg = null }
                else locMsg = "No se pudo obtener la ubicación. Intenta moverte al aire libre."
            }
        } else {
            locMsg = "Permiso de ubicación denegado."
        }
    }

    LaunchedEffect(Unit) {
        val r = ApiClient.get("/api/categorias")
        if (r.success) categorias = r.dataArray.toObjectList()
    }

    LaunchedEffect(userLat, userLng, selectedCategoriaId) {
        isLoading = true; errorMsg = null
        val base = if (isTecnico) "/api/buscar/convocatorias?limit=20" else "/api/buscar/tecnicos?limit=20"
        val path = buildString {
            append(base)
            if (userLat != null && userLng != null) append("&lat=$userLat&lng=$userLng&radio_km=10")
            if (selectedCategoriaId != null) append("&categoria=$selectedCategoriaId")
        }
        val result = ApiClient.get(path)
        if (result.success) resultList = result.data?.optJSONArray("items").toObjectList()
        else errorMsg = result.error ?: "Error al cargar"
        isLoading = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                if (isTecnico) "Convocatorias" else "Explorar técnicos",
                color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold
            )
            Text(
                if (isTecnico) "Encuentra oportunidades para ti" else "Encuentra el talento que necesitas",
                color = TextSecondary, fontSize = 13.sp
            )
            Spacer(Modifier.height(18.dp))
        }

        item {
            // Search bar + location button
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassCard(modifier = Modifier.weight(1f).height(50.dp)) {
                    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_nav_search), null, tint = Color.White.copy(alpha = 0.45f), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        BasicTextField(
                            value         = search,
                            onValueChange = { search = it },
                            textStyle     = TextStyle(color = Color.White, fontSize = 14.sp),
                            cursorBrush   = SolidColor(Color.White),
                            singleLine    = true,
                            modifier      = Modifier.weight(1f),
                            decorationBox = { inner ->
                                if (search.isEmpty()) Text(
                                    if (isTecnico) "Buscar convocatorias..." else "Buscar técnicos...",
                                    color = Color.White.copy(alpha = 0.38f), fontSize = 14.sp
                                )
                                inner()
                            }
                        )
                    }
                }
                // GPS button
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (userLat != null) BrandBlue.copy(alpha = 0.3f) else GlassBg)
                        .border(1.dp, if (userLat != null) BrandBlue else GlassBorder, RoundedCornerShape(14.dp))
                        .clickable {
                            if (userLat != null) {
                                userLat = null; userLng = null; locMsg = null
                            } else {
                                val hasPerm = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    scope.launch {
                                        val loc = fetchBuscarLocation(context)
                                        if (loc != null) { userLat = loc.first; userLng = loc.second; locMsg = null }
                                        else locMsg = "No se pudo obtener la ubicación."
                                    }
                                } else {
                                    permLauncher.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(R.drawable.ic_location), null,
                        tint = if (userLat != null) BrandBlue else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (userLat != null) {
                Spacer(Modifier.height(6.dp))
                Text("📍 Mostrando cerca de ti · 10km", color = BrandBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            locMsg?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, color = Color(0xFFF87171), fontSize = 11.sp)
            }
            Spacer(Modifier.height(14.dp))
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    val selected = selectedCategoriaId == null
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (selected) Brush.linearGradient(listOf(BrandBlue, BrandIndigo)) else Brush.linearGradient(listOf(GlassBg, GlassBg)))
                            .border(1.dp, if (selected) Color.Transparent else GlassBorder, RoundedCornerShape(50.dp))
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { selectedCategoriaId = null }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Todos", color = if (selected) Color.White else Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                itemsIndexed(categorias) { _, cat ->
                    val catId = cat.str("id")
                    val selected = selectedCategoriaId == catId
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (selected) Brush.linearGradient(listOf(BrandBlue, BrandIndigo)) else Brush.linearGradient(listOf(GlassBg, GlassBg)))
                            .border(1.dp, if (selected) Color.Transparent else GlassBorder, RoundedCornerShape(50.dp))
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { selectedCategoriaId = catId }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(cat.str("nombre"), color = if (selected) Color.White else Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        when {
            isLoading -> item {
                Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandBlue, strokeWidth = 2.dp)
                }
            }

            errorMsg != null -> item {
                Text(errorMsg!!, color = Color(0xFFF87171), fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 32.dp))
            }

            resultList.isEmpty() -> item {
                Text(
                    if (isTecnico) "No hay convocatorias disponibles." else "No se encontraron técnicos.",
                    color = Color.White.copy(alpha = 0.38f), fontSize = 13.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp)
                )
            }

            else -> {
                val filtered = resultList.filter {
                    val q = search.trim().lowercase()
                    if (q.isEmpty()) true
                    else if (isTecnico) it.str("titulo").lowercase().contains(q) || it.str("razon_social").lowercase().contains(q)
                    else it.str("nombre_completo").lowercase().contains(q)
                }

                item {
                    Text(
                        if (isTecnico) "CONVOCATORIAS ACTIVAS" else "TÉCNICOS DISPONIBLES",
                        color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(12.dp))
                }

                itemsIndexed(filtered, key = { _, obj -> obj.str(if (isTecnico) "id" else "usuario_id") }) { _, obj ->
                    if (isTecnico) {
                        ConvocatoriaCard(
                            conv        = obj,
                            yaPostulado = postuladas.contains(obj.str("id")),
                            context     = context,
                            onClick     = { onConvocatoriaClick(it) }
                        )
                    } else {
                        TecnicoCard(
                            tecnico = obj,
                            context = context,
                            onClick = { onTecnicoClick(obj.str("usuario_id")) }
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (filtered.isEmpty() && search.isNotBlank()) {
                    item {
                        Text(
                            "Sin resultados para \"$search\"",
                            color = Color.White.copy(alpha = 0.38f), fontSize = 13.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConvocatoriaCard(
    conv: JSONObject,
    yaPostulado: Boolean,
    context: Context,
    onClick: (JSONObject) -> Unit
) {
    val titulo    = conv.str("titulo")
    val empresa   = conv.str("razon_social")
    val desc      = conv.str("descripcion")
    val logoKey   = conv.str("logo_key")
    val libres    = (conv.optInt("plazas_disponibles", 0) - conv.optInt("plazas_ocupadas", 0)).coerceAtLeast(0)
    val estado    = conv.str("estado")
    val estadoColor = when (estado) { "ABIERTA" -> Color(0xFF34D399); "PAUSADA" -> Color(0xFFFBBF24); else -> Color(0xFF9CA3AF) }

    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable { onClick(conv) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Empresa mini-header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF1E1040)),
                    contentAlignment = Alignment.Center
                ) {
                    if (logoKey.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data("${ApiClient.BASE_URL}/api/uploads/$logoKey")
                                .crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Text("🏢", fontSize = 16.sp)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(empresa, color = BrandBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(50.dp))
                        .background(estadoColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(estado, color = estadoColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(titulo, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(50.dp))
                        .background(Color(0xFF34D399).copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("$libres plaza${if (libres != 1) "s" else ""}", color = Color(0xFF34D399), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (desc.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(desc, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 2)
            }

            if (yaPostulado) {
                Spacer(Modifier.height(8.dp))
                Text("✓ Ya postulado", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TecnicoCard(
    tecnico: JSONObject,
    context: Context,
    onClick: () -> Unit
) {
    val nombre  = tecnico.str("nombre_completo").ifBlank { "Técnico" }
    val fotoKey = tecnico.str("foto_key")
    val nivel   = when (tecnico.str("nivel")) {
        "PRACTICANTE" -> "Practicante"; "EGRESADO" -> "Egresado"; "CERTIFICADO" -> "Certificado"; else -> ""
    }
    val calif  = tecnico.optDouble("calificacion_promedio", 0.0)
    val colabs = tecnico.optInt("total_colaboraciones", 0)
    val initial = nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(OrbPurple, Color(0xFF9B6EFF)))),
                contentAlignment = Alignment.Center
            ) {
                if (fotoKey.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data("${ApiClient.BASE_URL}/api/uploads/$fotoKey")
                            .crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Text(initial, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(nombre, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                val detail = buildString {
                    if (nivel.isNotBlank()) append(nivel)
                    if (calif > 0.0) { if (isNotEmpty()) append("  ·  "); append("★ ${"%.1f".format(calif)}") }
                    if (colabs > 0)  { if (isNotEmpty()) append("  ·  "); append("$colabs colabs.") }
                }
                if (detail.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(detail, color = BrandBlue, fontSize = 11.sp)
                }
            }
            Text("›", color = TextSecondary, fontSize = 20.sp)
        }
    }
}
