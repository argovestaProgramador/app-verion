package com.verion.practicas.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.imageLoader
import com.verion.practicas.ApiClient
import com.verion.practicas.TokenManager
import com.verion.practicas.ui.components.GlassCard
import com.verion.practicas.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONArray

object StaticCache {
    private const val PREFS             = "veri_on_settings"
    private const val KEY_ENABLED       = "cache_enabled"
    private const val KEY_CAT           = "cache_categorias"
    private const val KEY_CAT_TS        = "cache_categorias_ts"
    private const val KEY_HAB           = "cache_habilidades"
    private const val KEY_HAB_TS        = "cache_habilidades_ts"
    private const val TTL               = 24L * 3600 * 1000   // 24 h in ms

    fun isEnabled(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, 0).getBoolean(KEY_ENABLED, true)

    fun setEnabled(ctx: Context, on: Boolean) =
        ctx.getSharedPreferences(PREFS, 0).edit().putBoolean(KEY_ENABLED, on).apply()

    fun getCategorias(ctx: Context): JSONArray? {
        if (!isEnabled(ctx)) return null
        val prefs = ctx.getSharedPreferences(PREFS, 0)
        val ts    = prefs.getLong(KEY_CAT_TS, 0L)
        if (System.currentTimeMillis() - ts > TTL) return null
        val raw   = prefs.getString(KEY_CAT, null) ?: return null
        return runCatching { JSONArray(raw) }.getOrNull()
    }

    fun saveCategorias(ctx: Context, arr: JSONArray) {
        ctx.getSharedPreferences(PREFS, 0).edit()
            .putString(KEY_CAT, arr.toString())
            .putLong(KEY_CAT_TS, System.currentTimeMillis())
            .apply()
    }

    fun getHabilidades(ctx: Context): JSONArray? {
        if (!isEnabled(ctx)) return null
        val prefs = ctx.getSharedPreferences(PREFS, 0)
        val ts    = prefs.getLong(KEY_HAB_TS, 0L)
        if (System.currentTimeMillis() - ts > TTL) return null
        val raw   = prefs.getString(KEY_HAB, null) ?: return null
        return runCatching { JSONArray(raw) }.getOrNull()
    }

    fun saveHabilidades(ctx: Context, arr: JSONArray) {
        ctx.getSharedPreferences(PREFS, 0).edit()
            .putString(KEY_HAB, arr.toString())
            .putLong(KEY_HAB_TS, System.currentTimeMillis())
            .apply()
    }

    fun clearAll(ctx: Context) {
        ctx.getSharedPreferences(PREFS, 0).edit()
            .remove(KEY_CAT).remove(KEY_CAT_TS)
            .remove(KEY_HAB).remove(KEY_HAB_TS)
            .apply()
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color      = TextSecondary,
        fontSize   = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier   = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

@OptIn(coil.annotation.ExperimentalCoilApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    tokenManager: TokenManager,
    onAccountDeleted: () -> Unit
) {
    val context       = LocalContext.current
    val scope         = rememberCoroutineScope()
    var cacheEnabled  by remember { mutableStateOf(StaticCache.isEnabled(context)) }
    var showPrivacy   by remember { mutableStateOf(false) }
    var showTerms     by remember { mutableStateOf(false) }
    var showDeleteDlg by remember { mutableStateOf(false) }
    var cacheCleared  by remember { mutableStateOf(false) }
    var imgCleared    by remember { mutableStateOf(false) }
    var deleting      by remember { mutableStateOf(false) }
    var deleteError   by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text("Configuración", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        SectionLabel("Datos y caché")
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Caché de metadatos", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "Guarda categorías y habilidades localmente (24 h) para reducir las consultas al servidor",
                            color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked         = cacheEnabled,
                        onCheckedChange = {
                            cacheEnabled = it
                            StaticCache.setEnabled(context, it)
                            if (!it) StaticCache.clearAll(context)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor   = Color.White,
                            checkedTrackColor   = BrandBlue,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }

                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { StaticCache.clearAll(context); cacheCleared = true; imgCleared = false }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Borrar caché de datos", color = TextPrimary, fontSize = 14.sp)
                        Text("Categorías y habilidades guardadas localmente", color = TextSecondary, fontSize = 11.sp)
                    }
                    if (cacheCleared) Text("✓ Listo", color = BrandBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            context.imageLoader.diskCache?.clear()
                            context.imageLoader.memoryCache?.clear()
                            imgCleared = true; cacheCleared = false
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Borrar caché de imágenes", color = TextPrimary, fontSize = 14.sp)
                        Text("Fotos y logos descargados en disco", color = TextSecondary, fontSize = 11.sp)
                    }
                    if (imgCleared) Text("✓ Listo", color = BrandBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        SectionLabel("Legal")
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showPrivacy = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Política de privacidad", color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text("›", color = TextSecondary, fontSize = 18.sp)
                }
                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.1f))
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showTerms = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Términos y condiciones", color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text("›", color = TextSecondary, fontSize = 18.sp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        SectionLabel("Cuenta")
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDeleteDlg = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Eliminar cuenta", color = Color(0xFFF87171), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Tu cuenta será desactivada y eliminada en 30 días", color = TextSecondary, fontSize = 11.sp)
                }
                Text("›", color = Color(0xFFF87171), fontSize = 18.sp)
            }
        }
        deleteError?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = Color(0xFFF87171), fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
        }

        Spacer(Modifier.height(32.dp))
        Text(
            "Veri-on v1.0.0",
            color    = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(32.dp))
    }
    if (showPrivacy) {
        LegalDialog(title = "Política de privacidad", text = PRIVACY_TEXT, onDismiss = { showPrivacy = false })
    }
    if (showTerms) {
        LegalDialog(title = "Términos y condiciones", text = TERMS_TEXT, onDismiss = { showTerms = false })
    }
    if (showDeleteDlg) {
        DeleteAccountDialog(
            deleting   = deleting,
            onDismiss  = { showDeleteDlg = false; deleteError = null },
            onConfirm  = {
                scope.launch {
                    deleting = true; deleteError = null
                    val token = tokenManager.getAccessToken()
                    if (token == null) { deleteError = "Sin sesión activa"; deleting = false; return@launch }
                    val r = ApiClient.delete("/api/auth/me", token)
                    deleting = false
                    if (r.success) {
                        showDeleteDlg = false
                        onAccountDeleted()
                    } else {
                        deleteError = r.error ?: "Error al eliminar la cuenta"
                        showDeleteDlg = false
                    }
                }
            }
        )
    }
}

