package com.example.bantunow

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.bantunow.databinding.FragmentWorkFormBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class WorkFormFragment : Fragment() {

    private var _binding: FragmentWorkFormBinding? = null
    private val binding get() = _binding!!

    private var latitude: Double? = null
    private var longitude: Double? = null
    private var imageUri: Uri? = null

    private val categories = listOf("General", "Cleaning", "Delivery", "Repair", "Pet Care", "Education")

    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            _binding?.let { b ->
                b.ivTaskPreview.setImageURI(it)
                b.ivTaskPreview.visibility = View.VISIBLE
                b.llAddImagePlaceholder.visibility = View.GONE
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()

        setupSpinner()

        binding.layoutGetLocation.setOnClickListener {
            fetchCurrentLocation()
        }

        // Auto-fetch location on load
        fetchCurrentLocation()

        binding.layoutAddImage.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnSubmitTask.setOnClickListener {
            val title = binding.etTaskTitle.text.toString().trim()
            val desc = binding.etTaskDesc.text.toString().trim()
            val payString = binding.etTaskPay.text.toString().trim()
            val phone = binding.etTaskPhone.text.toString().trim()
            val category = binding.spinnerCategory.selectedItem.toString()

            if (title.isEmpty() || desc.isEmpty() || payString.isEmpty() || phone.isEmpty() || latitude == null) {
                Toast.makeText(requireContext(), "Please fill in all fields and set location", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val pay = payString.toDoubleOrNull() ?: 0.0
            val currentUserId = auth.currentUser?.uid ?: return@setOnClickListener

            // Check Wallet Balance
            val userRef = db.collection("users").document(currentUserId)
            userRef.get().addOnSuccessListener { doc ->
                if (_binding == null) return@addOnSuccessListener
                
                val balance = doc.getDouble("walletBalance") ?: 0.0
                if (balance < pay) {
                    Toast.makeText(context, "Insufficient wallet balance (RM $balance). Please top up first.", Toast.LENGTH_LONG).show()
                } else {
                    // Deduct, Post and Award Points
                    db.runTransaction { transaction ->
                        val currentBalance = doc.getDouble("walletBalance") ?: 0.0
                        val currentPoints = doc.getLong("bantuPoints") ?: 0L
                        
                        transaction.update(userRef, "walletBalance", currentBalance - pay)
                        transaction.update(userRef, "bantuPoints", currentPoints + 10L) // +10 for giving task
                        
                        // Record transaction for owner
                        val transRef = db.collection("transactions").document()
                        val transData = mapOf(
                            "walletID" to currentUserId,
                            "type" to "posted_task",
                            "amount" to (pay * 100).toLong(),
                            "status" to "completed",
                            "timestamp" to com.google.firebase.Timestamp.now(),
                            "description" to "Deduction for posting: $title"
                        )
                        transaction.set(transRef, transData)

                        val newTaskRef = db.collection("tasks").document()
                        val taskData = mutableMapOf<String, Any>(
                            "ownerID" to currentUserId,
                            "title" to title,
                            "desc" to desc,
                            "paymentAmount" to (pay * 100).toLong(),
                            "latitude" to (latitude ?: 0.0),
                            "longitude" to (longitude ?: 0.0),
                            "contactNo" to phone,
                            "status" to "open",
                            "createdAt" to com.google.firebase.Timestamp.now(),
                            "progressPercentage" to 0L,
                            "category" to category
                        )
                        if (imageUri != null) {
                            taskData["imageUrl"] = imageUri.toString()
                        }
                        transaction.set(newTaskRef, taskData)
                    }.addOnSuccessListener {
                        if (context == null) return@addOnSuccessListener
                        Toast.makeText(requireContext(), "Task posted! RM $pay deducted from wallet.", Toast.LENGTH_LONG).show()
                        parentFragmentManager.popBackStack()
                    }.addOnFailureListener { e ->
                        if (context == null) return@addOnFailureListener
                        Log.e("WorkForm", "Transaction failed", e)
                        Toast.makeText(requireContext(), "Failed to post task", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
    }

    private fun fetchCurrentLocation() {
        val activity = activity ?: return
        if (activity is MainActivity) {
            activity.requestLocation().addOnSuccessListener { location ->
                latitude = location.latitude
                longitude = location.longitude
                _binding?.let { b ->
                    b.tvLocationLabel.text = String.format(Locale.US, "Coordinate: %.6f, %.6f", latitude, longitude)
                    b.ivLocationCheck.visibility = View.VISIBLE
                    b.layoutGetLocation.setBackgroundResource(R.drawable.bg_input_rounded)
                }
            }.addOnFailureListener {
                useDefaultLocation()
            }
        } else {
            useDefaultLocation()
        }
    }

    private fun useDefaultLocation() {
        latitude = 4.183993
        longitude = 101.218559
        _binding?.let { b ->
            b.tvLocationLabel.text = String.format(Locale.US, "Default: %.6f, %.6f", latitude, longitude)
            b.ivLocationCheck.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}