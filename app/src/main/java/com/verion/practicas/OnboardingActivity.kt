package com.verion.practicas

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.verion.practicas.databinding.ActivityOnboardingBinding
import kotlinx.coroutines.launch
import org.json.JSONObject

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var tokenManager: TokenManager
    private var selectedRol = "TECNICO"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        setupRoleCards()
        binding.btnContinue.setOnClickListener { attemptOnboarding() }
    }

    private fun setupRoleCards() {
        binding.cardTecnico.setOnClickListener { selectRole("TECNICO") }
        binding.cardEmpresa.setOnClickListener { selectRole("EMPRESA") }
        selectRole("TECNICO")
    }

    private fun selectRole(rol: String) {
        selectedRol = rol
        if (rol == "TECNICO") {
            binding.cardTecnico.setBackgroundResource(R.drawable.bg_role_card_selected)
            binding.cardEmpresa.setBackgroundResource(R.drawable.bg_role_card)
        } else {
            binding.cardEmpresa.setBackgroundResource(R.drawable.bg_role_card_selected)
            binding.cardTecnico.setBackgroundResource(R.drawable.bg_role_card)
        }
    }

    private fun attemptOnboarding() {
        val nombre = binding.etName.text.toString().trim()
        if (nombre.isEmpty()) {
            binding.tilName.error = getString(R.string.error_name_empty)
            return
        }
        binding.tilName.error = null

        val accessToken = tokenManager.getAccessToken() ?: run {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            val body = JSONObject().apply {
                put("rol", selectedRol)
                put("nombre", nombre)
            }

            val result = ApiClient.post("/api/auth/onboarding", body, accessToken)
            setLoading(false)

            if (result.success && result.data != null) {
                val newToken = result.data.optString("accessToken", accessToken)
                tokenManager.saveSession(
                    newToken,
                    tokenManager.getRefreshToken() ?: "",
                    tokenManager.getUserId() ?: "",
                    tokenManager.getUserEmail() ?: "",
                    selectedRol
                )
                startActivity(Intent(this@OnboardingActivity, MainActivity::class.java))
                finish()
            } else {
                showError(result.error ?: getString(R.string.error_generic))
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnContinue.isEnabled = !loading
        binding.cardTecnico.isClickable = !loading
        binding.cardEmpresa.isClickable = !loading
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}
