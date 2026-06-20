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
            /**
             * Special handler for map web view client when its rendering process
             * is stopped
             * This handles the error gracefully to avoid crashing the entire app
             */
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

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}