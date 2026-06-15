package com.verion.practicas.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.verion.practicas.ui.components.PillTabRow
import com.verion.practicas.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

private fun JSONObject.strOrNull(key: String): String? =
    if (isNull(key)) null else optString(key, "").takeIf { it.isNotBlank() }

private fun JSONArray?.toObjectList(): List<JSONObject> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { runCatching { getJSONObject(it) }.getOrNull() }
}

private fun colabDateLabel(ts: Long): String {
    if (ts <= 0L) return ""
    val MESES = arrayOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
    val cal = Calendar.getInstance().apply { timeInMillis = ts * 1000L }
    return "${cal.get(Calendar.DAY_OF_MONTH)} ${MESES[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}"
}

@Composable
fun ProfileScreen(
    isGuest: Boolean,
    tokenManager: TokenManager,
    onLoginRequest: () -> Unit,
    onLogout: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onCVRequest: () -> Unit = {},
    onNotificaciones: () -> Unit = {}
) {
    val email    = if (isGuest) "—" else tokenManager.getUserEmail() ?: "—"
    val rol      = if (isGuest) "Invitado" else tokenManager.getUserRol() ?: "—"
    val rolLabel = rolLabel(rol)

    var perfilJson by remember { mutableStateOf<JSONObject?>(null) }
    var isLoading  by remember { mutableStateOf(!isGuest) }

    var colabs        by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loadingColabs by remember { mutableStateOf(false) }
    var colabsLoaded  by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!isGuest) {
            var token = tokenManager.getAccessToken()
            if (token != null) {
                var result = ApiClient.get("/api/perfil/me", token)
                if (!result.success && result.isUnauthorized) {
                    val newToken = tokenManager.getRefreshToken()?.let { ApiClient.refreshToken(it) }
                    if (newToken != null) {
                        tokenManager.saveAccessToken(newToken)
                        result = ApiClient.get("/api/perfil/me", newToken)
                    } else {
                        onLoginRequest(); return@LaunchedEffect
                    }
                }
                if (result.success) perfilJson = result.data?.optJSONObject("perfil")
            }
            isLoading = false
        }
    }

    val nombre = when {
        perfilJson != null && rol == "TECNICO" ->
            perfilJson!!.optString("nombre_completo").takeIf { it.isNotBlank() } ?: email
        perfilJson != null && rol == "EMPRESA"  ->
            perfilJson!!.optString("razon_social").takeIf { it.isNotBlank() } ?: email
        isGuest -> "Invitado"
        else    -> email
    }
    val initial      = nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val fotoKey      = perfilJson?.strOrNull("foto_key")
    val logoKey      = perfilJson?.strOrNull("logo_key")
    val calif        = perfilJson?.optDouble("calificacion_promedio", 0.0) ?: 0.0
    val totalCol     = perfilJson?.optInt("total_colaboraciones", 0) ?: 0
    val totalCon     = perfilJson?.optInt("total_contrataciones", 0) ?: 0
    val nivel        = perfilJson?.optString("nivel")?.takeIf { it.isNotBlank() } ?: ""
    val disponi      = perfilJson?.optString("disponibilidad")?.takeIf { it.isNotBlank() } ?: ""
    val sector       = perfilJson?.optString("sector")?.takeIf { it.isNotBlank() } ?: ""
    val descripcion  = perfilJson?.strOrNull("descripcion")
    val ruc          = perfilJson?.strOrNull("ruc")
    val sitioWeb     = perfilJson?.strOrNull("sitio_web")
    val emailProf    = perfilJson?.strOrNull("email_profesional")
    val whatsapp     = perfilJson?.strOrNull("whatsapp")
    val linkedinUrl  = perfilJson?.strOrNull("linkedin_url")

    val completion = when {
        isGuest || isLoading -> 0f
        else -> {
            var pts = 0f
            if (email.isNotBlank() && email != "—") pts += 0.20f
            if (rol != "Invitado" && rol != "—") pts += 0.10f
            if (perfilJson != null) {
                if (rol == "TECNICO") {
                    if (perfilJson!!.optString("nombre_completo").isNotBlank()) pts += 0.20f
                    if (perfilJson!!.optString("descripcion").isNotBlank())     pts += 0.20f
                    if (perfilJson!!.optString("email_profesional").isNotBlank()) pts += 0.10f
                    if (perfilJson!!.optString("github_url").isNotBlank() ||
                        perfilJson!!.optString("linkedin_url").isNotBlank()) pts += 0.20f
                } else {
                    if (!logoKey.isNullOrBlank())                                  pts += 0.10f
                    if (perfilJson!!.optString("razon_social").isNotBlank())       pts += 0.20f
                    if (perfilJson!!.optString("descripcion").isNotBlank())        pts += 0.20f
                    if (perfilJson!!.optString("email_profesional").isNotBlank())  pts += 0.10f
                    if (perfilJson!!.optString("ruc").isNotBlank())                pts += 0.10f
                }
            }
            pts.coerceIn(0f, 1f)
        }
    }

    var selectedTab  by remember { mutableIntStateOf(0) }
    var menuExpanded by remember { mutableStateOf(false) }

    // Load colaboraciones lazily when tab 1 is first opened
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1 && !colabsLoaded && !isGuest) {
            loadingColabs = true
            val token = tokenManager.getAccessToken() ?: return@LaunchedEffect
            val r = ApiClient.get("/api/colaboraciones", token)
            if (r.success) colabs = r.dataArray.toObjectList()
            loadingColabs = false
            colabsLoaded = true
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Mi Perfil", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(painterResource(R.drawable.ic_menu_hamburger), contentDescription = "Menú", tint = Color.White.copy(alpha = 0.75f))
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text    = { Text("✏️  Editar Perfil") },
                        onClick = { menuExpanded = false; onEditProfile() }
                    )
                    if (rol == "TECNICO") {
                        DropdownMenuItem(
                            text    = { Text("📄  Mi CV") },
                            onClick = { menuExpanded = false; onCVRequest() }
                        )
                    }
                    if (!isGuest) {
                        DropdownMenuItem(
                            text    = { Text("🔔  Notificaciones") },
                            onClick = { menuExpanded = false; onNotificaciones() }
                        )
                    }
                    DropdownMenuItem(
                        text    = { Text("⚙️  Configuración") },
                        onClick = { menuExpanded = false }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.12f))
                    DropdownMenuItem(
                        text    = { Text("🚪  Cerrar sesión", color = Color(0xFFF87171)) },
                        onClick = { menuExpanded = false; onLogout() }
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        PillTabRow(
            tabs          = listOf("Perfil", "Colaboraciones"),
            selectedIndex = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier      = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(4.dp))

        when (selectedTab) {
            0 -> ProfileTabContent(
                    initial     = initial,    nombre      = nombre,    rol       = rol,
                    rolLabel    = rolLabel,   isGuest     = isGuest,   isLoading = isLoading,
                    completion  = completion, calif       = calif,     totalCol  = totalCol,
                    totalCon    = totalCon,   nivel       = nivel,     disponi   = disponi,
                    sector      = sector,     descripcion = descripcion, ruc     = ruc,
                    sitioWeb    = sitioWeb,   emailProf   = emailProf, whatsapp  = whatsapp,
                    linkedinUrl = linkedinUrl, fotoKey    = fotoKey,   logoKey   = logoKey,
                    onLoginRequest = onLoginRequest, onEditProfile = onEditProfile
                 )
            1 -> ColaboracionesTab(
                    isGuest      = isGuest,
                    isLoading    = loadingColabs,
                    colabs       = colabs,
                    rol          = rol,
                    totalCol     = totalCol,
                    totalCon     = totalCon
                 )
        }
    }
}

