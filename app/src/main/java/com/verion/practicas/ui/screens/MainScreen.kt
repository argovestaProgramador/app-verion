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
    var selectedNav      by remember { mutableIntStateOf(0) }
    var showEditProfile  by remember { mutableStateOf(false) }
    var showCVScreen     by remember { mutableStateOf(false) }
    val scope            = rememberCoroutineScope()

    if (showEditProfile) { BackHandler { showEditProfile = false } }
    if (showCVScreen)    { BackHandler { showCVScreen = false } }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF000000).copy(alpha = 0.26f))
        )

        AnimatedVisibility(
            visible = showEditProfile,
            enter = slideInHorizontally { it } + fadeIn(),
            exit  = slideOutHorizontally { it } + fadeOut()
        ) {
            EditProfileScreen(
                tokenManager = tokenManager,
                onBack = { showEditProfile = false }
            )
        }

        AnimatedVisibility(
            visible = showCVScreen,
            enter = slideInHorizontally { it } + fadeIn(),
            exit  = slideOutHorizontally { it } + fadeOut()
        ) {
            CVScreen(
                tokenManager = tokenManager,
                onBack = { showCVScreen = false }
            )
        }

        AnimatedVisibility(
            visible = !showEditProfile && !showCVScreen,
            enter = slideInHorizontally { -it } + fadeIn(),
            exit  = slideOutHorizontally { -it } + fadeOut()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedNav) {
                        0 -> BuscarScreen()
                        1 -> ProfileScreen(
                                isGuest        = isGuest,
                                tokenManager   = tokenManager,
                                onLoginRequest = onLoginRequest,
                                onLogout       = {
                                    scope.launch {
                                        val token = tokenManager.getAccessToken()
                                        if (token != null) {
                                            ApiClient.post("/api/auth/logout", JSONObject(), token)
                                        }
                                        tokenManager.clear()
                                        onLoginRequest()
                                    }
                                },
                                onEditProfile  = { showEditProfile = true },
                                onCVRequest    = { showCVScreen = true }
                             )
                        2 -> EmpresaScreen()
                    }
                }

                BottomNavBar(
                    selectedIndex = selectedNav,
                    onNavSelected = { selectedNav = it }
                )
            }
        }
    }
}
