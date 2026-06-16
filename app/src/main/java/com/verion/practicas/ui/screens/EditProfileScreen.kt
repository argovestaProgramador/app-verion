package com.verion.practicas.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import com.verion.practicas.ApiClient
import com.verion.practicas.R
import com.verion.practicas.TokenManager
import com.verion.practicas.ui.components.GlassCard
import com.verion.practicas.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private fun JSONObject.str(key: String): String =
    if (isNull(key)) "" else optString(key, "")

private fun JSONArray?.toObjectList(): List<JSONObject> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { runCatching { getJSONObject(it) }.getOrNull() }
}

@Composable
fun EditProfileScreen(
    tokenManager: TokenManager,
    onBack: () -> Unit
) {
    val rol     = tokenManager.getUserRol() ?: "TECNICO"
    val email   = tokenManager.getUserEmail() ?: ""
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var descripcion  by remember { mutableStateOf("") }
    var emailProf    by remember { mutableStateOf("") }

    var nombre       by remember { mutableStateOf("") }
    var nivel        by remember { mutableStateOf("PRACTICANTE") }
    var disponibili  by remember { mutableStateOf("INMEDIATA") }
    var githubUrl    by remember { mutableStateOf("") }
    var linkedinUrl  by remember { mutableStateOf("") }
    var instagramUrl by remember { mutableStateOf("") }
    var xUrl         by remember { mutableStateOf("") }
    var whatsapp     by remember { mutableStateOf("") }
    var fotoKey      by remember { mutableStateOf("") }
    var selectedFotoUri by remember { mutableStateOf<Uri?>(null) }
    var subiendoFoto by remember { mutableStateOf(false) }
    var msgFoto      by remember { mutableStateOf<String?>(null) }
    var categorias   by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selectedCatId by remember { mutableStateOf("") }
    var showCatDialog by remember { mutableStateOf(false) }
    var proyectos    by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var showProjForm by remember { mutableStateOf(false) }
    var editProjId   by remember { mutableStateOf<String?>(null) }
    var projTitulo   by remember { mutableStateOf("") }
    var projDesc     by remember { mutableStateOf("") }
    var projUrl      by remember { mutableStateOf("") }
    var savingProj   by remember { mutableStateOf(false) }
    var msgProj      by remember { mutableStateOf<String?>(null) }
    var pendingDeleteProj by remember { mutableStateOf<(() -> Unit)?>(null) }

    var razonSocial  by remember { mutableStateOf("") }
    var ruc          by remember { mutableStateOf("") }
    var sector       by remember { mutableStateOf("") }
    var sitioWeb     by remember { mutableStateOf("") }
    var logoKey      by remember { mutableStateOf("") }
    var selectedLogoUri by remember { mutableStateOf<Uri?>(null) }
    var subiendoLogo by remember { mutableStateOf(false) }
    var msgLogo      by remember { mutableStateOf<String?>(null) }
    var linkedinEmp  by remember { mutableStateOf("") }
    var instagramEmp by remember { mutableStateOf("") }
    var facebookUrl  by remember { mutableStateOf("") }
    var whatsappEmp  by remember { mutableStateOf("") }

    var locationLat  by remember { mutableStateOf<Double?>(null) }
    var locationLng  by remember { mutableStateOf<Double?>(null) }
    var locationAddr by remember { mutableStateOf("") }

    var todasHabilidades       by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var habilidadesSeleccionadas by remember { mutableStateOf<Set<String>>(emptySet()) }

    var isSaving       by remember { mutableStateOf(false) }
    var isLoading      by remember { mutableStateOf(true) }
    var saveMsg        by remember { mutableStateOf<String?>(null) }
    var showPhotoMenu  by remember { mutableStateOf(false) }
    var cameraUri      by remember { mutableStateOf<Uri?>(null) }

    val fotoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        selectedFotoUri = uri
        scope.launch {
            subiendoFoto = true; msgFoto = null
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                val token = tokenManager.getAccessToken()
                if (token != null) {
                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val ext = when { mimeType.contains("png") -> "png"; mimeType.contains("webp") -> "webp"; mimeType.contains("gif") -> "gif"; else -> "jpg" }
                    val r = ApiClient.upload("/api/uploads/foto", bytes, mimeType, "foto.$ext", token)
                    if (r.success) {
                        fotoKey = r.data?.optString("key") ?: fotoKey
                        msgFoto = "✓ Foto actualizada"
                    } else {
                        msgFoto = "✗ ${r.error ?: "Error al subir foto"}"
                        selectedFotoUri = null
                    }
                }
            }
            subiendoFoto = false
        }
    }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        selectedLogoUri = uri
        scope.launch {
            subiendoLogo = true; msgLogo = null
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                val token = tokenManager.getAccessToken()
                if (token != null) {
                    val mimeType = context.contentResolver.getType(uri) ?: "image/png"
                    val ext = when { mimeType.contains("png") -> "png"; mimeType.contains("webp") -> "webp"; mimeType.contains("gif") -> "gif"; else -> "jpg" }
                    val r = ApiClient.upload("/api/uploads/logo", bytes, mimeType, "logo.$ext", token)
                    if (r.success) {
                        logoKey = r.data?.optString("key") ?: logoKey
                        msgLogo = "✓ Logo actualizado"
                    } else {
                        msgLogo = "✗ ${r.error ?: "Error al subir logo"}"
                        selectedLogoUri = null
                    }
                }
            }
            subiendoLogo = false
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (!success) return@rememberLauncherForActivityResult
        val uri = cameraUri ?: return@rememberLauncherForActivityResult
        scope.launch {
            if (rol == "TECNICO") { subiendoFoto = true; msgFoto = null } else { subiendoLogo = true; msgLogo = null }
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                val token = tokenManager.getAccessToken()
                if (token != null) {
                    if (rol == "TECNICO") {
                        val r = ApiClient.upload("/api/uploads/foto", bytes, "image/jpeg", "foto.jpg", token)
                        if (r.success) { fotoKey = r.data?.optString("key") ?: fotoKey; selectedFotoUri = uri; msgFoto = "✓ Foto actualizada" }
                        else { msgFoto = "✗ ${r.error ?: "Error al subir foto"}"; selectedFotoUri = null }
                    } else {
                        val r = ApiClient.upload("/api/uploads/logo", bytes, "image/jpeg", "logo.jpg", token)
                        if (r.success) { logoKey = r.data?.optString("key") ?: logoKey; selectedLogoUri = uri; msgLogo = "✓ Logo actualizado" }
                        else { msgLogo = "✗ ${r.error ?: "Error al subir logo"}"; selectedLogoUri = null }
                    }
                }
            }
            if (rol == "TECNICO") subiendoFoto = false else subiendoLogo = false
        }
    }

    val camPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val file = File(context.cacheDir, "cam_${System.currentTimeMillis()}.jpg")
            val uri  = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    LaunchedEffect(Unit) {
        var token = tokenManager.getAccessToken()
        if (token != null) {
            var result = ApiClient.get("/api/perfil/me", token)
            if (!result.success && result.isUnauthorized) {
                val newToken = tokenManager.getRefreshToken()?.let { ApiClient.refreshToken(it) }
                if (newToken != null) { tokenManager.saveAccessToken(newToken); token = newToken; result = ApiClient.get("/api/perfil/me", newToken) }
            }
            if (result.success) {
                val p = result.data?.optJSONObject("perfil")
                if (p != null) {
                    descripcion = p.str("descripcion")
                    emailProf   = p.str("email_profesional")
                    locationLat   = if (p.isNull("lat")) null else p.optDouble("lat")
                    locationLng   = if (p.isNull("lng")) null else p.optDouble("lng")
                    locationAddr  = p.str("direccion")
                    if (rol == "TECNICO") {
                        nombre        = p.str("nombre_completo")
                        nivel         = p.str("nivel").takeIf { it.isNotBlank() } ?: "PRACTICANTE"
                        disponibili   = p.str("disponibilidad").takeIf { it.isNotBlank() } ?: "INMEDIATA"
                        fotoKey       = p.str("foto_key")
                        selectedCatId = p.str("categoria_principal_id")
                        githubUrl     = p.str("github_url")
                        linkedinUrl   = p.str("linkedin_url")
                        instagramUrl  = p.str("instagram_url")
                        xUrl          = p.str("x_url")
                        whatsapp      = p.str("whatsapp")
                    } else {
                        razonSocial  = p.str("razon_social")
                        ruc          = p.str("ruc")
                        sector       = p.str("sector")
                        sitioWeb     = p.str("sitio_web")
                        logoKey      = p.str("logo_key")
                        linkedinEmp  = p.str("linkedin_url")
                        instagramEmp = p.str("instagram_url")
                        facebookUrl  = p.str("facebook_url")
                        whatsappEmp  = p.str("whatsapp")
                    }
                }
                if (rol == "TECNICO") {
                    val userId = tokenManager.getUserId() ?: result.data?.optString("id") ?: ""
                    if (userId.isNotBlank()) {
                        val pr = ApiClient.get("/api/proyectos/$userId")
                        if (pr.success) proyectos = pr.dataArray.toObjectList()
                    }
                }
            }
        }
        val cachedCat = StaticCache.getCategorias(context)
        if (cachedCat != null) {
            categorias = (0 until cachedCat.length()).mapNotNull { runCatching { cachedCat.getJSONObject(it) }.getOrNull() }
        } else {
            val cr = ApiClient.get("/api/categorias")
            if (cr.success && cr.dataArray != null) { categorias = cr.dataArray.toObjectList(); StaticCache.saveCategorias(context, cr.dataArray) }
        }
        val cachedHab = StaticCache.getHabilidades(context)
        if (cachedHab != null) {
            todasHabilidades = (0 until cachedHab.length()).mapNotNull { runCatching { cachedHab.getJSONObject(it) }.getOrNull() }
        } else {
            val hr = ApiClient.get("/api/habilidades")
            if (hr.success && hr.dataArray != null) { todasHabilidades = hr.dataArray.toObjectList(); StaticCache.saveHabilidades(context, hr.dataArray) }
        }
        if (rol == "TECNICO" && token != null) {
            val cvr = ApiClient.get("/api/cv/mio", token)
            if (cvr.success) {
                val arr = cvr.data?.optJSONArray("habilidades")
                if (arr != null) {
                    habilidadesSeleccionadas = (0 until arr.length())
                        .mapNotNull { arr.optJSONObject(it)?.optString("id") }  // fix: API devuelve "id" no "habilidad_id"
                        .filter { it.isNotBlank() }
                        .toSet()
                }
            }
        }
        isLoading = false
    }

    val initial = when {
        nombre.isNotBlank()       -> nombre.first().uppercaseChar().toString()
        razonSocial.isNotBlank()  -> razonSocial.first().uppercaseChar().toString()
        else -> email.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    }
    val selectedCatName = categorias.find { it.str("id") == selectedCatId }?.str("nombre")
        ?: if (selectedCatId.isBlank()) "" else ""

    val completion = remember(nombre, descripcion, emailProf, githubUrl, linkedinUrl, fotoKey, selectedCatId, razonSocial, ruc) {
        var pts = 0f
        if (email.isNotBlank()) pts += 0.15f
        if (rol.isNotBlank())   pts += 0.05f
        if (rol == "TECNICO") {
            if (fotoKey.isNotBlank())      pts += 0.10f
            if (nombre.isNotBlank())       pts += 0.15f
            if (selectedCatId.isNotBlank()) pts += 0.05f
            if (descripcion.isNotBlank())  pts += 0.20f
            if (emailProf.isNotBlank())    pts += 0.10f
            if (githubUrl.isNotBlank() || linkedinUrl.isNotBlank()) pts += 0.20f
        } else {
            if (logoKey.isNotBlank())     pts += 0.10f
            if (razonSocial.isNotBlank()) pts += 0.20f
            if (descripcion.isNotBlank()) pts += 0.25f
            if (emailProf.isNotBlank())   pts += 0.10f
            if (ruc.isNotBlank())         pts += 0.10f
        }
        pts.coerceIn(0f, 1f)
    }

    val animatedProgress by animateFloatAsState(
        targetValue   = completion,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label         = "edit_progress"
    )

    pendingDeleteProj?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingDeleteProj = null },
            containerColor   = Color(0xFF1E1040),
            shape            = RoundedCornerShape(20.dp),
            title = { Text("¿Eliminar proyecto?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text  = { Text("Esta acción no se puede deshacer.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { action(); pendingDeleteProj = null }) {
                    Text("Eliminar", color = Color(0xFFF87171), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteProj = null }) { Text("Cancelar", color = TextSecondary) }
            }
        )
    }

    if (showCatDialog) {
        AlertDialog(
            onDismissRequest = { showCatDialog = false },
            containerColor   = Color(0xFF1E1040),
            shape            = RoundedCornerShape(20.dp),
            title = { Text("Categoría principal", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text  = {
                Column(modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    TextButton(
                        onClick = { selectedCatId = ""; showCatDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Sin categoría", color = TextSecondary, modifier = Modifier.fillMaxWidth()) }
                    categorias.forEach { cat ->
                        TextButton(
                            onClick = { selectedCatId = cat.str("id"); showCatDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                cat.str("nombre"),
                                color = if (selectedCatId == cat.str("id")) BrandBlue else Color.White,
                                fontWeight = if (selectedCatId == cat.str("id")) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
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
                "Editar Perfil",
                color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
            if (isSaving || isLoading || subiendoFoto || subiendoLogo) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).padding(end = 8.dp),
                    color = BrandBlue, strokeWidth = 2.dp
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Perfil completado", color = TextSecondary, fontSize = 11.sp)
                Text("${(animatedProgress * 100).toInt()}%", color = BrandBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(5.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(Color.White.copy(alpha = 0.10f))
            ) {
                Box(
                    modifier = Modifier.fillMaxHeight().fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(Brush.horizontalGradient(listOf(BrandBlue, BrandIndigo, BrandPurple)))
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        if (isLoading) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandBlue, strokeWidth = 2.dp)
            }
        } else {
            Column(
                modifier = Modifier.weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(96.dp).clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val hasImage = (if (rol == "TECNICO") fotoKey else logoKey).isNotBlank() || (if (rol == "TECNICO") selectedFotoUri else selectedLogoUri) != null
                            if (hasImage) {
                                val model = (if (rol == "TECNICO") selectedFotoUri else selectedLogoUri)
                                    ?: "${ApiClient.BASE_URL}/api/uploads/${if (rol == "TECNICO") fotoKey else logoKey}"
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(model).crossfade(true).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize()
                                        .background(Brush.linearGradient(listOf(OrbPurple, Color(0xFF9B6EFF)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(initial, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (subiendoFoto || subiendoLogo) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Color.White, strokeWidth = 2.5.dp)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        val fotoMsg = if (rol == "TECNICO") msgFoto else msgLogo
                        if (fotoMsg != null) {
                            Text(fotoMsg, color = if (fotoMsg.startsWith("✓")) Color(0xFF34D399) else Color(0xFFF87171), fontSize = 11.sp)
                            TextButton(
                                onClick = {
                                    if (rol == "TECNICO") { msgFoto = null } else { msgLogo = null }
                                    showPhotoMenu = true
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text("Cambiar de nuevo", color = BrandBlue, fontSize = 11.sp)
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = {
                                        if (rol == "TECNICO") fotoPickerLauncher.launch("image/*")
                                        else logoPickerLauncher.launch("image/*")
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("🖼 Galería", color = BrandBlue, fontSize = 12.sp)
                                }
                                TextButton(
                                    onClick = {
                                        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                        if (hasPerm) {
                                            val file = File(context.cacheDir, "cam_${System.currentTimeMillis()}.jpg")
                                            val uri  = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                            cameraUri = uri
                                            cameraLauncher.launch(uri)
                                        } else {
                                            camPermLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("📷 Cámara", color = BrandBlue, fontSize = 12.sp)
                                }
                            }
                        }
                        if (showPhotoMenu) {
                            AlertDialog(
                                onDismissRequest = { showPhotoMenu = false },
                                title = { Text(if (rol == "TECNICO") "Cambiar foto" else "Cambiar logo", style = MaterialTheme.typography.titleSmall) },
                                text = {
                                    Column {
                                        TextButton(onClick = {
                                            showPhotoMenu = false
                                            if (rol == "TECNICO") fotoPickerLauncher.launch("image/*")
                                            else logoPickerLauncher.launch("image/*")
                                        }) { Text("🖼 Elegir de galería", color = BrandBlue) }
                                        TextButton(onClick = {
                                            showPhotoMenu = false
                                            val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                            if (hasPerm) {
                                                val file = File(context.cacheDir, "cam_${System.currentTimeMillis()}.jpg")
                                                val uri  = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                                cameraUri = uri
                                                cameraLauncher.launch(uri)
                                            } else {
                                                camPermLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        }) { Text("📷 Tomar foto", color = BrandBlue) }
                                    }
                                },
                                confirmButton = {},
                                dismissButton = {
                                    TextButton(onClick = { showPhotoMenu = false }) { Text("Cancelar") }
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                if (rol == "TECNICO") {
                    SectionLabel("Información personal")
                    GlassField(nombre, { nombre = it }, "Nombre completo *")
                    GlassField(descripcion, { descripcion = it }, "Descripción / Bio", singleLine = false, minLines = 3)
                    GlassField(emailProf, { emailProf = it }, "Email profesional", keyboardType = KeyboardType.Email)

                    SectionLabel("Categoría principal")
                    GlassTapField(
                        value  = selectedCatName,
                        label  = "Seleccionar categoría",
                        onTap  = { showCatDialog = true }
                    )

                    SectionLabel("Nivel y disponibilidad")
                    OptionChipRow(
                        label    = "Nivel",
                        options  = listOf("PRACTICANTE" to "Practicante", "EGRESADO" to "Egresado", "CERTIFICADO" to "Certificado"),
                        selected = nivel,
                        onSelect = { nivel = it }
                    )
                    OptionChipRow(
                        label    = "Disponibilidad",
                        options  = listOf("INMEDIATA" to "Inmediata", "FECHA" to "A partir de fecha", "NO_DISPONIBLE" to "No disponible"),
                        selected = disponibili,
                        onSelect = { disponibili = it }
                    )

                    LocationSection(
                        tokenManager     = tokenManager,
                        label            = "Mi ubicación",
                        buttonGetText    = "Obtener mi ubicación GPS",
                        buttonUpdateText = "Actualizar mi ubicación GPS",
                        lat = locationLat, lng = locationLng, addr = locationAddr,
                        onSaved = { la, lo, ad -> locationLat = la; locationLng = lo; locationAddr = ad }
                    )

                    SectionLabel("Redes sociales")
                    GlassField(githubUrl, { githubUrl = it }, "GitHub (https://github.com/...)", keyboardType = KeyboardType.Uri)
                    GlassField(linkedinUrl, { linkedinUrl = it }, "LinkedIn (https://linkedin.com/in/...)", keyboardType = KeyboardType.Uri)
                    GlassField(instagramUrl, { instagramUrl = it }, "Instagram (https://instagram.com/...)", keyboardType = KeyboardType.Uri)
                    GlassField(xUrl, { xUrl = it }, "X / Twitter (https://x.com/...)", keyboardType = KeyboardType.Uri)
                    GlassField(whatsapp, { whatsapp = it }, "WhatsApp (+51 999 999 999)", keyboardType = KeyboardType.Phone)

                    if (todasHabilidades.isNotEmpty()) {
                        SectionLabel("Mis habilidades (${habilidadesSeleccionadas.size} seleccionadas)")
                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                var habSearch by remember { mutableStateOf("") }
                                GlassCard(modifier = Modifier.fillMaxWidth().height(42.dp), cornerRadius = 10.dp) {
                                    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(painterResource(R.drawable.ic_nav_search), null, tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(8.dp))
                                        BasicTextField(
                                            value = habSearch,
                                            onValueChange = { habSearch = it },
                                            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                                            cursorBrush = SolidColor(Color.White),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            decorationBox = { inner ->
                                                if (habSearch.isEmpty()) Text("Buscar habilidad...", color = Color.White.copy(alpha = 0.3f), fontSize = 13.sp)
                                                inner()
                                            }
                                        )
                                    }
                                }
                                val filtered = if (habSearch.isBlank()) todasHabilidades
                                               else todasHabilidades.filter { it.str("nombre").contains(habSearch, ignoreCase = true) }
                                val sorted = filtered.sortedWith(compareByDescending { habilidadesSeleccionadas.contains(it.str("id")) })
                                if (filtered.isEmpty()) {
                                    Text("Sin resultados para \"$habSearch\"", color = TextSecondary, fontSize = 11.sp)
                                } else {
                                    sorted.chunked(3).forEach { rowItems ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            rowItems.forEach { hab ->
                                                val habId    = hab.str("id")
                                                val selected = habilidadesSeleccionadas.contains(habId)
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(50.dp))
                                                        .background(if (selected) BrandBlue.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f))
                                                        .border(1.dp, if (selected) BrandBlue else Color.White.copy(alpha = 0.15f), RoundedCornerShape(50.dp))
                                                        .clickable {
                                                            habilidadesSeleccionadas = if (selected)
                                                                habilidadesSeleccionadas - habId
                                                            else
                                                                habilidadesSeleccionadas + habId
                                                        }
                                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        hab.str("nombre"),
                                                        color = if (selected) BrandBlue else Color.White.copy(alpha = 0.55f),
                                                        fontSize = 11.sp,
                                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "MIS PROYECTOS",
                            color = Color.White.copy(alpha = 0.38f), fontSize = 10.sp,
                            letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        GlassCard(
                            modifier = Modifier.clickable {
                                editProjId = null; projTitulo = ""; projDesc = ""; projUrl = ""
                                msgProj = null; showProjForm = true
                            },
                            cornerRadius = 50.dp
                        ) {
                            Text("+ Agregar", color = BrandBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
                        }
                    }

                    if (showProjForm) {
                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        if (editProjId != null) "Editar proyecto" else "Nuevo proyecto",
                                        color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { showProjForm = false; editProjId = null }, contentPadding = PaddingValues(8.dp)) {
                                        Text("Cancelar", color = TextSecondary, fontSize = 12.sp)
                                    }
                                }
                                GlassField(projTitulo, { projTitulo = it }, "Título del proyecto *")
                                GlassField(projDesc, { projDesc = it }, "Descripción (opcional)", singleLine = false, minLines = 2)
                                GlassField(projUrl, { projUrl = it }, "Enlace (GitHub, web, demo...)", keyboardType = KeyboardType.Uri)
                                msgProj?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp) }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            if (projTitulo.isBlank()) { msgProj = "El título es requerido"; return@launch }
                                            savingProj = true; msgProj = null
                                            val token = tokenManager.getAccessToken() ?: run { msgProj = "Sesión expirada"; savingProj = false; return@launch }
                                            val body = JSONObject().apply {
                                                put("titulo", projTitulo.trim())
                                                if (projDesc.isNotBlank()) put("descripcion", projDesc.trim())
                                                if (projUrl.isNotBlank()) put("url", projUrl.trim())
                                            }
                                            val result = if (editProjId != null)
                                                ApiClient.put("/api/proyectos/$editProjId", body, token)
                                            else
                                                ApiClient.post("/api/proyectos", body, token)
                                            savingProj = false
                                            if (result.success && result.data != null) {
                                                val item = result.data
                                                proyectos = if (editProjId != null)
                                                    proyectos.map { if (it.optString("id") == editProjId) item else it }
                                                else
                                                    listOf(item) + proyectos
                                                showProjForm = false; editProjId = null
                                            } else {
                                                msgProj = "✗ ${result.error ?: "Error al guardar"}"
                                            }
                                        }
                                    },
                                    enabled  = !savingProj,
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    shape    = RoundedCornerShape(50.dp),
                                    colors   = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1A0F3D))
                                ) { Text(if (savingProj) "Guardando…" else "Guardar", fontWeight = FontWeight.Bold) }
                            }
                        }
                    }

                    if (proyectos.isEmpty() && !showProjForm) {
                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                            Text(
                                "Aún no tienes proyectos. Agrega tu portafolio.",
                                color = Color.White.copy(alpha = 0.30f), fontSize = 13.sp,
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            )
                        }
                    }

                    proyectos.forEach { proj ->
                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(proj.str("titulo"), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    val url = proj.str("url")
                                    if (url.isNotBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(url, color = BrandBlue, fontSize = 11.sp, maxLines = 1)
                                    }
                                    val desc = proj.str("descripcion")
                                    if (desc.isNotBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(desc, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 2)
                                    }
                                }
                                Row {
                                    IconButton(onClick = {
                                        editProjId = proj.optString("id")
                                        projTitulo = proj.str("titulo")
                                        projDesc   = proj.str("descripcion")
                                        projUrl    = proj.str("url")
                                        msgProj    = null
                                        showProjForm = true
                                    }, modifier = Modifier.size(32.dp)) { Text("✏️", fontSize = 14.sp) }
                                    IconButton(onClick = {
                                        pendingDeleteProj = {
                                            scope.launch {
                                                val id = proj.optString("id")
                                                val token = tokenManager.getAccessToken() ?: return@launch
                                                val r = ApiClient.delete("/api/proyectos/$id", token)
                                                if (r.success) proyectos = proyectos.filter { it.optString("id") != id }
                                            }
                                        }
                                    }, modifier = Modifier.size(32.dp)) { Text("🗑️", fontSize = 14.sp) }
                                }
                            }
                        }
                    }
                }

                if (rol == "EMPRESA") {
                    SectionLabel("Información de la empresa")
                    GlassField(razonSocial, { razonSocial = it }, "Razón social *")
                    GlassField(ruc, { ruc = it }, "RUC (11 dígitos)", keyboardType = KeyboardType.Number)
                    GlassField(sector, { sector = it }, "Sector (ej. Tecnología, Construcción)")
                    GlassField(descripcion, { descripcion = it }, "Descripción de la empresa", singleLine = false, minLines = 3)
                    GlassField(sitioWeb, { sitioWeb = it }, "Sitio web (https://...)", keyboardType = KeyboardType.Uri)
                    GlassField(emailProf, { emailProf = it }, "Email de contacto", keyboardType = KeyboardType.Email)

                    SectionLabel("Redes sociales")
                    GlassField(linkedinEmp, { linkedinEmp = it }, "LinkedIn (https://linkedin.com/company/...)", keyboardType = KeyboardType.Uri)
                    GlassField(instagramEmp, { instagramEmp = it }, "Instagram (https://instagram.com/...)", keyboardType = KeyboardType.Uri)
                    GlassField(facebookUrl, { facebookUrl = it }, "Facebook (https://facebook.com/...)", keyboardType = KeyboardType.Uri)
                    GlassField(whatsappEmp, { whatsappEmp = it }, "WhatsApp (+51 999 999 999)", keyboardType = KeyboardType.Phone)

                    LocationSection(
                        tokenManager     = tokenManager,
                        label            = "Ubicación de la sede",
                        buttonGetText    = "Obtener ubicación de la sede",
                        buttonUpdateText = "Actualizar ubicación de la sede",
                        lat = locationLat, lng = locationLng, addr = locationAddr,
                        onSaved = { la, lo, ad -> locationLat = la; locationLng = lo; locationAddr = ad }
                    )
                }

                saveMsg?.let { msg ->
                    Text(
                        msg,
                        color = if (msg.startsWith("✓")) Color(0xFF34D399) else Color(0xFFF87171),
                        fontSize = 13.sp, modifier = Modifier.fillMaxWidth()
                    )
                }
                Button(
                    onClick = {
                        scope.launch {
                            isSaving = true; saveMsg = null
                            val token = tokenManager.getAccessToken() ?: run {
                                saveMsg = "Sesión expirada"; isSaving = false; return@launch
                            }
                            val body = JSONObject()
                            if (rol == "TECNICO") {
                                body.put("nombre_completo", nombre.trim())
                                body.put("nivel", nivel)
                                body.put("disponibilidad", disponibili)
                                body.put("descripcion", descripcion.trim())
                                if (selectedCatId.isNotBlank()) body.put("categoria_principal_id", selectedCatId)
                                if (emailProf.isNotBlank())    body.put("email_profesional", emailProf.trim())
                                if (githubUrl.isNotBlank())    body.put("github_url",   githubUrl.trim())
                                if (linkedinUrl.isNotBlank())  body.put("linkedin_url", linkedinUrl.trim())
                                if (instagramUrl.isNotBlank()) body.put("instagram_url", instagramUrl.trim())
                                if (xUrl.isNotBlank())         body.put("x_url", xUrl.trim())
                                if (whatsapp.isNotBlank())     body.put("whatsapp", whatsapp.trim())
                            } else {
                                body.put("razon_social", razonSocial.trim())
                                body.put("descripcion",  descripcion.trim())
                                if (ruc.isNotBlank())          body.put("ruc", ruc.trim())
                                if (sector.isNotBlank())       body.put("sector", sector.trim())
                                if (sitioWeb.isNotBlank())     body.put("sitio_web", sitioWeb.trim())
                                if (emailProf.isNotBlank())    body.put("email_profesional", emailProf.trim())
                                if (linkedinEmp.isNotBlank())  body.put("linkedin_url", linkedinEmp.trim())
                                if (instagramEmp.isNotBlank()) body.put("instagram_url", instagramEmp.trim())
                                if (facebookUrl.isNotBlank())  body.put("facebook_url", facebookUrl.trim())
                                if (whatsappEmp.isNotBlank())  body.put("whatsapp", whatsappEmp.trim())
                            }
                            val endpoint = if (rol == "TECNICO") "/api/perfil/tecnico" else "/api/perfil/empresa"
                            var result = ApiClient.put(endpoint, body, token)
                            if (!result.success && result.isUnauthorized) {
                                val newToken = tokenManager.getRefreshToken()?.let { ApiClient.refreshToken(it) }
                                if (newToken != null) { tokenManager.saveAccessToken(newToken); result = ApiClient.put(endpoint, body, newToken) }
                            }
                            if (result.success && rol == "TECNICO" && habilidadesSeleccionadas.isNotEmpty()) {
                                val habBody = JSONObject().apply {
                                    put("habilidad_ids", org.json.JSONArray(habilidadesSeleccionadas.toList()))
                                }
                                ApiClient.put("/api/cv/habilidades", habBody, token)
                            }
                            isSaving = false
                            saveMsg  = if (result.success) "✓ Perfil guardado correctamente"
                                       else "✗ ${result.error ?: "Error al guardar"}"
                            if (result.success) {
                                kotlinx.coroutines.delay(800)
                                onBack()
                            }
                        }
                    },
                    enabled   = !isSaving && !isLoading,
                    modifier  = Modifier.fillMaxWidth().height(52.dp),
                    shape     = RoundedCornerShape(50.dp),
                    colors    = ButtonDefaults.buttonColors(
                        containerColor         = Color.White,
                        contentColor           = Color(0xFF1A0F3D),
                        disabledContainerColor = Color.White.copy(alpha = 0.35f)
                    )
                ) {
                    Text(if (isSaving) "Guardando…" else "Guardar cambios", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun GlassField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            if (value.isNotEmpty()) {
                Text(label, color = BrandBlue.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
            }
            BasicTextField(
                value = value, onValueChange = onValueChange,
                singleLine = singleLine,
                minLines = if (singleLine) 1 else minLines,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                cursorBrush = SolidColor(BrandBlue),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(label, color = Color.White.copy(alpha = 0.38f), fontSize = 15.sp)
                    inner()
                }
            )
        }
    }
}

@Composable
private fun GlassTapField(value: String, label: String, onTap: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onTap), cornerRadius = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (value.isNotBlank()) {
                    Text(label, color = BrandBlue.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    value.ifBlank { label },
                    color = if (value.isBlank()) Color.White.copy(alpha = 0.38f) else Color.White,
                    fontSize = 15.sp
                )
            }
            Text("▾", color = Color.White.copy(alpha = 0.5f), fontSize = 18.sp)
        }
    }
}

@Composable
private fun OptionChipRow(label: String, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Text(label.uppercase(), color = Color.White.copy(alpha = 0.38f), fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        options.forEach { (value, display) ->
            val isSelected = value == selected
            GlassCard(modifier = Modifier.clickable { onSelect(value) }, cornerRadius = 50.dp) {
                Text(
                    display,
                    color = if (isSelected) BrandBlue else Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), color = Color.White.copy(alpha = 0.38f), fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun LocationSection(
    tokenManager: TokenManager,
    label: String,
    buttonGetText: String,
    buttonUpdateText: String,
    lat: Double?,
    lng: Double?,
    addr: String,
    onSaved: (Double, Double, String) -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    var gettingLoc  by remember { mutableStateOf(false) }
    var msgLocation by remember { mutableStateOf<String?>(null) }

    suspend fun fetchAndSave() {
        gettingLoc = true; msgLocation = null
        val loc = fetchCurrentLocation(context)
        if (loc != null) {
            val token = tokenManager.getAccessToken() ?: run { gettingLoc = false; return }
            val body = JSONObject().apply { put("lat", loc.first); put("lng", loc.second) }
            val result = ApiClient.put("/api/perfil/ubicacion", body, token)
            if (result.success && result.data != null) {
                onSaved(result.data.optDouble("lat"), result.data.optDouble("lng"), result.data.str("direccion"))
                msgLocation = "✓ Ubicación guardada"
            } else { msgLocation = "✗ ${result.error ?: "Error al guardar"}" }
        } else {
            msgLocation = "No se pudo obtener la ubicación. Activa el GPS."
        }
        gettingLoc = false
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) scope.launch { fetchAndSave() }
        else msgLocation = "Se necesita permiso de ubicación para guardar"
    }

    SectionLabel(label)
    if (lat != null && lng != null) {
        LocationMapView(lat = lat, lng = lng)
        Spacer(Modifier.height(6.dp))
        if (addr.isNotBlank()) {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("📍", fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(addr, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !gettingLoc) {
            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (hasFine || hasCoarse) scope.launch { fetchAndSave() }
            else permLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        },
        cornerRadius = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (gettingLoc) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BrandBlue, strokeWidth = 2.dp)
            } else {
                Text("📍", fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                if (lat != null) buttonUpdateText else buttonGetText,
                color = if (gettingLoc) TextSecondary else Color.White,
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold
            )
        }
    }
    msgLocation?.let { msg ->
        Text(msg, color = if (msg.startsWith("✓")) Color(0xFF34D399) else Color(0xFFF87171), fontSize = 12.sp)
    }
}

@Composable
private fun LocationMapView(lat: Double, lng: Double) {
    val context = LocalContext.current
    val mapRef   = remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(Unit) { onDispose { mapRef.value?.onDetach() } }

    AndroidView(
        factory = { ctx ->
            Configuration.getInstance().apply {
                load(ctx, ctx.getSharedPreferences("osm_prefs", 0))
                userAgentValue  = ctx.packageName
                osmdroidBasePath      = File(ctx.cacheDir, "osmdroid")
                osmdroidTileCache     = File(ctx.cacheDir, "osmdroid/tiles")
            }
            MapView(ctx).also { mv ->
                mapRef.value = mv
                mv.setTileSource(TileSourceFactory.MAPNIK)
                mv.setMultiTouchControls(true)
                mv.isTilesScaledToDpi = true
                val pt = GeoPoint(lat, lng)
                mv.controller.setZoom(15.0)
                mv.controller.setCenter(pt)
                val m = Marker(mv).apply {
                    position = pt
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                mv.overlays.add(m)
            }
        },
        update = { mv ->
            mv.overlays.clear()
            val pt = GeoPoint(lat, lng)
            mv.controller.setCenter(pt)
            mv.overlays.add(Marker(mv).apply {
                position = pt
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            })
            mv.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(14.dp))
    )
}

private suspend fun fetchCurrentLocation(context: Context): Pair<Double, Double>? {
    val hasFine   = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)   == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!hasFine && !hasCoarse) return null

    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)) {
        try {
            if (lm.isProviderEnabled(provider)) {
                val loc = lm.getLastKnownLocation(provider)
                if (loc != null) return Pair(loc.latitude, loc.longitude)
            }
        } catch (_: Exception) {}
    }

    return withTimeoutOrNull(15_000L) {
        suspendCancellableCoroutine { cont ->
            val ht = HandlerThread("loc_thread").apply { start() }
            val listener = object : LocationListener {
                override fun onLocationChanged(loc: android.location.Location) {
                    lm.removeUpdates(this); ht.quitSafely()
                    if (cont.isActive) cont.resumeWith(Result.success(Pair(loc.latitude, loc.longitude)))
                }
                override fun onProviderDisabled(p: String) {}
            }
            val provider = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .firstOrNull { lm.isProviderEnabled(it) }
            if (provider == null) {
                ht.quitSafely()
                cont.resumeWith(Result.success(null))
                return@suspendCancellableCoroutine
            }
            try {
                lm.requestLocationUpdates(provider, 0L, 0f, listener, ht.looper)
                cont.invokeOnCancellation { _ -> lm.removeUpdates(listener); ht.quitSafely() }
            } catch (e: Exception) {
                ht.quitSafely()
                cont.resumeWith(Result.success(null))
            }
        }
    }
}
