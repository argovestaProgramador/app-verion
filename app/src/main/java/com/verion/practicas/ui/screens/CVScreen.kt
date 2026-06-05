package com.verion.practicas.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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

private fun tsToMonthStr(ts: Long): String {
    if (ts <= 0) return ""
    val cal = Calendar.getInstance().apply { timeInMillis = ts * 1000 }
    val y = cal.get(Calendar.YEAR)
    val m = cal.get(Calendar.MONTH) + 1
    return "$y-${m.toString().padStart(2, '0')}"
}

private fun monthStrToTs(str: String): Long? {
    if (str.isBlank()) return null
    return runCatching {
        val parts = str.trim().split("-")
        val cal = Calendar.getInstance()
        cal.set(parts[0].toInt(), parts[1].toInt() - 1, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis / 1000
    }.getOrNull()
}

private fun tsToShort(ts: Long): String {
    if (ts <= 0) return ""
    val cal = Calendar.getInstance().apply { timeInMillis = ts * 1000 }
    val months = arrayOf("Ene", "Feb", "Mar", "Abr", "May", "Jun",
        "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
    return "${months[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}"
}

private fun JSONObject.monthStr(key: String) = if (isNull(key)) "" else tsToMonthStr(optLong(key))

@Composable
fun CVScreen(
    tokenManager: TokenManager,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var experiencias by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var educacion    by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var certificados by remember { mutableStateOf<List<JSONObject>>(emptyList()) }

    var showExpForm  by remember { mutableStateOf(false) }
    var editExpId    by remember { mutableStateOf<String?>(null) }
    var expTipo      by remember { mutableStateOf("EMPLEO") }
    var expTitulo    by remember { mutableStateOf("") }
    var expDesc      by remember { mutableStateOf("") }
    var expInicio    by remember { mutableStateOf("") }
    var expFin       by remember { mutableStateOf("") }
    var savingExp    by remember { mutableStateOf(false) }
    var msgExp       by remember { mutableStateOf<String?>(null) }

    var showEduForm  by remember { mutableStateOf(false) }
    var editEduId    by remember { mutableStateOf<String?>(null) }
    var eduInstit    by remember { mutableStateOf("") }
    var eduTitulo    by remember { mutableStateOf("") }
    var eduInicio    by remember { mutableStateOf("") }
    var eduFin       by remember { mutableStateOf("") }
    var savingEdu    by remember { mutableStateOf(false) }
    var msgEdu       by remember { mutableStateOf<String?>(null) }

    var showCertForm by remember { mutableStateOf(false) }
    var editCertId   by remember { mutableStateOf<String?>(null) }
    var certNombre   by remember { mutableStateOf("") }
    var certInstit   by remember { mutableStateOf("") }
    var certFecha    by remember { mutableStateOf("") }
    var certUrl      by remember { mutableStateOf("") }
    var savingCert   by remember { mutableStateOf(false) }
    var msgCert      by remember { mutableStateOf<String?>(null) }

    var pendingDelete by remember { mutableStateOf<(() -> Unit)?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        var token = tokenManager.getAccessToken()
        if (token != null) {
            var result = ApiClient.get("/api/cv/mio", token)
            if (!result.success && result.isUnauthorized) {
                val newToken = tokenManager.getRefreshToken()?.let { ApiClient.refreshToken(it) }
                if (newToken != null) {
                    tokenManager.saveAccessToken(newToken)
                    result = ApiClient.get("/api/cv/mio", newToken)
                }
            }
            if (result.success && result.data != null) {
                experiencias = result.data.optJSONArray("experiencias").toObjectList()
                educacion    = result.data.optJSONArray("educacion").toObjectList()
                certificados = result.data.optJSONArray("certificados").toObjectList()
            }
        }
        isLoading = false
    }

    val openExpForm = { exp: JSONObject? ->
        editExpId = exp?.optString("id")
        expTipo   = exp?.str("tipo") ?: "EMPLEO"
        expTitulo = exp?.str("titulo") ?: ""
        expDesc   = exp?.str("descripcion") ?: ""
        expInicio = exp?.monthStr("fecha_inicio") ?: ""
        expFin    = exp?.monthStr("fecha_fin") ?: ""
        msgExp    = null
        showExpForm = true
    }

    val openEduForm = { edu: JSONObject? ->
        editEduId  = edu?.optString("id")
        eduInstit  = edu?.str("institucion") ?: ""
        eduTitulo  = edu?.str("titulo") ?: ""
        eduInicio  = edu?.monthStr("fecha_inicio") ?: ""
        eduFin     = edu?.monthStr("fecha_fin") ?: ""
        msgEdu     = null
        showEduForm = true
    }

    val openCertForm = { cert: JSONObject? ->
        editCertId  = cert?.optString("id")
        certNombre  = cert?.str("nombre") ?: ""
        certInstit  = cert?.str("institucion") ?: ""
        certFecha   = cert?.monthStr("fecha") ?: ""
        certUrl     = cert?.str("url") ?: ""
        msgCert     = null
        showCertForm = true
    }

    pendingDelete?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor   = Color(0xFF1E1040),
            shape            = RoundedCornerShape(20.dp),
            title = { Text("¿Eliminar?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text  = { Text("Esta acción no se puede deshacer.", color = TextSecondary, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { action(); pendingDelete = null }) {
                    Text("Eliminar", color = Color(0xFFF87171), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Volver",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                "Mi CV Digital",
                color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
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
                CVSectionHeader(
                    iconRes  = R.drawable.ic_cv_experience,
                    label    = "EXPERIENCIA",
                    onAdd    = { openExpForm(null) }
                )
            }

            if (showExpForm) {
                item {
                    CVForm(
                        title    = if (editExpId != null) "Editar experiencia" else "Nueva experiencia",
                        msg      = msgExp,
                        isSaving = savingExp,
                        onCancel = { showExpForm = false; editExpId = null }
                    ) {
                        OptionChipRowCV(
                            label    = "Tipo",
                            options  = listOf("EMPLEO" to "Empleo", "PRACTICA" to "Práctica", "PROYECTO" to "Proyecto"),
                            selected = expTipo,
                            onSelect = { expTipo = it }
                        )
                        Spacer(Modifier.height(8.dp))
                        CVField(expTitulo, { expTitulo = it }, "Título del cargo / proyecto *")
                        Spacer(Modifier.height(8.dp))
                        CVField(expDesc, { expDesc = it }, "Descripción (opcional)", singleLine = false, minLines = 2)
                        Spacer(Modifier.height(8.dp))
                        DatePickerField(expInicio, { expInicio = it }, "Fecha inicio *")
                        Spacer(Modifier.height(8.dp))
                        DatePickerField(expFin, { expFin = it }, "Fecha fin (vacío = actualidad)", optional = true)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    if (expTitulo.isBlank()) { msgExp = "El título es requerido"; return@launch }
                                    val inicio = monthStrToTs(expInicio)
                                    if (inicio == null) { msgExp = "Selecciona la fecha de inicio"; return@launch }
                                    savingExp = true; msgExp = null
                                    val token = tokenManager.getAccessToken() ?: run { msgExp = "Sesión expirada"; savingExp = false; return@launch }
                                    val body = JSONObject().apply {
                                        put("tipo", expTipo)
                                        put("titulo", expTitulo.trim())
                                        put("descripcion", expDesc.trim().ifBlank { null })
                                        put("fecha_inicio", inicio)
                                        put("fecha_fin", monthStrToTs(expFin))
                                    }
                                    val result = if (editExpId != null)
                                        ApiClient.put("/api/cv/experiencia/$editExpId", body, token)
                                    else
                                        ApiClient.post("/api/cv/experiencia", body, token)
                                    savingExp = false
                                    if (result.success && result.data != null) {
                                        val item = result.data
                                        if (editExpId != null) {
                                            experiencias = experiencias.map { if (it.optString("id") == editExpId) item else it }
                                        } else {
                                            experiencias = listOf(item) + experiencias
                                        }
                                        showExpForm = false; editExpId = null
                                    } else {
                                        msgExp = "✗ ${result.error ?: "Error al guardar"}"
                                    }
                                }
                            },
                            enabled  = !savingExp,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape    = RoundedCornerShape(50.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1A0F3D))
                        ) {
                            Text(if (savingExp) "Guardando…" else "Guardar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (experiencias.isEmpty() && !showExpForm) {
                item { EmptySection("Aún no tienes experiencia registrada") }
            }

            items(experiencias, key = { it.optString("id") }) { exp ->
                ExpCard(
                    exp      = exp,
                    onEdit   = { openExpForm(exp); },
                    onDelete = {
                        pendingDelete = {
                            scope.launch {
                                val id = exp.optString("id")
                                val token = tokenManager.getAccessToken() ?: return@launch
                                val r = ApiClient.delete("/api/cv/experiencia/$id", token)
                                if (r.success) experiencias = experiencias.filter { it.optString("id") != id }
                            }
                        }
                    }
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
            item {
                CVSectionHeader(
                    iconRes = R.drawable.ic_cv_education,
                    label   = "EDUCACIÓN",
                    onAdd   = { openEduForm(null) }
                )
            }

            if (showEduForm) {
                item {
                    CVForm(
                        title    = if (editEduId != null) "Editar educación" else "Nueva educación",
                        msg      = msgEdu,
                        isSaving = savingEdu,
                        onCancel = { showEduForm = false; editEduId = null }
                    ) {
                        CVField(eduInstit, { eduInstit = it }, "Institución *")
                        Spacer(Modifier.height(8.dp))
                        CVField(eduTitulo, { eduTitulo = it }, "Título / Carrera *")
                        Spacer(Modifier.height(8.dp))
                        DatePickerField(eduInicio, { eduInicio = it }, "Fecha inicio *")
                        Spacer(Modifier.height(8.dp))
                        DatePickerField(eduFin, { eduFin = it }, "Fecha fin (vacío = en curso)", optional = true)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    if (eduInstit.isBlank() || eduTitulo.isBlank()) { msgEdu = "Institución y título son requeridos"; return@launch }
                                    val inicio = monthStrToTs(eduInicio)
                                    if (inicio == null) { msgEdu = "Selecciona la fecha de inicio"; return@launch }
                                    savingEdu = true; msgEdu = null
                                    val token = tokenManager.getAccessToken() ?: run { msgEdu = "Sesión expirada"; savingEdu = false; return@launch }
                                    val body = JSONObject().apply {
                                        put("institucion", eduInstit.trim())
                                        put("titulo", eduTitulo.trim())
                                        put("fecha_inicio", inicio)
                                        put("fecha_fin", monthStrToTs(eduFin))
                                    }
                                    val result = if (editEduId != null)
                                        ApiClient.put("/api/cv/educacion/$editEduId", body, token)
                                    else
                                        ApiClient.post("/api/cv/educacion", body, token)
                                    savingEdu = false
                                    if (result.success && result.data != null) {
                                        val item = result.data
                                        if (editEduId != null) {
                                            educacion = educacion.map { if (it.optString("id") == editEduId) item else it }
                                        } else {
                                            educacion = listOf(item) + educacion
                                        }
                                        showEduForm = false; editEduId = null
                                    } else {
                                        msgEdu = "✗ ${result.error ?: "Error al guardar"}"
                                    }
                                }
                            },
                            enabled  = !savingEdu,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape    = RoundedCornerShape(50.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1A0F3D))
                        ) {
                            Text(if (savingEdu) "Guardando…" else "Guardar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (educacion.isEmpty() && !showEduForm) {
                item { EmptySection("Aún no tienes educación registrada") }
            }

            items(educacion, key = { it.optString("id") }) { edu ->
                EduCard(
                    edu      = edu,
                    onEdit   = { openEduForm(edu) },
                    onDelete = {
                        pendingDelete = {
                            scope.launch {
                                val id = edu.optString("id")
                                val token = tokenManager.getAccessToken() ?: return@launch
                                val r = ApiClient.delete("/api/cv/educacion/$id", token)
                                if (r.success) educacion = educacion.filter { it.optString("id") != id }
                            }
                        }
                    }
                )
            }


            item { Spacer(Modifier.height(8.dp)) }
            item {
                CVSectionHeader(
                    iconRes = R.drawable.ic_cv_cert,
                    label   = "CERTIFICADOS Y LICENCIAS",
                    onAdd   = { openCertForm(null) }
                )
            }

            if (showCertForm) {
                item {
                    CVForm(
                        title    = if (editCertId != null) "Editar certificado" else "Nuevo certificado",
                        msg      = msgCert,
                        isSaving = savingCert,
                        onCancel = { showCertForm = false; editCertId = null }
                    ) {
                        CVField(certNombre, { certNombre = it }, "Nombre del certificado *")
                        Spacer(Modifier.height(8.dp))
                        CVField(certInstit, { certInstit = it }, "Institución emisora *")
                        Spacer(Modifier.height(8.dp))
                        DatePickerField(certFecha, { certFecha = it }, "Fecha de obtención *")
                        Spacer(Modifier.height(8.dp))
                        CVField(certUrl, { certUrl = it }, "URL de verificación (opcional)", keyboardType = KeyboardType.Uri)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    if (certNombre.isBlank() || certInstit.isBlank()) { msgCert = "Nombre e institución son requeridos"; return@launch }
                                    val fecha = monthStrToTs(certFecha)
                                    if (fecha == null) { msgCert = "Selecciona la fecha de obtención"; return@launch }
                                    savingCert = true; msgCert = null
                                    val token = tokenManager.getAccessToken() ?: run { msgCert = "Sesión expirada"; savingCert = false; return@launch }
                                    val body = JSONObject().apply {
                                        put("nombre", certNombre.trim())
                                        put("institucion", certInstit.trim())
                                        put("fecha", fecha)
                                        if (certUrl.isNotBlank()) put("url", certUrl.trim())
                                    }
                                    val result = if (editCertId != null)
                                        ApiClient.put("/api/cv/certificado/$editCertId", body, token)
                                    else
                                        ApiClient.post("/api/cv/certificado", body, token)
                                    savingCert = false
                                    if (result.success && result.data != null) {
                                        val item = result.data
                                        if (editCertId != null) {
                                            certificados = certificados.map { if (it.optString("id") == editCertId) item else it }
                                        } else {
                                            certificados = listOf(item) + certificados
                                        }
                                        showCertForm = false; editCertId = null
                                    } else {
                                        msgCert = "✗ ${result.error ?: "Error al guardar"}"
                                    }
                                }
                            },
                            enabled  = !savingCert,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape    = RoundedCornerShape(50.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1A0F3D))
                        ) {
                            Text(if (savingCert) "Guardando…" else "Guardar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (certificados.isEmpty() && !showCertForm) {
                item { EmptySection("Aún no tienes certificados registrados") }
            }

            items(certificados, key = { it.optString("id") }) { cert ->
                CertCard(
                    cert     = cert,
                    onEdit   = { openCertForm(cert) },
                    onDelete = {
                        pendingDelete = {
                            scope.launch {
                                val id = cert.optString("id")
                                val token = tokenManager.getAccessToken() ?: return@launch
                                val r = ApiClient.delete("/api/cv/certificado/$id", token)
                                if (r.success) certificados = certificados.filter { it.optString("id") != id }
                            }
                        }
                    }
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun CVSectionHeader(iconRes: Int, label: String, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(BrandBlue, BrandPurple))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        GlassCard(
            modifier = Modifier.clickable(onClick = onAdd),
            cornerRadius = 50.dp
        ) {
            Text(
                "+ Agregar",
                color = BrandBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun ExpCard(exp: JSONObject, onEdit: () -> Unit, onDelete: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(exp.str("titulo"), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    val tipoStr = when (exp.str("tipo")) {
                        "EMPLEO" -> "Empleo"; "PRACTICA" -> "Práctica"; else -> "Proyecto"
                    }
                    val inicio = tsToShort(exp.optLong("fecha_inicio"))
                    val fin = if (exp.isNull("fecha_fin")) "Actualidad" else tsToShort(exp.optLong("fecha_fin"))
                    Text("$tipoStr  •  $inicio – $fin", color = TextSecondary, fontSize = 11.sp)
                }
                ItemActions(onEdit, onDelete)
            }
            val desc = exp.str("descripcion")
            if (desc.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(desc, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun EduCard(edu: JSONObject, onEdit: () -> Unit, onDelete: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(edu.str("titulo"), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    val inicio = tsToShort(edu.optLong("fecha_inicio"))
                    val fin = if (edu.isNull("fecha_fin")) "En curso" else tsToShort(edu.optLong("fecha_fin"))
                    Text("${edu.str("institucion")}  •  $inicio – $fin", color = TextSecondary, fontSize = 11.sp)
                }
                ItemActions(onEdit, onDelete)
            }
        }
    }
}

@Composable
private fun CertCard(cert: JSONObject, onEdit: () -> Unit, onDelete: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(cert.str("nombre"), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${cert.str("institucion")}  •  ${tsToShort(cert.optLong("fecha"))}",
                        color = TextSecondary, fontSize = 11.sp
                    )
                    val url = cert.str("url")
                    if (url.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text("🔗 Verificar credencial", color = BrandBlue, fontSize = 11.sp)
                    }
                }
                ItemActions(onEdit, onDelete)
            }
        }
    }
}

@Composable
private fun ItemActions(onEdit: () -> Unit, onDelete: () -> Unit) {
    Row {
        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Text("✏️", fontSize = 14.sp)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Text("🗑️", fontSize = 14.sp)
        }
    }
}

@Composable
private fun CVForm(
    title: String,
    msg: String?,
    isSaving: Boolean,
    onCancel: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BrandBlue, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                TextButton(onClick = onCancel, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("Cancelar", color = TextSecondary, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            content()
            msg?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Color(0xFFF87171), fontSize = 12.sp)
            }
        }
    }
}


@Composable
private fun CVField(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    optional: Boolean = false
) {
    var showPicker by remember { mutableStateOf(false) }
    val initialMs  = remember(value) { monthStrToTs(value)?.times(1000L) }
    val state      = rememberDatePickerState(initialSelectedDateMillis = initialMs)

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        val cal = Calendar.getInstance().apply { timeInMillis = ms }
                        val y = cal.get(Calendar.YEAR)
                        val m = cal.get(Calendar.MONTH) + 1
                        onValueChange("$y-${m.toString().padStart(2, '0')}")
                    }
                    showPicker = false
                }) { Text("Aceptar", color = BrandBlue, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                Row {
                    if (optional && value.isNotBlank()) {
                        TextButton(onClick = { onValueChange(""); showPicker = false }) {
                            Text("Borrar", color = Color(0xFFF87171))
                        }
                    }
                    TextButton(onClick = { showPicker = false }) {
                        Text("Cancelar", color = TextSecondary)
                    }
                }
            }
        ) { DatePicker(state = state) }
    }

    GlassCard(modifier = Modifier.fillMaxWidth().clickable { showPicker = true }, cornerRadius = 12.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                if (value.isNotBlank()) {
                    Text(label, color = BrandBlue.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(3.dp))
                }
                Text(
                    if (value.isBlank()) label else tsToShort(monthStrToTs(value) ?: 0L),
                    color = if (value.isBlank()) Color.White.copy(alpha = 0.35f) else Color.White,
                    fontSize = 14.sp
                )
            }
            Text("📅", fontSize = 16.sp)
        }
    }
}

@Composable
private fun OptionChipRowCV(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Text(label, color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (value, display) ->
            val isSelected = value == selected
            GlassCard(modifier = Modifier.clickable { onSelect(value) }, cornerRadius = 50.dp) {
                Text(
                    display,
                    color = if (isSelected) BrandBlue else Color.White.copy(alpha = 0.45f),
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptySection(text: String) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
        Text(
            text,
            color = Color.White.copy(alpha = 0.30f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(20.dp)
        )
    }
}
