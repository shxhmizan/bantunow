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

class UserJobListFragment : Fragment() {

    private var _binding: FragmentUserJobListBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private lateinit var postedAdapter: TaskAdapter
    private lateinit var acceptedAdapter: TaskAdapter

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
        fetchUserTasks()
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
    }

    private fun fetchUserTasks() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("tasks")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    if (isAdded) Toast.makeText(context, "Error fetching tasks: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val allTasks = snapshots?.documents?.mapNotNull { doc ->
                    doc.toObject(Task::class.java)
                } ?: emptyList()

                val postedTasks = allTasks.filter { it.ownerID == userId }
                val acceptedTasks = allTasks.filter { it.workerID == userId }

                postedAdapter.updateTasks(postedTasks)
                acceptedAdapter.updateTasks(acceptedTasks)

                binding.tvPostedCount.text = "POSTED BY ME (${postedTasks.size})"
                binding.tvAcceptedCount.text = "ACCEPTED BY ME (${acceptedTasks.size})"
            }
    }

    private fun showDeleteConfirmation(task: Task) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete '${task.title}'?")
            .setPositiveButton("Yes") { _, _ ->
                db.collection("tasks")
                    .whereEqualTo("title", task.title)
                    .whereEqualTo("ownerID", task.ownerID)
                    .get()
                    .addOnSuccessListener { result ->
                        for (doc in result) {
                            db.collection("tasks").document(doc.id).delete()
                                .addOnSuccessListener {
                                    if (isAdded) Toast.makeText(context, "Task deleted", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showEditDialog(task: Task) {
        val input = EditText(requireContext())
        input.setText(task.title)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Edit Task Title")
            .setView(input)
            .setPositiveButton("Update") { _, _ ->
                val newTitle = input.text.toString().trim()
                if (newTitle.isNotEmpty()) {
                    db.collection("tasks")
                        .whereEqualTo("title", task.title)
                        .whereEqualTo("ownerID", task.ownerID)
                        .get()
                        .addOnSuccessListener { result ->
                            for (doc in result) {
                                db.collection("tasks").document(doc.id).update("title", newTitle)
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
        val fragment = TaskDetailsFragment(task, 0.0, null)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
