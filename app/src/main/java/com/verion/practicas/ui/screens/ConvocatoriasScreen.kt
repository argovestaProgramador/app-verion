package com.verion.practicas.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verion.practicas.ApiClient
import com.verion.practicas.R
import com.verion.practicas.TokenManager
import com.verion.practicas.ui.components.GlassCard
import com.verion.practicas.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

private fun JSONObject.str(key: String): String =
    if (isNull(key)) "" else optString(key, "")

private fun JSONArray?.toObjectList(): List<JSONObject> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { runCatching { getJSONObject(it) }.getOrNull() }
}

private val MESES = arrayOf("Ene", "Feb", "Mar", "Abr", "May", "Jun",
    "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")

private fun tsToDateLabel(ts: Long?): String {
    if (ts == null || ts <= 0) return ""
    val cal = Calendar.getInstance().apply { timeInMillis = ts * 1000 }
    return "${cal.get(Calendar.DAY_OF_MONTH)} ${MESES[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}"
}

private fun estadoLabel(e: String) = when (e) {
    "ABIERTA"      -> "Abierta"
    "EN_SELECCION" -> "En selección"
    "CERRADA"      -> "Cerrada"
    else           -> e
}

private fun estadoColor(e: String) = when (e) {
    "ABIERTA"      -> Color(0xFF34D399)
    "EN_SELECCION" -> Color(0xFFFBBF24)
    "CERRADA"      -> Color(0xFFF87171)
    else           -> Color.White
}

