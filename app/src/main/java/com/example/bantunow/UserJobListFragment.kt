package com.example.bantunow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        val content = binding.llMainContainer

        for (i in 0 until content.childCount) {
            val child = content.getChildAt(i)
            (child as? com.google.android.material.card.MaterialCardView)?.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, JobDetailFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}