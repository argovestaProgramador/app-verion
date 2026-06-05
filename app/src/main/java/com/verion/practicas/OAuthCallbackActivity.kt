package com.verion.practicas

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

class OAuthCallbackActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = intent?.data
        if (data == null) {
            goToLogin()
            return
        }

        val oauthError = data.getQueryParameter("oauth_error")
        if (oauthError != null) {
            goToLogin(error = "Error con Google: $oauthError")
            return
        }

        val accessToken = data.getQueryParameter("accessToken")
        val refreshToken = data.getQueryParameter("refreshToken")
        val userId = data.getQueryParameter("id") ?: ""
        val email = data.getQueryParameter("email") ?: ""
        val rol = data.getQueryParameter("rol") ?: "TECNICO"
        val isNew = data.getQueryParameter("is_new") == "1"

        if (accessToken == null || refreshToken == null) {
            goToLogin(error = getString(R.string.error_generic))
            return
        }

        val tokenManager = TokenManager(this)
        tokenManager.saveSession(accessToken, refreshToken, userId, email, rol)

        if (isNew) {
            val intent = Intent(this, OnboardingActivity::class.java).apply {
                putExtra("accessToken", accessToken)
            }
            startActivity(intent)
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }

    private fun goToLogin(error: String? = null) {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
        error?.let {
        }
    }
}
