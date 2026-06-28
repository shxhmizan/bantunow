package com.example.bantunow

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.bantunow.data.model.Task
import com.example.bantunow.data.model.UserExtra
import com.example.bantunow.databinding.FragmentJobDetailBinding
import com.google.firebase.firestore.FirebaseFirestore

class TaskDetailsFragment : Fragment() {
    private var _binding: FragmentJobDetailBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    private lateinit var task: Task
    private var distance: Double = 0.0
    private var userExtra: UserExtra? = null

    companion object {
        private const val ARG_TASK = "task"
        private const val ARG_DISTANCE = "distance"
        private const val ARG_USER_EXTRA = "user_extra"

        fun newInstance(task: Task, distance: Double, userExtra: UserExtra?): TaskDetailsFragment {
            val fragment = TaskDetailsFragment()
            val args = Bundle()
            args.putSerializable(ARG_TASK, task)
            args.putDouble(ARG_DISTANCE, distance)
            args.putSerializable(ARG_USER_EXTRA, userExtra)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            task = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable(ARG_TASK, Task::class.java)!!
            } else {
                @Suppress("DEPRECATION")
                it.getSerializable(ARG_TASK) as Task
            }
            distance = it.getDouble(ARG_DISTANCE)
            userExtra = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable(ARG_USER_EXTRA, UserExtra::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getSerializable(ARG_USER_EXTRA) as? UserExtra
            }
        }
    }

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
        
        userExtra?.profileImageUrl?.let { url ->
            Glide.with(this)
                .load(url)
                .circleCrop()
                .placeholder(R.drawable.ic_user)
                .into(binding.ivOwnerAvatar)
        }

        binding.tvTaskTitle.text = task.title
        binding.tvTaskDesc.text = task.desc
        binding.tvTaskPhoneNo.text = task.contactNo
        binding.tvTaskPay.text = "RM ${(task.paymentAmount?.toDouble()?.div(100) ?: 0.0).toInt()}"
        binding.tvTaskDistance.text = "${String.format("%.1f", distance)} km away"
        
        if (!task.imageUrl.isNullOrEmpty()) {
            binding.cardTaskImage.visibility = View.VISIBLE
            try {
                binding.ivTaskImage.setImageURI(Uri.parse(task.imageUrl))
            } catch (e: SecurityException) {
                Log.e("TaskDetails", "No permission to access URI: ${task.imageUrl}", e)
                binding.cardTaskImage.visibility = View.GONE
            }
        }

        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        
        binding.tvStatusBadge.text = task.status.replace("_", " ").uppercase()
        
        updateWorkflowIndicator(task.status)

        if (task.ownerID == currentUserId) {
            binding.tvRoleBadge.text = "YOU ARE OWNER"
            binding.btnCall.visibility = View.GONE
            binding.btnWhatsapp.visibility = View.GONE
            if (task.status == "done_by_worker") {
                binding.btnComplete.text = "CONFIRM JOB DONE"
                binding.btnComplete.visibility = View.VISIBLE
            } else if (task.status == "open" || task.status == "in_progress" || task.status == "ongoing") {
                binding.btnComplete.text = "DELETE TASK"
                binding.btnComplete.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.RED)
                binding.btnComplete.visibility = View.VISIBLE
            } else {
                binding.btnComplete.visibility = View.GONE
            }
        } else {
            binding.tvRoleBadge.text = "YOU ARE DOER"
            if (task.status == "in_progress" || task.status == "ongoing") {
                binding.btnComplete.text = "MARK AS DONE"
                binding.btnComplete.visibility = View.VISIBLE
                binding.btnComplete.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#26A69A"))
            } else if (task.status == "done_by_worker") {
                binding.btnComplete.text = "AWAITING CONFIRMATION"
                binding.btnComplete.isEnabled = false
                binding.btnComplete.visibility = View.VISIBLE
            } else {
                binding.btnComplete.text = "CANCEL JOB"
                binding.btnComplete.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.RED)
                binding.btnComplete.visibility = View.GONE // Can only cancel if in progress? Logic varies.
            }
        }

        binding.btnCall.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:${task.contactNo}")
            startActivity(intent)
        }

        binding.btnWhatsapp.setOnClickListener {
            val phone = task.contactNo?.replace(" ", "")?.replace("-", "")
            val url = "https://wa.me/$phone"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }

        binding.btnComplete.setOnClickListener {
            if (task.ownerID == currentUserId) {
                if (task.status == "done_by_worker") {
                    confirmTaskByOwner()
                } else if (task.status == "open" || task.status == "in_progress" || task.status == "ongoing") {
                    showDeleteConfirmation()
                }
            } else {
                markDoneByWorker()
            }
        }
    }

    private fun showDeleteConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete this task? You will be refunded RM ${((task.paymentAmount ?: 0L) / 100)}.")
            .setPositiveButton("Delete") { _, _ ->
                deleteTaskByOwner()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTaskByOwner() {
        val userId = task.ownerID ?: return
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
                activity?.applicationContext?.let {
                    Toast.makeText(it, "Task deleted and refunded", Toast.LENGTH_SHORT).show()
                }
                parentFragmentManager.popBackStack()
            }
        }.addOnFailureListener { e ->
            if (isAdded) {
                activity?.applicationContext?.let {
                    Toast.makeText(it, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateWorkflowIndicator(status: String) {
        val teal = android.graphics.Color.parseColor("#26A69A")
        val grey = android.graphics.Color.parseColor("#E0E0E0")

        binding.vStep1.backgroundTintList = android.content.res.ColorStateList.valueOf(teal)
        
        when (status) {
            "open" -> {
                binding.vLine1.backgroundTintList = android.content.res.ColorStateList.valueOf(grey)
                binding.vStep2.backgroundTintList = android.content.res.ColorStateList.valueOf(grey)
                binding.vLine2.backgroundTintList = android.content.res.ColorStateList.valueOf(grey)
                binding.vStep3.backgroundTintList = android.content.res.ColorStateList.valueOf(grey)
                binding.vLine3.backgroundTintList = android.content.res.ColorStateList.valueOf(grey)
                binding.vStep4.backgroundTintList = android.content.res.ColorStateList.valueOf(grey)
            }
            "in_progress", "ongoing" -> {
                binding.vLine1.backgroundTintList = android.content.res.ColorStateList.valueOf(teal)
                binding.vStep2.backgroundTintList = android.content.res.ColorStateList.valueOf(teal)
                binding.vLine2.backgroundTintList = android.content.res.ColorStateList.valueOf(grey)
                binding.vStep3.backgroundTintList = android.content.res.ColorStateList.valueOf(grey)
                binding.vLine3.backgroundTintList = android.content.res.ColorStateList.valueOf(grey)
                binding.vStep4.backgroundTintList = android.content.res.ColorStateList.valueOf(grey)
            }
            "done_by_worker" -> {
                binding.vLine1.backgroundTintList = android.content.res.ColorStateList.valueOf(teal)
                binding.vStep2.backgroundTintList = android.content.res.ColorStateList.valueOf(teal)
                binding.vLine2.backgroundTintList = android.content.res.ColorStateList.valueOf(teal)
                binding.vStep3.backgroundTintList = android.content.res.ColorStateList.valueOf(teal)
                binding.vLine3.backgroundTintList = android.content.res.ColorStateList.valueOf(grey)
                binding.vStep4.backgroundTintList = android.content.res.ColorStateList.valueOf(grey)
            }
            "completed", "done" -> {
                binding.vLine1.backgroundTintList = android.content.res.ColorStateList.valueOf(teal)
                binding.vStep2.backgroundTintList = android.content.res.ColorStateList.valueOf(teal)
                binding.vLine2.backgroundTintList = android.content.res.ColorStateList.valueOf(teal)
                binding.vStep3.backgroundTintList = android.content.res.ColorStateList.valueOf(teal)
                binding.vLine3.backgroundTintList = android.content.res.ColorStateList.valueOf(teal)
                binding.vStep4.backgroundTintList = android.content.res.ColorStateList.valueOf(teal)
            }
        }
    }

    private fun markDoneByWorker() {
        val taskId = task.taskId ?: return
        db.collection("tasks").document(taskId).update("status", "done_by_worker")
            .addOnSuccessListener {
                Toast.makeText(context, "Task marked as done. Waiting for owner to confirm.", Toast.LENGTH_LONG).show()
                binding.tvStatusBadge.text = "DONE BY WORKER"
                updateWorkflowIndicator("done_by_worker")
                binding.btnComplete.text = "AWAITING CONFIRMATION"
                binding.btnComplete.isEnabled = false
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error updating status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmTaskByOwner() {
        val taskId = task.taskId ?: return
        val workerId = task.workerID ?: return
        val payAmount = (task.paymentAmount ?: 0L).toDouble() / 100.0

        // Transaction: Mark task as done, Pay worker and award points
        db.runTransaction { transaction ->
            val workerRef = db.collection("users").document(workerId)
            val workerDoc = transaction.get(workerRef)
            val currentBalance = workerDoc.getDouble("walletBalance") ?: 0.0
            val currentPoints = workerDoc.getLong("bantuPoints") ?: 0L
            
            transaction.update(db.collection("tasks").document(taskId), "status", "completed")
            transaction.update(workerRef, "walletBalance", currentBalance + payAmount)
            transaction.update(workerRef, "bantuPoints", currentPoints + 15L) // +15 for completing task

            // Record transaction for worker
            val transRef = db.collection("transactions").document()
            val transData = mapOf(
                "walletID" to workerId,
                "type" to "received_payment",
                "amount" to (payAmount * 100).toLong(),
                "status" to "completed",
                "timestamp" to com.google.firebase.Timestamp.now(),
                "description" to "Payment received for: ${task.title}"
            )
            transaction.set(transRef, transData)
        }.addOnSuccessListener {
            Toast.makeText(context, "Job confirmed! Payment sent to worker.", Toast.LENGTH_LONG).show()
            updateWorkflowIndicator("completed")
            binding.btnComplete.visibility = View.GONE
            binding.tvStatusBadge.text = "COMPLETED"
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelTaskByWorker() {
        val taskId = task.taskId ?: return
        db.collection("tasks").document(taskId)
            .update("status", "open", "workerID", null)
            .addOnSuccessListener {
                Toast.makeText(context, "Task cancelled.", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}