@Composable
private fun LegalDialog(title: String, text: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF120C2F))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
                .padding(20.dp)
        ) {
            Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick  = onDismiss,
                modifier = Modifier.align(Alignment.End),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = BrandBlue)
            ) {
                Text("Cerrar", color = Color.White)
            }
        }
    }
}

@Composable
private fun DeleteAccountDialog(
    deleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = { if (!deleting) onDismiss() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF120C2F))
                .border(1.dp, Color(0xFFF87171).copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                .padding(20.dp)
        ) {
            Text("Eliminar cuenta", color = Color(0xFFF87171), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(
                "Tu cuenta será desactivada inmediatamente y eliminada de forma permanente en 30 días.\n\nSi vuelves a iniciar sesión antes de esa fecha, tu cuenta se restaurará automáticamente.",
                color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "⚠ Esta acción cerrará tu sesión ahora.",
                color = Color(0xFFFBBF24), fontSize = 12.sp, fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick  = onDismiss,
                    enabled  = !deleting,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick  = onConfirm,
                    enabled  = !deleting,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = Color(0xFFF87171),
                        contentColor           = Color.White,
                        disabledContainerColor = Color(0xFFF87171).copy(alpha = 0.4f),
                        disabledContentColor   = Color.White.copy(alpha = 0.6f)
                    )
                ) {
                    if (deleting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Eliminar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private val PRIVACY_TEXT = """
Veri-on recoge la información necesaria para conectar técnicos y empresas dentro del territorio peruano.

1. Datos que recopilamos
   - Nombre, correo electrónico y foto de perfil.
   - Información profesional: categoría, nivel, habilidades, experiencia y proyectos.
   - Ubicación aproximada (latitud/longitud) únicamente cuando el usuario la proporciona de forma explícita.
   - Archivos subidos: foto de perfil, logo de empresa y CV en PDF.

2. Uso de la información
   - Mostrar tu perfil a empresas o técnicos que realizan búsquedas en la plataforma.
   - Gestionar solicitudes, colaboraciones y calificaciones entre usuarios.
   - Enviar notificaciones relacionadas con la actividad en la plataforma.

3. Almacenamiento y seguridad
   - Los datos se almacenan en Cloudflare D1 (base de datos SQLite en la nube) y Cloudflare R2 (almacenamiento de archivos).
   - Las contraseñas se guardan usando PBKDF2-SHA256 con 100 000 iteraciones; nunca en texto plano.
   - Las sesiones se gestionan mediante JWT firmados con HMAC-SHA256.

4. Compartición de datos
   - No vendemos ni cedemos tus datos a terceros.
   - Únicamente se exponen los datos de perfil que el propio usuario decide hacer públicos.

5. Eliminación de cuenta
   - El usuario puede eliminar su cuenta desde la aplicación. Los datos se marcan como inactivos por 30 días y luego pueden ser borrados definitivamente.

6. Contacto
   - Para consultas sobre privacidad escríbenos a: soporte@veri-on.com
""".trimIndent()

private val TERMS_TEXT = """
Al usar Veri-on aceptas las siguientes condiciones:

1. Uso permitido
   - Veri-on está destinado a técnicos y empresas del Perú que buscan establecer colaboraciones laborales.
   - Queda prohibido usar la plataforma para fines fraudulentos, publicitarios no autorizados o actividades ilegales.

2. Cuentas de usuario
   - Cada persona natural o jurídica puede tener una sola cuenta activa.
   - Eres responsable de mantener la confidencialidad de tus credenciales.

3. Contenido publicado
   - El usuario es responsable de la veracidad de la información de su perfil, CV, proyectos y convocatorias.
   - Veri-on puede eliminar contenido que infrinja estas condiciones sin previo aviso.

4. Calificaciones y reseñas
   - Las calificaciones post-colaboración son definitivas y visibles en el perfil público.
   - Queda prohibido publicar reseñas falsas o malintencionadas.

5. Propiedad intelectual
   - El contenido generado por los usuarios (fotos, CV, proyectos) pertenece a sus respectivos autores.
   - Veri-on tiene licencia limitada para mostrar ese contenido dentro de la plataforma.

6. Limitación de responsabilidad
   - Veri-on actúa como intermediario y no garantiza la calidad de los servicios pactados entre usuarios.
   - No somos responsables de disputas entre técnicos y empresas.

7. Modificaciones
   - Podemos actualizar estos términos notificando a los usuarios mediante la aplicación.

8. Contacto
   - soporte@veri-on.com
""".trimIndent()
