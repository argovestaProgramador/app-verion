package com.verion.practicas.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.verion.practicas.ApiClient
import com.verion.practicas.TokenManager
import com.verion.practicas.ui.components.AnimatedBackground
import com.verion.practicas.ui.components.BottomNavBar
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun MainScreen(
    isGuest: Boolean,
    tokenManager: TokenManager,
    onLoginRequest: () -> Unit
) {
    val rol             = if (isGuest) "GUEST" else tokenManager.getUserRol() ?: "GUEST"
    var selectedNav     by remember { mutableIntStateOf(0) }
    var showEditProfile       by remember { mutableStateOf(false) }
    var showCVScreen          by remember { mutableStateOf(false) }
    var showNotificaciones    by remember { mutableStateOf(false) }
    var showSettings          by remember { mutableStateOf(false) }
    val scope           = rememberCoroutineScope()

    var selectedConvocatoria by remember { mutableStateOf<JSONObject?>(null) }
    var selectedEmpresaId    by remember { mutableStateOf<String?>(null) }
    var selectedTecnicoId    by remember { mutableStateOf<String?>(null) }
    var postuladas           by remember { mutableStateOf<Set<String>>(emptySet()) }

    if (showSettings)                BackHandler { showSettings = false }
    if (showEditProfile)             BackHandler { showEditProfile = false }
    if (showCVScreen)                BackHandler { showCVScreen = false }
    if (showNotificaciones)          BackHandler { showNotificaciones = false }
    if (selectedConvocatoria != null) BackHandler { selectedConvocatoria = null }
    if (selectedTecnicoId != null)    BackHandler { selectedTecnicoId = null }
    if (selectedEmpresaId != null)    BackHandler { selectedEmpresaId = null }

    val onLogout: () -> Unit = {
        scope.launch {
            val token = tokenManager.getAccessToken()
            if (token != null) ApiClient.post("/api/auth/logout", JSONObject(), token)
            tokenManager.clear()
            onLoginRequest()
        }
    }

    val anyOverlay = showEditProfile || showCVScreen || showNotificaciones || showSettings ||
            selectedConvocatoria != null || selectedEmpresaId != null || selectedTecnicoId != null

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground()
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF000000).copy(alpha = 0.26f)))

        AnimatedVisibility(
            visible = !anyOverlay,
            enter   = slideInHorizontally { -it } + fadeIn(),
            exit    = slideOutHorizontally { -it } + fadeOut()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedNav) {
                        0 -> BuscarScreen(
                                tokenManager         = tokenManager,
                                rol                  = rol,
                                isGuest              = isGuest,
                                postuladas           = postuladas,
                                onConvocatoriaClick  = { selectedConvocatoria = it },
                                onTecnicoClick       = { selectedTecnicoId = it },
                                onEmpresaClick       = { selectedEmpresaId = it }
                             )
                        1 -> when (rol) {
                                "TECNICO" -> PostulacionesScreen(
                                                tokenManager   = tokenManager,
                                                onLoginRequest = onLoginRequest
                                             )
                                "EMPRESA" -> ConvocatoriasScreen(
                                                tokenManager = tokenManager,
                                                onBack       = null
                                             )
                                else      -> ProfileScreen(
                                                isGuest          = isGuest,
                                                tokenManager     = tokenManager,
                                                onLoginRequest   = onLoginRequest,
                                                onLogout         = onLogout,
                                                onEditProfile    = { showEditProfile = true },
                                                onCVRequest      = { showCVScreen = true },
                                                onNotificaciones = { showNotificaciones = true },
                                                onTecnicoClick   = { selectedTecnicoId = it },
                                                onEmpresaClick   = { selectedEmpresaId = it },
                                                onSettings       = { showSettings = true }
                                             )
                             }
                        else -> ProfileScreen(
                                    isGuest          = isGuest,
                                    tokenManager     = tokenManager,
                                    onLoginRequest   = onLoginRequest,
                                    onLogout         = onLogout,
                                    onEditProfile    = { showEditProfile = true },
                                    onCVRequest      = { showCVScreen = true },
                                    onNotificaciones = { showNotificaciones = true },
                                    onTecnicoClick   = { selectedTecnicoId = it },
                                    onEmpresaClick   = { selectedEmpresaId = it },
                                    onSettings       = { showSettings = true }
                                )
                    }
                }
                BottomNavBar(
                    selectedIndex = selectedNav,
                    onNavSelected = { selectedNav = it },
                    rol           = rol
                )
            }
        }

        AnimatedVisibility(
            visible = showEditProfile,
            enter   = slideInHorizontally { it } + fadeIn(),
            exit    = slideOutHorizontally { it } + fadeOut()
        ) {
            EditProfileScreen(tokenManager = tokenManager, onBack = { showEditProfile = false })
        }

        AnimatedVisibility(
            visible = showCVScreen,
            enter   = slideInHorizontally { it } + fadeIn(),
            exit    = slideOutHorizontally { it } + fadeOut()
        ) {
            CVScreen(tokenManager = tokenManager, onBack = { showCVScreen = false })
        }

        AnimatedVisibility(
            visible = showNotificaciones,
            enter   = slideInHorizontally { it } + fadeIn(),
            exit    = slideOutHorizontally { it } + fadeOut()
        ) {
            NotificacionesScreen(tokenManager = tokenManager, onBack = { showNotificaciones = false })
        }

        AnimatedVisibility(
            visible = showSettings,
            enter   = slideInHorizontally { it } + fadeIn(),
            exit    = slideOutHorizontally { it } + fadeOut()
        ) {
            SettingsScreen(
            onBack           = { showSettings = false },
            tokenManager     = tokenManager,
            onAccountDeleted = {
                scope.launch {
                    tokenManager.clear()
                    onLoginRequest()
                }
            }
        )
        }

        AnimatedVisibility(
            visible = selectedConvocatoria != null && selectedEmpresaId == null,
            enter   = slideInHorizontally { it } + fadeIn(),
            exit    = slideOutHorizontally { it } + fadeOut()
        ) {
            selectedConvocatoria?.let { conv ->
                ConvocatoriaDetalleScreen(
                    conv               = conv,
                    tokenManager       = tokenManager,
                    rol                = rol,
                    isGuest            = isGuest,
                    yaPostulado        = postuladas.contains(conv.optString("id", "")),
                    onBack             = { selectedConvocatoria = null },
                    onVerEmpresa       = { id -> selectedEmpresaId = id },
                    onPostuladoSuccess = { id -> postuladas = postuladas + id }
                )
            }
        }

        AnimatedVisibility(
            visible = selectedTecnicoId != null && selectedEmpresaId == null,
            enter   = slideInHorizontally { it } + fadeIn(),
            exit    = slideOutHorizontally { it } + fadeOut()
        ) {
            selectedTecnicoId?.let { id ->
                TecnicoPublicoScreen(
                    tecnicoId    = id,
                    tokenManager = tokenManager,
                    rol          = rol,
                    onBack       = { selectedTecnicoId = null }
                )
            }
        }

        AnimatedVisibility(
            visible = selectedEmpresaId != null,
            enter   = slideInHorizontally { it } + fadeIn(),
            exit    = slideOutHorizontally { it } + fadeOut()
        ) {
            selectedEmpresaId?.let { id ->
                EmpresaPublicaScreen(
                    empresaId    = id,
                    tokenManager = tokenManager,
                    rol          = rol,
                    onBack       = { selectedEmpresaId = null }
                )
            }
        }
    }
}
