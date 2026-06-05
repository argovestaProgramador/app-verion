package com.verion.practicas

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.verion.practicas.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch
import org.json.JSONObject


class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var tokenManager: TokenManager
    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        tokenManager = TokenManager(this)
        intent.getStringExtra("email")?.let { binding.etEmail.setText(it) }
        binding.ivEyeToggle.setOnClickListener { togglePassword() }
        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.btnGoogle.setOnClickListener { openGoogleSignIn() }
        binding.tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.tvGuestMode.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("is_guest", true)
            }
            startActivity(intent)
            finish()
        }
    }

    private fun togglePassword() {
        passwordVisible = !passwordVisible
        if (passwordVisible) {
            binding.etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            binding.ivEyeToggle.alpha = 1f
        } else {
            binding.etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            binding.ivEyeToggle.alpha = 0.4f
        }
        // Mueve cursor al final
        binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
    }

    private fun openGoogleSignIn() {
        lifecycleScope.launch {
            setLoading(true)
            val result = ApiClient.get("/api/auth/google?platform=ANDROID")
            setLoading(false)

            if (result.success && result.data != null) {
                val googleUrl = result.data.getString("url")
                CustomTabsIntent.Builder().setShowTitle(false).build()
                    .launchUrl(this@LoginActivity, Uri.parse(googleUrl))
            } else {
                showError(getString(R.string.error_generic))
            }
        }
    }

    private fun attemptLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (email.isEmpty()) { showError(getString(R.string.error_email_empty)); return }
        if (!email.contains("@")) { showError(getString(R.string.error_email_invalid)); return }
        if (password.isEmpty()) { showError(getString(R.string.error_password_empty)); return }

        setLoading(true)

        lifecycleScope.launch {
            val body = JSONObject().apply {
                put("email", email)
                put("password", password)
                put("plataforma", "ANDROID")
            }

            val result = ApiClient.post("/api/auth/login", body)
            setLoading(false)

            if (result.success && result.data != null) {
                val accessToken = result.data.getString("accessToken")
                val refreshToken = result.data.getString("refreshToken")
                val user = result.data.getJSONObject("user")

                tokenManager.saveSession(
                    accessToken, refreshToken,
                    user.getString("id"),
                    user.getString("email"),
                    user.getString("rol")
                )

                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            } else {
                showError(result.error ?: getString(R.string.error_generic))
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnLogin.isEnabled = !loading
        binding.btnGoogle.isEnabled = !loading
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}
