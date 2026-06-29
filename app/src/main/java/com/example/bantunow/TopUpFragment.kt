package com.example.bantunow

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.bantunow.databinding.FragmentTopupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TopUpFragment : DialogFragment() {
    private var _binding: FragmentTopupBinding? = null
    private val binding get() = _binding!!

    private val malaysianBanks = arrayOf(
        "Maybank (Maybank2u)",
        "CIMB Clicks",
        "Public Bank",
        "RHB Now",
        "Hong Leong Connect",
        "AmOnline",
        "UOB TMRW",
        "Bank Islam",
        "Bank Rakyat",
        "OCBC Online Banking",
        "HSBC Online Banking",
        "Affin Always",
        "Alliance Online",
        "Standard Chartered",
        "MBSB Bank",
        "Bank Muamalat"
    )

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTopupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupBankDropdown()
        
        binding.btnBack.setOnClickListener { dismiss() }
        
        binding.btnProceed.setOnClickListener {
            val amountStr = binding.etAmount.text.toString()
            if (amountStr.isEmpty()) {
                Toast.makeText(requireContext(), "Enter amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val selectedBank = binding.actvBank.text.toString()
            if (selectedBank.isEmpty()) {
                Toast.makeText(requireContext(), "Please select a bank", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull() ?: 0.0
            showConfirmationDialog(amount, selectedBank)
        }
    }

    private fun setupBankDropdown() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, malaysianBanks)
        binding.actvBank.setAdapter(adapter)
    }

    private fun showConfirmationDialog(amount: Double, bank: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Top Up")
            .setMessage("Are you sure you want to top up RM ${String.format("%.2f", amount)} via $bank?")
            .setPositiveButton("Confirm") { dialog, _ ->
                startMockAuth(amount, bank)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startMockAuth(amount: Double, bank: String) {
        binding.llAuthView.visibility = View.VISIBLE
        binding.tvAuthStatus.text = "Connecting to $bank..."
        
        Handler(Looper.getMainLooper()).postDelayed({
            binding.tvAuthStatus.text = "Transaction Approved!"
            binding.pbPayment.visibility = View.GONE
            
            updateWallet(amount, bank)
        }, 3000)
    }

    private fun updateWallet(amount: Double, bank: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(uid)
        
        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val balance = snapshot.getDouble("walletBalance") ?: 0.0
            transaction.update(userRef, "walletBalance", balance + amount)
            
            // Record transaction
            val transRef = db.collection("transactions").document()
            val transData = mapOf(
                "walletID" to uid,
                "type" to "topup",
                "amount" to (amount * 100).toLong(),
                "status" to "completed",
                "timestamp" to com.google.firebase.Timestamp.now(),
                "description" to "Top-up via $bank"
            )
            transaction.set(transRef, transData)
        }.addOnSuccessListener {
            Handler(Looper.getMainLooper()).postDelayed({
                Toast.makeText(requireContext(), "Success! RM $amount added.", Toast.LENGTH_SHORT).show()
                dismiss()
            }, 1000)
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            binding.llAuthView.visibility = View.GONE
            binding.pbPayment.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}