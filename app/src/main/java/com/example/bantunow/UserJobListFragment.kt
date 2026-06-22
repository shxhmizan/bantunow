package com.example.bantunow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    private lateinit var taskAdapter: TaskAdapter

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

        setupRecyclerView()
        fetchUserTasks()
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            tasks = emptyList(),
            onTaskClick = { task -> navigateToDetail(task) },
            onCancelClick = { task -> showCancelConfirmation(task) }
        )
        binding.rvJobs.layoutManager = LinearLayoutManager(context)
        binding.rvJobs.adapter = taskAdapter
    }

    private fun fetchUserTasks() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("tasks")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(context, "Error fetching tasks: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val allTasks = snapshots?.documents?.mapNotNull { doc ->
                    doc.toObject(Task::class.java)
                } ?: emptyList()

                val userTasks = allTasks.filter { it.ownerID == userId || it.workerID == userId }
                taskAdapter.updateTasks(userTasks)
                binding.tvInProgressCount.text = "IN PROGRESS (${userTasks.size})"
            }
    }

    private fun showCancelConfirmation(task: Task) {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Task")
            .setMessage("Are you sure you want to cancel '${task.title}'?")
            .setPositiveButton("Yes") { _, _ ->
                db.collection("tasks")
                    .whereEqualTo("title", task.title)
                    .whereEqualTo("ownerID", task.ownerID)
                    .get()
                    .addOnSuccessListener { result ->
                        for (doc in result) {
                            db.collection("tasks").document(doc.id).delete()
                        }
                        Toast.makeText(context, "Task removed", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("No", null)
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
