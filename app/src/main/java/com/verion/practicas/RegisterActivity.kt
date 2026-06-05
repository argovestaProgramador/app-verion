package com.verion.practicas

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.verion.practicas.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch
import org.json.JSONObject

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private var selectedRol = "TECNICO"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRoleSelector()

        binding.btnRegister.setOnClickListener { attemptRegister() }
        binding.tvLoginLink.setOnClickListener { finish() }
    }

    private fun setupRoleSelector() {
        binding.btnRolTecnico.setOnClickListener { selectRole("TECNICO") }
        binding.btnRolEmpresa.setOnClickListener { selectRole("EMPRESA") }
    }

    private fun selectRole(rol: String) {
        selectedRol = rol
        if (rol == "TECNICO") {
            binding.btnRolTecnico.setBackgroundResource(R.drawable.bg_role_button_selected)
            binding.btnRolTecnico.setTextColor(getColor(R.color.white))
            binding.btnRolEmpresa.setBackgroundResource(R.drawable.bg_role_button)
            binding.btnRolEmpresa.setTextColor(getColor(R.color.text_white_70))
        } else {
            binding.btnRolEmpresa.setBackgroundResource(R.drawable.bg_role_button_selected)
            binding.btnRolEmpresa.setTextColor(getColor(R.color.white))
            binding.btnRolTecnico.setBackgroundResource(R.drawable.bg_role_button)
            binding.btnRolTecnico.setTextColor(getColor(R.color.text_white_70))
        }
    }

    private fun attemptRegister() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        if (!validateFields(email, password, confirmPassword)) return

        setLoading(true)

        lifecycleScope.launch {
            val body = JSONObject().apply {
                put("email", email)
                put("password", password)
                put("rol", selectedRol)
            }

            val result = ApiClient.post("/api/auth/register", body)

            setLoading(false)

            if (result.success) {
                Snackbar.make(
                    binding.root,
                    "Cuenta creada. Inicia sesión para continuar.",
                    Snackbar.LENGTH_LONG
                ).show()
                val intent = Intent(this@RegisterActivity, LoginActivity::class.java).apply {
                    putExtra("email", email)
                }
                startActivity(intent)
                finish()
            } else {
                showError(result.error ?: getString(R.string.error_generic))
            }
        }
    }

    private fun validateFields(email: String, password: String, confirmPassword: String): Boolean {
        var valid = true

        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.error_email_empty)
            valid = false
        } else if (!email.contains("@")) {
            binding.tilEmail.error = getString(R.string.error_email_invalid)
            valid = false
        } else {
            binding.tilEmail.error = null
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.error_password_empty)
            valid = false
        } else if (password.length < 8) {
            binding.tilPassword.error = getString(R.string.error_password_short)
            valid = false
        } else {
            binding.tilPassword.error = null
        }

        if (confirmPassword != password) {
            binding.tilConfirmPassword.error = getString(R.string.error_passwords_mismatch)
            valid = false
        } else {
            binding.tilConfirmPassword.error = null
        }

        return valid
    }

    private fun setLoading(loading: Boolean) {
        binding.btnRegister.isEnabled = !loading
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}
