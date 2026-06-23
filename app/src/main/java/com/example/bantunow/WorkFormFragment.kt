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
import java.util.Locale

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
            fetchCurrentLocation()
        }

        binding.btnSubmitTask.setOnClickListener {
            val title = binding.etTaskTitle.text.toString().trim()
            val desc = binding.etTaskDesc.text.toString().trim()
            val payString = binding.etTaskPay.text.toString().trim()
            val phone = binding.etTaskPhone.text.toString().trim()

            Log.d("WorkForm", "Title: '$title', Desc: '$desc', Pay: '$payString', Phone: '$phone', Lat: $latitude")

            if (title.isEmpty() || desc.isEmpty() || payString.isEmpty() || phone.isEmpty() || latitude == null) {
                val errorMsg = StringBuilder("Missing fields:")
                if (title.isEmpty()) errorMsg.append(" Title")
                if (desc.isEmpty()) errorMsg.append(" Description")
                if (payString.isEmpty()) errorMsg.append(" Pay")
                if (phone.isEmpty()) errorMsg.append(" Phone")
                if (latitude == null) errorMsg.append(" Location")
                
                Toast.makeText(requireContext(), errorMsg.toString(), Toast.LENGTH_SHORT).show()
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
                "createdAt" to com.google.firebase.Timestamp.now(),
                "progressPercentage" to 0L,
                "category" to "General"
            )

            db.collection("tasks").add(taskData)
                .addOnSuccessListener {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Task posted successfully!", Toast.LENGTH_LONG).show()
                        parentFragmentManager.popBackStack()
                    }
                }
                .addOnFailureListener { e ->
                    if (isAdded) {
                        Log.e("WorkForm", "Error adding task", e)
                        Toast.makeText(requireContext(), "Failed to post task", Toast.LENGTH_SHORT).show()
                    }
                }
        }
        
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun fetchCurrentLocation() {
        // Automatically use the requested coordinate: 4.183993, 101.218559
        latitude = 4.183993
        longitude = 101.218559
        
        binding.tvLocationLabel.text = String.format(Locale.US, "Coordinate: %.6f, %.6f", latitude, longitude)
        binding.ivLocationCheck.visibility = View.VISIBLE
        binding.layoutGetLocation.setBackgroundResource(R.drawable.bg_input_rounded)
        Toast.makeText(requireContext(), "Location retrieved!", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}