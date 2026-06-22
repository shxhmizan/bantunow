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
import com.example.bantunow.data.model.Task
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
        
        binding.switchInsights.setOnCheckedChangeListener { _, isChecked ->
            binding.insightContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Setup listener for JS console logs to update native insights
        binding.mapWebView.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                val msg = consoleMessage?.message() ?: ""
                if (msg.startsWith("INSIGHTS:")) {
                    try {
                        val json = msg.substring(9)
                        val data = Json.decodeFromString<InsightData>(json)
                        activity?.runOnUiThread {
                            binding.statTotal.tvStatValue.text = data.count.toString()
                            binding.miniActive.tvMiniValue.text = data.count.toString()
                            binding.miniAverage.tvMiniValue.text = "RM ${data.avg}"
                            binding.miniPopular.tvMiniValue.text = data.pop
                        }
                    } catch (e: Exception) {
                        Log.e("MapFragment", "Error parsing insights", e)
                    }
                }
                return super.onConsoleMessage(consoleMessage)
            }
        }

        // Simple UI-only WebView setup
        binding.mapWebView.settings.javaScriptEnabled = true
        binding.mapWebView.settings.domStorageEnabled = true

        val mainActivity = requireActivity() as MainActivity

        if(WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)){
            val allowedOrigins = setOf("https://appassets.androidplatform.net")
            WebViewCompat.addWebMessageListener(
                binding.mapWebView,
                "appWebMsgListener",
                allowedOrigins,
                mainActivity.getTaskMapManager()
                )
        }
        else {
            Log.w("MapFragment","The current WebView version does not support WebMessageListener !")
        }

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(requireContext()))
            .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(requireContext()))
            .build()

        binding.mapWebView.webViewClient = object : WebViewClient(){
            override fun onRenderProcessGone(
                view: WebView?,
                detail: RenderProcessGoneDetail?
            ): Boolean {
                if (detail?.didCrash() == true){
                    Log.e("WebView", "Render process crashed!")
                }
                else {
                    Log.e("WebView", "Render process killed by system!")
                }
                val parent = view?.parent as? ViewGroup
                parent?.removeView(view)
                view?.destroy()

                return true
            }

            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }
        }

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