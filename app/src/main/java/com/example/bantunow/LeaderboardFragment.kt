package com.example.bantunow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bantunow.databinding.FragmentLeaderboardBinding
import com.example.bantunow.databinding.ItemLeaderboardBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        
        binding.rvLeaderboard.layoutManager = LinearLayoutManager(context)
        fetchLeaderboard()
    }

    private fun fetchLeaderboard() {
        db.collection("users")
            .orderBy("bantuPoints", Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener { documents ->
                val list = mutableListOf<LeaderboardUser>()
                for (doc in documents) {
                    list.add(LeaderboardUser(
                        doc.getString("displayName") ?: "User",
                        doc.getLong("bantuPoints") ?: 0L
                    ))
                }
                binding.rvLeaderboard.adapter = LeaderboardAdapter(list)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class LeaderboardUser(val name: String, val points: Long)

    class LeaderboardAdapter(private val users: List<LeaderboardUser>) : RecyclerView.Adapter<LeaderboardAdapter.VH>() {
        class VH(val b: ItemLeaderboardBinding) : RecyclerView.ViewHolder(b.root)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(ItemLeaderboardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun onBindViewHolder(holder: VH, position: Int) {
            val u = users[position]
            holder.b.tvRank.text = (position + 1).toString()
            holder.b.tvName.text = u.name
            holder.b.tvPoints.text = "${u.points} pts"
        }
        override fun getItemCount() = users.size
    }
}