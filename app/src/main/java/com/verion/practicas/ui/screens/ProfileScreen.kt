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
import com.verion.practicas.ui.components.GlassCardGradient
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
    val m = arrayOf("Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic")
    val cal = Calendar.getInstance().apply { timeInMillis = ts * 1000L }
    return "${cal.get(Calendar.DAY_OF_MONTH)} ${m[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}"
}

private data class ContactInfo(val iconRes: Int, val label: String, val value: String)


@Composable
fun ProfileScreen(
    isGuest: Boolean,
    tokenManager: TokenManager,
    onLoginRequest: () -> Unit,
    onLogout: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onCVRequest: () -> Unit = {},
    onNotificaciones: () -> Unit = {},
    onTecnicoClick: (String) -> Unit = {},
    onEmpresaClick: (String) -> Unit = {},
    onSettings: () -> Unit = {}
) {
    val email    = if (isGuest) "—" else tokenManager.getUserEmail() ?: "—"
    val rol      = if (isGuest) "Invitado" else tokenManager.getUserRol() ?: "—"
    val rolLabel = rolLabel(rol)

    var perfilJson    by remember { mutableStateOf<JSONObject?>(null) }
    var isLoading     by remember { mutableStateOf(!isGuest) }
    var habilidades   by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var favoritos     by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loadingFavs   by remember { mutableStateOf(false) }
    var favsLoaded    by remember { mutableStateOf(false) }
    var colabs        by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loadingColabs by remember { mutableStateOf(false) }
    var colabsLoaded  by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!isGuest) {
            var token = tokenManager.getAccessToken()
            if (token != null) {
                var result = ApiClient.get("/api/perfil/me", token)
                if (!result.success && result.isUnauthorized) {
                    val nt = tokenManager.getRefreshToken()?.let { ApiClient.refreshToken(it) }
                    if (nt != null) { tokenManager.saveAccessToken(nt); token = nt; result = ApiClient.get("/api/perfil/me", nt) }
                    else { onLoginRequest(); return@LaunchedEffect }
                }
                if (result.success) perfilJson = result.data?.optJSONObject("perfil")
                if (rol == "TECNICO") {
                    val cv = ApiClient.get("/api/cv/mio", token)
                    if (cv.success) habilidades = cv.data?.optJSONArray("habilidades")?.toObjectList() ?: emptyList()
                }
            }
            isLoading = false
        }
    }

    val nombre = when {
        perfilJson != null && rol == "TECNICO" -> perfilJson!!.optString("nombre_completo").takeIf { it.isNotBlank() } ?: email
        perfilJson != null && rol == "EMPRESA"  -> perfilJson!!.optString("razon_social").takeIf { it.isNotBlank() } ?: email
        isGuest -> "Bienvenido"
        else    -> email
    }
    val initial     = nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val fotoKey     = perfilJson?.strOrNull("foto_key")
    val logoKey     = perfilJson?.strOrNull("logo_key")
    val calif       = perfilJson?.optDouble("calificacion_promedio", 0.0) ?: 0.0
    val totalCol    = perfilJson?.optInt("total_colaboraciones", 0) ?: 0
    val totalCon    = perfilJson?.optInt("total_contrataciones", 0) ?: 0
    val nivel       = perfilJson?.optString("nivel")?.takeIf { it.isNotBlank() } ?: ""
    val disponi     = perfilJson?.optString("disponibilidad")?.takeIf { it.isNotBlank() } ?: ""
    val sector      = perfilJson?.optString("sector")?.takeIf { it.isNotBlank() } ?: ""
    val descripcion = perfilJson?.strOrNull("descripcion")
    val ruc         = perfilJson?.strOrNull("ruc")
    val sitioWeb    = perfilJson?.strOrNull("sitio_web")
    val emailProf   = perfilJson?.strOrNull("email_profesional")
    val whatsapp    = perfilJson?.strOrNull("whatsapp")
    val linkedinUrl = perfilJson?.strOrNull("linkedin_url")

    val completion = when {
        isGuest || isLoading -> 0f
        else -> {
            var pts = 0f
            if (email.isNotBlank() && email != "—") pts += 0.20f
            if (rol !in listOf("Invitado", "—")) pts += 0.10f
            if (perfilJson != null) {
                if (rol == "TECNICO") {
                    if (perfilJson!!.optString("nombre_completo").isNotBlank())  pts += 0.20f
                    if (perfilJson!!.optString("descripcion").isNotBlank())       pts += 0.20f
                    if (perfilJson!!.optString("email_profesional").isNotBlank()) pts += 0.10f
                    if (perfilJson!!.optString("github_url").isNotBlank() ||
                        perfilJson!!.optString("linkedin_url").isNotBlank())      pts += 0.20f
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
    val context = LocalContext.current

    LaunchedEffect(selectedTab) {
        if (isGuest) return@LaunchedEffect
        val token = tokenManager.getAccessToken() ?: return@LaunchedEffect
        when (selectedTab) {
            1 -> if (!favsLoaded) {
                loadingFavs = true
                val r = ApiClient.get("/api/favoritos", token)
                if (r.success) favoritos = r.dataArray.toObjectList()
                loadingFavs = false; favsLoaded = true
            }
            2 -> if (!colabsLoaded) {
                loadingColabs = true
                val r = ApiClient.get("/api/colaboraciones", token)
                if (r.success) colabs = r.dataArray.toObjectList()
                loadingColabs = false; colabsLoaded = true
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {

            if (selectedTab == 0) {
                Box(Modifier.fillMaxWidth().height(265.dp)) {
                    when {
                        rol == "EMPRESA" -> {
                            Box(
                                Modifier.fillMaxSize()
                                    .background(Brush.verticalGradient(listOf(Color(0xFF0D0122), Color(0xFF050515)))),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!logoKey.isNullOrBlank()) {
                                    Box(Modifier.size(110.dp).clip(RoundedCornerShape(20.dp))) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data("${ApiClient.BASE_URL}/api/uploads/$logoKey")
                                                .crossfade(true).build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                } else {
                                    Text(initial, color = Color.White.copy(alpha = 0.06f), fontSize = 130.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                        !fotoKey.isNullOrBlank() -> {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data("${ApiClient.BASE_URL}/api/uploads/$fotoKey")
                                    .crossfade(true).build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> {
                            Box(
                                Modifier.fillMaxSize()
                                    .background(Brush.verticalGradient(listOf(Color(0xFF1A0040), Color(0xFF050515)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(initial, color = Color.White.copy(alpha = 0.06f), fontSize = 130.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Box(Modifier.fillMaxSize().background(
                        Brush.verticalGradient(0f to Color.Transparent, 0.45f to Color.Transparent, 1f to Color.Black)
                    ))
                    Box(Modifier.fillMaxSize().background(
                        Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.45f), 0.3f to Color.Transparent)
                    ))

                    if (!isLoading) {
                        Row(
                            Modifier.fillMaxWidth().align(Alignment.BottomStart)
                                .padding(start = 20.dp, end = 16.dp, bottom = 18.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    nombre,
                                    color = Color.White,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Black,
                                    lineHeight = 34.sp
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        Modifier.clip(RoundedCornerShape(50.dp))
                                            .background(BrandBlue.copy(alpha = 0.22f))
                                            .border(1.dp, BrandBlue.copy(alpha = 0.5f), RoundedCornerShape(50.dp))
                                            .padding(horizontal = 10.dp, vertical = 3.dp)
                                    ) { Text(rolLabel, color = BrandBlue, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                                    if (nivel.isNotBlank()) {
                                        Text("·", color = Color.White.copy(alpha = 0.28f), fontSize = 11.sp)
                                        Text(nivelLabel(nivel), color = Color.White.copy(alpha = 0.48f), fontSize = 11.sp)
                                    }
                                    if (sector.isNotBlank()) {
                                        Text("·", color = Color.White.copy(alpha = 0.28f), fontSize = 11.sp)
                                        Text(sector, color = Color.White.copy(alpha = 0.48f), fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                            }
                            if (!isGuest) {
                                Box(
                                    Modifier.clip(RoundedCornerShape(50.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(50.dp))
                                        .clickable { onEditProfile() }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) { Text("Editar", color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                            }
                        }
                    }
                }

                if (!isLoading && !isGuest) {
                    Row(
                        Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.18f)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileStatItem(
                            value = if (calif > 0.0) "%.1f★".format(calif) else "—",
                            label = "Calificación",
                            modifier = Modifier.weight(1f)
                        )
                        Box(Modifier.width(1.dp).height(26.dp).background(Color.White.copy(alpha = 0.10f)))
                        ProfileStatItem(
                            value = if (rol == "TECNICO") "$totalCol" else "$totalCon",
                            label = if (rol == "TECNICO") "Trabajos" else "Contratos",
                            modifier = Modifier.weight(1f)
                        )
                        if (rol == "TECNICO") {
                            Box(Modifier.width(1.dp).height(26.dp).background(Color.White.copy(alpha = 0.10f)))
                            ProfileStatItem(
                                value = "${habilidades.size}",
                                label = "Habilidades",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.07f))
                }

            } else {
                Box(
                    Modifier.fillMaxWidth().statusBarsPadding().height(52.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text("Mi Perfil", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 20.dp))
                }
            }

            PillTabRow(
                tabs          = listOf("Perfil", "Favoritos", "Colaboraciones"),
                selectedIndex = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier      = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
            )

            Box(Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> ProfileTabContent(
                            isLoading      = isLoading,
                            isGuest        = isGuest,
                            rol            = rol,
                            descripcion    = descripcion,
                            habilidades    = habilidades,
                            emailProf      = emailProf,
                            whatsapp       = whatsapp,
                            linkedinUrl    = linkedinUrl,
                            sitioWeb       = sitioWeb,
                            ruc            = ruc,
                            disponi        = disponi,
                            completion     = completion,
                            onLoginRequest = onLoginRequest,
                            onEditProfile  = onEditProfile
                        )
                    1 -> FavoritosTab(isGuest, loadingFavs, favoritos, onTecnicoClick, onEmpresaClick)
                    2 -> ColaboracionesTab(isGuest, loadingColabs, colabs, rol, totalCol, totalCon)
                }
            }
        }

        Box(
            Modifier.fillMaxWidth().statusBarsPadding(),
            contentAlignment = Alignment.TopEnd
        ) {
            Box(Modifier.padding(top = 2.dp, end = 4.dp)) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        painterResource(R.drawable.ic_menu_hamburger),
                        contentDescription = "Menú",
                        tint = Color.White.copy(alpha = if (selectedTab == 0) 0.92f else 0.70f)
                    )
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("✏️  Editar Perfil") }, onClick = { menuExpanded = false; onEditProfile() })
                    if (rol == "TECNICO") DropdownMenuItem(text = { Text("📄  Mi CV") }, onClick = { menuExpanded = false; onCVRequest() })
                    if (!isGuest) DropdownMenuItem(text = { Text("🔔  Notificaciones") }, onClick = { menuExpanded = false; onNotificaciones() })
                    DropdownMenuItem(text = { Text("⚙️  Configuración") }, onClick = { menuExpanded = false; onSettings() })
                    HorizontalDivider(Modifier.padding(horizontal = 8.dp), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.12f))
                    DropdownMenuItem(text = { Text("🚪  Cerrar sesión", color = Color(0xFFF87171)) }, onClick = { menuExpanded = false; onLogout() })
                }
            }
        }
    }
}

@Composable
private fun ProfileTabContent(
    isLoading: Boolean,
    isGuest: Boolean,
    rol: String,
    descripcion: String?,
    habilidades: List<JSONObject>,
    emailProf: String?,
    whatsapp: String?,
    linkedinUrl: String?,
    sitioWeb: String?,
    ruc: String?,
    disponi: String,
    completion: Float,
    onLoginRequest: () -> Unit,
    onEditProfile: () -> Unit
) {
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BrandBlue, strokeWidth = 2.dp)
        }
        return
    }

    val contacts = buildList {
        if (!emailProf.isNullOrBlank()) add(ContactInfo(R.drawable.ic_nav_person, "Email de contacto", emailProf))
        if (!whatsapp.isNullOrBlank()) add(ContactInfo(R.drawable.ic_nav_person, "WhatsApp", whatsapp))
        if (!linkedinUrl.isNullOrBlank()) add(ContactInfo(R.drawable.ic_nav_person, "LinkedIn", linkedinUrl))
        if (!sitioWeb.isNullOrBlank()) add(ContactInfo(R.drawable.ic_nav_search, "Sitio web", sitioWeb))
        if (!ruc.isNullOrBlank()) add(ContactInfo(R.drawable.ic_nav_building, "RUC", ruc))
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {

        if (!descripcion.isNullOrBlank()) {
            item {
                Text(
                    descripcion,
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
                )
            }
        }

        if (rol == "TECNICO" && disponi.isNotBlank()) {
            item {
                val dotColor = when (disponi) {
                    "INMEDIATA"     -> Color(0xFF34D399)
                    "NO_DISPONIBLE" -> Color(0xFFF87171)
                    else            -> Color(0xFFFBBF24)
                }
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(dotColor))
                    Spacer(Modifier.width(8.dp))
                    Text(disponibilidadLabel(disponi), color = Color.White.copy(alpha = 0.48f), fontSize = 13.sp)
                }
            }
        }

        if (rol == "TECNICO" && habilidades.isNotEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
                    Text("HABILIDADES", color = Color.White.copy(alpha = 0.28f), fontSize = 10.sp,
                        letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    habilidades.chunked(3).forEach { row ->
                        Row(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { h ->
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(Color.White.copy(alpha = 0.07f))
                                        .border(1.dp, Color.White.copy(alpha = 0.11f), RoundedCornerShape(50.dp))
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) { Text(h.optString("nombre",""), color = Color.White.copy(alpha = 0.68f), fontSize = 12.sp) }
                            }
                        }
                    }
                }
            }
        }

        if (contacts.isNotEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    Text("CONTACTO", color = Color.White.copy(alpha = 0.28f), fontSize = 10.sp,
                        letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    GlassCard(Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                        Column(Modifier.fillMaxWidth()) {
                            contacts.forEachIndexed { idx, c ->
                                if (idx > 0) HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.07f))
                                ProfileContactRow(c.iconRes, c.label, c.value)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }

        when {
            isGuest -> item {
                Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    CompletionCard(0f, isGuest = true, onCta = onLoginRequest)
                }
            }
            completion < 0.30f -> item {
                Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    CompletionCard(completion, isGuest = false, rol = rol, onCta = onEditProfile)
                }
            }
        }
    }
}

@Composable
private fun ProfileStatItem(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
        Spacer(Modifier.height(1.dp))
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun ProfileContactRow(iconRes: Int, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(BrandBlue.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) { Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(14.dp), tint = BrandBlue) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = TextSecondary, fontSize = 10.sp)
            Text(value, color = TextPrimary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun CompletionCard(completion: Float, isGuest: Boolean, rol: String = "TECNICO", onCta: () -> Unit) {
    val anim by animateFloatAsState(completion, animationSpec = tween(1200, easing = FastOutSlowInEasing), label = "prog")
    GlassCardGradient(Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (isGuest) "Únete a Veri-on" else "Completa tu perfil", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                if (!isGuest) Text("${(anim * 100).toInt()}%", color = BrandBlue, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            if (!isGuest) {
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.08f))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(anim.coerceIn(0f,1f)).clip(RoundedCornerShape(3.dp))
                        .background(Brush.horizontalGradient(listOf(BrandBlue, BrandIndigo, BrandPurple))))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                if (isGuest) "Inicia sesión para acceder a todas las funciones"
                else if (rol == "EMPRESA") "Agrega logo, descripción y datos de contacto"
                else "Agrega descripción, redes sociales y disponibilidad",
                color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 17.sp
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick  = onCta,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape    = RoundedCornerShape(50.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = Color.White)
            ) { Text(if (isGuest) "Iniciar sesión" else "Editar perfil", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun FavoritosTab(
    isGuest: Boolean,
    isLoading: Boolean,
    favoritos: List<JSONObject>,
    onTecnicoClick: (String) -> Unit,
    onEmpresaClick: (String) -> Unit
) {
    if (isGuest) { EmptyTabState(R.drawable.ic_star, "Inicia sesión", "Para ver tus favoritos necesitas una cuenta"); return }
    if (isLoading) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BrandBlue, strokeWidth = 2.dp) }; return }
    if (favoritos.isEmpty()) { EmptyTabState(R.drawable.ic_star, "Sin favoritos aún", "Guarda técnicos o empresas para verlos aquí"); return }

    val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
        items(favoritos, key = { it.optString("id") }) { fav ->
            val tipo       = fav.optString("tipo", "")
            val objId      = fav.optString("objetivo_id", "")
            val nombre     = if (fav.isNull("objetivo_nombre")) "" else fav.optString("objetivo_nombre", "")
            val fotoKey    = if (fav.isNull("objetivo_foto_key")) "" else fav.optString("objetivo_foto_key", "")
            val calif      = fav.optDouble("objetivo_calificacion", 0.0)
            val nivel      = if (fav.isNull("objetivo_nivel")) "" else fav.optString("objetivo_nivel", "")
            val sector     = if (fav.isNull("objetivo_sector")) "" else fav.optString("objetivo_sector", "")
            val isTec      = tipo == "TECNICO_GUARDADO"
            val initial    = nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            val shape      = if (isTec) CircleShape else RoundedCornerShape(12.dp)
            val grad       = if (isTec) listOf(OrbPurple, Color(0xFF9B6EFF)) else listOf(BrandBlue, BrandIndigo)

            GlassCard(Modifier.fillMaxWidth().clickable { if (isTec) onTecnicoClick(objId) else onEmpresaClick(objId) }) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(shape).background(Brush.linearGradient(grad)), contentAlignment = Alignment.Center) {
                        if (fotoKey.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data("${ApiClient.BASE_URL}/api/uploads/$fotoKey").crossfade(true).build(),
                                contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(shape)
                            )
                        } else Text(initial, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(nombre.ifBlank { "—" }, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        val detail = buildString {
                            val sub = if (isTec) nivelLabel(nivel) else sector
                            if (sub.isNotBlank()) append(sub)
                            if (calif > 0.0) { if (isNotEmpty()) append("  ·  "); append("★ ${"%.1f".format(calif)}") }
                        }
                        if (detail.isNotBlank()) { Spacer(Modifier.height(2.dp)); Text(detail, color = BrandBlue, fontSize = 11.sp) }
                    }
                    Box(Modifier.clip(RoundedCornerShape(50.dp)).background(if (isTec) OrbPurple.copy(alpha=0.18f) else BrandBlue.copy(alpha=0.18f)).padding(horizontal=8.dp, vertical=3.dp)) {
                        Text(if (isTec) "Técnico" else "Empresa", color = if (isTec) OrbPurple else BrandBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("›", color = TextSecondary, fontSize = 20.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ColaboracionesTab(
    isGuest: Boolean, isLoading: Boolean, colabs: List<JSONObject>,
    rol: String, totalCol: Int, totalCon: Int
) {
    if (isGuest) { EmptyTabState(R.drawable.ic_nav_building, "Inicia sesión", "Para ver tus colaboraciones necesitas una cuenta"); return }
    if (isLoading) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BrandBlue, strokeWidth = 2.dp) }; return }
    if (colabs.isEmpty()) {
        EmptyTabState(R.drawable.ic_nav_building, "Sin colaboraciones aún",
            if (rol == "TECNICO") "Cuando completes proyectos con empresas, aparecerán aquí"
            else "Cuando gestiones colaboraciones con técnicos, aparecerán aquí")
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
        item {
            val count = if (rol == "TECNICO") totalCol else totalCon
            val label = if (rol == "TECNICO") "colaboraciones completadas" else "contrataciones realizadas"
            GlassCardGradient(Modifier.fillMaxWidth().padding(bottom = 14.dp), cornerRadius = 16.dp) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("$count", color = BrandBlue, style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.width(12.dp))
                    Text(label, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        items(colabs, key = { it.optString("id") }) { colab ->
            ColabCard(colab)
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ColabCard(colab: JSONObject) {
    val estado = colab.optString("estado", "")
    val ts     = if (colab.isNull("fecha_inicio")) 0L else colab.optLong("fecha_inicio", 0L)
    val color  = when (estado) { "EN_CURSO" -> Color(0xFF34D399); "FINALIZADA" -> BrandBlue; "CANCELADA" -> Color(0xFFF87171); else -> Color.White }
    val label  = when (estado) { "EN_CURSO" -> "En curso"; "FINALIZADA" -> "Finalizada"; "CANCELADA" -> "Cancelada"; else -> estado }
    GlassCard(Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Colaboración", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (ts > 0L) Text("Inicio: ${colabDateLabel(ts)}", color = TextSecondary, fontSize = 11.sp)
            }
            Box(Modifier.clip(RoundedCornerShape(50.dp)).background(color.copy(alpha=0.15f)).padding(horizontal=10.dp, vertical=4.dp)) {
                Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyTabState(iconRes: Int, title: String, subtitle: String) {
    Column(Modifier.fillMaxSize().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        GlassCard(Modifier.size(100.dp), cornerRadius = 50.dp) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(44.dp), tint = Color.White.copy(alpha = 0.20f))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(title,    color = Color.White.copy(alpha = 0.52f), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = Color.White.copy(alpha = 0.32f), fontSize = 13.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp))
    }
}

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