// ── Tab 0: datos del perfil ──────────────────────────────────────────────────

@Composable
private fun ProfileTabContent(
    initial: String, nombre: String, rol: String, rolLabel: String,
    isGuest: Boolean, isLoading: Boolean, completion: Float,
    calif: Double, totalCol: Int, totalCon: Int,
    nivel: String, disponi: String, sector: String,
    descripcion: String?, ruc: String?, sitioWeb: String?,
    emailProf: String?, whatsapp: String?, linkedinUrl: String?,
    fotoKey: String?, logoKey: String?,
    onLoginRequest: () -> Unit, onEditProfile: () -> Unit
) {
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BrandBlue, strokeWidth = 2.dp)
        }
        return
    }

    val context     = LocalContext.current
    val avatarShape = if (rol == "EMPRESA") RoundedCornerShape(18.dp) else CircleShape
    val imageKey    = if (rol == "EMPRESA") logoKey else fotoKey

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        // Avatar + nombre
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.size(96.dp).clip(avatarShape), contentAlignment = Alignment.Center) {
                    if (!imageKey.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data("${ApiClient.BASE_URL}/api/uploads/$imageKey").crossfade(true).build(),
                            contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(OrbPurple, Color(0xFF9B6EFF)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(initial, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(nombre, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                if (!isGuest && rol == "TECNICO" && nivel.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(nivelLabel(nivel), color = TextSecondary, fontSize = 13.sp)
                }
                if (!isGuest && rol == "EMPRESA" && sector.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(sector, color = TextSecondary, fontSize = 13.sp)
                }
                Spacer(Modifier.height(10.dp))
                GlassCard(cornerRadius = 50.dp) {
                    Text(rolLabel, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                }
            }
        }

        // Encabezado sección
        item {
            Text("INFORMACIÓN", color = Color.White.copy(alpha = 0.38f), fontSize = 10.sp, letterSpacing = 1.5.sp, modifier = Modifier.padding(bottom = 10.dp))
        }

        // Calificación
        item {
            val califText = if (calif > 0.0) "${"%.1f".format(calif)} ★  (calificación promedio)" else "Sin calificaciones aún"
            InfoRow(R.drawable.ic_star, califText, "Calificación", primaryAlpha = if (calif > 0.0) 1f else 0.42f)
            Spacer(Modifier.height(10.dp))
        }

        // Colaboraciones / contrataciones
        if (!isGuest && rol == "TECNICO") {
            item {
                InfoRow(
                    R.drawable.ic_nav_person,
                    if (totalCol > 0) "$totalCol colaboraciones completadas" else "Sin colaboraciones aún",
                    "Experiencia en la plataforma", primaryAlpha = if (totalCol > 0) 1f else 0.42f
                )
                Spacer(Modifier.height(10.dp))
            }
            if (disponi.isNotBlank()) {
                item {
                    InfoRow(R.drawable.ic_location, disponibilidadLabel(disponi), "Disponibilidad")
                    Spacer(Modifier.height(10.dp))
                }
            }
        }

        if (!isGuest && rol == "EMPRESA") {
            item {
                InfoRow(
                    R.drawable.ic_nav_building,
                    if (totalCon > 0) "$totalCon contrataciones realizadas" else "Sin contrataciones aún",
                    "Historial en la plataforma", primaryAlpha = if (totalCon > 0) 1f else 0.42f
                )
                Spacer(Modifier.height(10.dp))
            }
            if (!descripcion.isNullOrBlank()) {
                item { InfoRow(R.drawable.ic_nav_building, descripcion, "Descripción"); Spacer(Modifier.height(10.dp)) }
            }
            if (!ruc.isNullOrBlank()) {
                item { InfoRow(R.drawable.ic_nav_person, ruc, "RUC"); Spacer(Modifier.height(10.dp)) }
            }
            if (!sitioWeb.isNullOrBlank()) {
                item { InfoRow(R.drawable.ic_nav_search, sitioWeb, "Sitio web"); Spacer(Modifier.height(10.dp)) }
            }
            if (!emailProf.isNullOrBlank()) {
                item { InfoRow(R.drawable.ic_nav_person, emailProf, "Email de contacto"); Spacer(Modifier.height(10.dp)) }
            }
            if (!linkedinUrl.isNullOrBlank()) {
                item { InfoRow(R.drawable.ic_nav_person, linkedinUrl, "LinkedIn"); Spacer(Modifier.height(10.dp)) }
            }
            if (!whatsapp.isNullOrBlank()) {
                item { InfoRow(R.drawable.ic_nav_person, whatsapp, "WhatsApp"); Spacer(Modifier.height(10.dp)) }
            }
        }

        item { InfoRow(R.drawable.ic_nav_person, rolLabel, "Tipo de cuenta"); Spacer(Modifier.height(20.dp)) }

        // Tarjeta de completitud / CTA
        item {
            CompletionCard(
                completion    = completion,
                isGuest       = isGuest,
                rol           = rol,
                onCta         = if (isGuest) onLoginRequest else onEditProfile
            )
        }
    }
}

// ── Tab 1: colaboraciones ────────────────────────────────────────────────────

@Composable
private fun ColaboracionesTab(
    isGuest: Boolean,
    isLoading: Boolean,
    colabs: List<JSONObject>,
    rol: String,
    totalCol: Int,
    totalCon: Int
) {
    if (isGuest) {
        EmptyTabState(R.drawable.ic_nav_building, "Inicia sesión", "Para ver tus colaboraciones necesitas una cuenta")
        return
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BrandBlue, strokeWidth = 2.dp)
        }
        return
    }

    if (colabs.isEmpty()) {
        EmptyTabState(
            R.drawable.ic_nav_building,
            "Sin colaboraciones aún",
            if (rol == "TECNICO") "Cuando completes proyectos con empresas, aparecerán aquí"
            else "Cuando gestiones colaboraciones con técnicos, aparecerán aquí"
        )
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        // Resumen de estadísticas
        item {
            val count = if (rol == "TECNICO") totalCol else totalCon
            val label = if (rol == "TECNICO") "colaboraciones completadas" else "contrataciones realizadas"
            GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp), cornerRadius = 16.dp) {
                Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("$count", color = BrandBlue, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(12.dp))
                    Text(label, color = TextSecondary, fontSize = 13.sp)
                }
            }
        }

        // Lista de colaboraciones
        items(colabs, key = { it.optString("id") }) { colab ->
            ColabCard(colab)
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ColabCard(colab: JSONObject) {
    val estado        = colab.optString("estado", "")
    val fechaInicioTs = if (colab.isNull("fecha_inicio")) 0L else colab.optLong("fecha_inicio", 0L)

    val estadoColor = when (estado) {
        "EN_CURSO"   -> Color(0xFF34D399)
        "FINALIZADA" -> BrandBlue
        "CANCELADA"  -> Color(0xFFF87171)
        else         -> Color.White
    }
    val estadoLabel = when (estado) {
        "EN_CURSO"   -> "En curso"
        "FINALIZADA" -> "Finalizada"
        "CANCELADA"  -> "Cancelada"
        else         -> estado
    }

    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(estadoColor))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Colaboración", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (fechaInicioTs > 0L) {
                    Text("Inicio: ${colabDateLabel(fechaInicioTs)}", color = TextSecondary, fontSize = 11.sp)
                }
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(estadoColor.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(estadoLabel, color = estadoColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Componentes compartidos ──────────────────────────────────────────────────

@Composable
private fun CompletionCard(completion: Float, isGuest: Boolean, rol: String = "TECNICO", onCta: () -> Unit) {
    val animatedProgress by animateFloatAsState(targetValue = completion, animationSpec = tween(1200, easing = FastOutSlowInEasing), label = "profile_progress")
    val pct = (animatedProgress * 100).toInt()

    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (isGuest) "Únete a Veri-on" else "Completa tu perfil", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (!isGuest) Text("$pct%", color = BrandBlue, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            if (!isGuest) {
                Spacer(Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.10f))) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(animatedProgress.coerceIn(0f, 1f)).clip(RoundedCornerShape(3.dp)).background(Brush.horizontalGradient(listOf(BrandBlue, BrandIndigo, BrandPurple))))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                if (isGuest) "Inicia sesión para acceder a todas las funciones de la plataforma"
                else if (rol == "EMPRESA") "Agrega logo, descripción y datos de contacto\npara que los técnicos te encuentren fácilmente"
                else "Agrega descripción, redes sociales y disponibilidad\npara que las empresas te encuentren fácilmente",
                color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick  = onCta,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(50.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1A0F3D))
            ) {
                Text(if (isGuest) "Iniciar sesión" else "Editar Perfil", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun InfoRow(iconRes: Int, primary: String, secondary: String, primaryAlpha: Float = 1f) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Brush.linearGradient(listOf(BrandBlue, BrandPurple))),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(iconRes), contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(primary, color = Color.White.copy(alpha = primaryAlpha), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(secondary, color = TextSecondary.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun EmptyTabState(iconRes: Int, title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxSize().padding(top = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        GlassCard(modifier = Modifier.size(110.dp), cornerRadius = 55.dp) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(painterResource(iconRes), contentDescription = null, tint = Color.White.copy(alpha = 0.22f), modifier = Modifier.size(48.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(title,    color = Color.White.copy(alpha = 0.55f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = Color.White.copy(alpha = 0.35f), fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun rolLabel(rol: String) = when (rol) {
    "TECNICO"  -> "Técnico / Practicante"
    "EMPRESA"  -> "Empresa / MiPyme"
    "Invitado" -> "Invitado"
    else       -> "—"
}

private fun nivelLabel(nivel: String) = when (nivel) {
    "PRACTICANTE" -> "Practicante"
    "EGRESADO"    -> "Egresado"
    "CERTIFICADO" -> "Certificado"
    else          -> nivel
}

private fun disponibilidadLabel(d: String) = when (d) {
    "INMEDIATA"     -> "Disponible de inmediato"
    "FECHA"         -> "Disponible a partir de una fecha"
    "NO_DISPONIBLE" -> "No disponible actualmente"
    else            -> d
}
