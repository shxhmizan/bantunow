package com.example.bantunow

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.bantunow.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var webView: WebView? = null
    private var isMapReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        
        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(MapFragment())
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // Set up local webview inside map view fragment
    @SuppressLint("SetJavaScriptEnabled")
    fun initializeLeafletMap(mapWebView: WebView) {
        webView = mapWebView
        mapWebView.settings.javaScriptEnabled = true
        mapWebView.settings.domStorageEnabled = true

        // Add Interface Object referenced by map.html (window.AndroidInterface)
        mapWebView.addJavascriptInterface(MapInterface(), "AndroidInterface")

        mapWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Page loaded, safe to call JS
            }
        }
        mapWebView.loadUrl("file:///android_asset/map.html")
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
                    // Load My Tasks List Fragment (placeholder)
                    true
                }
                R.id.nav_profile -> {
                    // Load Profile Screen (placeholder)
                    true
                }
                else -> false
            }
        }

        binding.fabBantubot.setOnClickListener {
            Toast.makeText(this, "BantuBot Dipanggil", Toast.LENGTH_SHORT).show()
            // Launch BantuBot Chat Dialog/Overlay
        }
    }

    // JS interface callback class
    inner class MapInterface {
        @JavascriptInterface
        fun onMapReady() {
            runOnUiThread {
                isMapReady = true
                // Plot tasks on map once ready
                plotSampleMarkers()
            }
        }

        @JavascriptInterface
        fun onJobSelected(jobId: String) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Tugasan Dipilih: $jobId", Toast.LENGTH_SHORT).show()
                // Navigate to Native Detail Screen
            }
        }
    }

    private fun plotSampleMarkers() {
        // Run Javascript function inside WebView
        webView?.evaluateJavascript(
            "addJobMarker('1', 3.1412, 101.6860, 'Pembersihan Pejabat', '150');",
            null
        )
        webView?.evaluateJavascript(
            "addJobMarker('2', 3.1350, 101.6900, 'Baiki Sinki Rosak', '80');",
            null
        )
    }
}
