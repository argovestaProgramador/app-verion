package com.verion.practicas.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.verion.practicas.ApiClient
import com.verion.practicas.R
import com.verion.practicas.TokenManager
import com.verion.practicas.ui.components.GlassCard
import com.verion.practicas.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Calendar

private fun JSONObject.str(key: String): String = if (isNull(key)) "" else optString(key, "")

private fun tsLabelDet(ts: Long): String {
    if (ts <= 0L) return ""
    val c = Calendar.getInstance().apply { timeInMillis = ts * 1000L }
    val mes = arrayOf("Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic")[c.get(Calendar.MONTH)]
    return "${c.get(Calendar.DAY_OF_MONTH)} $mes ${c.get(Calendar.YEAR)}"
}

@Composable
fun ConvocatoriaDetalleScreen(
    conv: JSONObject,
    tokenManager: TokenManager,
    rol: String,
    isGuest: Boolean,
    yaPostulado: Boolean,
    onBack: () -> Unit,
    onVerEmpresa: (String) -> Unit,
    onPostuladoSuccess: (String) -> Unit
) {
    val context   = LocalContext.current
    val scope     = rememberCoroutineScope()
    val empresaId = conv.str("empresa_usuario_id")

    var empresaPerfil    by remember { mutableStateOf<JSONObject?>(null) }
    var mensaje          by remember { mutableStateOf("") }
    var postulando       by remember { mutableStateOf(false) }
    var postularMsg      by remember { mutableStateOf<String?>(null) }
    var yaPostuladoLocal by remember { mutableStateOf(yaPostulado) }

    LaunchedEffect(empresaId) {
        if (empresaId.isNotBlank()) {
            val r = ApiClient.get("/api/perfil/empresa/$empresaId")
            if (r.success) empresaPerfil = r.data?.optJSONObject("perfil")
        }
    }

    val estado      = conv.str("estado")
    val isAbierta   = estado == "ABIERTA"
    val estadoColor = when (estado) {
        "ABIERTA" -> Color(0xFF34D399); "PAUSADA" -> Color(0xFFFBBF24); else -> Color(0xFF9CA3AF)
    }
    val libres  = (conv.optInt("plazas_disponibles", 0) - conv.optInt("plazas_ocupadas", 0)).coerceAtLeast(0)
    val logoKey = conv.str("logo_key").ifBlank { empresaPerfil?.str("logo_key") ?: "" }
    val sector  = empresaPerfil?.str("sector") ?: ""
    val dir     = empresaPerfil?.str("direccion") ?: ""
    val calif   = empresaPerfil?.optDouble("calificacion_promedio", 0.0) ?: 0.0

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), null, tint = Color.White)
            }
            Text("Convocatoria", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Empresa header — tappable
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = empresaId.isNotBlank()) { onVerEmpresa(empresaId) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp))
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
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            Text("🏢", fontSize = 24.sp)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(conv.str("razon_social"), color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        if (sector.isNotBlank()) Text(sector, color = BrandBlue, fontSize = 12.sp)
                        if (dir.isNotBlank()) Text("📍 $dir", color = TextSecondary, fontSize = 11.sp, maxLines = 1)
                        if (calif > 0.0) Text("★ ${"%.1f".format(calif)}", color = Color(0xFFFBBF24), fontSize = 11.sp)
                    }
                    if (empresaId.isNotBlank()) {
                        Text("›", color = TextSecondary, fontSize = 22.sp, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }

            if (empresaId.isNotBlank()) {
                TextButton(
                    onClick  = { onVerEmpresa(empresaId) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Ver perfil completo →", color = BrandBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Convocatoria detail card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            conv.str("titulo"),
                            color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(estadoColor.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(estado, color = estadoColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        "$libres plaza${if (libres != 1) "s" else ""} disponible${if (libres != 1) "s" else ""}",
                        color = BrandBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                    )

                    val fi = conv.optLong("fecha_inicio", 0L)
                    val ff = conv.optLong("fecha_fin", 0L)
                    if (fi > 0L || ff > 0L) {
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            if (fi > 0L) Column {
                                Text("Inicio", color = TextSecondary, fontSize = 10.sp)
                                Text(tsLabelDet(fi), color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            if (ff > 0L) Column {
                                Text("Cierre", color = TextSecondary, fontSize = 10.sp)
                                Text(tsLabelDet(ff), color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    val desc = conv.str("descripcion")
                    if (desc.isNotBlank()) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        Text(desc, color = Color.White.copy(alpha = 0.78f), fontSize = 13.sp, lineHeight = 20.sp)
                    }

                    val eDesc = conv.str("empresa_descripcion").ifBlank { empresaPerfil?.str("descripcion") ?: "" }
                    if (eDesc.isNotBlank()) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        Text("Sobre la empresa", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(eDesc, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
            }

            // Postular section — TECNICO only
            if (!isGuest && rol == "TECNICO") {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Postular", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)

                        if (!yaPostuladoLocal && isAbierta) {
                            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                                BasicTextField(
                                    value         = mensaje,
                                    onValueChange = { mensaje = it },
                                    textStyle     = TextStyle(color = Color.White, fontSize = 13.sp),
                                    cursorBrush   = SolidColor(BrandBlue),
                                    minLines      = 3,
                                    modifier      = Modifier.fillMaxWidth().padding(12.dp),
                                    decorationBox = { inner ->
                                        if (mensaje.isEmpty()) Text(
                                            "Mensaje opcional para la empresa...",
                                            color = Color.White.copy(alpha = 0.35f), fontSize = 13.sp
                                        )
                                        inner()
                                    }
                                )
                            }
                        }

                        postularMsg?.let {
                            Text(
                                it,
                                color = if (it.startsWith("✓")) Color(0xFF34D399) else Color(0xFFF87171),
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = {
                                if (yaPostuladoLocal || !isAbierta) return@Button
                                scope.launch {
                                    postulando = true; postularMsg = null
                                    val token = tokenManager.getAccessToken()
                                        ?: run { postularMsg = "✗ Debes iniciar sesión"; postulando = false; return@launch }
                                    val body = JSONObject().apply {
                                        if (mensaje.isNotBlank()) put("mensaje", mensaje.trim())
                                    }
                                    val r = ApiClient.post("/api/convocatorias/${conv.str("id")}/postulaciones", body, token)
                                    postulando = false
                                    if (r.success) {
                                        yaPostuladoLocal = true
                                        mensaje = ""
                                        postularMsg = "✓ Postulación enviada"
                                        onPostuladoSuccess(conv.str("id"))
                                    } else {
                                        postularMsg = "✗ ${r.error ?: "Error al postular"}"
                                    }
                                }
                            },
                            enabled  = !postulando && !yaPostuladoLocal && isAbierta,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape    = RoundedCornerShape(50.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor         = Color.White,
                                contentColor           = Color(0xFF1A0F3D),
                                disabledContainerColor = Color.White.copy(alpha = 0.12f),
                                disabledContentColor   = Color.White.copy(alpha = 0.5f)
                            )
                        ) {
                            if (postulando) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BrandPurple, strokeWidth = 2.dp)
                            } else {
                                Text(
                                    when {
                                        yaPostuladoLocal -> "Ya postulado"
                                        !isAbierta       -> "Convocatoria cerrada"
                                        else             -> "Postular"
                                    },
                                    fontWeight = FontWeight.Bold, fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
