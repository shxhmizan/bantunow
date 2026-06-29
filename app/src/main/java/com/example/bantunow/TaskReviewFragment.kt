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
import com.bumptech.glide.Glide
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
        
        // Hide bottom navigation
        (requireActivity() as? MainActivity)?.setBottomNavigationVisibility(false)

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val currentUserId = auth.currentUser?.uid
        val isOwner = task.ownerID == currentUserId

        if (isOwner) {
            binding.tvOwnerBadge.visibility = View.VISIBLE
            binding.tvTaskOwner.text = "You are the owner"
            
            binding.btnAccept.text = "EDIT TASK"
            binding.btnAccept.setIconResource(R.drawable.ic_edit)
            binding.btnAccept.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2196F3"))
            binding.btnAccept.setOnClickListener {
                // Navigate to Task Dashboard and trigger Edit
                (requireActivity() as MainActivity).navigateToTasks(task.taskId)
            }

            binding.btnDeleteTask.visibility = View.VISIBLE
            binding.btnDeleteTask.setOnClickListener {
                showDeleteConfirmation()
            }
        } else {
            binding.tvOwnerBadge.visibility = View.GONE
            binding.tvTaskOwner.text = userExtra?.displayName ?: "Unknown"
            binding.btnAccept.text = "ACCEPT TASK"
            binding.btnAccept.setOnClickListener {
                acceptTask()
            }
            binding.btnDeleteTask.visibility = View.GONE
        }

        binding.tvTaskTitle.text = task.title
        binding.tvTaskDesc.text = task.desc
        binding.tvTaskPhoneNo.text = task.contactNo
        binding.tvTaskPay.text = "RM ${(task.paymentAmount?.toDouble()?.div(100) ?: 0.0).toInt()}"
        binding.tvLocationCoords.text = String.format(Locale.US, "%.4f, %.4f", task.latitude, task.longitude)

        if (!task.imageUrl.isNullOrEmpty()) {
            binding.cardTaskImage.visibility = View.VISIBLE
            Glide.with(this).load(task.imageUrl).into(binding.ivTaskImage)
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

    private fun showDeleteConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete '${task.title}'? You will be refunded RM ${((task.paymentAmount ?: 0L) / 100)}.")
            .setPositiveButton("Delete") { _, _ ->
                deleteTask()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTask() {
        val userId = auth.currentUser?.uid ?: return
        val taskId = task.taskId ?: return

        db.runTransaction { transaction ->
            // Refund money
            val userRef = db.collection("users").document(userId)
            val userDoc = transaction.get(userRef)
            val currentBalance = userDoc.getDouble("walletBalance") ?: 0.0
            val currentPoints = userDoc.getLong("bantuPoints") ?: 0L
            val refundAmount = (task.paymentAmount ?: 0L).toDouble() / 100.0

            transaction.update(userRef, "walletBalance", currentBalance + refundAmount)
            transaction.update(userRef, "bantuPoints", maxOf(0L, currentPoints - 10L))

            // Delete task
            transaction.delete(db.collection("tasks").document(taskId))

            // Record transaction
            val transRef = db.collection("transactions").document()
            val transData = mapOf(
                "walletID" to userId,
                "type" to "topup",
                "amount" to (task.paymentAmount ?: 0L),
                "status" to "completed",
                "timestamp" to com.google.firebase.Timestamp.now(),
                "description" to "Refund for deleted task: ${task.title}"
            )
            transaction.set(transRef, transData)
        }.addOnSuccessListener {
            if (isAdded) {
                Toast.makeText(context, "Task deleted and refunded", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }.addOnFailureListener { e ->
            if (isAdded) {
                Toast.makeText(context, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Show bottom navigation again
        (requireActivity() as? MainActivity)?.setBottomNavigationVisibility(true)
        _binding = null
    }
}