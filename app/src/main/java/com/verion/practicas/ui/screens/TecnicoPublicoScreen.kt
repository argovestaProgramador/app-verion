package com.verion.practicas.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.verion.practicas.ApiClient
import com.verion.practicas.R
import com.verion.practicas.TokenManager
import com.verion.practicas.ui.components.GlassCard
import com.verion.practicas.ui.theme.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

private fun JSONObject.strT(key: String): String = if (isNull(key)) "" else optString(key, "")

private fun JSONArray?.toTecnicoList(): List<JSONObject> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { runCatching { getJSONObject(it) }.getOrNull() }
}

private fun tsMonthYear(ts: Long): String {
    if (ts <= 0L) return ""
    val c = Calendar.getInstance().apply { timeInMillis = ts * 1000L }
    val mes = arrayOf("Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic")[c.get(Calendar.MONTH)]
    return "$mes ${c.get(Calendar.YEAR)}"
}

@Composable
fun TecnicoPublicoScreen(
    tecnicoId: String,
    tokenManager: TokenManager,
    rol: String,
    onBack: () -> Unit
) {
    val context    = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope      = rememberCoroutineScope()

    var perfil         by remember { mutableStateOf<JSONObject?>(null) }
    var favoritosCount by remember { mutableIntStateOf(0) }
    var habilidades    by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var experiencias   by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var educacion      by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var certificados   by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var proyectos      by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var calificaciones by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var resenas        by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var isLoading      by remember { mutableStateOf(true) }

    var resenaText     by remember { mutableStateOf("") }
    var enviandoResena by remember { mutableStateOf(false) }
    var resenaMsg      by remember { mutableStateOf<String?>(null) }
    var yaReseno       by remember { mutableStateOf(false) }

    var guardando by remember { mutableStateOf(false) }
    var guardado  by remember { mutableStateOf(false) }
    var favMsg    by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tecnicoId) {
        isLoading = true
        coroutineScope {
            val a1 = async { ApiClient.get("/api/perfil/tecnico/$tecnicoId") }
            val a2 = async { ApiClient.get("/api/cv/$tecnicoId") }
            val a3 = async { ApiClient.get("/api/proyectos/$tecnicoId") }
            val a4 = async { ApiClient.get("/api/calificaciones/$tecnicoId") }
            val a5 = async { ApiClient.get("/api/calificaciones/resenas/$tecnicoId") }
            val r1 = a1.await(); val r2 = a2.await(); val r3 = a3.await()
            val r4 = a4.await(); val r5 = a5.await()
            if (r1.success) {
                perfil         = r1.data?.optJSONObject("perfil")
                favoritosCount = r1.data?.optInt("favoritos_count", 0) ?: 0
            }
            if (r2.success) {
                habilidades  = r2.data?.optJSONArray("habilidades").toTecnicoList()
                experiencias = r2.data?.optJSONArray("experiencias").toTecnicoList()
                educacion    = r2.data?.optJSONArray("educacion").toTecnicoList()
                certificados = r2.data?.optJSONArray("certificados").toTecnicoList()
            }
            proyectos      = r3.dataArray.toTecnicoList()
            calificaciones = r4.dataArray.toTecnicoList()
            resenas        = r5.dataArray.toTecnicoList()
        }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), null, tint = Color.White)
            }
            Text("Perfil técnico", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandBlue, strokeWidth = 2.dp)
            }
            return@Column
        }

        val nombre       = perfil?.strT("nombre_completo") ?: "Técnico"
        val fotoKey      = perfil?.strT("foto_key") ?: ""
        val nivel        = when (perfil?.strT("nivel")) {
            "PRACTICANTE" -> "Practicante"; "EGRESADO" -> "Egresado"; "CERTIFICADO" -> "Certificado"; else -> ""
        }
        val disponib     = when (perfil?.strT("disponibilidad")) {
            "DISPONIBLE" -> "Disponible"; "NO_DISPONIBLE" -> "No disponible"; "POR_CONSULTAR" -> "Por consultar"; else -> ""
        }
        val disponibColor = when (perfil?.strT("disponibilidad")) {
            "DISPONIBLE" -> Color(0xFF34D399); "NO_DISPONIBLE" -> Color(0xFFF87171); else -> Color(0xFFFBBF24)
        }
        val calif        = perfil?.optDouble("calificacion_promedio", 0.0) ?: 0.0
        val totalCalif   = perfil?.optInt("total_calificaciones", 0) ?: 0
        val totalColabs  = perfil?.optInt("total_colaboraciones", 0) ?: 0
        val descripcion  = perfil?.strT("descripcion") ?: ""
        val emailProf    = perfil?.strT("email_profesional") ?: ""
        val direccion    = perfil?.strT("direccion") ?: ""
        val github       = perfil?.strT("github_url") ?: ""
        val linkedin     = perfil?.strT("linkedin_url") ?: ""
        val instagram    = perfil?.strT("instagram_url") ?: ""
        val xUrl         = perfil?.strT("x_url") ?: ""
        val whatsapp     = perfil?.strT("whatsapp") ?: ""
        val initial      = nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(88.dp).clip(CircleShape)
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
                            Text(initial, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(nombre, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    if (nivel.isNotBlank()) Text(nivel, color = BrandBlue, fontSize = 13.sp)
                    if (disponib.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(50.dp))
                                .background(disponibColor.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(disponib, color = disponibColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        if (calif > 0.0) TStatBox("★ ${"%.1f".format(calif)}", "$totalCalif calific.")
                        TStatBox("$totalColabs", "colabs.")
                        TStatBox("$favoritosCount", "guardados")
                    }
                }
            }

            // Info
            if (descripcion.isNotBlank() || emailProf.isNotBlank() || direccion.isNotBlank()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (descripcion.isNotBlank()) {
                            Text(descripcion, color = Color.White.copy(alpha = 0.78f), fontSize = 13.sp, lineHeight = 19.sp)
                        }
                        if (emailProf.isNotBlank()) TInfoRow("✉️", emailProf)
                        if (direccion.isNotBlank()) TInfoRow("📍", direccion)
                    }
                }
            }

            // Redes sociales
            if (github.isNotBlank() || linkedin.isNotBlank() || instagram.isNotBlank() || xUrl.isNotBlank() || whatsapp.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    if (github.isNotBlank())    TSocialChip("GitHub")    { runCatching { uriHandler.openUri(github) } }
                    if (linkedin.isNotBlank())  TSocialChip("LinkedIn")  { runCatching { uriHandler.openUri(linkedin) } }
                    if (instagram.isNotBlank()) TSocialChip("Instagram") { runCatching { uriHandler.openUri(instagram) } }
                    if (xUrl.isNotBlank())      TSocialChip("X")         { runCatching { uriHandler.openUri(xUrl) } }
                    if (whatsapp.isNotBlank())  TSocialChip("WhatsApp")  { runCatching { uriHandler.openUri("https://wa.me/${whatsapp.filter { it.isDigit() }}") } }
                }
            }

            // Habilidades
            if (habilidades.isNotEmpty()) {
                Text("HABILIDADES", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    habilidades.forEach { h ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(BrandBlue.copy(alpha = 0.15f))
                                .border(1.dp, BrandBlue.copy(alpha = 0.3f), RoundedCornerShape(50.dp))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(h.strT("nombre"), color = BrandBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Proyectos
            if (proyectos.isNotEmpty()) {
                Text("PROYECTOS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                proyectos.forEach { p ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            val portadaKey = p.strT("portada_key")
                            if (portadaKey.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data("${ApiClient.BASE_URL}/api/uploads/$portadaKey")
                                        .crossfade(true).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().height(140.dp)
                                )
                            }
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(p.strT("titulo"), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                val desc = p.strT("descripcion")
                                if (desc.isNotBlank()) Text(desc, color = TextSecondary, fontSize = 12.sp, maxLines = 3)
                                val url = p.strT("url")
                                if (url.isNotBlank()) {
                                    TextButton(onClick = { runCatching { uriHandler.openUri(url) } }, contentPadding = PaddingValues(0.dp)) {
                                        Text("Ver proyecto →", color = BrandBlue, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Experiencia
            if (experiencias.isNotEmpty()) {
                Text("EXPERIENCIA", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                experiencias.forEach { e ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(e.strT("titulo"), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            if (e.strT("empresa").isNotBlank()) Text(e.strT("empresa"), color = BrandBlue, fontSize = 12.sp)
                            val fi = e.optLong("fecha_inicio", 0L)
                            val ff = e.optLong("fecha_fin", 0L)
                            val dateRange = buildString {
                                if (fi > 0L) append(tsMonthYear(fi))
                                append(" — ")
                                append(if (ff > 0L) tsMonthYear(ff) else "Actualidad")
                            }
                            Text(dateRange, color = TextSecondary, fontSize = 11.sp)
                            val desc = e.strT("descripcion")
                            if (desc.isNotBlank()) Text(desc, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
                        }
                    }
                }
            }

            // Educación
            if (educacion.isNotEmpty()) {
                Text("EDUCACIÓN", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                educacion.forEach { e ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(e.strT("titulo"), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            if (e.strT("institucion").isNotBlank()) Text(e.strT("institucion"), color = BrandBlue, fontSize = 12.sp)
                            val fi = e.optLong("fecha_inicio", 0L)
                            val ff = e.optLong("fecha_fin", 0L)
                            val dateRange = buildString {
                                if (fi > 0L) append(tsMonthYear(fi))
                                append(" — ")
                                append(if (ff > 0L) tsMonthYear(ff) else "En curso")
                            }
                            Text(dateRange, color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Certificados
            if (certificados.isNotEmpty()) {
                Text("CERTIFICADOS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                certificados.forEach { c ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(c.strT("nombre"), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            if (c.strT("institucion").isNotBlank()) Text(c.strT("institucion"), color = BrandBlue, fontSize = 12.sp)
                            val fecha = c.optLong("fecha", 0L)
                            if (fecha > 0L) Text(tsMonthYear(fecha), color = TextSecondary, fontSize = 11.sp)
                            val url = c.strT("url")
                            if (url.isNotBlank()) {
                                TextButton(onClick = { runCatching { uriHandler.openUri(url) } }, contentPadding = PaddingValues(0.dp)) {
                                    Text("Ver certificado →", color = BrandBlue, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // EMPRESA: guardar + reseña
            if (rol == "EMPRESA") {
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                Button(
                    onClick = {
                        scope.launch {
                            guardando = true; favMsg = null
                            val token = tokenManager.getAccessToken()
                                ?: run { favMsg = "Inicia sesión para guardar"; guardando = false; return@launch }
                            val body = JSONObject().apply { put("objetivo_id", tecnicoId); put("tipo", "TECNICO_GUARDADO") }
                            val r = ApiClient.post("/api/favoritos", body, token)
                            guardando = false
                            when {
                                r.success -> { guardado = true; favMsg = "✓ Guardado en favoritos" }
                                r.error?.contains("Ya guardaste") == true -> { guardado = true; favMsg = "Ya está en favoritos" }
                                else -> favMsg = "✗ ${r.error ?: "Error"}"
                            }
                        }
                    },
                    enabled  = !guardando && !guardado,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape    = RoundedCornerShape(50.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = if (guardado) Color(0xFF34D399).copy(alpha = 0.15f) else BrandBlue.copy(alpha = 0.18f),
                        contentColor           = if (guardado) Color(0xFF34D399) else BrandBlue,
                        disabledContainerColor = Color.White.copy(alpha = 0.06f),
                        disabledContentColor   = Color.White.copy(alpha = 0.4f)
                    )
                ) {
                    Text(if (guardado) "★ Guardado" else "☆ Guardar técnico", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                favMsg?.let { Text(it, color = if (it.startsWith("✓") || it.startsWith("Ya")) Color(0xFF34D399) else Color(0xFFF87171), fontSize = 12.sp) }

                if (!yaReseno) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Escribir reseña", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                                BasicTextField(
                                    value         = resenaText,
                                    onValueChange = { resenaText = it },
                                    textStyle     = TextStyle(color = Color.White, fontSize = 13.sp),
                                    cursorBrush   = SolidColor(BrandBlue),
                                    minLines      = 3,
                                    modifier      = Modifier.fillMaxWidth().padding(12.dp),
                                    decorationBox = { inner ->
                                        if (resenaText.isEmpty()) Text(
                                            "Tu experiencia con este técnico...",
                                            color = Color.White.copy(alpha = 0.35f), fontSize = 13.sp
                                        )
                                        inner()
                                    }
                                )
                            }
                            resenaMsg?.let {
                                Text(it, color = if (it.startsWith("✓")) Color(0xFF34D399) else Color(0xFFF87171), fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    if (resenaText.isBlank()) { resenaMsg = "✗ Escribe tu reseña"; return@Button }
                                    scope.launch {
                                        enviandoResena = true; resenaMsg = null
                                        val token = tokenManager.getAccessToken()
                                            ?: run { resenaMsg = "✗ Inicia sesión"; enviandoResena = false; return@launch }
                                        val body = JSONObject().apply {
                                            put("destinatario_id", tecnicoId)
                                            put("contenido", resenaText.trim())
                                        }
                                        val r = ApiClient.post("/api/calificaciones/resenas", body, token)
                                        enviandoResena = false
                                        if (r.success) { yaReseno = true; resenaMsg = "✓ Reseña publicada" }
                                        else resenaMsg = "✗ ${r.error ?: "Error"}"
                                    }
                                },
                                enabled  = !enviandoResena,
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                shape    = RoundedCornerShape(50.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1A0F3D))
                            ) {
                                if (enviandoResena) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BrandPurple, strokeWidth = 2.dp)
                                else Text("Publicar reseña", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    resenaMsg?.let { Text(it, color = Color(0xFF34D399), fontSize = 12.sp) }
                }
            }

            // Reseñas
            if (resenas.isNotEmpty()) {
                Text("RESEÑAS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                resenas.forEach { r ->
                    val autorNombre = r.strT("autor_nombre")
                    val autorFoto   = r.strT("autor_foto_key")
                    val fecha       = r.optLong("fecha", 0L)
                    val puntaje     = r.optInt("puntaje", 0)
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF2D1B6E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (autorFoto.isNotBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context).data("${ApiClient.BASE_URL}/api/uploads/$autorFoto").crossfade(true).build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                                        )
                                    } else {
                                        Text(autorNombre.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    if (autorNombre.isNotBlank()) Text(autorNombre, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    if (fecha > 0L) {
                                        val cal = Calendar.getInstance().apply { timeInMillis = fecha * 1000L }
                                        val mes = arrayOf("Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic")[cal.get(Calendar.MONTH)]
                                        Text("${cal.get(Calendar.DAY_OF_MONTH)} $mes ${cal.get(Calendar.YEAR)}", color = TextSecondary, fontSize = 10.sp)
                                    }
                                }
                                if (puntaje > 0) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        repeat(puntaje) {
                                            Icon(painterResource(R.drawable.ic_star), null, tint = Color(0xFFFBBF24), modifier = Modifier.size(13.dp))
                                        }
                                    }
                                }
                            }
                            Text(r.strT("contenido"), color = TextPrimary, fontSize = 13.sp)
                            val respuesta = r.strT("respuesta")
                            if (respuesta.isNotBlank()) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                Text("Respuesta:", color = BrandBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text(respuesta, color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Calificaciones
            if (calificaciones.isNotEmpty()) {
                Text("CALIFICACIONES", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                calificaciones.take(10).forEach { c ->
                    val puntaje = c.optDouble("puntaje", 0.0)
                    val comentario = c.strT("comentario")
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("★ ${"%.1f".format(puntaje)}", color = Color(0xFFFBBF24), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            if (comentario.isNotBlank()) {
                                Spacer(Modifier.width(10.dp))
                                Text(comentario, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun TStatBox(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun TInfoRow(icon: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(icon, fontSize = 13.sp, modifier = Modifier.padding(top = 1.dp, end = 6.dp))
        Text(text, color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun TSocialChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(GlassBg)
            .border(1.dp, GlassBorder, RoundedCornerShape(50.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, color = BrandBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
