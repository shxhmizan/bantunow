package com.example.bantunow

import android.app.Dialog
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
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    private lateinit var database: FirebaseDatabase

    private lateinit var authWebClientId:String

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authWebClientId = BuildConfig.AUTH_WEB_CLIENT_ID

        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(BuildConfig.DATABASE_URL)
        credentialManager = CredentialManager.create(this)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //If user clicks normal login button with email and password
        binding.btnLogin.setOnClickListener { view ->
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if(email.isEmpty() || password.isEmpty()){
                Toast.makeText(this@LoginActivity,"Please fill in all fields.",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginWithFirebase(email, password)
        }

        //If user clicks login with Google
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

    /**
     * Sign in using Firebase email and password authentication
     */
    private fun loginWithFirebase(email:String,password:String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                task -> onUserLogin(task.isSuccessful)
            }
            .addOnFailureListener {
                ex -> onUserLogin(false)
                Log.e("LoginActivity", "Login failed due to exception", ex)
            }
    }

    /**
     * Makes a request for Google account credentials to Google servers
     */
    private suspend fun requestGoogleLogin(){
        //Metadata specifying request is for Google IDs
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(authWebClientId)
            .build()
        //Construct request to server
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            //Display dialog for user to pick Google account to sign in with, and sends
            //request to Google for their account's credentials
            val result = credentialManager.getCredential(this, request)

            //Once server has responded, use the returned credentials to attempt sign in
            loginWithGoogle(result)
        }
        catch (ex : Exception){
            Toast.makeText(
                this,
                "Sorry, authentication failed.",
                Toast.LENGTH_SHORT)
                .show()
            Log.e("ERR_LOGIN",ex.toString())
        }

    }

    /**
     * Sign in using Google credentials returned by server
     * Must request user credentials using requestGoogleSignIn() first
     */
    private fun loginWithGoogle(response: GetCredentialResponse){
        val credential = response.credential
        //Check if server responded with correct Google Account credentials
        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) try {
            val googleTokenIdCred = GoogleIdTokenCredential.createFrom(credential.data)

            //Unique token returned by Google server for the user's account
            val token = googleTokenIdCred.idToken
            //Convert token to Firebase credential object
            val cred = GoogleAuthProvider.getCredential(token,null)

            //Attempt sign in using Firebase, with Google authentication provider
            //Note: New users will automatically be registered by this method
            firebaseAuth.signInWithCredential(cred).addOnCompleteListener {
                    task -> onUserLogin(task.isSuccessful)
            }
        }
        catch(ex : Exception){
            Log.e("ERR_AUTH",ex.toString())
        }
    }

    private fun onUserLogin(success : Boolean){
        val user = firebaseAuth.currentUser
        if (success && user != null){
            Toast.makeText(
                this@LoginActivity,
                "Authentication succeeded.",
                Toast.LENGTH_SHORT)
                .show()
            RegisterActivity.validateUserDBData(firebaseAuth,database)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        else {
            Toast.makeText(
                this@LoginActivity,
                "Sorry, authentication failed.",
                Toast.LENGTH_SHORT)
                .show()
        }
    }
}