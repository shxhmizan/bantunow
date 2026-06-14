package com.example.bantunow

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.bantunow.databinding.FragmentLeaderboardBinding

class LeaderboardFragment : Fragment() {
    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        // Setup Heatmap
        binding.heatmapWebview.settings.javaScriptEnabled = true
        binding.heatmapWebview.settings.domStorageEnabled = true
        binding.heatmapWebview.webViewClient = WebViewClient()
        binding.heatmapWebview.loadUrl("file:///android_asset/heatmap.html")

        // Setup Leaderboard List
        val sampleData = listOf(
            Pair("Ahmad Ali", 42),
            Pair("Siti Aminah", 38),
            Pair("John Doe", 35),
            Pair("Sarah Tan", 29),
            Pair("Miz nordin", 25)
        )

        sampleData.forEachIndexed { index, data ->
            val itemView = layoutInflater.inflate(R.layout.item_leaderboard, binding.leaderboardContainer, false)
            val rankText = (index + 1).toString()
            itemView.findViewById<TextView>(R.id.tv_rank).text = rankText
            itemView.findViewById<TextView>(R.id.tv_name).text = data.first
            itemView.findViewById<TextView>(R.id.tv_tasks_count).text = getString(R.string.leaderboard_tasks_suffix, data.second)
            binding.leaderboardContainer.addView(itemView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}