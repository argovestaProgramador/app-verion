package com.verion.practicas

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.verion.practicas.ui.screens.MainScreen
import com.verion.practicas.ui.theme.VeriOnTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val tokenManager = TokenManager(this)
        val isGuest = intent.getBooleanExtra("is_guest", false)

        setContent {
            VeriOnTheme {
                MainScreen(
                    isGuest      = isGuest,
                    tokenManager = tokenManager,
                    onLoginRequest = {
                        tokenManager.clear()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}
