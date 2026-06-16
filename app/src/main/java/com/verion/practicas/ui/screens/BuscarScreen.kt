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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.*
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
    onTecnicoClick: (String) -> Unit = {},
    onEmpresaClick: (String) -> Unit = {}
) {
    val isTecnico = rol == "TECNICO"
    val context   = LocalContext.current
    val scope     = rememberCoroutineScope()

    var search              by remember { mutableStateOf("") }
    var searchMode          by remember { mutableIntStateOf(0) }
    var selectedCategoriaId by remember { mutableStateOf<String?>(null) }
    var categorias          by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var isLoading           by remember { mutableStateOf(true) }
    var resultList          by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var errorMsg            by remember { mutableStateOf<String?>(null) }
    var showCategoryPicker  by remember { mutableStateOf(false) }
    var catSearch           by remember { mutableStateOf("") }

    val modeTabs = if (isTecnico) listOf("Convocatorias", "Técnicos", "Empresas") else listOf("Técnicos", "Empresas")
    val isConvocatoriasMode = isTecnico && searchMode == 0
    val isTecnicosMode      = (isTecnico && searchMode == 1) || (!isTecnico && searchMode == 0)
    val isEmpresasMode      = (isTecnico && searchMode == 2) || (!isTecnico && searchMode == 1)

    var userLat      by remember { mutableStateOf<Double?>(null) }
    var userLng      by remember { mutableStateOf<Double?>(null) }
    var locMsg       by remember { mutableStateOf<String?>(null) }
    var showMapSearch by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            scope.launch {
                val loc = fetchBuscarLocation(context)
                if (loc != null) { userLat = loc.first; userLng = loc.second; locMsg = null; showMapSearch = true }
                else locMsg = "No se pudo obtener la ubicación. Intenta moverte al aire libre."
            }
        } else {
            locMsg = "Permiso de ubicación denegado."
        }
    }

    LaunchedEffect(Unit) {
        val cached = StaticCache.getCategorias(context)
        if (cached != null) {
            categorias = (0 until cached.length()).mapNotNull { runCatching { cached.getJSONObject(it) }.getOrNull() }
        } else {
            val r = ApiClient.get("/api/categorias")
            if (r.success && r.dataArray != null) {
                categorias = r.dataArray.toObjectList()
                StaticCache.saveCategorias(context, r.dataArray)
            }
        }
    }

    LaunchedEffect(userLat, userLng, selectedCategoriaId, searchMode) {
        isLoading = true; errorMsg = null
        val base = when {
            isConvocatoriasMode -> "/api/buscar/convocatorias?limit=20"
            isTecnicosMode      -> "/api/buscar/tecnicos?limit=20"
            else                -> "/api/buscar/empresas?limit=20"
        }
        val path = buildString {
            append(base)
            if (userLat != null && userLng != null) append("&lat=$userLat&lng=$userLng&radio_km=10")
            if (selectedCategoriaId != null && !isEmpresasMode) append("&categoria=$selectedCategoriaId")
        }
        val result = ApiClient.get(path)
        if (result.success) resultList = result.data?.optJSONArray("items").toObjectList()
        else errorMsg = result.error ?: "Error al cargar"
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (userLat != null) BrandBlue.copy(alpha = 0.3f) else GlassBg)
                        .border(1.dp, if (userLat != null) BrandBlue else GlassBorder, RoundedCornerShape(14.dp))
                        .clickable {
                            if (userLat != null) {
                                showMapSearch = true
                            } else {
                                val hasPerm = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    scope.launch {
                                        val loc = fetchBuscarLocation(context)
                                        if (loc != null) { userLat = loc.first; userLng = loc.second; locMsg = null; showMapSearch = true }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mostrando cerca de ti · 10km", color = BrandBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "✕ Limpiar",
                        color    = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null
                        ) { userLat = null; userLng = null; locMsg = null }
                    )
                }
            }
            locMsg?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, color = Color(0xFFF87171), fontSize = 11.sp)
            }
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                modeTabs.forEachIndexed { idx, label ->
                    val sel = idx == searchMode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (sel) Brush.linearGradient(listOf(BrandBlue, BrandIndigo)) else Brush.linearGradient(listOf(GlassBg, GlassBg)))
                            .border(1.dp, if (sel) Color.Transparent else GlassBorder, RoundedCornerShape(50.dp))
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                                resultList = emptyList(); isLoading = true; searchMode = idx; search = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = if (sel) Color.White else Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (!isEmpresasMode) {
            item {
                val selectedCat = categorias.firstOrNull { it.str("id") == selectedCategoriaId }
                GlassCard(
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                        .clickable { showCategoryPicker = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_nav_search),
                            contentDescription = null,
                            tint = if (selectedCat != null) BrandBlue else Color.White.copy(alpha = 0.38f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = selectedCat?.str("nombre") ?: "Todas las categorías",
                            color = if (selectedCat != null) BrandBlue else Color.White.copy(alpha = 0.45f),
                            fontSize = 13.sp,
                            fontWeight = if (selectedCat != null) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (selectedCat != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(BrandBlue.copy(alpha = 0.15f))
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) { selectedCategoriaId = null }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("✕ Limpiar", color = BrandBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Text("›", color = Color.White.copy(alpha = 0.35f), fontSize = 18.sp)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        } else {
            item { Spacer(Modifier.height(4.dp)) }
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
                    when {
                        isConvocatoriasMode -> "No hay convocatorias disponibles."
                        isTecnicosMode      -> "No se encontraron técnicos."
                        else                -> "No se encontraron empresas."
                    },
                    color = Color.White.copy(alpha = 0.38f), fontSize = 13.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp)
                )
            }

            else -> {
                val filtered = resultList.filter {
                    val q = search.trim().lowercase()
                    if (q.isEmpty()) true
                    else when {
                        isConvocatoriasMode -> it.str("titulo").lowercase().contains(q) || it.str("razon_social").lowercase().contains(q)
                        isTecnicosMode      -> it.str("nombre_completo").lowercase().contains(q)
                        else                -> it.str("razon_social").lowercase().contains(q) || it.str("sector").lowercase().contains(q)
                    }
                }

                item {
                    Text(
                        when {
                            isConvocatoriasMode -> "CONVOCATORIAS ACTIVAS"
                            isTecnicosMode      -> "TÉCNICOS DISPONIBLES"
                            else                -> "EMPRESAS"
                        },
                        color = TextSecondary, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(12.dp))
                }

                itemsIndexed(filtered, key = { _, obj -> obj.str(if (isConvocatoriasMode) "id" else "usuario_id") }) { _, obj ->
                    when {
                        isConvocatoriasMode -> ConvocatoriaCard(
                            conv        = obj,
                            yaPostulado = postuladas.contains(obj.str("id")),
                            context     = context,
                            onClick     = { onConvocatoriaClick(it) }
                        )
                        isTecnicosMode -> TecnicoCard(
                            tecnico = obj,
                            context = context,
                            onClick = { onTecnicoClick(obj.str("usuario_id")) }
                        )
                        else -> EmpresaSearchCard(
                            empresa = obj,
                            context = context,
                            onClick = { onEmpresaClick(obj.str("usuario_id")) }
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

    AnimatedVisibility(
        visible = showMapSearch && userLat != null && userLng != null,
        enter   = fadeIn() + slideInVertically { it / 5 },
        exit    = fadeOut() + slideOutVertically { it / 5 }
    ) {
        if (userLat != null && userLng != null) {
            MapSearchScreen(
                results             = resultList,
                centerLat           = userLat!!,
                centerLng           = userLng!!,
                isConvocatoriasMode = isConvocatoriasMode,
                isTecnicosMode      = isTecnicosMode,
                onBack              = { showMapSearch = false },
                onTecnicoClick      = onTecnicoClick,
                onEmpresaClick      = onEmpresaClick,
                onConvocatoriaClick = onConvocatoriaClick
            )
        }
    }

    AnimatedVisibility(
        visible = showCategoryPicker,
        enter   = fadeIn(),
        exit    = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { showCategoryPicker = false; catSearch = "" }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.65f)
                    .align(Alignment.BottomCenter)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(Color(0xFF100B28))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { }
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Filtrar por categoría", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (selectedCategoriaId != null) {
                        Text(
                            "Limpiar",
                            color = BrandBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { selectedCategoriaId = null; showCategoryPicker = false; catSearch = "" }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                        .padding(horizontal = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_nav_search),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.38f),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        BasicTextField(
                            value         = catSearch,
                            onValueChange = { catSearch = it },
                            textStyle     = TextStyle(color = Color.White, fontSize = 13.sp),
                            cursorBrush   = SolidColor(Color.White),
                            singleLine    = true,
                            modifier      = Modifier.weight(1f),
                            decorationBox = { inner ->
                                if (catSearch.isEmpty()) Text("Buscar categoría...", color = Color.White.copy(alpha = 0.35f), fontSize = 13.sp)
                                inner()
                            }
                        )
                        if (catSearch.isNotEmpty()) {
                            Text(
                                "✕",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 13.sp,
                                modifier = Modifier.clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { catSearch = "" }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                val filteredCats = if (catSearch.isBlank()) categorias
                                   else categorias.filter { it.str("nombre").contains(catSearch, ignoreCase = true) }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 40.dp)
                ) {
                    item {
                        val sel = selectedCategoriaId == null
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { selectedCategoriaId = null; showCategoryPicker = false; catSearch = "" }
                                .padding(horizontal = 20.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(8.dp).clip(RoundedCornerShape(50.dp))
                                    .background(if (sel) BrandBlue else Color.White.copy(alpha = 0.15f))
                            )
                            Spacer(Modifier.width(14.dp))
                            Text(
                                "Todas las categorías",
                                color = if (sel) BrandBlue else TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (sel) Text("✓", color = BrandBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 20.dp))
                    }

                    itemsIndexed(filteredCats) { _, cat ->
                        val catId = cat.str("id")
                        val sel   = selectedCategoriaId == catId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { selectedCategoriaId = catId; showCategoryPicker = false; catSearch = "" }
                                .padding(horizontal = 20.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(8.dp).clip(RoundedCornerShape(50.dp))
                                    .background(if (sel) BrandBlue else Color.White.copy(alpha = 0.15f))
                            )
                            Spacer(Modifier.width(14.dp))
                            Text(
                                cat.str("nombre"),
                                color = if (sel) BrandBlue else TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (sel) Text("✓", color = BrandBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        if (filteredCats.indexOf(cat) < filteredCats.lastIndex) {
                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 20.dp))
                        }
                    }

                    if (filteredCats.isEmpty()) {
                        item {
                            Text(
                                "Sin resultados para \"$catSearch\"",
                                color = Color.White.copy(alpha = 0.38f),
                                fontSize = 13.sp,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp)
                            )
                        }
                    }
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
                Text(titulo, color = TextPrimary, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
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
                Text(nombre, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                val detail = buildString {
                    if (nivel.isNotBlank()) append(nivel)
                    if (calif > 0.0) { if (isNotEmpty()) append("  ·  "); append("★ ${"%.1f".format(calif)}") }
                    if (colabs > 0)  { if (isNotEmpty()) append("  ·  "); append("$colabs colabs.") }
                }
                if (detail.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(detail, color = BrandBlue, style = MaterialTheme.typography.labelSmall)
                }
            }
            Text("›", color = TextSecondary, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun EmpresaSearchCard(
    empresa: JSONObject,
    context: Context,
    onClick: () -> Unit
) {
    val nombre  = empresa.str("razon_social").ifBlank { "Empresa" }
    val logoKey = empresa.str("logo_key")
    val sector  = empresa.str("sector")
    val calif   = empresa.optDouble("calificacion_promedio", 0.0)
    val contrat = empresa.optInt("total_contrataciones", 0)
    val initial = nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(BrandBlue, BrandIndigo))),
                contentAlignment = Alignment.Center
            ) {
                if (logoKey.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data("${ApiClient.BASE_URL}/api/uploads/$logoKey")
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
                Text(nombre, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                val detail = buildString {
                    if (sector.isNotBlank()) append(sector)
                    if (calif > 0.0) { if (isNotEmpty()) append("  ·  "); append("★ ${"%.1f".format(calif)}") }
                    if (contrat > 0) { if (isNotEmpty()) append("  ·  "); append("$contrat contrat.") }
                }
                if (detail.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(detail, color = BrandBlue, style = MaterialTheme.typography.labelSmall)
                }
            }
            Text("›", color = TextSecondary, style = MaterialTheme.typography.titleMedium)
        }
    }
}
