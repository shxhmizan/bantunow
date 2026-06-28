package com.example.bantunow

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.webkit.WebViewAssetLoader
import com.bumptech.glide.Glide
import com.example.bantunow.databinding.FragmentLeaderboardBinding
import com.example.bantunow.databinding.ItemLeaderboardBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private var leaderboardListener: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        
        binding.rvLeaderboard.layoutManager = LinearLayoutManager(context)
        setupRealtimeLeaderboard()
        setupHeatmap()
    }

    private fun setupRealtimeLeaderboard() {
        leaderboardListener = db.collection("users")
            .orderBy("bantuPoints", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    if (isAdded) Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val list = mutableListOf<LeaderboardUser>()
                    for (doc in snapshots) {
                        list.add(LeaderboardUser(
                            doc.getString("displayName") ?: "User",
                            doc.getLong("bantuPoints") ?: 0L,
                            doc.getString("profileImageUrl") ?: ""
                        ))
                    }
                    binding.rvLeaderboard.adapter = LeaderboardAdapter(list)
                }
            }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupHeatmap() {
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(requireContext()))
            .build()

        binding.heatmapWebview.settings.javaScriptEnabled = true
        binding.heatmapWebview.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }
        }
        binding.heatmapWebview.loadUrl("https://appassets.androidplatform.net/assets/heatmap.html")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        leaderboardListener?.remove()
        _binding = null
    }

    data class LeaderboardUser(val name: String, val points: Long, val imageUrl: String)

    class LeaderboardAdapter(private val users: List<LeaderboardUser>) : RecyclerView.Adapter<LeaderboardAdapter.VH>() {
        class VH(val b: ItemLeaderboardBinding) : RecyclerView.ViewHolder(b.root)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(ItemLeaderboardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        
        override fun onBindViewHolder(holder: VH, position: Int) {
            val u = users[position]
            val context = holder.itemView.context
            
            holder.b.tvRank.text = (position + 1).toString()
            holder.b.tvName.text = u.name
            holder.b.tvPoints.text = "${u.points} pts"

            // Handle Top 3 Medals
            when (position) {
                0 -> { // Gold
                    holder.b.tvRank.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.medal_gold))
                    holder.b.tvRank.setTextColor(ContextCompat.getColor(context, R.color.white))
                    holder.b.cardUser.strokeColor = ContextCompat.getColor(context, R.color.medal_gold)
                    holder.b.cardUser.strokeWidth = 4
                }
                1 -> { // Silver
                    holder.b.tvRank.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.medal_silver))
                    holder.b.tvRank.setTextColor(ContextCompat.getColor(context, R.color.white))
                    holder.b.cardUser.strokeColor = ContextCompat.getColor(context, R.color.medal_silver)
                    holder.b.cardUser.strokeWidth = 3
                }
                2 -> { // Bronze
                    holder.b.tvRank.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.medal_bronze))
                    holder.b.tvRank.setTextColor(ContextCompat.getColor(context, R.color.white))
                    holder.b.cardUser.strokeColor = ContextCompat.getColor(context, R.color.medal_bronze)
                    holder.b.cardUser.strokeWidth = 2
                }
                else -> {
                    holder.b.tvRank.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.ui_light_grey))
                    holder.b.tvRank.setTextColor(ContextCompat.getColor(context, R.color.brand_dark))
                    holder.b.cardUser.strokeColor = ContextCompat.getColor(context, R.color.ui_light_grey)
                    holder.b.cardUser.strokeWidth = 2
                }
            }

            if (u.imageUrl.isNotEmpty()) {
                Glide.with(context)
                    .load(u.imageUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_user)
                    .into(holder.b.ivAvatar)
            } else {
                holder.b.ivAvatar.setImageResource(R.drawable.ic_user)
            }
        }
        override fun getItemCount() = users.size
    }
}