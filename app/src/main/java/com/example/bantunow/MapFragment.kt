package com.example.bantunow

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
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
        }
        binding.mapWebView.loadUrl("file:///android_asset/map.html")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}