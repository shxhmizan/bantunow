package com.example.bantunow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bantunow.databinding.FragmentUserJobListBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserJobListFragment : Fragment() {

    private var _binding: FragmentUserJobListBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var jobAdapter: JobAdapter

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
        fetchUserJobs()
    }

    private fun setupRecyclerView() {
        jobAdapter = JobAdapter(
            jobs = emptyList(),
            onJobClick = { job -> navigateToDetail(job) },
            onCancelClick = { job -> showCancelConfirmation(job) }
        )
        binding.rvJobs.layoutManager = LinearLayoutManager(context)
        binding.rvJobs.adapter = jobAdapter
    }

    private fun fetchUserJobs() {
        val userId = auth.currentUser?.uid ?: return

        // Fetch jobs where user is either owner or performer
        // Note: In a real app, you might need two separate queries and merge them 
        // or use an 'in' query if the data structure supports it.
        // For simplicity, we query jobs where ownerId == userId
        db.collection("jobs")
            .whereEqualTo("ownerId", userId)
            // .orderBy("createdAt", Query.Direction.DESCENDING) // Requires a composite index
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(context, "Error fetching tasks: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val jobList = snapshots?.toObjects(Job::class.java) ?: emptyList()
                // Sort manually in client side to avoid index requirement for now
                val sortedList = jobList.sortedByDescending { it.createdAt }
                jobAdapter.updateJobs(sortedList)
                binding.tvInProgressCount.text = "IN PROGRESS (${sortedList.size})"
            }
    }

    private fun showCancelConfirmation(job: Job) {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Task")
            .setMessage("Are you sure you want to cancel '${job.title}'?")
            .setPositiveButton("Yes") { _, _ ->
                job.jobId?.let { id ->
                    db.collection("jobs").document(id).delete()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Task removed", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun navigateToDetail(job: Job) {
        // You can pass job ID to the detail fragment via Arguments
        val fragment = JobDetailFragment().apply {
            arguments = Bundle().apply {
                putString("jobId", job.jobId)
            }
        }
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
