package com.example.bantunow

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.lifecycle.lifecycleScope
import com.example.bantunow.databinding.ActivityLoginBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var authWebClientId: String
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authWebClientId = BuildConfig.AUTH_WEB_CLIENT_ID
        firebaseAuth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginWithFirebase(email, password)
        }

        binding.btnGoogleLogin.setOnClickListener {
            lifecycleScope.launch {
                requestGoogleLogin()
            }
        }

        binding.tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginWithFirebase(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                onUserLogin(task.isSuccessful)
            }
            .addOnFailureListener { ex ->
                onUserLogin(false)
                Log.e("LoginActivity", "Login failed", ex)
            }
    }

    private suspend fun requestGoogleLogin() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(authWebClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result = credentialManager.getCredential(this, request)
            loginWithGoogle(result)
        } catch (e: Exception) {
            Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
            Log.e("ERR_LOGIN", e.toString())
        }
    }

    private fun loginWithGoogle(response: GetCredentialResponse) {
        val credential = response.credential
        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleTokenIdCred = GoogleIdTokenCredential.createFrom(credential.data)
            val authCred = GoogleAuthProvider.getCredential(googleTokenIdCred.idToken, null)

            firebaseAuth.signInWithCredential(authCred).addOnCompleteListener { task ->
                onUserLogin(task.isSuccessful)
            }
        }
    }

    private fun onUserLogin(success: Boolean) {
        val user = firebaseAuth.currentUser
        if (success && user != null) {
            RegisterActivity.validateUserDBData()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        } else {
            Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
        }
    }
}
