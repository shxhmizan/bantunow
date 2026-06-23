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
import androidx.fragment.app.Fragment
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.bantunow.databinding.FragmentMapBinding
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class InsightData(val count: Int, val avg: String, val pop: String)

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
        Log.d("MapFragment", "onViewCreated")
        
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
                                binding.miniActive.tvMiniValue.text = data.count.toString()
                                binding.miniAverage.tvMiniValue.text = "RM ${data.avg}"
                                binding.miniPopular.tvMiniValue.text = data.pop
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
        } else {
            Log.w("MapFragment","The current WebView version does not support WebMessageListener !")
        }

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(requireContext()))
            .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(requireContext()))
            .build()

        binding.mapWebView.webViewClient = object : WebViewClient(){
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("MapFragment", "Page finished loading: $url")
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                Log.e("MapFragment", "WebView Error: ${error?.description} at ${request?.url}")
            }

            override fun onRenderProcessGone(
                view: WebView?,
                detail: RenderProcessGoneDetail?
            ): Boolean {
                Log.e("MapFragment", "Render process gone!")
                return false // Let it crash or handle better
            }

            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                Log.d("MapFragment", "Intercepting: ${request.url}")
                return assetLoader.shouldInterceptRequest(request.url)
            }
        }

        Log.d("MapFragment", "Loading URL...")
        binding.mapWebView.loadUrl("https://appassets.androidplatform.net/assets/map.html")

        setupInsights()
    }

    private fun setupInsights() {
        binding.statTotal.tvStatLabel.text = "ACTIVE WORK"
        binding.statTotal.tvStatValue.text = "0"

        binding.miniActive.tvMiniLabel.text = "ACTIVE 10KM"
        binding.miniActive.tvMiniValue.text = "0"

        binding.miniAverage.tvMiniLabel.text = "AVERAGE INCOME"
        binding.miniAverage.tvMiniValue.text = "RM 0"

        binding.miniPopular.tvMiniLabel.text = "POPULAR"
        binding.miniPopular.tvMiniValue.text = "-"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}