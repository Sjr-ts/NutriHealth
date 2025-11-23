package com.example.nutrih.ui.report

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.nutrih.databinding.FragmentReportBinding
import com.example.nutrih.ui.home.HomeViewModel
import com.example.nutrih.ui.home.HomeViewModelFactory
import com.example.nutrih.domain.Recipe
import java.util.concurrent.TimeUnit

class ReportFragment : Fragment() {

    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by activityViewModels {
        HomeViewModelFactory(requireContext().applicationContext)
    }

    private var currentCal = 0
    private var currentProt = 0
    private var currentCarbs = 0
    private var currentFats = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeViewModel.userData.observe(viewLifecycleOwner) { userData ->
            updateMetaDisplay(userData)
        }

        homeViewModel.allRecipes.observe(viewLifecycleOwner) { recipes ->
            calculateWeeklyReport(recipes)
        }

        binding.cardMeta.setOnClickListener { showEditGoalDialog() }
    }

    private fun updateMetaDisplay(userData: Map<String, Any>) {
        currentCal = (userData["goalCalories"] as? Long)?.toInt() ?: 0
        currentProt = (userData["goalProtein"] as? Long)?.toInt() ?: 0
        currentCarbs = (userData["goalCarbs"] as? Long)?.toInt() ?: 0
        currentFats = (userData["goalFats"] as? Long)?.toInt() ?: 0

        if (currentCal == 0 && currentProt == 0) {
            binding.tvMetaTitle.text = "CRIE SUA META"
            binding.layoutMetaValues.isVisible = false
        } else {
            binding.tvMetaTitle.text = "SUA META SEMANAL"
            binding.layoutMetaValues.isVisible = true
            binding.tvMetaCalories.text = "$currentCal Kcal"
            binding.tvMetaProtein.text = "${currentProt}g"
            binding.tvMetaCarbs.text = "${currentCarbs}g"
            binding.tvMetaFats.text = "${currentFats}g"
        }
    }

    private fun showEditGoalDialog() {
        val dialog = EditGoalDialogFragment(currentCal, currentProt, currentCarbs, currentFats) { cal, prot, carbs, fats ->
            homeViewModel.updateGoals(cal, prot, carbs, fats)
        }
        dialog.show(childFragmentManager, "EditGoalDialog")
    }

    private fun calculateWeeklyReport(allRecipes: List<Recipe>) {
        val oneWeekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        val weeklyRecipes = allRecipes.filter { it.timestamp >= oneWeekAgo }

        if (weeklyRecipes.isEmpty()) {
            binding.tvNoData.isVisible = true
            binding.tvRealCalories.text = "0"
            binding.tvRealProtein.text = "0g"
            binding.tvRealCarbs.text = "0g"
            binding.tvRealFats.text = "0g"
            return
        }

        binding.tvNoData.isVisible = false

        val totalCalories = weeklyRecipes.sumOf { it.calories }
        val totalProtein = weeklyRecipes.sumOf { it.protein }
        val totalCarbs = weeklyRecipes.sumOf { it.carbs }
        val totalFats = weeklyRecipes.sumOf { it.fats }

        binding.tvRealCalories.text = totalCalories.toString()
        binding.tvRealProtein.text = "${totalProtein.toInt()}g"
        binding.tvRealCarbs.text = "${totalCarbs.toInt()}g"
        binding.tvRealFats.text = "${totalFats.toInt()}g"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}