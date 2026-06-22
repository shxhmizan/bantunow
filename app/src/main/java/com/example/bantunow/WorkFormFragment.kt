package com.example.bantunow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bantunow.databinding.FragmentWorkFormBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint

class WorkFormFragment : Fragment() {

    private var _binding: FragmentWorkFormBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnSubmitTask.setOnClickListener {
            submitJob()
        }
    }

    private fun submitJob() {
        val title = binding.etTaskTitle.text.toString().trim()
        val description = binding.etTaskDesc.text.toString().trim()
        val payStr = binding.etTaskPay.text.toString().trim()
        val phone = binding.etTaskPhone.text.toString().trim()
        val userId = auth.currentUser?.uid

        if (title.isEmpty() || description.isEmpty() || payStr.isEmpty() || phone.isEmpty() || userId == null) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val pay = payStr.toDoubleOrNull() ?: 0.0
        
        binding.btnSubmitTask.isEnabled = false
        
        val jobId = db.collection("jobs").document().id
        val newJob = Job(
            jobId = jobId,
            ownerId = userId,
            title = title,
            description = description,
            pay = pay,
            phone = phone,
            status = "OPEN",
            // For now, setting a default location if geolocation isn't implemented yet
            location = GeoPoint(3.1390, 101.6869), 
            createdAt = System.currentTimeMillis()
        )

        db.collection("jobs").document(jobId)
            .set(newJob)
            .addOnSuccessListener {
                Toast.makeText(context, "Job posted successfully!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                binding.btnSubmitTask.isEnabled = true
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}