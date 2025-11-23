package com.example.nutrih.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nutrih.di.ServiceLocator
import com.example.nutrih.domain.IAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class ProfileViewModel(private val repository: IAuthRepository) : ViewModel() {

    private val _profileState = MutableLiveData<ProfileState>()
    val profileState: LiveData<ProfileState> = _profileState

    private val _userData = MutableLiveData<Map<String, Any?>>()
    val userData: LiveData<Map<String, Any?>> = _userData

    var currentPhotoBase64: String? = null
    private val _currentPhotoBitmap = MutableLiveData<Bitmap?>()
    val currentPhotoBitmap: LiveData<Bitmap?> = _currentPhotoBitmap

    var selectedTargetDate: Long? = null
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

    init {
        fetchUserData()
    }

    private fun fetchUserData() {
        _profileState.value = ProfileState.Loading
        viewModelScope.launch {
            val data = repository.getUserProfile()
            if (data != null) {

                val userMap = mutableMapOf<String, Any?>()
                userMap["name"] = data.name
                userMap["age"] = data.age.toLong()
                userMap["goal"] = ""
                userMap["targetDate"] = null

                _userData.value = userMap

                currentPhotoBase64 = data.photoBase64
                if (currentPhotoBase64 != null) {
                    val bitmap = decodeBase64ToBitmap(currentPhotoBase64!!)
                    _currentPhotoBitmap.value = bitmap
                }
                _profileState.value = ProfileState.SuccessLoad
            } else {
                _profileState.value = ProfileState.Error("Erro ao carregar dados.")
            }
        }
    }

    fun processSelectedImage(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 300, 300, true)
                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                val byteArray = outputStream.toByteArray()
                val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)

                currentPhotoBase64 = base64String
                withContext(Dispatchers.Main) {
                    _currentPhotoBitmap.value = scaledBitmap
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    fun formatTargetDate(timestamp: Long): String {
        return dateFormatter.format(java.util.Date(timestamp))
    }

    fun saveProfile(name: String, ageString: String, phone: String, weightString: String, goal: String) {
        if (name.isBlank()) {
            _profileState.value = ProfileState.Error("O nome não pode ser vazio.")
            return
        }
        val age = ageString.toIntOrNull() ?: 0
        val weight = weightString.toDoubleOrNull()

        _profileState.value = ProfileState.Loading

        viewModelScope.launch {
            val success = repository.updateUserProfile(
                name = name,
                age = age,
                phone = phone,
                weight = weight,
                goal = goal.ifBlank { null },
                targetDate = selectedTargetDate,
                photoBase64 = currentPhotoBase64
            )

            if (success) {
                _profileState.value = ProfileState.SuccessSave
                fetchUserData()
            } else {
                _profileState.value = ProfileState.Error("Erro ao salvar perfil.")
            }
        }
    }
}

sealed class ProfileState {
    object Loading : ProfileState()
    object SuccessLoad : ProfileState()
    object SuccessSave : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ProfileViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(ServiceLocator.provideAuthRepository()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}