@Composable
fun ConvocatoriasScreen(
    tokenManager: TokenManager,
    onBack: (() -> Unit)?
) {
    val scope = rememberCoroutineScope()

    var isLoading    by remember { mutableStateOf(true) }
    var convocatorias by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var categorias   by remember { mutableStateOf<List<JSONObject>>(emptyList()) }

    var showForm     by remember { mutableStateOf(false) }
    var editId       by remember { mutableStateOf<String?>(null) }
    var fTitulo      by remember { mutableStateOf("") }
    var fDescripcion by remember { mutableStateOf("") }
    var fCategoriaId by remember { mutableStateOf("") }
    var fPlazas      by remember { mutableStateOf("1") }
    var fInicio      by remember { mutableStateOf<Long?>(null) }
    var fFin         by remember { mutableStateOf<Long?>(null) }
    var showCatDialog by remember { mutableStateOf(false) }
    var saving       by remember { mutableStateOf(false) }
    var msgForm      by remember { mutableStateOf<String?>(null) }

    var expandedId   by remember { mutableStateOf<String?>(null) }
    var postulaciones by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loadingPost  by remember { mutableStateOf(false) }

    var pendingDelete by remember { mutableStateOf<(() -> Unit)?>(null) }

    suspend fun authedGet(path: String): ApiClient.ApiResult {
        var token = tokenManager.getAccessToken() ?: return ApiClient.ApiResult(false, null, "Sesión expirada")
        var r = ApiClient.get(path, token)
        if (!r.success && r.isUnauthorized) {
            val nt = tokenManager.getRefreshToken()?.let { ApiClient.refreshToken(it) }
            if (nt != null) { tokenManager.saveAccessToken(nt); token = nt; r = ApiClient.get(path, nt) }
        }
        return r
    }

    LaunchedEffect(Unit) {
        val listRes = authedGet("/api/convocatorias")
        if (listRes.success) convocatorias = listRes.dataArray.toObjectList()
        val catRes = ApiClient.get("/api/categorias")
        if (catRes.success) categorias = catRes.dataArray.toObjectList()
        isLoading = false
    }

    val openForm = { conv: JSONObject? ->
        editId       = conv?.optString("id")
        fTitulo      = conv?.str("titulo") ?: ""
        fDescripcion = conv?.str("descripcion") ?: ""
        fCategoriaId = conv?.str("categoria_id") ?: ""
        fPlazas      = conv?.optInt("plazas_disponibles", 1)?.toString() ?: "1"
        fInicio      = conv?.takeIf { !it.isNull("fecha_inicio") }?.optLong("fecha_inicio")
        fFin         = conv?.takeIf { !it.isNull("fecha_fin") }?.optLong("fecha_fin")
        msgForm      = null
        showForm     = true
    }

    val categoriaNombre = categorias.find { it.str("id") == fCategoriaId }?.str("nombre") ?: ""

    // ── Diálogo categoría ──────────────────────────────
    if (showCatDialog) {
        AlertDialog(
            onDismissRequest = { showCatDialog = false },
            containerColor   = Color(0xFF1E1040),
            shape            = RoundedCornerShape(20.dp),
            title = { Text("Categoría", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text  = {
                Column(modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    categorias.forEach { cat ->
                        TextButton(
                            onClick = { fCategoriaId = cat.str("id"); showCatDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                cat.str("nombre"),
                                color = if (fCategoriaId == cat.str("id")) BrandBlue else Color.White,
                                fontWeight = if (fCategoriaId == cat.str("id")) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // ── Diálogo eliminar ───────────────────────────────
    pendingDelete?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor   = Color(0xFF1E1040),
            shape            = RoundedCornerShape(20.dp),
            title = { Text("¿Eliminar convocatoria?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text  = { Text("Esta acción no se puede deshacer.", color = TextSecondary, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { action(); pendingDelete = null }) {
                    Text("Eliminar", color = Color(0xFFF87171), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancelar", color = TextSecondary) }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = "Volver",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                "Mis Convocatorias",
                color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = if (onBack != null) 4.dp else 20.dp)
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).padding(end = 8.dp),
                    color = BrandBlue, strokeWidth = 2.dp
                )
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandBlue, strokeWidth = 2.dp)
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (showForm && editId == null) { showForm = false }
                        else openForm(null)
                    },
                    cornerRadius = 14.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            if (showForm && editId == null) "✕  Cancelar" else "+  Nueva convocatoria",
                            color = BrandBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── Formulario ──────────────────────────────
            if (showForm) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (editId != null) "Editar convocatoria" else "Nueva convocatoria",
                                    color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                if (saving) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BrandBlue, strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                }
                                TextButton(onClick = { showForm = false; editId = null }, contentPadding = PaddingValues(8.dp)) {
                                    Text("Cancelar", color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                            Spacer(Modifier.height(10.dp))

                            ConvField(fTitulo, { fTitulo = it }, "Título *")
                            Spacer(Modifier.height(8.dp))
                            ConvField(fDescripcion, { fDescripcion = it }, "Descripción (opcional)", singleLine = false, minLines = 3)
                            Spacer(Modifier.height(8.dp))
                            ConvTapField(categoriaNombre, "Categoría *") { showCatDialog = true }
                            Spacer(Modifier.height(8.dp))
                            ConvField(fPlazas, { fPlazas = it.filter { c -> c.isDigit() } }, "Plazas disponibles *", keyboardType = KeyboardType.Number)
                            Spacer(Modifier.height(8.dp))
                            ConvDateField(fInicio, { fInicio = it }, "Fecha de inicio *")
                            Spacer(Modifier.height(8.dp))
                            ConvDateField(fFin, { fFin = it }, "Fecha de fin (opcional)", optional = true)

                            msgForm?.let {
                                Spacer(Modifier.height(8.dp))
                                Text(it, color = if (it.startsWith("✓")) Color(0xFF34D399) else Color(0xFFF87171), fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        if (fTitulo.isBlank()) { msgForm = "El título es requerido"; return@launch }
                                        if (fCategoriaId.isBlank()) { msgForm = "Selecciona una categoría"; return@launch }
                                        val plazas = fPlazas.toIntOrNull()
                                        if (plazas == null || plazas < 1) { msgForm = "Indica un número de plazas válido"; return@launch }
                                        if (fInicio == null) { msgForm = "Selecciona la fecha de inicio"; return@launch }
                                        saving = true; msgForm = null
                                        val token = tokenManager.getAccessToken() ?: run { msgForm = "Sesión expirada"; saving = false; return@launch }
                                        val body = JSONObject().apply {
                                            put("titulo", fTitulo.trim())
                                            put("descripcion", fDescripcion.trim().ifBlank { null })
                                            put("categoria_id", fCategoriaId)
                                            put("plazas_disponibles", plazas)
                                            put("fecha_inicio", fInicio)
                                            put("fecha_fin", fFin)
                                        }
                                        val result = if (editId != null)
                                            ApiClient.put("/api/convocatorias/$editId", body, token)
                                        else
                                            ApiClient.post("/api/convocatorias", body, token)
                                        saving = false
                                        if (result.success && result.data != null) {
                                            val item = result.data
                                            convocatorias = if (editId != null)
                                                convocatorias.map { if (it.optString("id") == editId) item else it }
                                            else
                                                listOf(item) + convocatorias
                                            showForm = false; editId = null
                                        } else {
                                            msgForm = "✗ ${result.error ?: "Error al guardar"}"
                                        }
                                    }
                                },
                                enabled  = !saving,
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape    = RoundedCornerShape(50.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1A0F3D))
                            ) {
                                Text(if (saving) "Guardando…" else if (editId != null) "Actualizar" else "Crear convocatoria", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (convocatorias.isEmpty() && !showForm) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                        Text(
                            "Aún no tienes convocatorias. Publica una para empezar a recibir postulaciones.",
                            color = Color.White.copy(alpha = 0.30f), fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(20.dp)
                        )
                    }
                }
            }

            items(convocatorias, key = { it.optString("id") }) { conv ->
                val id = conv.optString("id")
                val estado = conv.str("estado")
                val plazasTot = conv.optInt("plazas_disponibles", 0)
                val plazasOcu = conv.optInt("plazas_ocupadas", 0)
                val isExpanded = expandedId == id

                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                            Text(conv.str("titulo"), color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(estadoColor(estado).copy(alpha = 0.18f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(estadoLabel(estado), color = estadoColor(estado), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${(plazasTot - plazasOcu).coerceAtLeast(0)} libres / $plazasTot plazas",
                            color = TextSecondary, fontSize = 12.sp
                        )
                        if (fInicioLabel(conv).isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(fInicioLabel(conv), color = TextSecondary.copy(alpha = 0.8f), fontSize = 11.sp)
                        }

                        val desc = conv.str("descripcion")
                        if (desc.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(desc, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp, maxLines = 3)
                        }

                        Spacer(Modifier.height(12.dp))

                        // ── Acciones ─────────────────────
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionChip("Editar") { openForm(conv) }
                            if (estado == "ABIERTA") {
                                ActionChip("En selección") {
                                    scope.launch { cambiarEstado(tokenManager, id, "EN_SELECCION") { convocatorias = convocatorias.map { c -> if (c.optString("id") == id) it else c } } }
                                }
                            } else if (estado == "EN_SELECCION") {
                                ActionChip("Reabrir") {
                                    scope.launch { cambiarEstado(tokenManager, id, "ABIERTA") { convocatorias = convocatorias.map { c -> if (c.optString("id") == id) it else c } } }
                                }
                            }
                            if (estado != "CERRADA") {
                                ActionChip("Cerrar") {
                                    scope.launch { cambiarEstado(tokenManager, id, "CERRADA") { convocatorias = convocatorias.map { c -> if (c.optString("id") == id) it else c } } }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionChip(if (isExpanded) "Ocultar postulaciones" else "Ver postulaciones") {
                                if (isExpanded) { expandedId = null }
                                else {
                                    expandedId = id
                                    scope.launch {
                                        loadingPost = true; postulaciones = emptyList()
                                        val r = authedGet("/api/convocatorias/$id/postulaciones")
                                        postulaciones = if (r.success) r.dataArray.toObjectList() else emptyList()
                                        loadingPost = false
                                    }
                                }
                            }
                            if (estado == "ABIERTA") {
                                ActionChip("Eliminar", danger = true) {
                                    pendingDelete = {
                                        scope.launch {
                                            val token = tokenManager.getAccessToken() ?: return@launch
                                            val r = ApiClient.delete("/api/convocatorias/$id", token)
                                            if (r.success) convocatorias = convocatorias.filter { it.optString("id") != id }
                                        }
                                    }
                                }
                            }
                        }

                        // ── Postulaciones ────────────────
                        if (isExpanded) {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.12f))
                            Spacer(Modifier.height(10.dp))
                            if (loadingPost) {
                                Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BrandBlue, strokeWidth = 2.dp)
                                }
                            } else if (postulaciones.isEmpty()) {
                                Text("No hay postulaciones todavía.", color = Color.White.copy(alpha = 0.35f), fontSize = 12.sp)
                            } else {
                                postulaciones.forEach { post ->
                                    PostulacionRow(
                                        post = post,
                                        onGestionar = { nuevoEstado ->
                                            scope.launch {
                                                val token = tokenManager.getAccessToken() ?: return@launch
                                                val body = JSONObject().put("estado", nuevoEstado)
                                                val r = ApiClient.patch("/api/postulaciones/${post.optString("id")}/estado", body, token)
                                                if (r.success) {
                                                    postulaciones = postulaciones.map {
                                                        if (it.optString("id") == post.optString("id")) {
                                                            JSONObject(it.toString()).put("estado", nuevoEstado)
                                                        } else it
                                                    }
                                                    if (nuevoEstado == "ACEPTADA") {
                                                        convocatorias = convocatorias.map { c ->
                                                            if (c.optString("id") == id)
                                                                JSONObject(c.toString()).put("plazas_ocupadas", c.optInt("plazas_ocupadas", 0) + 1)
                                                            else c
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

private fun fInicioLabel(conv: JSONObject): String {
    val inicio = if (conv.isNull("fecha_inicio")) null else conv.optLong("fecha_inicio")
    val fin    = if (conv.isNull("fecha_fin")) null else conv.optLong("fecha_fin")
    val ini = tsToDateLabel(inicio)
    if (ini.isBlank()) return ""
    val f = tsToDateLabel(fin)
    return if (f.isBlank()) "Inicia: $ini" else "$ini – $f"
}

private suspend fun cambiarEstado(
    tokenManager: TokenManager,
    id: String,
    estado: String,
    onUpdated: (JSONObject) -> Unit
) {
    val token = tokenManager.getAccessToken() ?: return
    val body = JSONObject().put("estado", estado)
    val r = ApiClient.patch("/api/convocatorias/$id/estado", body, token)
    if (r.success && r.data != null) onUpdated(r.data)
}

@Composable
private fun PostulacionRow(post: JSONObject, onGestionar: (String) -> Unit) {
    val estado = post.str("estado")
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    post.str("nombre_completo").ifBlank { "Técnico" },
                    color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(estadoPostLabel(estado), color = estadoPostColor(estado), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            val nivel = post.str("nivel")
            val calif = post.optDouble("calificacion_promedio", 0.0)
            if (nivel.isNotBlank() || calif > 0) {
                Spacer(Modifier.height(2.dp))
                val nivelTxt = when (nivel) { "PRACTICANTE" -> "Practicante"; "EGRESADO" -> "Egresado"; "CERTIFICADO" -> "Certificado"; else -> nivel }
                val califTxt = if (calif > 0) "  •  ${"%.1f".format(calif)} ★" else ""
                Text("$nivelTxt$califTxt", color = TextSecondary, fontSize = 11.sp)
            }
            val mensaje = post.str("mensaje")
            if (mensaje.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(mensaje, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, maxLines = 3)
            }
            if (estado == "PENDIENTE") {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionChip("Aceptar") { onGestionar("ACEPTADA") }
                    ActionChip("Rechazar", danger = true) { onGestionar("RECHAZADA") }
                }
            }
        }
    }
}

private fun estadoPostLabel(e: String) = when (e) {
    "PENDIENTE" -> "Pendiente"; "ACEPTADA" -> "Aceptada"; "RECHAZADA" -> "Rechazada"; else -> e
}

private fun estadoPostColor(e: String) = when (e) {
    "PENDIENTE" -> Color(0xFFFBBF24); "ACEPTADA" -> Color(0xFF34D399); "RECHAZADA" -> Color(0xFFF87171); else -> Color.White
}

@Composable
private fun ActionChip(text: String, danger: Boolean = false, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.clickable(onClick = onClick), cornerRadius = 50.dp) {
        Text(
            text,
            color = if (danger) Color(0xFFF87171) else BrandBlue,
            fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ConvField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
            if (value.isNotEmpty()) {
                Text(label, color = BrandBlue.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
            }
            BasicTextField(
                value           = value,
                onValueChange   = onValueChange,
                singleLine      = singleLine,
                minLines        = if (singleLine) 1 else minLines,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                textStyle       = TextStyle(color = Color.White, fontSize = 14.sp),
                cursorBrush     = SolidColor(BrandBlue),
                modifier        = Modifier.fillMaxWidth(),
                decorationBox   = { inner ->
                    if (value.isEmpty()) Text(label, color = Color.White.copy(alpha = 0.35f), fontSize = 14.sp)
                    inner()
                }
            )
        }
    }
}

@Composable
private fun ConvTapField(value: String, label: String, onTap: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onTap), cornerRadius = 12.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (value.isNotBlank()) {
                    Text(label, color = BrandBlue.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(3.dp))
                }
                Text(
                    value.ifBlank { label },
                    color = if (value.isBlank()) Color.White.copy(alpha = 0.35f) else Color.White,
                    fontSize = 14.sp
                )
            }
            Text("▾", color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConvDateField(
    value: Long?,
    onValueChange: (Long?) -> Unit,
    label: String,
    optional: Boolean = false
) {
    var showPicker by remember { mutableStateOf(false) }
    val state = rememberDatePickerState(initialSelectedDateMillis = value?.times(1000L))

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms -> onValueChange(ms / 1000) }
                    showPicker = false
                }) { Text("Aceptar", color = BrandBlue, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                Row {
                    if (optional && value != null) {
                        TextButton(onClick = { onValueChange(null); showPicker = false }) {
                            Text("Borrar", color = Color(0xFFF87171))
                        }
                    }
                    TextButton(onClick = { showPicker = false }) { Text("Cancelar", color = TextSecondary) }
                }
            }
        ) { DatePicker(state = state) }
    }

    GlassCard(modifier = Modifier.fillMaxWidth().clickable { showPicker = true }, cornerRadius = 12.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                if (value != null) {
                    Text(label, color = BrandBlue.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(3.dp))
                }
                Text(
                    if (value == null) label else tsToDateLabel(value),
                    color = if (value == null) Color.White.copy(alpha = 0.35f) else Color.White,
                    fontSize = 14.sp
                )
            }
            Text("📅", fontSize = 16.sp)
        }
    }
}
