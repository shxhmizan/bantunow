package com.example.bantunow

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bantunow.data.model.Task
import com.example.bantunow.databinding.FragmentWorkFormBinding

class WorkFormFragment() : Fragment() {

    private var _binding: FragmentWorkFormBinding? = null
    private val binding get() = _binding!!

    private var location:String? = null

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
        binding.layoutGetLocation.setOnClickListener {
            val activity = requireActivity()
            if(activity !is MainActivity) return@setOnClickListener

            activity.requestLocation().addOnCompleteListener {
                task ->
                if(task.isSuccessful){
                    location = task.result.latitude.toString() + "," + task.result.longitude.toString()
                    binding.tvLocationLabel.text = getString(R.string.update_location)
                }
            }

        }

        binding.btnSubmitTask.setOnClickListener {
            _ ->
            val activity = requireActivity()
            if(activity !is MainActivity) return@setOnClickListener

            val title = binding.etTaskTitle.text.toString()
            val desc = binding.etTaskDesc.text.toString()
            val taskPaymentAmount = binding.etTaskPay.text.toString()
            val contactNo = binding.etTaskPhone.text.toString()

            if(title.isEmpty() || desc.isEmpty() || taskPaymentAmount.isEmpty() || contactNo.isEmpty()){
                Toast.makeText(activity,"Please fill in all fields.",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(location == null){
                Toast.makeText(activity,"Please select a location.",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = activity.requireUser()
            val database = activity.getDatabase()

            val task = Task(
                ownerID = user.uid,
                taskTitle = title,
                taskDesc = desc,
                taskPaymentAmount = taskPaymentAmount.toLong() * 100,
                location = location,
                contactNo = contactNo
            )
            task.insert(database).addOnCompleteListener {
                task ->
                if(task.isSuccessful){
                    Toast.makeText(activity,"Task submitted successfully.",Toast.LENGTH_SHORT).show()
                }
                else {
                    Log.e("Database","Error inserting task",task.exception)
                    Toast.makeText(activity,"Task submission failed.",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}