package com.example.nutrih.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.room.*
import com.example.nutrih.domain.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fats: Double,
    val timestamp: Long
) {
    fun toDomain() = Recipe(id, name, quantity, calories, protein, carbs, fats, timestamp)
}
fun Recipe.toEntity() = RecipeEntity(id, name, quantity, calories, protein, carbs, fats, timestamp)

@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val specialistName: String,
    val specialistSpecialty: String,
    val appointmentTimestamp: Long
) {
    fun toDomain() = Appointment(id, specialistName, specialistSpecialty, appointmentTimestamp)
}
fun Appointment.toEntity() = AppointmentEntity(id, specialistName, specialistSpecialty, timestamp)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appointmentId: Long,
    val messageText: String,
    val timestamp: Long,
    val isUserSender: Boolean
) {
    fun toDomain() = ChatMessage(id, appointmentId, messageText, timestamp, isUserSender)
}
fun ChatMessage.toEntity() = ChatMessageEntity(id, appointmentId, text, timestamp, isUserSender)

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY id DESC")
    suspend fun getAll(): List<RecipeEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recipe: RecipeEntity)
    @Delete
    suspend fun delete(recipe: RecipeEntity)
}

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments ORDER BY appointmentTimestamp DESC")
    fun getAllLive(): LiveData<List<AppointmentEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appointment: AppointmentEntity)
    @Delete
    suspend fun delete(appointment: AppointmentEntity)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE appointmentId = :appId ORDER BY timestamp ASC")
    fun getMessages(appId: Long): LiveData<List<ChatMessageEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity)
}

@Database(entities = [RecipeEntity::class, AppointmentEntity::class, ChatMessageEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "nutrih_clean_v3.db")
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}

class RecipeRepositoryImpl(private val dao: RecipeDao) : IRecipeRepository {
    override suspend fun getRecipes() = dao.getAll().map { it.toDomain() }
    override suspend fun addRecipe(recipe: Recipe) = dao.insert(recipe.toEntity())
    override suspend fun deleteRecipe(recipe: Recipe) = dao.delete(recipe.toEntity())
}

class AppointmentRepositoryImpl(private val dao: AppointmentDao) : IAppointmentRepository {
    override val allAppointments: LiveData<List<Appointment>> = dao.getAllLive().map { list -> list.map { it.toDomain() } }
    override suspend fun bookAppointment(appointment: Appointment) = dao.insert(appointment.toEntity())
    override suspend fun cancelAppointment(appointment: Appointment) = dao.delete(appointment.toEntity())
}

class ChatRepositoryImpl(private val dao: ChatDao) : IChatRepository {
    override fun getMessages(appointmentId: Long): LiveData<List<ChatMessage>> {
        return dao.getMessages(appointmentId).map { list -> list.map { it.toDomain() } }
    }
    override suspend fun sendMessage(message: ChatMessage) {
        dao.insert(message.toEntity())
    }
}

class AuthRepositoryImpl : IAuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override suspend fun signInAnonymously(): String? {
        return try { auth.signInAnonymously().await().user?.uid } catch (e: Exception) { null }
    }
    override suspend fun saveUserData(uid: String, name: String, age: Int, phone: String): Boolean {
        return try {
            db.collection("users").document(uid).set(hashMapOf("uid" to uid, "name" to name, "age" to age, "phone" to phone, "createdAt" to System.currentTimeMillis())).await()
            true
        } catch (e: Exception) { false }
    }
    override suspend fun getUserProfile(): UserProfile? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val snap = db.collection("users").document(uid).get().await()
            UserProfile(uid, snap.getString("name") ?: "", snap.getLong("age")?.toInt() ?: 0,
                snap.getLong("goalCalories")?.toInt() ?: 2000, snap.getLong("goalProtein")?.toInt() ?: 100,
                snap.getLong("goalCarbs")?.toInt() ?: 150, snap.getLong("goalFats")?.toInt() ?: 50,
                snap.getString("photoBase64"))
        } catch (e: Exception) { null }
    }
    override suspend fun updateGoals(cal: Int, prot: Int, carbs: Int, fats: Int): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try { db.collection("users").document(uid).update(mapOf("goalCalories" to cal, "goalProtein" to prot, "goalCarbs" to carbs, "goalFats" to fats)).await(); true } catch (e: Exception) { false }
    }
    override suspend fun updateUserProfile(name: String, age: Int, phone: String, weight: Double?, goal: String?, targetDate: Long?, photoBase64: String?): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val updates = mutableMapOf<String, Any>("name" to name, "age" to age, "phone" to phone)
        weight?.let { updates["weight"] = it }; goal?.let { updates["goal"] = it }
        targetDate?.let { updates["targetDate"] = it }; photoBase64?.let { updates["photoBase64"] = it }
        return try { db.collection("users").document(uid).update(updates).await(); true } catch (e: Exception) { false }
    }
}