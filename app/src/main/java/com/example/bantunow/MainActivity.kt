package com.example.bantunow

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bantunow.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var database: FirebaseDatabase

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = FirebaseDatabase.getInstance(BuildConfig.DATABASE_URL)
        firebaseAuth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()

        if (savedInstanceState == null) {
            loadFragment(MapFragment())
        }


    }

    fun getDatabase() : FirebaseDatabase {
        return database
    }

    fun requireUser(): FirebaseUser {
        val user = firebaseAuth.currentUser ?: throw Exception("User not logged in")
        return user
    }

    fun requestLocation() : Task<Location> {
        val permGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (permGranted != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),1001)
        }
        return fusedLocationClient.lastLocation
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_map -> {
                    loadFragment(MapFragment())
                    true
                }
                R.id.nav_create -> {
                    loadFragment(WorkFormFragment())
                    true
                }
                R.id.nav_tasks -> {
                    // Placeholder for UI navigation
                    true
                }
                R.id.nav_profile -> {
                    // Placeholder for UI navigation
                    true
                }
                else -> false
            }
        }

        binding.fabBantubot.setOnClickListener {
            // Placeholder for UI action
        }
    }
}