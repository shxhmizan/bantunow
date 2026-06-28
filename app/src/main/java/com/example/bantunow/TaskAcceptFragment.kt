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
import com.example.bantunow.databinding.FragmentJobAcceptanceBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TaskAcceptFragment(val task: Task, val distance: Double, val userExtra: UserExtra?) : Fragment() {
    private var _binding: FragmentJobAcceptanceBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJobAcceptanceBinding.inflate(inflater, container, false)
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
        binding.tvLocationCoords.text = "${String.format("%.4f", task.latitude)}, ${String.format("%.4f", task.longitude)}"

        if (!task.imageUrl.isNullOrEmpty()) {
            binding.cardTaskImage.visibility = View.VISIBLE
            try {
                binding.ivTaskImage.setImageURI(android.net.Uri.parse(task.imageUrl))
            } catch (e: SecurityException) {
                binding.cardTaskImage.visibility = View.GONE
            }
        }

        binding.btnAccept.setOnClickListener {
            acceptTask()
        }
    }

    private fun acceptTask() {
        val currentUserId = auth.currentUser?.uid ?: return
        
        // Find the document ID of the task
        db.collection("tasks")
            .whereEqualTo("title", task.title)
            .whereEqualTo("ownerID", task.ownerID)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val docId = documents.documents[0].id
                    
                    db.runTransaction { transaction ->
                        val taskRef = db.collection("tasks").document(docId)
                        val userRef = db.collection("users").document(currentUserId)
                        val userDoc = transaction.get(userRef)
                        val currentPoints = userDoc.getLong("bantuPoints") ?: 0L
                        
                        transaction.update(taskRef, "status", "in_progress")
                        transaction.update(taskRef, "workerID", currentUserId)
                        transaction.update(userRef, "bantuPoints", currentPoints + 5L) // +5 for accepting
                    }
                    .addOnSuccessListener {
                        Toast.makeText(context, "Task accepted! +5 pts", Toast.LENGTH_SHORT).show()
                        // Navigate to details/summary
                        val fragment = TaskDetailsFragment.newInstance(task, distance, userExtra)
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit()
                    }
                    .addOnFailureListener { e ->
                        Log.e("AcceptTask", "Error updating task", e)
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}