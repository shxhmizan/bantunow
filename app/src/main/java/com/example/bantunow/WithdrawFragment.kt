package com.example.bantunow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bantunow.databinding.FragmentWithdrawBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class WithdrawFragment : Fragment() {
    private var _binding: FragmentWithdrawBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWithdrawBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        
        binding.btnWithdraw.setOnClickListener {
            val amountStr = binding.etAmount.text.toString()
            if (amountStr.isEmpty()) return@setOnClickListener
            
            val amount = amountStr.toDoubleOrNull() ?: 0.0
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(uid)

            userRef.get().addOnSuccessListener { doc ->
                val current = doc.getDouble("walletBalance") ?: 0.0
                if (current >= amount) {
                    db.runTransaction { transaction ->
                        transaction.update(userRef, "walletBalance", current - amount)
                        
                        val transRef = db.collection("transactions").document()
                        val transData = mapOf(
                            "walletID" to uid,
                            "type" to "withdrawal",
                            "amount" to (amount * 100).toLong(),
                            "status" to "completed",
                            "timestamp" to com.google.firebase.Timestamp.now(),
                            "description" to "Withdraw to bank"
                        )
                        transaction.set(transRef, transData)
                    }.addOnSuccessListener {
                        Toast.makeText(context, "Withdrawal RM $amount processing...", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                } else {
                    Toast.makeText(context, "Insufficient balance", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}