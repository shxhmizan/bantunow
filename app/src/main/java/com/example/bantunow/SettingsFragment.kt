package com.example.bantunow

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.bantunow.databinding.FragmentSettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var profileImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            profileImageUri = uri
            Glide.with(this).load(uri).circleCrop().into(binding.ivSettingsAvatar)
            Toast.makeText(requireContext(), "New profile picture selected!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = auth.currentUser
        binding.etSettingsName.setText(currentUser?.displayName ?: "")
        
        // Load current profile image
        db.collection("users").document(currentUser?.uid ?: "").get().addOnSuccessListener { doc ->
            val imageUrl = doc.getString("profileImageUrl")
            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(imageUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_user)
                    .into(binding.ivSettingsAvatar)
            }
        }

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnChangeAvatar.setOnClickListener {
            pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnSaveSettings.setOnClickListener {
            validateAndShowConfirmation()
        }
    }

    private fun validateAndShowConfirmation() {
        val newPassword = binding.etSettingsPassword.text.toString().trim()
        val confirmPassword = binding.etSettingsConfirmPassword.text.toString().trim()

        if (newPassword.isNotEmpty() || confirmPassword.isNotEmpty()) {
            if (newPassword != confirmPassword) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                return
            }
            if (newPassword.length < 6) {
                Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return
            }
        }

        showConfirmationDialog()
    }

    private fun showConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Changes")
            .setMessage("Are you sure you want to save these changes to your profile?")
            .setPositiveButton("Yes") { _, _ ->
                saveChanges()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun saveChanges() {
        val currentUser = auth.currentUser ?: return
        val newName = binding.etSettingsName.text.toString().trim()
        val newPassword = binding.etSettingsPassword.text.toString().trim()

        val updates = mutableMapOf<String, Any>()

        // Update Display Name if changed
        if (newName.isNotEmpty() && newName != currentUser.displayName) {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build()

            currentUser.updateProfile(profileUpdates)
            updates["displayName"] = newName
        }

        // Update Password if provided
        if (newPassword.isNotEmpty()) {
            currentUser.updatePassword(newPassword)
                .addOnCompleteListener { pwTask ->
                    if (!pwTask.isSuccessful) {
                        Toast.makeText(requireContext(), "Password update failed: ${pwTask.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // Handle Profile Picture
        if (profileImageUri != null) {
             val uriString = profileImageUri.toString()
             updates["profileImageUrl"] = uriString
             
             val profileUpdates = UserProfileChangeRequest.Builder()
                .setPhotoUri(profileImageUri)
                .build()
             currentUser.updateProfile(profileUpdates)
        }

        if (updates.isNotEmpty()) {
            db.collection("users").document(currentUser.uid).update(updates)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Profile updated!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to update Firestore", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), "No changes made", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}