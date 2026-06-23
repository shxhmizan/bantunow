package com.example.bantunow

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bantunow.data.model.Task
import com.example.bantunow.data.model.UserExtra
import com.example.bantunow.databinding.FragmentTaskReviewBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class TaskReviewFragment(val task: Task, val distance: Double, val userExtra: UserExtra?) : Fragment() {
    private var _binding: FragmentTaskReviewBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskReviewBinding.inflate(inflater, container, false)
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
        binding.tvLocationCoords.text = String.format(Locale.US, "%.4f, %.4f", task.latitude, task.longitude)

        binding.btnAccept.setOnClickListener {
            acceptTask()
        }
    }

    private fun acceptTask() {
        val currentUserId = auth.currentUser?.uid ?: return
        
        // Find the document ID of the task in Firestore
        db.collection("tasks")
            .whereEqualTo("title", task.title)
            .whereEqualTo("ownerID", task.ownerID)
            .whereEqualTo("status", "open")
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val docId = documents.documents[0].id
                    db.collection("tasks").document(docId)
                        .update(
                            "status", "in_progress",
                            "workerID", currentUserId
                        )
                        .addOnSuccessListener {
                            if (isAdded) {
                                Toast.makeText(context, "Task accepted!", Toast.LENGTH_SHORT).show()
                                // Navigate to My Tasks dashboard
                                (requireActivity() as MainActivity).binding.bottomNavigation.selectedItemId = R.id.nav_tasks
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("AcceptTask", "Error updating task", e)
                        }
                } else {
                    Toast.makeText(context, "Task no longer available", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}