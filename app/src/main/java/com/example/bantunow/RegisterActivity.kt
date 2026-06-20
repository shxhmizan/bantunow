package com.example.bantunow

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.bantunow.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            // After register, usually go back to login or straight to main
            // For now, let's just finish and go back to login
            finish()
        }

        binding.tvBackLogin.setOnClickListener {
            finish()
        }
    }
}
