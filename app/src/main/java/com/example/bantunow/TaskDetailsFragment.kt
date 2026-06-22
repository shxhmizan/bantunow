package com.example.bantunow

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bantunow.data.model.Task
import com.example.bantunow.data.model.UserExtra
import com.example.bantunow.databinding.FragmentJobDetailBinding
import com.google.firebase.firestore.FirebaseFirestore

class TaskDetailsFragment(val task: Task, val distance: Double, val userExtra: UserExtra?) : Fragment() {
    private var _binding: FragmentJobDetailBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJobDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.tvTaskOwner.text = userExtra?.displayName ?: "Unknown"
        binding.tvTaskTitle.text = task.title
        binding.tvTaskDesc.text = task.desc
        binding.tvTaskPhoneNo.text = task.contactNo
        binding.tvTaskPay.text = "RM ${(task.paymentAmount?.toDouble()?.div(100) ?: 0.0).toInt()}"
        binding.tvTaskDistance.text = "${String.format("%.1f", distance)} km dari anda"
        
        binding.tvProgressPercent.text = "0%"
        binding.progressBarTask.progress = 0

        binding.btnCall.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:${task.contactNo}")
            startActivity(intent)
        }

        binding.btnWhatsapp.setOnClickListener {
            val url = "https://api.whatsapp.com/send?phone=${task.contactNo}"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }

        binding.btnComplete.setOnClickListener {
            completeTask()
        }

        binding.btnBackToMap.setOnClickListener {
            parentFragmentManager.popBackStack("MAP", 0)
        }
    }

    private fun completeTask() {
        // Find document and mark as completed
        db.collection("tasks")
            .whereEqualTo("title", task.title)
            .whereEqualTo("ownerID", task.ownerID)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val docId = documents.documents[0].id
                    db.collection("tasks").document(docId)
                        .update("status", "completed")
                        .addOnSuccessListener {
                            Toast.makeText(context, "Tahniah! Kerja Selesai.", Toast.LENGTH_SHORT).show()
                            binding.tvProgressPercent.text = "100%"
                            binding.progressBarTask.progress = 100
                            binding.btnComplete.isEnabled = false
                        }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}