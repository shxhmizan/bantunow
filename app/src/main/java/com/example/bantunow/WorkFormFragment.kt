package com.example.bantunow

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bantunow.databinding.FragmentWorkFormBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class WorkFormFragment : Fragment() {

    private var _binding: FragmentWorkFormBinding? = null
    private val binding get() = _binding!!

    private var latitude: Double? = null
    private var longitude: Double? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val activity = requireActivity() as MainActivity
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()

        binding.layoutGetLocation.setOnClickListener {
            // Check for permissions first
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    1001
                )
                return@setOnClickListener
            }

            activity.requestLocation().addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    latitude = task.result.latitude
                    longitude = task.result.longitude
                    binding.tvLocationLabel.text = "Coordinate: ${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}"
                    Toast.makeText(context, "Location retrieved!", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("WorkForm", "Location task failed: ${task.exception}")
                    Toast.makeText(context, "Failed to get location. Ensure GPS is on.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnSubmitTask.setOnClickListener {
            val title = binding.etTaskTitle.text.toString()
            val desc = binding.etTaskDesc.text.toString()
            val payString = binding.etTaskPay.text.toString()
            val phone = binding.etTaskPhone.text.toString()

            if (title.isEmpty() || desc.isEmpty() || payString.isEmpty() || phone.isEmpty() || latitude == null) {
                Toast.makeText(context, "Please fill in all fields and get location", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val pay = payString.toDoubleOrNull() ?: 0.0
            val currentUserId = auth.currentUser?.uid ?: return@setOnClickListener

            val taskData = mapOf(
                "ownerID" to currentUserId,
                "title" to title,
                "desc" to desc,
                "paymentAmount" to (pay * 100).toLong(), // stored in cents
                "latitude" to latitude,
                "longitude" to longitude,
                "contactNo" to phone,
                "status" to "open",
                "createdAt" to com.google.firebase.Timestamp.now()
            )

            db.collection("tasks").add(taskData)
                .addOnSuccessListener {
                    Toast.makeText(context, "Task posted successfully!", Toast.LENGTH_LONG).show()
                    parentFragmentManager.popBackStack()
                }
                .addOnFailureListener { e ->
                    Log.e("WorkForm", "Error adding task", e)
                    Toast.makeText(context, "Failed to post task", Toast.LENGTH_SHORT).show()
                }
        }
        
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}