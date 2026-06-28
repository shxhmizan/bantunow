package com.example.bantunow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bantunow.data.model.Task
import com.example.bantunow.databinding.FragmentUserJobListBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class UserJobListFragment : Fragment() {

    private var _binding: FragmentUserJobListBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var taskListener: ListenerRegistration? = null
    
    private lateinit var postedAdapter: TaskAdapter
    private lateinit var acceptedAdapter: TaskAdapter
    private lateinit var historyAdapter: TaskAdapter

    private var allPostedTasks: List<Task> = emptyList()
    private var allAcceptedTasks: List<Task> = emptyList()
    private var allHistoryTasks: List<Task> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentUserJobListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        setupRecyclerViews()
        setupSearch()
        fetchUserTasks()
        fetchUserLocation()
    }

    private fun setupSearch() {
        binding.etSearchTasks.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterTasks(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun filterTasks(query: String) {
        val filteredPosted = allPostedTasks.filter { it.title?.contains(query, ignoreCase = true) == true }
        val filteredAccepted = allAcceptedTasks.filter { it.title?.contains(query, ignoreCase = true) == true }
        val filteredHistory = allHistoryTasks.filter { it.title?.contains(query, ignoreCase = true) == true }

        postedAdapter.updateTasks(filteredPosted)
        acceptedAdapter.updateTasks(filteredAccepted)
        historyAdapter.updateTasks(filteredHistory)

        binding.tvPostedCount.text = getString(R.string.posted_by_me, filteredPosted.size)
        binding.tvAcceptedCount.text = getString(R.string.accepted_by_me, filteredAccepted.size)
        binding.tvHistoryCount.text = getString(R.string.history, filteredHistory.size)
    }

    private fun setupRecyclerViews() {
        postedAdapter = TaskAdapter(
            tasks = emptyList(),
            onTaskClick = { task -> navigateToDetail(task) },
            onCancelClick = { task -> showDeleteConfirmation(task) },
            onEditClick = { task -> showEditDialog(task) }
        )
        binding.rvPosted.layoutManager = LinearLayoutManager(context)
        binding.rvPosted.adapter = postedAdapter

        acceptedAdapter = TaskAdapter(
            tasks = emptyList(),
            onTaskClick = { task -> navigateToDetail(task) },
            onCancelClick = { /* N/A */ }
        )
        binding.rvAccepted.layoutManager = LinearLayoutManager(context)
        binding.rvAccepted.adapter = acceptedAdapter

        historyAdapter = TaskAdapter(
            tasks = emptyList(),
            onTaskClick = { task -> navigateToDetail(task) },
            onCancelClick = { /* N/A */ }
        )
        binding.rvHistory.layoutManager = LinearLayoutManager(context)
        binding.rvHistory.adapter = historyAdapter
    }

    private fun fetchUserLocation() {
        (requireActivity() as MainActivity).requestLocation().addOnSuccessListener { location ->
            postedAdapter.updateUserLocation(location)
            acceptedAdapter.updateUserLocation(location)
            historyAdapter.updateUserLocation(location)
        }
    }

    private fun fetchUserTasks() {
        val userId = auth.currentUser?.uid ?: return

        taskListener = db.collection("tasks")
            .addSnapshotListener { snapshots, e ->
                val binding = _binding ?: return@addSnapshotListener

                if (e != null) {
                    if (isAdded) {
                        activity?.applicationContext?.let {
                            Toast.makeText(it, "Error fetching tasks: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    return@addSnapshotListener
                }

                val allTasksFromDb = snapshots?.documents?.mapNotNull { doc ->
                    doc.toObject(Task::class.java)?.apply { taskId = doc.id }
                } ?: emptyList()

                allPostedTasks = allTasksFromDb.filter { it.ownerID == userId && it.status != "completed" }
                allAcceptedTasks = allTasksFromDb.filter { it.workerID == userId && it.status != "completed" }
                allHistoryTasks = allTasksFromDb.filter { (it.ownerID == userId || it.workerID == userId) && it.status == "completed" }

                // Apply current filter
                filterTasks(binding.etSearchTasks.text.toString())
            }
    }

    private fun showDeleteConfirmation(task: Task) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_task)
            .setMessage("Are you sure you want to delete '${task.title}'? This will remove it from the database and refund your RM ${((task.paymentAmount ?: 0L) / 100)}.")
            .setPositiveButton("Yes") { _, _ ->
                val userId = auth.currentUser?.uid ?: return@setPositiveButton
                val taskId = task.taskId ?: return@setPositiveButton
                
                db.runTransaction { transaction ->
                    // Refund money
                    val userRef = db.collection("users").document(userId)
                    val userDoc = transaction.get(userRef)
                    val currentBalance = userDoc.getDouble("walletBalance") ?: 0.0
                    val currentPoints = userDoc.getLong("bantuPoints") ?: 0L
                    val refundAmount = (task.paymentAmount ?: 0L).toDouble() / 100.0
                    
                    transaction.update(userRef, "walletBalance", currentBalance + refundAmount)
                    transaction.update(userRef, "bantuPoints", maxOf(0L, currentPoints - 10L)) // Deduct points for deleting
                    
                    // Delete task
                    transaction.delete(db.collection("tasks").document(taskId))
                    
                    // Record transaction
                    val transRef = db.collection("transactions").document()
                    val transData = mapOf(
                        "walletID" to userId,
                        "type" to "topup", // Refund is like a top-up
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
                    }
                }.addOnFailureListener { e ->
                    if (isAdded) {
                        activity?.applicationContext?.let {
                            Toast.makeText(it, "Error deleting task: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showEditDialog(task: Task) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_task, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.etEditTitle)
        val etPay = dialogView.findViewById<EditText>(R.id.etEditPay)
        val etPhone = dialogView.findViewById<EditText>(R.id.etEditPhone)
        val btnRemoveImg = dialogView.findViewById<View>(R.id.btnRemoveImage)

        etTitle.setText(task.title)
        etPay.setText(((task.paymentAmount ?: 0L) / 100).toString())
        etPhone.setText(task.contactNo)

        var imageRemoved = false
        btnRemoveImg.visibility = if (task.imageUrl.isNullOrEmpty()) View.GONE else View.VISIBLE
        btnRemoveImg.setOnClickListener {
            imageRemoved = true
            btnRemoveImg.visibility = View.GONE
            Toast.makeText(context, "Image will be removed on save", Toast.LENGTH_SHORT).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.edit_task)
            .setView(dialogView)
            .setPositiveButton(R.string.update) { dialog, _ ->
                val newTitle = etTitle.text.toString().trim()
                val newPay = etPay.text.toString().toDoubleOrNull() ?: 0.0
                val newPhone = etPhone.text.toString().trim()

                if (newTitle.isNotEmpty()) {
                    db.collection("tasks")
                        .whereEqualTo("title", task.title)
                        .whereEqualTo("ownerID", task.ownerID)
                        .get()
                        .addOnSuccessListener { result ->
                            for (doc in result) {
                                val updates = mutableMapOf<String, Any>(
                                    "title" to newTitle,
                                    "paymentAmount" to (newPay * 100).toLong(),
                                    "contactNo" to newPhone
                                )
                                if (imageRemoved) {
                                    updates["imageUrl"] = ""
                                }
                                
                                db.collection("tasks").document(doc.id).update(updates)
                                    .addOnSuccessListener {
                                        if (isAdded) Toast.makeText(context, "Task updated", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateToDetail(task: Task) {
        val fragment = TaskDetailsFragment.newInstance(task, 0.0, null)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        taskListener?.remove()
        _binding = null
    }
}
