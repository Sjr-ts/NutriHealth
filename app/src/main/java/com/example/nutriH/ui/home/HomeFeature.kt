package com.example.nutrih.ui.home

import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nutrih.R
import com.example.nutrih.databinding.FragmentHomeBinding
import com.example.nutrih.di.ServiceLocator
import com.example.nutrih.domain.IAuthRepository
import com.example.nutrih.domain.IRecipeRepository
import com.example.nutrih.domain.Recipe
import com.example.nutrih.ui.suggestion.Suggestion
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import android.graphics.BitmapFactory

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(requireContext().applicationContext)
    }

    private lateinit var recipeAdapter: RecipeListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        setupClickListeners()
        observeSuggestions()
        observeUserData()
        setupProfileIconClick()
        setupSearch()

        homeViewModel.loadRecipes()
    }

    private fun setupSearch() {
        binding.etSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = v.text.toString()
                if (query.isNotBlank()) performSmartSearch("receita de $query")
                true
            } else false
        }
    }

    private fun performSmartSearch(naturalQuery: String) {
        val url = "https://www.google.com/search?q=${Uri.encode(naturalQuery)}"
        findNavController().navigate(R.id.navigation_webview, bundleOf("url" to url))
    }

    private fun setupCategories(userData: Map<String, Any>, recipes: List<Recipe>) {
        binding.linearCategories.removeAllViews()

        val recTerm = generateRecommendationTerm(userData, recipes, "geral")
        addCategoryItem("Recomendação", R.drawable.ic_cat_star, true) { performSmartSearch(recTerm) }

        val fishTerm = generateRecommendationTerm(userData, recipes, "peixe")
        addCategoryItem("Peixes", R.drawable.ic_cat_fish, false) { performSmartSearch(fishTerm) }

        val meatTerm = generateRecommendationTerm(userData, recipes, "carne")
        addCategoryItem("Carnes", R.drawable.ic_cat_meat, false) { performSmartSearch(meatTerm) }

        val vegTerm = generateRecommendationTerm(userData, recipes, "vegetariano")
        addCategoryItem("Vegetariano", R.drawable.ic_cat_veg, false) { performSmartSearch(vegTerm) }
    }

    private fun generateRecommendationTerm(userData: Map<String, Any>, recipes: List<Recipe>, type: String): String {
        val goalCal = (userData["goalCalories"] as? Long)?.toInt() ?: 2000
        val goalProt = (userData["goalProtein"] as? Long)?.toInt() ?: 100
        val oneWeekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        val weeklyRecipes = recipes.filter { it.timestamp >= oneWeekAgo }
        val totalCal = weeklyRecipes.sumOf { it.calories }
        val totalProt = weeklyRecipes.sumOf { it.protein.toInt() }
        val days = if (weeklyRecipes.isNotEmpty()) 7 else 1
        val avgCal = totalCal / days
        val avgProt = totalProt / days

        val suffix = if (avgCal > goalCal) "baixa caloria leve"
        else if (avgProt < goalProt) "rica em proteína"
        else "saudável balanceada"

        return when (type) {
            "peixe" -> "receita de peixe $suffix"
            "carne" -> "receita de carne $suffix"
            "vegetariano" -> "receita vegetariana $suffix"
            else -> "receita $suffix para jantar"
        }
    }

    private fun addCategoryItem(name: String, iconRes: Int, isHighlight: Boolean, onClick: () -> Unit) {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.item_category, binding.linearCategories, false)
        val tvName = view.findViewById<TextView>(R.id.tv_cat_name)
        val ivIcon = view.findViewById<ImageView>(R.id.iv_cat_icon)
        val card = view.findViewById<MaterialCardView>(R.id.card_cat_image)

        tvName.text = name
        ivIcon.setImageResource(iconRes)

        if (isHighlight) {
            card.strokeWidth = 4
            card.strokeColor = requireContext().getColor(android.R.color.holo_red_light)
            tvName.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
        }
        view.setOnClickListener { onClick() }
        binding.linearCategories.addView(view)
    }

    private fun setupProfileIconClick() {
        binding.ivProfileIconHome.setOnClickListener {
            findNavController().navigate(R.id.navigation_profile)
        }
    }

    private fun observeUserData() {
        homeViewModel.userData.observe(viewLifecycleOwner) { userData ->
            updateHeader(userData)
            homeViewModel.allRecipes.value?.let { recipes -> setupCategories(userData, recipes) }
        }
    }

    private fun updateHeader(userData: Map<String, Any>) {
        val fullName = userData["name"] as? String ?: "Usuário"
        val firstName = fullName.split(" ").firstOrNull() ?: fullName
        binding.tvGreeting.text = getString(R.string.home_greeting, firstName)

        val photoBase64 = userData["photoBase64"] as? String
        if (photoBase64 != null) {
            try {
                val decodedBytes = Base64.decode(photoBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                binding.ivProfileIconHome.setImageBitmap(bitmap)
            } catch (e: Exception) {
                binding.ivProfileIconHome.setImageResource(R.drawable.ic_default_avatar)
            }
        } else {
            binding.ivProfileIconHome.setImageResource(R.drawable.ic_default_avatar)
        }
    }

    private fun setupRecyclerView() {
        recipeAdapter = RecipeListAdapter(
            onEditClick = { recipe -> showAddEditDialog(recipe) },
            onDeleteClick = { recipe -> homeViewModel.delete(recipe) }
        )
        binding.recyclerViewRecipes.apply {
            adapter = recipeAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeViewModel() {
        homeViewModel.allRecipes.observe(viewLifecycleOwner) { recipes ->
            recipes?.let {
                recipeAdapter.submitList(it)
                homeViewModel.userData.value?.let { userData -> setupCategories(userData, it) }
            }
        }
    }

    private fun setupClickListeners() {
        binding.cardWeeklyPlan.setOnClickListener { showAddEditDialog(null) }
    }

    private fun showAddEditDialog(recipe: Recipe?) {
        val dialog = AddEditRecipeDialogFragment(recipe) { newOrUpdatedRecipe ->
            if (recipe == null) homeViewModel.insert(newOrUpdatedRecipe)
            else homeViewModel.update(newOrUpdatedRecipe)
        }
        dialog.show(parentFragmentManager, "AddEditRecipeDialog")
    }

    private fun observeSuggestions() {
        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.suggestionEvent.collect { suggestion -> showSuggestionDialog(suggestion) }
        }
    }

    private fun showSuggestionDialog(suggestion: Suggestion) {
        val message = getString(R.string.suggestion_message, suggestion.unhealthyFood, suggestion.healthyAlternative, suggestion.reason)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.suggestion_title)
            .setMessage(message)
            .setPositiveButton(R.string.suggestion_ok, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class HomeViewModel(
    private val recipeRepository: IRecipeRepository,
    private val authRepository: IAuthRepository
) : ViewModel() {

    private val _allRecipes = MutableLiveData<List<Recipe>>()
    val allRecipes: LiveData<List<Recipe>> = _allRecipes

    private val _suggestionEvent = MutableSharedFlow<Suggestion>()
    val suggestionEvent: SharedFlow<Suggestion> = _suggestionEvent.asSharedFlow()

    private val _userData = MutableLiveData<Map<String, Any>>()
    val userData: LiveData<Map<String, Any>> = _userData

    init {
        fetchUserData()
        loadRecipes()
    }

    fun loadRecipes() = viewModelScope.launch {
        _allRecipes.value = recipeRepository.getRecipes()
    }

    private fun fetchUserData() = viewModelScope.launch {
        val profile = authRepository.getUserProfile()
        if (profile != null) {
            _userData.value = mapOf(
                "name" to profile.name,
                "goalCalories" to profile.goalCalories.toLong()
            )
        } else {
            _userData.value = mapOf("name" to "Usuário")
        }
    }

    fun updateGoals(cal: Int, prot: Int, carbs: Int, fats: Int) = viewModelScope.launch {
        val success = authRepository.updateGoals(cal, prot, carbs, fats)
        if (success) fetchUserData()
    }

    fun insert(recipe: Recipe) = viewModelScope.launch {
        recipeRepository.addRecipe(recipe)
        loadRecipes()
        checkSuggestion(recipe.name)
    }

    fun update(recipe: Recipe) = viewModelScope.launch {
        recipeRepository.addRecipe(recipe)
        loadRecipes()
    }

    fun delete(recipe: Recipe) = viewModelScope.launch {
        recipeRepository.deleteRecipe(recipe)
        loadRecipes()
    }

    private fun checkSuggestion(name: String) {
        viewModelScope.launch {
            val suggestions = mapOf(
                "refrigerante" to Suggestion("Refrigerante", "Água com Gás", "Sem calorias"),
                "fritura" to Suggestion("Fritura", "Assado", "Menos gordura")
            )
            suggestions.entries.find { name.lowercase().contains(it.key) }?.value?.let {
                _suggestionEvent.emit(it)
            }
        }
    }
}

class HomeViewModelFactory(private val context: android.content.Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            val recipeRepo = ServiceLocator.provideRecipeRepository(context)
            val authRepo = ServiceLocator.provideAuthRepository()
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(recipeRepo, authRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class RecipeListAdapter(
    private val onEditClick: (Recipe) -> Unit,
    private val onDeleteClick: (Recipe) -> Unit
) : ListAdapter<Recipe, RecipeListAdapter.RecipeViewHolder>(RecipeComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(getItem(position), onEditClick, onDeleteClick)
    }

    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_recipe_name)
        private val tvInfo: TextView = itemView.findViewById(R.id.tv_recipe_info)
        private val btnOptions: ImageButton = itemView.findViewById(R.id.btn_options)

        fun bind(recipe: Recipe, onEditClick: (Recipe) -> Unit, onDeleteClick: (Recipe) -> Unit) {
            tvName.text = recipe.name
            tvInfo.text = "${recipe.calories} Kcal • P: ${recipe.protein}g • C: ${recipe.carbs}g • G: ${recipe.fats}g"

            btnOptions.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menu.add(0, 1, 0, "Editar")
                popup.menu.add(0, 2, 1, "Excluir")
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        1 -> { onEditClick(recipe); true }
                        2 -> { onDeleteClick(recipe); true }
                        else -> false
                    }
                }
                popup.show()
            }
        }
    }

    class RecipeComparator : DiffUtil.ItemCallback<Recipe>() {
        override fun areItemsTheSame(oldItem: Recipe, newItem: Recipe) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Recipe, newItem: Recipe) = oldItem == newItem
    }
}