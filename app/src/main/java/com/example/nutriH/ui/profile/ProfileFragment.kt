package com.example.nutrih.ui.profile

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.nutrih.R
import com.example.nutrih.databinding.FragmentProfileBinding
import java.util.Calendar

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory()
    }
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.processSelectedImage(requireContext(), uri)
        }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.toolbarProfile.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.ivProfileImageLarge.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.etProfileDate.setOnClickListener { showDatePicker() }
        binding.btnSaveProfile.setOnClickListener {
            val name = binding.etProfileName.text.toString()
            val age = binding.etProfileAge.text.toString()
            val phone = binding.etProfilePhone.text.toString()
            val weight = binding.etProfileWeight.text.toString()
            val goal = binding.etProfileGoal.text.toString()
            viewModel.saveProfile(name, age, phone, weight, goal)
        }
    }

    private fun observeViewModel() {
        viewModel.userData.observe(viewLifecycleOwner) { data ->
            binding.etProfileName.setText(data["name"] as? String)
            binding.etProfileAge.setText((data["age"] as? Long)?.toString())
            binding.etProfilePhone.setText(data["phone"] as? String)
            binding.etProfileWeight.setText((data["weight"] as? Double)?.toString())
            binding.etProfileGoal.setText(data["goal"] as? String)
            val targetDate = data["targetDate"] as? Long
            if (targetDate != null) {
                binding.etProfileDate.setText(viewModel.formatTargetDate(targetDate))
            }
        }
        viewModel.currentPhotoBitmap.observe(viewLifecycleOwner) { bitmap ->
            if (bitmap != null) {
                binding.ivProfileImageLarge.setImageBitmap(bitmap)
            } else {
                binding.ivProfileImageLarge.setImageResource(R.drawable.ic_default_avatar)
            }
        }
        viewModel.profileState.observe(viewLifecycleOwner) { state ->
            binding.profileProgressBar.isVisible = state is ProfileState.Loading
            binding.btnSaveProfile.isEnabled = state !is ProfileState.Loading
            when (state) {
                is ProfileState.SuccessSave -> Toast.makeText(requireContext(), "Perfil atualizado!", Toast.LENGTH_SHORT).show()
                is ProfileState.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                else -> {}
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        viewModel.selectedTargetDate?.let { calendar.timeInMillis = it }
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance()
                selectedCal.set(year, month, dayOfMonth)
                val timestamp = selectedCal.timeInMillis
                viewModel.selectedTargetDate = timestamp
                binding.etProfileDate.setText(viewModel.formatTargetDate(timestamp))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}