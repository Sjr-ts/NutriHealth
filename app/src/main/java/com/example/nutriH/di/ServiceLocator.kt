package com.example.nutrih.di

import android.content.Context
import com.example.nutrih.data.*
import com.example.nutrih.domain.*

object ServiceLocator {

    private var database: AppDatabase? = null

    @Volatile var recipeRepository: IRecipeRepository? = null
    @Volatile var authRepository: IAuthRepository? = null
    @Volatile var appointmentRepository: IAppointmentRepository? = null
    @Volatile var chatRepository: IChatRepository? = null

    fun provideRecipeRepository(context: Context): IRecipeRepository {
        synchronized(this) { return recipeRepository ?: createRecipeRepo(context) }
    }

    fun provideAuthRepository(): IAuthRepository {
        synchronized(this) { return authRepository ?: AuthRepositoryImpl().also { authRepository = it } }
    }

    fun provideAppointmentRepository(context: Context): IAppointmentRepository {
        synchronized(this) { return appointmentRepository ?: createAppointmentRepo(context) }
    }

    // NOVO: Provider do Chat
    fun provideChatRepository(context: Context): IChatRepository {
        synchronized(this) { return chatRepository ?: createChatRepo(context) }
    }

    private fun createRecipeRepo(context: Context): IRecipeRepository {
        return RecipeRepositoryImpl(getDb(context).recipeDao()).also { recipeRepository = it }
    }

    private fun createAppointmentRepo(context: Context): IAppointmentRepository {
        return AppointmentRepositoryImpl(getDb(context).appointmentDao()).also { appointmentRepository = it }
    }

    private fun createChatRepo(context: Context): IChatRepository {
        return ChatRepositoryImpl(getDb(context).chatDao()).also { chatRepository = it }
    }

    private fun getDb(context: Context): AppDatabase {
        return database ?: AppDatabase.getDatabase(context).also { database = it }
    }
}