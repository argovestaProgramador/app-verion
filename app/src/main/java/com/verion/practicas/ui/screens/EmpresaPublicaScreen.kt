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

private fun JSONObject.strE(key: String): String = if (isNull(key)) "" else optString(key, "")

private fun JSONArray?.toEmpresaList(): List<JSONObject> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { runCatching { getJSONObject(it) }.getOrNull() }
}

@Composable
fun EmpresaPublicaScreen(
    empresaId: String,
    tokenManager: TokenManager,
    rol: String,
    onBack: () -> Unit
) {
    val context    = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope      = rememberCoroutineScope()

    var perfil         by remember { mutableStateOf<JSONObject?>(null) }
    var favoritosCount by remember { mutableIntStateOf(0) }
    var calificaciones by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var resenas        by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var isLoading      by remember { mutableStateOf(true) }

    var resenaText     by remember { mutableStateOf("") }
    var resenaPuntaje  by remember { mutableIntStateOf(0) }
    var enviandoResena by remember { mutableStateOf(false) }
    var resenaMsg      by remember { mutableStateOf<String?>(null) }
    var yaReseno       by remember { mutableStateOf(false) }

    var guardando by remember { mutableStateOf(false) }
    var guardado  by remember { mutableStateOf(false) }
    var favMsg    by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(empresaId) {
        isLoading = true
        coroutineScope {
            val a1 = async { ApiClient.get("/api/perfil/empresa/$empresaId") }
            val a2 = async { ApiClient.get("/api/calificaciones/$empresaId") }
            val a3 = async { ApiClient.get("/api/calificaciones/resenas/$empresaId") }
            val r1 = a1.await(); val r2 = a2.await(); val r3 = a3.await()
            if (r1.success) {
                perfil         = r1.data?.optJSONObject("perfil")
                favoritosCount = r1.data?.optInt("favoritos_count", 0) ?: 0
            }
            calificaciones = r2.dataArray.toEmpresaList()
            resenas        = r3.dataArray.toEmpresaList()
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
            Text("Perfil empresa", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandBlue, strokeWidth = 2.dp)
            }
            return@Column
        }

        val logoKey       = perfil?.strE("logo_key") ?: ""
        val razonSocial   = perfil?.strE("razon_social") ?: "Empresa"
        val sector        = perfil?.strE("sector") ?: ""
        val descripcion   = perfil?.strE("descripcion") ?: ""
        val ruc           = perfil?.strE("ruc") ?: ""
        val direccion     = perfil?.strE("direccion") ?: ""
        val sitioWeb      = perfil?.strE("sitio_web") ?: ""
        val emailContacto = perfil?.strE("email_contacto") ?: ""
        val telefono      = perfil?.strE("telefono_contacto") ?: ""
        val linkedin      = perfil?.strE("linkedin_url") ?: ""
        val instagram     = perfil?.strE("instagram_url") ?: ""
        val facebook      = perfil?.strE("facebook_url") ?: ""
        val whatsapp      = perfil?.strE("whatsapp") ?: ""
        val calif         = perfil?.optDouble("calificacion_promedio", 0.0) ?: 0.0
        val totalCalif    = perfil?.optInt("total_calificaciones", 0) ?: 0
        val totalContrat  = perfil?.optInt("total_contrataciones", 0) ?: 0

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E1040)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (logoKey.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data("${ApiClient.BASE_URL}/api/uploads/$logoKey")
                                    .crossfade(true).build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                            )
                        } else {
                            Text("🏢", fontSize = 36.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(razonSocial, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    if (sector.isNotBlank()) Text(sector, color = BrandBlue, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        if (calif > 0.0) EStatBox("★ ${"%.1f".format(calif)}", "$totalCalif calific.")
                        EStatBox("$totalContrat", "contrat.")
                        EStatBox("$favoritosCount", "guardados")
                    }
                }
            }

            // Info card
            if (descripcion.isNotBlank() || ruc.isNotBlank() || direccion.isNotBlank() || emailContacto.isNotBlank()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (descripcion.isNotBlank()) {
                            Text(descripcion, color = Color.White.copy(alpha = 0.78f), fontSize = 13.sp, lineHeight = 19.sp)
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))
                        }
                        if (ruc.isNotBlank())           EInfoRow("🪪", "RUC $ruc")
                        if (direccion.isNotBlank())     EInfoRow("📍", direccion)
                        if (emailContacto.isNotBlank()) EInfoRow("✉️", emailContacto)
                        if (telefono.isNotBlank())      EInfoRow("📞", telefono)
                        if (sitioWeb.isNotBlank()) {
                            TextButton(
                                onClick = { runCatching { uriHandler.openUri(sitioWeb) } },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("🌐 $sitioWeb", color = BrandBlue, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Redes sociales
            if (linkedin.isNotBlank() || instagram.isNotBlank() || facebook.isNotBlank() || whatsapp.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (linkedin.isNotBlank())  ESocialChip("LinkedIn")  { runCatching { uriHandler.openUri(linkedin) } }
                    if (instagram.isNotBlank()) ESocialChip("Instagram") { runCatching { uriHandler.openUri(instagram) } }
                    if (facebook.isNotBlank())  ESocialChip("Facebook")  { runCatching { uriHandler.openUri(facebook) } }
                    if (whatsapp.isNotBlank())  ESocialChip("WhatsApp")  { runCatching { uriHandler.openUri("https://wa.me/${whatsapp.filter { it.isDigit() }}") } }
                }
            }

            // TECNICO: guardar + reseña
            if (rol == "TECNICO") {
                Button(
                    onClick = {
                        scope.launch {
                            guardando = true; favMsg = null
                            val token = tokenManager.getAccessToken()
                                ?: run { favMsg = "Inicia sesión para guardar"; guardando = false; return@launch }
                            val body = JSONObject().apply { put("objetivo_id", empresaId); put("tipo", "EMPRESA_GUARDADA") }
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
                    Text(if (guardado) "★ Guardado" else "☆ Guardar empresa", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                favMsg?.let { Text(it, color = if (it.startsWith("✓") || it.startsWith("Ya")) Color(0xFF34D399) else Color(0xFFF87171), fontSize = 12.sp) }

                if (!yaReseno) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Escribir reseña", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            EStarSelector(selected = resenaPuntaje, onSelect = { resenaPuntaje = it })
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
                                            "Tu experiencia con esta empresa...",
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
                                            put("destinatario_id", empresaId)
                                            put("contenido", resenaText.trim())
                                            if (resenaPuntaje > 0) put("puntaje", resenaPuntaje)
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

            // Reseñas section
            if (resenas.isNotEmpty()) {
                Text("RESEÑAS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                resenas.forEach { r -> EResenaCard(r) }
            }

            // Calificaciones section
            if (calificaciones.isNotEmpty()) {
                Text("CALIFICACIONES", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                calificaciones.take(10).forEach { c -> ECalifCard(c) }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun EStatBox(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun EInfoRow(icon: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(icon, fontSize = 13.sp, modifier = Modifier.padding(top = 1.dp, end = 6.dp))
        Text(text, color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun ESocialChip(label: String, onClick: () -> Unit) {
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

@Composable
private fun EStarSelector(selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..5).forEach { i ->
            Icon(
                painterResource(R.drawable.ic_star), null,
                tint     = if (i <= selected) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(30.dp).clickable { onSelect(i) }
            )
        }
    }
}

@Composable
private fun EResenaCard(r: JSONObject) {
    val context     = LocalContext.current
    val puntaje     = r.optInt("puntaje", 0)
    val autorNombre = r.strE("autor_nombre")
    val autorFoto   = r.strE("autor_foto_key")
    val fecha       = r.optLong("fecha", 0L)
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
            Text(r.strE("contenido"), color = TextPrimary, fontSize = 13.sp)
            val respuesta = r.strE("respuesta")
            if (respuesta.isNotBlank()) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Text("Respuesta:", color = BrandBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(respuesta, color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ECalifCard(c: JSONObject) {
    val puntaje   = c.optDouble("puntaje", 0.0)
    val comentario = c.strE("comentario")
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
