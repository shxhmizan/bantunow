package com.example.bantunow

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.lifecycle.lifecycleScope
import com.example.bantunow.databinding.FragmentMapBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object StringOrNumberSerializer : KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor("StringOrNumber", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): String {
        return if (decoder is JsonDecoder) {
            decoder.decodeJsonElement().jsonPrimitive.content
        } else {
            decoder.decodeString()
        }
    }
    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}

@Serializable
data class InsightData(
    val count: Int,
    val nearbyCount: Int = 0,
    @Serializable(with = StringOrNumberSerializer::class) val avg: String,
    val pop: String,
    val rawTasks: String? = null
)

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

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
        Log.d("MapFragment", "onViewCreated")

        val user = FirebaseAuth.getInstance().currentUser
        val firstName = user?.displayName?.split(" ")?.firstOrNull() ?: "User"
        binding.tvGreeting.text = "Hi, $firstName\nLooking for a job?"

        // Automatically get user location to sync map
        lifecycleScope.launch {
            try {
                val mainActivity = requireActivity() as MainActivity
                mainActivity.requestLocation().addOnSuccessListener {
                    // Location fetched, map will refresh via webview client once ready
                }
            } catch (e: Exception) {
                Log.e("MapFragment", "Auto-location failed", e)
            }
        }

        binding.switchInsights.setOnCheckedChangeListener { _, isChecked ->
            binding.insightContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Setup listener for JS console logs to update native insights
        binding.mapWebView.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                val msg = consoleMessage?.message() ?: ""
                Log.d("MapFragment", "Console: $msg")
                if (msg.startsWith("INSIGHTS:")) {
                    try {
                        val json = msg.substring(9)
                        val data = Json.decodeFromString<InsightData>(json)
                        activity?.runOnUiThread {
                            if (_binding != null) {
                                binding.statTotal.tvStatValue.text = data.count.toString()
                                binding.miniActive.tvMiniValue.text = data.nearbyCount.toString()
                                binding.miniAverage.tvMiniValue.text = "RM ${data.avg}"
                                binding.miniPopular.tvMiniValue.text = data.pop
                                
                                if (!data.rawTasks.isNullOrEmpty()) {
                                    summarizeTasks(data.rawTasks)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MapFragment", "Error parsing insights", e)
                    }
                }
                return super.onConsoleMessage(consoleMessage)
            }
        }

        // WebView setup
        val settings = binding.mapWebView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.databaseEnabled = true
        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        WebView.setWebContentsDebuggingEnabled(true)

        val mainActivity = requireActivity() as MainActivity

        if(WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)){
            val allowedOrigins = setOf("https://appassets.androidplatform.net")
            WebViewCompat.addWebMessageListener(
                binding.mapWebView,
                "appWebMsgListener",
                allowedOrigins,
                mainActivity.getTaskMapManager()
                )
            Log.d("MapFragment", "WebMessageListener added")
        }

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(requireContext()))
            .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(requireContext()))
            .build()

        binding.mapWebView.webViewClient = object : WebViewClient(){
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("MapFragment", "Page finished loading: $url")
                // Automatically show current location on load
                refreshLocation()
            }

            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }
        }

        Log.d("MapFragment", "Loading URL...")
        binding.mapWebView.loadUrl("https://appassets.androidplatform.net/assets/map.html")

        binding.fabMyLocation.setOnClickListener {
            refreshLocation()
        }

        binding.fabFilter.setOnClickListener {
            showFilterDialog()
        }

        setupInsights()
    }

    private var selectedRadius = 10f
    private var selectedCategory = "All"
    private val allCategories = arrayOf("All", "General", "Cleaning", "Delivery", "Repair", "Pet Care", "Education")

    private fun showFilterDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_map_filter, null)
        val slider = dialogView.findViewById<com.google.android.material.slider.Slider>(R.id.radiusSlider)
        val spinner = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerCategory)
        
        slider.value = selectedRadius
        
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, allCategories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(allCategories.indexOf(selectedCategory))

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Map Filters")
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                selectedRadius = slider.value
                selectedCategory = spinner.selectedItem.toString()
                applyFilters()
            }
            .setNegativeButton("Reset") { _, _ ->
                selectedRadius = 10f
                selectedCategory = "All"
                applyFilters()
            }
            .show()
    }

    private fun applyFilters() {
        binding.mapWebView.evaluateJavascript("setFilters($selectedRadius, '$selectedCategory');", null)
        Toast.makeText(context, "Filtering: $selectedRadius km, $selectedCategory", Toast.LENGTH_SHORT).show()
    }

    fun refreshLocation() {
        _binding?.mapWebView?.evaluateJavascript("requestLocationRefresh();", null)
        Log.d("MapFragment", "Location refresh requested")
    }

    private fun setupInsights() {
        binding.statTotal.tvStatLabel.text = "ACTIVE WORK"
        binding.statTotal.tvStatValue.text = "0"
        binding.miniActive.tvMiniLabel.text = "ACTIVE 10KM"
        binding.miniAverage.tvMiniLabel.text = "AVG INCOME"
        binding.miniPopular.tvMiniLabel.text = "POPULAR"
    }

    private fun summarizeTasks(rawTasks: String) {
        lifecycleScope.launch {
            try {
                val summary = callOpenRouterForSummary(rawTasks)
                _binding?.tvInsightDesc?.text = summary
            } catch (e: Exception) {
                Log.e("MapFragment", "AI Summary error", e)
            }
        }
    }

    private suspend fun callOpenRouterForSummary(rawTasks: String): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            val field = BuildConfig::class.java.getField("OPENROUTER_API_KEY")
            field.get(null) as String
        } catch (e: Exception) {
            ""
        }
        
        if (apiKey.isEmpty()) return@withContext "Check out these new opportunities!"

        val jsonBody = JsonObject()
        jsonBody.addProperty("model", "google/gemini-flash-1.5")
        
        val messages = JsonArray()
        val msg = JsonObject()
        msg.addProperty("role", "user")
        msg.addProperty("content", "Summarize these micro-jobs for a student. Keep it under 15 words. Jobs: $rawTasks")
        messages.add(msg)
        
        jsonBody.add("messages", messages)

        val body = jsonBody.toString().toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("HTTP-Referer", "https://bantunow.app")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext "Check out these new opportunities!"
            val result = response.body?.string() ?: return@withContext "Check out these new opportunities!"
            val jsonResponse = gson.fromJson(result, JsonObject::class.java)
            val choices = jsonResponse.getAsJsonArray("choices")
            if (choices.size() > 0) {
                choices[0].asJsonObject.getAsJsonObject("message").get("content").asString
            } else {
                "Check out these new opportunities!"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
