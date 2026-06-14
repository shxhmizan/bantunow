package com.example.bantunow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.bantunow.databinding.FragmentWorkFormBinding

class WorkFormFragment : Fragment() {

    private var _binding: FragmentWorkFormBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // UI logic only (e.g. setup listeners if needed for UI state)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}