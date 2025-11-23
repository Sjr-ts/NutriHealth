package com.example.nutrih.data.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// ============================================================================================
// 1. ENTIDADES (TABELAS)
// ============================================================================================

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val quantity: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fats: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val specialistName: String,
    val specialistSpecialty: String,
    val appointmentTimestamp: Long
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = AppointmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["appointmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["appointmentId"])]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val appointmentId: Long,
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val senderType: SenderType
)

enum class SenderType {
    USER,
    SPECIALIST
}

// ============================================================================================
// 2. DAOs (OPERAÇÕES)
// ============================================================================================

@Dao
interface RecipeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity)

    @Update
    suspend fun updateRecipe(recipe: RecipeEntity)

    @Delete
    suspend fun deleteRecipe(recipe: RecipeEntity)

    @Query("SELECT * FROM recipes ORDER BY id DESC")
    fun getAllRecipes(): LiveData<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getRecipeById(id: Long): RecipeEntity?
}

@Dao
interface AppointmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: AppointmentEntity)

    @Delete
    suspend fun deleteAppointment(appointment: AppointmentEntity)

    @Query("SELECT * FROM appointments ORDER BY appointmentTimestamp DESC")
    fun getAllAppointments(): LiveData<List<AppointmentEntity>>
}

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("SELECT * FROM chat_messages WHERE appointmentId = :appointmentId ORDER BY timestamp ASC")
    fun getMessagesForAppointment(appointmentId: Long): LiveData<List<ChatMessageEntity>>
}

// ============================================================================================
// 3. CONVERSORES E BANCO DE DADOS
// ============================================================================================

class Converters {
    @TypeConverter
    fun fromSenderType(value: SenderType): String = value.name

    @TypeConverter
    fun toSenderType(value: String): SenderType = SenderType.valueOf(value)
}

@Database(
    entities = [RecipeEntity::class, AppointmentEntity::class, ChatMessageEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recipeDao(): RecipeDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE recipes ADD COLUMN timestamp LONG NOT NULL DEFAULT ${System.currentTimeMillis()}")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `appointments` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`specialistName` TEXT NOT NULL, " +
                            "`specialistSpecialty` TEXT NOT NULL, " +
                            "`appointmentTimestamp` LONG NOT NULL)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `chat_messages` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`appointmentId` LONG NOT NULL, " +
                            "`messageText` TEXT NOT NULL, " +
                            "`timestamp` LONG NOT NULL, " +
                            "`senderType` TEXT NOT NULL, " +
                            "FOREIGN KEY(`appointmentId`) REFERENCES `appointments`(`id`) ON DELETE CASCADE)"
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_appointmentId` ON `chat_messages` (`appointmentId`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nutrih_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}