package com.example.bantunow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.bantunow.databinding.FragmentUserJobListBinding

class UserJobListFragment : Fragment() {

    private var _binding: FragmentUserJobListBinding? = null
    private val binding get() = _binding!!

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

        setupActiveTasks()
    }

    private fun setupActiveTasks() {
        // Setup Active Task 1
        val task1 = binding.itemActive1.root
        task1.setOnClickListener { navigateToDetail() }
        
        val btnCancel1 = task1.findViewById<ImageView>(R.id.btn_delete_job)
        btnCancel1.visibility = View.VISIBLE
        btnCancel1.setOnClickListener {
            showCancelConfirmation(task1, "Garden Lamp Wiring")
        }

        // Setup Active Task 2
        val task2 = binding.itemActive2.root
        task2.setOnClickListener { navigateToDetail() }
        
        val btnCancel2 = task2.findViewById<ImageView>(R.id.btn_delete_job)
        btnCancel2.visibility = View.VISIBLE
        btnCancel2.setOnClickListener {
            showCancelConfirmation(task2, "Garden Lamp Wiring")
        }
    }

    private fun showCancelConfirmation(view: View, taskTitle: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Task")
            .setMessage("Are you sure you want to cancel '$taskTitle'? This task will be returned to the map for others.")
            .setPositiveButton("Cancel Task") { _, _ ->
                view.visibility = View.GONE
                Toast.makeText(context, "Task cancelled and removed from your list", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("No, keep it", null)
            .show()
    }

    private fun navigateToDetail() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, JobDetailFragment())
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}