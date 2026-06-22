package com.example.bantunow

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bantunow.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserProfile()

        binding.btnMenuLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        binding.btnMenuLeaderboard.setOnClickListener {
            Toast.makeText(context, "Leaderboard coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserProfile() {
        val user = auth.currentUser ?: return
        val userId = user.uid

        binding.tvProfileEmail.text = user.email

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("displayName") ?: "No Name"
                    val points = document.getLong("bantuPoints") ?: 0L
                    
                    binding.tvProfileName.text = name
                    binding.tvPoints.text = points.toString()
                }
            }

        db.collection("tasks")
            .whereEqualTo("workerID", userId)
            .whereEqualTo("status", "completed")
            .get()
            .addOnSuccessListener { result ->
                val count = result.size()
                binding.tvCompleted.text = count.toString()
                binding.tvRating.text = if (count > 0) "5.0" else "0.0"
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}