package com.example.bantunow

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.bantunow.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var userListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserData()

        binding.btnTopup.setOnClickListener {
            TopUpFragment().show(parentFragmentManager, "TopUpDialog")
        }

        binding.btnWithdrawEntry.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, WithdrawFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnMenuLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        binding.btnMenuTransactions.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, TransactionHistoryFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnMenuLeaderboard.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LeaderboardFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnMenuSettings.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SettingsFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnMenuTerms.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, TermsFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnMenuFounders.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, FoundersFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun loadUserData() {
        val user = auth.currentUser ?: return
        binding.tvProfileEmail.text = user.email

        userListener = db.collection("users").document(user.uid).addSnapshotListener { snapshot, e ->
            val binding = _binding ?: return@addSnapshotListener
            if (snapshot != null && snapshot.exists()) {
                val name = snapshot.getString("displayName") ?: user.displayName ?: "User"
                val balance = snapshot.getDouble("walletBalance") ?: 0.0
                val points = snapshot.getLong("bantuPoints") ?: 0L
                val imageUrl = snapshot.getString("profileImageUrl")
                val rating = snapshot.getDouble("rating") ?: 0.0
                
                binding.tvProfileName.text = name
                binding.tvWalletBalance.text = "RM ${String.format("%.2f", balance)}"
                binding.tvPoints.text = points.toString()
                binding.tvRating.text = String.format("%.1f", rating)
                
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(imageUrl)
                        .circleCrop()
                        .placeholder(R.drawable.ic_user)
                        .into(binding.ivAvatar)
                }
            }
        }
        
        // Count completed jobs
        db.collection("tasks")
            .whereEqualTo("workerID", user.uid)
            .whereEqualTo("status", "completed")
            .get()
            .addOnSuccessListener { docs ->
                val binding = _binding ?: return@addOnSuccessListener
                binding.tvCompleted.text = docs.size().toString()
            }
    }

    private fun topUpWallet() {
        val user = auth.currentUser ?: return
        val userRef = db.collection("users").document(user.uid)
        
        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentBalance = snapshot.getDouble("walletBalance") ?: 0.0
            transaction.update(userRef, "walletBalance", currentBalance + 50.0) // Demo top up
        }.addOnSuccessListener {
            context?.let {
                Toast.makeText(it, "Wallet topped up with RM 50.00!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        userListener?.remove()
        _binding = null
    }
}