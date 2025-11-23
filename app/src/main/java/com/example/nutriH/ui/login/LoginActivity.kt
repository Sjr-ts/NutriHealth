package com.example.nutrih.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.nutrih.databinding.ActivityLoginBinding
import com.example.nutrih.ui.home.HomeActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginState.Success -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }
                is LoginState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                is LoginState.Loading -> { }
                else -> {}
            }
        }
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val name = binding.etName.text.toString()
            val age = binding.etAge.text.toString()
            val phone = binding.etPhone.text.toString()
            viewModel.login(name, age, phone)
        }
    }
}