package com.example.bantunow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.bantunow.databinding.FragmentJobHistoryBinding

class JobHistoryFragment : Fragment() {

    private var _binding: FragmentJobHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentJobHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        setupHistoryItems()
    }

    private fun setupHistoryItems() {
        // Setup Item 1
        val item1 = binding.itemHistory1.root
        val btnDelete1 = item1.findViewById<ImageView>(R.id.btn_delete_job)
        btnDelete1.visibility = View.VISIBLE
        btnDelete1.setOnClickListener {
            showDeleteConfirmation(item1, "Garden Lamp Wiring")
        }

        // Setup Item 2
        val item2 = binding.itemHistory2.root
        val btnDelete2 = item2.findViewById<ImageView>(R.id.btn_delete_job)
        btnDelete2.visibility = View.VISIBLE
        btnDelete2.setOnClickListener {
            showDeleteConfirmation(item2, "Garden Lamp Wiring") // Both samples have same title currently
        }
    }

    private fun showDeleteConfirmation(view: View, jobTitle: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete History")
            .setMessage("Are you sure you want to delete '$jobTitle' from your history?")
            .setPositiveButton("Delete") { _, _ ->
                view.visibility = View.GONE
                if (isAdded) {
                    activity?.applicationContext?.let {
                        Toast.makeText(it, "Item deleted", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}