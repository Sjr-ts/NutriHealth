package com.example.nutrih.domain

import androidx.lifecycle.LiveData

// ================== MODELOS ==================

data class Recipe(
    val id: Long = 0,
    val name: String,
    val quantity: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fats: Double,
    val timestamp: Long
)

data class UserProfile(
    val uid: String,
    val name: String,
    val age: Int,
    val goalCalories: Int = 2000,
    val goalProtein: Int = 100,
    val goalCarbs: Int = 150,
    val goalFats: Int = 50,
    val photoBase64: String? = null
)

data class Appointment(
    val id: Long = 0,
    val specialistName: String,
    val specialistSpecialty: String,
    val timestamp: Long
)

data class ChatMessage(
    val id: Long = 0,
    val appointmentId: Long,
    val text: String,
    val timestamp: Long,
    val isUserSender: Boolean
)
interface IRecipeRepository {
    suspend fun getRecipes(): List<Recipe>
    suspend fun addRecipe(recipe: Recipe)
    suspend fun deleteRecipe(recipe: Recipe)
}

interface IAuthRepository {
    suspend fun signInAnonymously(): String?
    suspend fun saveUserData(uid: String, name: String, age: Int, phone: String): Boolean
    suspend fun getUserProfile(): UserProfile?
    suspend fun updateGoals(cal: Int, prot: Int, carbs: Int, fats: Int): Boolean
    suspend fun updateUserProfile(name: String, age: Int, phone: String, weight: Double?, goal: String?, targetDate: Long?, photoBase64: String?): Boolean
}

interface IAppointmentRepository {
    val allAppointments: LiveData<List<Appointment>>
    suspend fun bookAppointment(appointment: Appointment)
    suspend fun cancelAppointment(appointment: Appointment)
}

interface IChatRepository {
    fun getMessages(appointmentId: Long): LiveData<List<ChatMessage>>
    suspend fun sendMessage(message: ChatMessage)
}