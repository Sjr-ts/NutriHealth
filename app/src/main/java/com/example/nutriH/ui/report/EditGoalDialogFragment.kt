package com.example.nutrih.ui.report

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.nutrih.databinding.DialogEditGoalBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class EditGoalDialogFragment(
    private val currentCalories: Int,
    private val currentProtein: Int,
    private val currentCarbs: Int,
    private val currentFats: Int,
    private val onSave: (Int, Int, Int, Int) -> Unit
) : DialogFragment() {

    private var _binding: DialogEditGoalBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogEditGoalBinding.inflate(LayoutInflater.from(context))

        if (currentCalories > 0) binding.etGoalCalories.setText(currentCalories.toString())
        if (currentProtein > 0) binding.etGoalProtein.setText(currentProtein.toString())
        if (currentCarbs > 0) binding.etGoalCarbs.setText(currentCarbs.toString())
        if (currentFats > 0) binding.etGoalFats.setText(currentFats.toString())

        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnSave.setOnClickListener {
            val cal = binding.etGoalCalories.text.toString().toIntOrNull() ?: 0
            val prot = binding.etGoalProtein.text.toString().toIntOrNull() ?: 0
            val carbs = binding.etGoalCarbs.text.toString().toIntOrNull() ?: 0
            val fats = binding.etGoalFats.text.toString().toIntOrNull() ?: 0

            onSave(cal, prot, carbs, fats)
            dismiss()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}