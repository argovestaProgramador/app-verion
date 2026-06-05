package com.verion.practicas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.verion.practicas.databinding.FragmentBuscarBinding

class BuscarFragment : Fragment() {

    private var _binding: FragmentBuscarBinding? = null
    private val binding get() = _binding!!
    private var selectedChip: TextView? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBuscarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChips()
    }

    private fun setupChips() {
        val chips = listOf(
            binding.chipTodos,
            binding.chipInformatica,
            binding.chipDiseno,
            binding.chipMecanica,
            binding.chipElec
        )

        chips.forEach { chip ->
            chip.setOnClickListener {
                selectedChip?.setBackgroundResource(R.drawable.bg_chip)
                selectedChip?.alpha = 1f
                selectedChip?.setTextColor(0x80FFFFFF.toInt())
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(0xFFFFFFFF.toInt())
                chip.animate().scaleX(1.05f).scaleY(1.05f).setDuration(100)
                    .withEndAction { chip.animate().scaleX(1f).scaleY(1f).setDuration(80) }

                selectedChip = chip
            }
        }
        selectedChip = binding.chipTodos
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
