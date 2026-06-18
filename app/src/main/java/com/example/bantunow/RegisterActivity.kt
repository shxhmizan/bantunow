package com.example.bantunow

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import com.example.bantunow.data.model.UserExtra
import com.example.bantunow.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding

    private lateinit var databaseUrl:String

    private lateinit var database: FirebaseDatabase

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var credentialManager: CredentialManager

    companion object {
        /**
         * Validate that the current user has their data stored in realtime database
         * If the user is not in database,this method attempts to add the user
         * into the database
         * Note: The user data in realtime database is stored separately from user
         * data in firebase authentication service
         */
        fun validateUserDBData(firebaseAuth: FirebaseAuth, database: FirebaseDatabase){
            val user = firebaseAuth.currentUser ?: return
            val dbRef = database.getReference(UserExtra.USER_DOCS_ROOT)
            val userData = dbRef.child(user.uid)
            userData.get().addOnSuccessListener {
                    data ->
                val dataExists = data.exists()
                if(! dataExists){
                    val userInitData = UserExtra(0, user.displayName)
                    userData.setValue(userInitData).addOnCompleteListener {
                            task -> if(! task.isSuccessful){
                        Log.e("UserRegistration","Failed to save user data in database!")
                    }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        databaseUrl = BuildConfig.DATABASE_URL
        database = FirebaseDatabase.getInstance(databaseUrl)
        firebaseAuth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            view ->
            val name = binding.etName.text.toString()
            val password = binding.etPassword.text.toString()
            val email = binding.etEmail.text.toString()

            //TODO: Improve form validation rules for user registration
            if(name.isEmpty() || password.isEmpty() || email.isEmpty()){
                Toast.makeText(this@RegisterActivity,"Please fill in all fields.",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerUser(name,password,email)
        }
    }

    private fun registerUser(name:String,password:String,email:String){
        firebaseAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener {
            task ->
            if(task.isSuccessful){
                val user = firebaseAuth.currentUser
                val builder = UserProfileChangeRequest.Builder()
                builder.displayName = name
                val request = builder.build()
                user?.updateProfile(request)?.addOnCompleteListener {
                    onUserRegistration(task.isSuccessful)
                }
            }
            else onUserRegistration(false)
        }
    }

    private fun onUserRegistration(success:Boolean){
        if (success){
            Toast.makeText(
                this@RegisterActivity,
                "Registration successful, please proceed to login.",
                Toast.LENGTH_SHORT)
                .show()
            validateUserDBData(firebaseAuth,database)
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
        else {
            Toast.makeText(
                this@RegisterActivity,
                "Sorry, registration failed.",
                Toast.LENGTH_SHORT)
                .show()
        }
    }

}