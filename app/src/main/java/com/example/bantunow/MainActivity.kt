package com.example.bantunow

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bantunow.data.model.UserExtra
import com.example.bantunow.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var taskMapManager: TaskMapManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate")

        firebaseAuth = FirebaseAuth.getInstance()
        if (firebaseAuth.currentUser == null) {
            Log.w("MainActivity", "No user logged in, redirecting to Login")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val firestore = FirebaseFirestore.getInstance()
        taskMapManager = object : TaskMapManager(firestore, fusedLocationClient) {
            @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
            override fun onTaskSelected(task: com.example.bantunow.data.model.Task) {
                val taskCreator = task.ownerID ?: return
                
                firestore.collection("users").document(taskCreator).get()
                    .addOnSuccessListener { document ->
                        val userExtra = document.toObject(UserExtra::class.java)
                        
                        fusedLocationClient.lastLocation.addOnCompleteListener { locationTask ->
                            val location = locationTask.result
                            val distance = if (location != null && task.latitude != null && task.longitude != null) {
                                val taskLocation = Location("task")
                                taskLocation.latitude = task.latitude!!
                                taskLocation.longitude = task.longitude!!
                                location.distanceTo(taskLocation).toDouble() / 1000.0 // in KM
                            } else 0.0
                            
                            if(taskCreator == firebaseAuth.currentUser?.uid){
                                loadFragment(TaskDetailsFragment(task, distance, userExtra), true)
                            }
                            else loadFragment(TaskReviewFragment(task, distance, userExtra), true)
                        }
                    }
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()

        if (savedInstanceState == null) {
            Log.d("MainActivity", "Loading initial MapFragment")
            loadFragment(MapFragment())
        }

        // Seed data after UI is ready
        DataSeeder.seedDataIfNeeded()
    }

    fun requireUser(): FirebaseUser {
        return firebaseAuth.currentUser ?: throw Exception("User not logged in")
    }

    fun requestLocation() : Task<Location> {
        val finePerm = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarsePerm = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        
        if (finePerm != PackageManager.PERMISSION_GRANTED && coarsePerm != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1001)
            return Tasks.forException(SecurityException("Location permission not granted"))
        }
        
        return fusedLocationClient.getCurrentLocation(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            com.google.android.gms.tasks.CancellationTokenSource().token
        )
    }

    fun getTaskMapManager() : TaskMapManager {
        return taskMapManager
    }

    private fun loadFragment(fragment: Fragment, addToBackStack: Boolean = false) {
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
        
        if (addToBackStack) {
            transaction.addToBackStack(null)
        }
        transaction.commit()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_map -> {
                    loadFragment(MapFragment())
                    true
                }
                R.id.nav_create -> {
                    loadFragment(WorkFormFragment(), true)
                    true
                }
                R.id.nav_chatbot -> {
                    loadFragment(ChatbotFragment(), true)
                    true
                }
                R.id.nav_tasks -> {
                    loadFragment(UserJobListFragment(), true)
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment(), true)
                    true
                }
                else -> false
            }
        }
    }
}
