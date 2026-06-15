package com.verion.practicas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

private fun JSONObject.strN(key: String): String = if (isNull(key)) "" else optString(key, "")

private fun JSONArray?.toNotifList(): List<JSONObject> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { runCatching { getJSONObject(it) }.getOrNull() }
}

private fun formatFechaN(ts: Long): String {
    if (ts <= 0L) return ""
    val cal = Calendar.getInstance().apply { timeInMillis = ts * 1000L }
    val mes = arrayOf("Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic")[cal.get(Calendar.MONTH)]
    return "${cal.get(Calendar.DAY_OF_MONTH)} $mes ${cal.get(Calendar.YEAR)}"
}

private fun tipoIcono(tipo: String): String = when (tipo) {
    "POSTULACION_ACEPTADA"    -> "✅"
    "POSTULACION_RECHAZADA"   -> "❌"
    "COLABORACION_INICIADA"   -> "🤝"
    "COLABORACION_FINALIZADA" -> "🏁"
    "CALIFICACION"            -> "⭐"
    "NUEVA_CONVOCATORIA"      -> "📢"
    "RECOMENDADO"             -> "🌟"
    "MENSAJE"                 -> "💬"
    else                      -> "🔔"
}

@Composable
fun NotificacionesScreen(
    tokenManager: TokenManager,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var notificaciones by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var isLoading      by remember { mutableStateOf(true) }
    var errorMsg       by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val token = tokenManager.getAccessToken()
        if (token == null) { errorMsg = "Sesión no válida"; isLoading = false; return@LaunchedEffect }
        val r = ApiClient.get("/api/notificaciones", token)
        if (r.success) notificaciones = r.dataArray.toNotifList()
        else errorMsg = r.error ?: "Error al cargar notificaciones"
        isLoading = false
    }

    fun marcarLeida(id: String) {
        scope.launch {
            val token = tokenManager.getAccessToken() ?: return@launch
            ApiClient.patch("/api/notificaciones/$id/leer", JSONObject(), token)
            notificaciones = notificaciones.map { n ->
                if (n.strN("id") == id) JSONObject(n.toString()).apply { put("leido", 1) } else n
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), null, tint = Color.White)
            }
            Text(
                "Notificaciones",
                color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            val sinLeer = notificaciones.count { it.optInt("leido", 0) == 0 }
            if (sinLeer > 0) {
                TextButton(onClick = {
                    scope.launch {
                        val token = tokenManager.getAccessToken() ?: return@launch
                        ApiClient.patch("/api/notificaciones/leer-todas", JSONObject(), token)
                        notificaciones = notificaciones.map { n ->
                            JSONObject(n.toString()).apply { put("leido", 1) }
                        }
                    }
                }) {
                    Text("Marcar todas", color = BrandBlue, fontSize = 12.sp)
                }
            }
        }

        when {
            isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandBlue, strokeWidth = 2.dp)
            }
            errorMsg != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(errorMsg!!, color = Color(0xFFF87171), fontSize = 13.sp, textAlign = TextAlign.Center)
            }
            notificaciones.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔔", fontSize = 40.sp)
                    Text("Sin notificaciones", color = TextSecondary, fontSize = 14.sp)
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(notificaciones, key = { _, n -> n.strN("id") }) { _, notif ->
                    val leido = notif.optInt("leido", 0) == 1
                    val tipo  = notif.strN("tipo")
                    val fecha = notif.optLong("fecha", 0L)
                    val id    = notif.strN("id")

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (!leido) marcarLeida(id) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(tipoIcono(tipo), fontSize = 22.sp)
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    notif.strN("contenido"),
                                    color = if (leido) TextSecondary else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = if (leido) FontWeight.Normal else FontWeight.SemiBold
                                )
                                if (fecha > 0L) Text(formatFechaN(fecha), color = TextSecondary, fontSize = 10.sp)
                            }
                            if (!leido) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(BrandBlue)
                                        .align(Alignment.CenterVertically)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
