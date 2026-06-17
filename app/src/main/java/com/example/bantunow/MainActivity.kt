package com.example.bantunow

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.bantunow.databinding.ActivityMainBinding
import com.example.bantunow.BuildConfig

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /***
     * This field stores the URL to the application's Firebase Realtime Database.
     * It must be set using the DATABASE_URL property in the database.properties file within
     * the project source code root directory.
     */
    private lateinit var databaseUrl:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseUrl = BuildConfig.DATABASE_URL

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()

        if (savedInstanceState == null) {
            loadFragment(MapFragment())
        }
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