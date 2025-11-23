package com.example.nutrih.data.repository

import androidx.lifecycle.LiveData
import com.example.nutrih.data.database.AppointmentDao
import com.example.nutrih.data.database.AppointmentEntity
import com.example.nutrih.data.database.ChatDao
import com.example.nutrih.data.database.ChatMessageEntity
import com.example.nutrih.data.database.RecipeDao
import com.example.nutrih.data.database.RecipeEntity
import com.example.nutrih.ui.suggestion.Suggestion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class RecipeRepository(private val recipeDao: RecipeDao) {

    val allRecipes: LiveData<List<RecipeEntity>> = recipeDao.getAllRecipes()

    suspend fun insert(recipe: RecipeEntity) {
        recipeDao.insertRecipe(recipe)
    }

    suspend fun update(recipe: RecipeEntity) {
        recipeDao.updateRecipe(recipe)
    }

    suspend fun delete(recipe: RecipeEntity) {
        recipeDao.deleteRecipe(recipe)
    }
}

// ============================================================================================
// 2. APPOINTMENT REPOSITORY
// ============================================================================================

class AppointmentRepository(private val appointmentDao: AppointmentDao) {

    val allAppointments: LiveData<List<AppointmentEntity>> = appointmentDao.getAllAppointments()

    suspend fun bookAppointment(appointment: AppointmentEntity) {
        appointmentDao.insertAppointment(appointment)
    }

    suspend fun deleteAppointment(appointment: AppointmentEntity) {
        appointmentDao.deleteAppointment(appointment)
    }
}
class ChatRepository(private val chatDao: ChatDao) {

    fun getMessages(appointmentId: Long): LiveData<List<ChatMessageEntity>> {
        return chatDao.getMessagesForAppointment(appointmentId)
    }

    suspend fun sendMessage(message: ChatMessageEntity) {
        chatDao.insertMessage(message)
    }
}
class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    suspend fun signInAnonymously(): String? {
        return try {
            val result = auth.signInAnonymously().await()
            result.user?.uid
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveUserData(uid: String, name: String, age: Int, phone: String): Boolean {
        return try {
            val userMap = hashMapOf(
                "uid" to uid,
                "name" to name,
                "age" to age,
                "phone" to phone,
                "photoBase64" to null,
                "createdAt" to System.currentTimeMillis()
            )
            db.collection("users").document(uid).set(userMap).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    suspend fun getCurrentUserData(): Map<String, Any>? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val documentSnapshot = db.collection("users").document(uid).get().await()
            documentSnapshot.data
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateUserProfile(
        name: String, age: Int, phone: String,
        weight: Double?, goal: String?, targetDate: Long?, photoBase64: String?
    ): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val updates = mutableMapOf<String, Any>("name" to name, "age" to age, "phone" to phone)
            if (weight != null) updates["weight"] = weight
            if (goal != null) updates["goal"] = goal
            if (targetDate != null) updates["targetDate"] = targetDate
            if (photoBase64 != null) updates["photoBase64"] = photoBase64

            db.collection("users").document(uid).update(updates).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateUserGoals(cal: Int, prot: Int, carbs: Int, fats: Int): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val updates = mapOf(
                "goalCalories" to cal,
                "goalProtein" to prot,
                "goalCarbs" to carbs,
                "goalFats" to fats
            )
            db.collection("users").document(uid).update(updates).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
object SubstitutionService {

    private val substitutionMap = mapOf(
        "pão francês" to Suggestion(
            unhealthyFood = "Pão Francês",
            healthyAlternative = "Pão Integral",
            reason = "mais rica em fibras"
        ),
        "refrigerante" to Suggestion(
            unhealthyFood = "Refrigerante",
            healthyAlternative = "Água com Gás e Limão",
            reason = "sem açúcar e calorias"
        ),
        "chocolate ao leite" to Suggestion(
            unhealthyFood = "Chocolate ao Leite",
            healthyAlternative = "Chocolate 70% Cacau",
            reason = "com menos açúcar e mais antioxidantes"
        ),
        "batata frita" to Suggestion(
            unhealthyFood = "Batata Frita",
            healthyAlternative = "Batata Doce Assada",
            reason = "com mais nutrientes e menos gordura"
        )
    )
    fun getSubstitution(foodName: String): Suggestion? {
        val normalizedName = foodName.lowercase()
        val matchingKey = substitutionMap.keys.find { key ->
            normalizedName.contains(key)
        }
        return matchingKey?.let { substitutionMap[it] }
    }
}