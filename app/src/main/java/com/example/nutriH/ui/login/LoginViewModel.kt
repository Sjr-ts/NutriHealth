package com.example.nutrih.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nutrih.di.ServiceLocator
import com.example.nutrih.domain.IAuthRepository
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: IAuthRepository) : ViewModel() {

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    fun login(name: String, ageString: String, phone: String) {
        if (name.isBlank() || ageString.isBlank() || phone.isBlank()) {
            _loginState.value = LoginState.Error("Preencha todos os campos")
            return
        }
        if (name.length > 15) {
            _loginState.value = LoginState.Error("O nome é muito longo. Use apenas seu primeiro nome (máx 15 letras).")
            return
        }
        val age = ageString.toIntOrNull()
        if (age == null) {
            _loginState.value = LoginState.Error("Informe uma idade válida.")
            return
        }
        if (age < 15) {
            _loginState.value = LoginState.Error("Este app só pode ser usado por pessoas acima de 15 anos.")
            return
        }
        if (age > 100) {
            _loginState.value = LoginState.Error("Por favor, insira sua idade correta.")
            return
        }
        if (phone.length < 11) {
            _loginState.value = LoginState.Error("Telefone inválido (use DDD).")
            return
        }

        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            val uid = repository.signInAnonymously()
            if (uid != null) {
                val success = repository.saveUserData(uid, name, age, phone)
                if (success) {
                    _loginState.value = LoginState.Success
                } else {
                    _loginState.value = LoginState.Error("Erro ao salvar dados")
                }
            } else {
                _loginState.value = LoginState.Error("Erro na autenticação")
            }
        }
    }
}

sealed class LoginState {
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(ServiceLocator.provideAuthRepository()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}