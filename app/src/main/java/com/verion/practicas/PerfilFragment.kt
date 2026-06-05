package com.verion.practicas

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.verion.practicas.databinding.FragmentPerfilBinding

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private var currentSubTab = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tokenManager = TokenManager(requireContext())
        val isGuest = activity?.intent?.getBooleanExtra("is_guest", false) ?: false

        setupSubNav()
        setupMenuButton()

        if (isGuest || !tokenManager.isLoggedIn()) {
            populateGuest()
        } else {
            populateUser(tokenManager)
        }

        binding.btnEditProfile.setOnClickListener {
            Toast.makeText(requireContext(), "Editar Perfil — próximamente", Toast.LENGTH_SHORT).show()
        }
    }


    private fun setupSubNav() {
        binding.subTabPerfil.setOnClickListener { selectSubTab(0) }
        binding.subTabColabs.setOnClickListener  { selectSubTab(1) }
        binding.subTabPosts.setOnClickListener   { selectSubTab(2) }
        binding.subNavContainer.post { initIndicator() }
    }

    private fun initIndicator() {
        val cw = binding.subNavContainer.width
        if (cw == 0) return
        val tabWidth = cw / 3
        binding.subNavIndicator.layoutParams.width = tabWidth
        binding.subNavIndicator.requestLayout()
        binding.subNavIndicator.translationX = 0f
    }

    private fun selectSubTab(index: Int) {
        if (currentSubTab == index) return
        currentSubTab = index
        animateIndicator(index)
        updateTabLabels(index)
        showContent(index)
    }

    private fun animateIndicator(index: Int) {
        val cw = binding.subNavContainer.width
        if (cw == 0) return
        val tabWidth = cw / 3
        binding.subNavIndicator.animate()
            .translationX(index * tabWidth.toFloat())
            .setDuration(240)
            .setInterpolator(DecelerateInterpolator(1.4f))
            .start()
    }

    private fun updateTabLabels(selected: Int) {
        val tabs = listOf(binding.subTabPerfil, binding.subTabColabs, binding.subTabPosts)
        tabs.forEachIndexed { i, tv ->
            val active = i == selected
            tv.animate().alpha(if (active) 1f else 0.38f).setDuration(180).start()
            tv.setTypeface(null, if (active) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }
    }

    private fun showContent(index: Int) {
        // Fade out all, then fade in selected
        val layouts = listOf(binding.layoutPerfil, binding.layoutColabs, binding.layoutPosts)
        layouts.forEachIndexed { i, layout ->
            if (i == index) {
                layout.visibility = View.VISIBLE
                layout.alpha = 0f
                layout.animate().alpha(1f).setDuration(220)
                    .setInterpolator(DecelerateInterpolator()).start()
            } else {
                layout.animate().alpha(0f).setDuration(150).withEndAction {
                    layout.visibility = View.GONE
                }.start()
            }
        }
    }


    private fun setupMenuButton() {
        binding.btnMenu.setOnClickListener { anchor ->
            val popup = PopupMenu(requireContext(), anchor)
            popup.menu.apply {
                add(0, 1, 0, "✏️   Editar Perfil")
                add(0, 2, 0, "📄   Mi CV")
                add(0, 3, 0, "⚙️   Configuración")
            }
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> toast("Editar Perfil — próximamente")
                    2 -> toast("Mi CV — próximamente")
                    3 -> toast("Configuración — próximamente")
                }
                true
            }
            popup.show()
        }
    }

    private fun populateUser(tm: TokenManager) {
        val email = tm.getUserEmail() ?: ""
        val rol   = tm.getUserRol()   ?: ""
        val initial = email.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

        binding.tvAvatar.text       = initial
        binding.tvProfileName.text  = email
        binding.tvProfileEmail.text = email
        binding.tvInfoRol.text      = rolLabel(rol)
        binding.tvProfileRol.text   = rolLabel(rol)
        binding.cardLocation.visibility = View.GONE
        binding.tvRating.text = "Sin calificaciones aún"
        binding.tvRating.alpha = 0.5f
    }

    private fun populateGuest() {
        binding.tvAvatar.text       = "?"
        binding.tvProfileName.text  = "Invitado"
        binding.tvProfileEmail.text = "Sin correo"
        binding.tvInfoRol.text      = "Modo Invitado"
        binding.tvProfileRol.text   = "Invitado"
        binding.cardLocation.visibility = View.GONE
        binding.tvRating.text = "—"
        binding.btnEditProfile.text = "Iniciar sesión"
        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            activity?.finish()
        }
    }

    private fun rolLabel(rol: String) = when (rol) {
        "TECNICO" -> "Técnico / Practicante"
        "EMPRESA" -> "Empresa / MiPyme"
        else      -> "Sin rol"
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
