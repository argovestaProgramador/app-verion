package com.verion.practicas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verion.practicas.ApiClient
import com.verion.practicas.R
import com.verion.practicas.TokenManager
import com.verion.practicas.ui.components.GlassCard
import com.verion.practicas.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

private fun JSONObject.str(key: String): String =
    if (isNull(key)) "" else optString(key, "")

private fun JSONArray?.toObjectList(): List<JSONObject> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { runCatching { getJSONObject(it) }.getOrNull() }
}

private fun postTsLabel(ts: Long): String {
    if (ts <= 0L) return ""
    val MESES = arrayOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
    val cal = Calendar.getInstance().apply { timeInMillis = ts * 1000L }
    return "${cal.get(Calendar.DAY_OF_MONTH)} ${MESES[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}"
}

@Composable
fun PostulacionesScreen(
    tokenManager: TokenManager,
    onLoginRequest: () -> Unit
) {
    var isLoading     by remember { mutableStateOf(true) }
    var postulaciones by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var errorMsg      by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        var token = tokenManager.getAccessToken() ?: run { onLoginRequest(); return@LaunchedEffect }
        var result = ApiClient.get("/api/postulaciones/mis-postulaciones", token)
        if (!result.success && result.isUnauthorized) {
            val nt = tokenManager.getRefreshToken()?.let { ApiClient.refreshToken(it) }
            if (nt != null) {
                tokenManager.saveAccessToken(nt); token = nt
                result = ApiClient.get("/api/postulaciones/mis-postulaciones", nt)
            } else {
                onLoginRequest(); return@LaunchedEffect
            }
        }
        if (result.success) {
            postulaciones = result.dataArray.toObjectList()
        } else {
            errorMsg = result.error ?: "Error al cargar postulaciones"
        }
        isLoading = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("Mis Postulaciones", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("El estado de tus aplicaciones", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))
        }

        when {
            isLoading -> item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandBlue, strokeWidth = 2.dp)
                }
            }

            errorMsg != null -> item {
                Text(
                    errorMsg!!,
                    color = Color(0xFFF87171), fontSize = 13.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp)
                )
            }

            postulaciones.isEmpty() -> item {
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_nav_building),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.22f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Sin postulaciones aún", color = Color.White.copy(alpha = 0.55f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Explora convocatorias en la pestaña Buscar y postula a las que te interesen.",
                            color = Color.White.copy(alpha = 0.35f), fontSize = 13.sp, textAlign = TextAlign.Center
                        )
                    }
                }
            }

            else -> {
                item {
                    Text(
                        "${postulaciones.size} POSTULACI${if (postulaciones.size == 1) "ÓN" else "ONES"}",
                        color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(12.dp))
                }
                items(postulaciones, key = { it.str("id") }) { post ->
                    PostulacionCard(post)
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun PostulacionCard(post: JSONObject) {
    val estado  = post.str("estado")
    val titulo  = post.str("titulo")
    val empresa = post.str("razon_social")
    val mensaje = post.str("mensaje")
    val fechaTs = if (post.isNull("fecha_creacion")) 0L else post.optLong("fecha_creacion", 0L)
    val fecha   = postTsLabel(fechaTs)

    val estadoColor = when (estado) {
        "PENDIENTE" -> Color(0xFFFBBF24)
        "ACEPTADA"  -> Color(0xFF34D399)
        "RECHAZADA" -> Color(0xFFF87171)
        else        -> Color.White
    }
    val estadoLabel = when (estado) {
        "PENDIENTE" -> "Pendiente"
        "ACEPTADA"  -> "Aceptada"
        "RECHAZADA" -> "Rechazada"
        else        -> estado
    }

    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(titulo.ifBlank { "Convocatoria" }, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    if (empresa.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(empresa, color = BrandBlue, fontSize = 12.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(estadoColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(estadoLabel, color = estadoColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (mensaje.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("\"$mensaje\"", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 2)
            }

            if (fecha.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text("Postulado: $fecha", color = TextSecondary.copy(alpha = 0.7f), fontSize = 11.sp)
            }

            if (estado == "ACEPTADA") {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tu postulación fue aceptada. La empresa se pondrá en contacto contigo.",
                    color = Color(0xFF34D399).copy(alpha = 0.85f), fontSize = 11.sp
                )
            }
        }
    }
}
