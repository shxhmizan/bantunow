package com.example.bantunow

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.bantunow.databinding.ActivityRegisterBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var credentialManager: CredentialManager
    private lateinit var authWebClientId: String

    companion object {
        fun validateUserDBData(onComplete: (() -> Unit)? = null) {
            val user = FirebaseAuth.getInstance().currentUser ?: run {
                onComplete?.invoke()
                return
            }
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(user.uid)

            userRef.get().addOnSuccessListener { document ->
                if (!document.exists()) {
                    val userInitData = mutableMapOf(
                        "displayName" to (user.displayName ?: "New User"),
                        "bantuPoints" to 0L,
                        "walletBalance" to 100.0, // Give some starting balance for testing
                        "rating" to 5.0,
                        "profileImageUrl" to (user.photoUrl?.toString() ?: "")
                    )
                    userRef.set(userInitData).addOnCompleteListener {
                        onComplete?.invoke()
                    }
                } else {
                    // Update profile image if it exists in auth but not in DB
                    val dbImageUrl = document.getString("profileImageUrl")
                    val authImageUrl = user.photoUrl?.toString()
                    if (dbImageUrl.isNullOrEmpty() && !authImageUrl.isNullOrEmpty()) {
                        userRef.update("profileImageUrl", authImageUrl)
                    }
                    onComplete?.invoke()
                }
            }.addOnFailureListener {
                onComplete?.invoke()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        credentialManager = CredentialManager.create(this)
        authWebClientId = BuildConfig.AUTH_WEB_CLIENT_ID

        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString()
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerWithEmail(name, email, password)
        }

        binding.btnGoogleRegister.setOnClickListener {
            lifecycleScope.launch {
                requestGoogleLogin()
            }
        }

        binding.tvBackLogin.setOnClickListener {
            finish()
        }
    }

    private fun registerWithEmail(name: String, email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                        onSuccess()
                    }
                } else {
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private suspend fun requestGoogleLogin() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(authWebClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result = credentialManager.getCredential(this, request)
            loginWithGoogle(result)
        } catch (e: GetCredentialException) {
            Log.e("GoogleRegister", "Error: ${e.message}")
            Toast.makeText(this, "Google Sign-Up failed: ${e.message}. Check if you're signed into Google on this device.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("GoogleRegister", "Unexpected Error: ${e.message}")
            Toast.makeText(this, "An unexpected error occurred.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loginWithGoogle(response: GetCredentialResponse) {
        val credential = response.credential
        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)

            firebaseAuth.signInWithCredential(authCredential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    Toast.makeText(this, "Google sign-up failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun onSuccess() {
        validateUserDBData {
            Toast.makeText(this, "Welcome to BantuNow!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}
