package com.example.bantunow

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.bantunow.databinding.FragmentMapBinding
import com.example.bantunow.R

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupStats()
        setupInsight()

        binding.mapWebView.settings.javaScriptEnabled = true
        binding.mapWebView.settings.domStorageEnabled = true
        binding.mapWebView.addJavascriptInterface(MapInterface(), "AndroidInterface")

        binding.mapWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                _binding?.let { safeBinding ->
                    val isDark = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
                    safeBinding.mapWebView.evaluateJavascript("setTheme('${if (isDark) "dark" else "light"}')", null)
                    addDummyMarkers()
                }
            }
        }
        binding.mapWebView.loadUrl("file:///android_asset/map.html")
    }

    private fun setupStats() {
        binding.statTotal.tvStatLabel.text = getString(R.string.total_jobs)
        binding.statTotal.tvStatValue.text = "30"
        binding.statTotal.ivStatIcon.setImageResource(R.drawable.ic_briefcase)

        binding.statOpen.tvStatLabel.text = getString(R.string.open_jobs)
        binding.statOpen.tvStatValue.text = "6"
        binding.statOpen.ivStatIcon.setImageResource(R.drawable.ic_map)
        binding.statOpen.ivStatIcon.setColorFilter(resources.getColor(R.color.primary_neon, null))

        binding.statProgress.tvStatLabel.text = getString(R.string.in_progress_jobs)
        binding.statProgress.tvStatValue.text = "12"
        binding.statProgress.ivStatIcon.setImageResource(R.drawable.ic_robot)
        binding.statProgress.ivStatIcon.setColorFilter(resources.getColor(R.color.danger, null))

        binding.statCompleted.tvStatLabel.text = getString(R.string.completed_jobs)
        binding.statCompleted.tvStatValue.text = "12"
        binding.statCompleted.ivStatIcon.setImageResource(R.drawable.ic_list)
    }

    private fun setupInsight() {
        binding.miniActive.tvMiniLabel.text = getString(R.string.active_10km)
        binding.miniActive.tvMiniValue.text = "2"

        binding.miniAverage.tvMiniLabel.text = getString(R.string.average_pay)
        binding.miniAverage.tvMiniValue.text = "RM 325"

        binding.miniPopular.tvMiniLabel.text = getString(R.string.popular_type)
        binding.miniPopular.tvMiniValue.text = "Cleaning"
        binding.miniPopular.tvMiniValue.setTextColor(resources.getColor(R.color.primary_neon, null))
    }

    private fun addDummyMarkers() {
        _binding?.let { safeBinding ->
            // addJobMarker(id, lat, lng, title, pay, category, desc, distance, status)
            safeBinding.mapWebView.evaluateJavascript("addJobMarker('1', 3.1522, 101.6965, 'IKEA Table Assembly', '100', 'CARPENTRY', 'Just assemble the table', '125.4', 'open')", null)
            safeBinding.mapWebView.evaluateJavascript("addJobMarker('2', 3.1390, 101.6869, 'Office Cleaning', '150', 'CLEANING', 'Daily office cleaning task', '2.5', 'progress')", null)
            safeBinding.mapWebView.evaluateJavascript("addJobMarker('3', 3.1450, 101.7000, 'Sink Repair', '80', 'PLUMBING', 'Leaking sink in kitchen', '5.1', 'open')", null)
        }
    }

    inner class MapInterface {
        @JavascriptInterface
        fun onJobSelected(jobId: String) {
            activity?.runOnUiThread {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, JobAcceptanceFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}