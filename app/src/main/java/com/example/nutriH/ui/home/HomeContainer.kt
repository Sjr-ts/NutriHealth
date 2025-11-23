package com.example.nutrih.ui.home

import android.app.Dialog
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.example.nutrih.R
import com.example.nutrih.domain.Recipe
import com.example.nutrih.databinding.ActivityHomeBinding
import com.example.nutrih.databinding.DialogAddEditRecipeBinding
import com.example.nutrih.databinding.FragmentWebviewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigationView.setupWithNavController(navController)
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            if (item.itemId != binding.bottomNavigationView.selectedItemId) {
                NavigationUI.onNavDestinationSelected(item, navController)
            } else {
                navController.popBackStack(item.itemId, false)
            }
            true
        }
    }
}

class WebViewFragment : Fragment() {
    private var _binding: FragmentWebviewBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWebviewBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val url = arguments?.getString("url") ?: "https://www.google.com"
        setupWebView(url)
    }
    private fun setupWebView(url: String) {
        binding.webview.settings.javaScriptEnabled = true
        binding.webview.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.progressBarWeb.isVisible = true
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progressBarWeb.isVisible = false
            }
        }
        binding.webview.loadUrl(url)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class AddEditRecipeDialogFragment(private val recipeToEdit: Recipe?, private val onSave: (Recipe) -> Unit) : DialogFragment() {
    private var _binding: DialogAddEditRecipeBinding? = null
    private val binding get() = _binding!!
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddEditRecipeBinding.inflate(LayoutInflater.from(context))
        return MaterialAlertDialogBuilder(requireContext()).setView(binding.root).create()
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View { return binding.root }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        setupListeners()
    }
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    private fun setupView() {
        if (recipeToEdit != null) {
            binding.tvDialogTitle.text = getString(R.string.dialog_title_edit)
            binding.etFoodName.setText(recipeToEdit.name)
            binding.etQuantity.setText(recipeToEdit.quantity)
            binding.etCalories.setText(recipeToEdit.calories.toString())
            binding.etProtein.setText(recipeToEdit.protein.toString())
            binding.etCarbs.setText(recipeToEdit.carbs.toString())
            binding.etFats.setText(recipeToEdit.fats.toString())
        } else {
            binding.tvDialogTitle.text = getString(R.string.dialog_title_add)
        }
    }
    private fun setupListeners() {
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnSave.setOnClickListener { saveRecipe() }
    }
    private fun saveRecipe() {
        val name = binding.etFoodName.text.toString()
        val quantity = binding.etQuantity.text.toString()
        val caloriesStr = binding.etCalories.text.toString()
        if (name.isBlank() || quantity.isBlank() || caloriesStr.isBlank()) {
            Toast.makeText(requireContext(), "Preencha os campos obrigatórios", Toast.LENGTH_SHORT).show()
            return
        }
        val timestamp = recipeToEdit?.timestamp ?: System.currentTimeMillis()
        val recipe = Recipe(
            id = recipeToEdit?.id ?: 0,
            name = name,
            quantity = quantity,
            calories = caloriesStr.toIntOrNull() ?: 0,
            protein = binding.etProtein.text.toString().toDoubleOrNull() ?: 0.0,
            carbs = binding.etCarbs.text.toString().toDoubleOrNull() ?: 0.0,
            fats = binding.etFats.text.toString().toDoubleOrNull() ?: 0.0,
            timestamp = timestamp
        )
        onSave(recipe)
        dismiss()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}