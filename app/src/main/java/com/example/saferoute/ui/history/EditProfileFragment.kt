package com.example.saferoute.ui.history

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.saferoute.R
import com.example.saferoute.databinding.FragmentEditProfileBinding
import com.example.saferoute.ui.auth.AuthViewModel

class EditProfileFragment : Fragment(R.layout.fragment_edit_profile) {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!


    private val viewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEditProfileBinding.bind(view)

        val currentUid = viewModel.getCurrentUserId()
        if (currentUid == null) {
            Toast.makeText(context, "Error: User not found", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }


        viewModel.getUserProfile(currentUid) { success, data, errorMessage ->
            if (success && data != null) {
                binding.etEditName.setText(data["name"] as? String)
                binding.etEditEmail.setText(data["email"] as? String)
            } else {
                Toast.makeText(context, errorMessage ?: "Failed to load data", Toast.LENGTH_SHORT).show()
            }
        }


        binding.btnSaveProfile.setOnClickListener {
            val newName = binding.etEditName.text.toString().trim()
            val newEmail = binding.etEditEmail.text.toString().trim()

            if (newName.isEmpty() || newEmail.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            viewModel.updateProfile(newName, newEmail) { success, message ->
                if (success) {
                    if (message != null) {

                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    }
                    findNavController().navigateUp()
                } else {
                    Toast.makeText(context, "Update failed: $message", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}