package com.example.bantunow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.bantunow.data.model.Task
import com.example.bantunow.data.model.UserExtra
import com.example.bantunow.databinding.FragmentJobDetailBinding

class TaskDetailsFragment(val task: Task,val distance:Double, val userExtra: UserExtra?) : Fragment() {
    private var _binding: FragmentJobDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJobDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()
        if(activity !is MainActivity) return


        binding.tvTaskOwner.text = userExtra?.displayName
        binding.tvTaskTitle.text = task.title
        binding.tvTaskDesc.text = task.desc
        binding.tvTaskPhoneNo.text = task.contactNo
        binding.tvTaskPay.text = (task.paymentAmount?.toDouble()?.div(100)).toString()
        binding.tvTaskDistance.text = distance.toString()
    }
}