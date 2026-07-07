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
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class EditProfileFragment : Fragment(R.layout.fragment_edit_profile) {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!


    private val viewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEditProfileBinding.bind(view)

        val currentUid = viewModel.getCurrentUserId()
        if (currentUid == null) {
            Toast.makeText(requireContext(), "Error: User not found", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }


        viewModel.getUserProfile(currentUid) { success, data, errorMessage ->
            if (success && data != null) {
                binding.etEditName.setText(data["name"] as? String)
                binding.etEditEmail.setText(data["email"] as? String)
            } else {
                context?.let { ctx ->
                    Toast.makeText(ctx, errorMessage ?: "Failed to load data", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }


        binding.btnSaveProfile.setOnClickListener {
            val newName = binding.etEditName.text.toString().trim()
            val newEmail = binding.etEditEmail.text.toString().trim()

            if (newName.isEmpty() || newEmail.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }


            viewModel.updateProfile(newName, newEmail) { success, message ->
                if (success) {
                    if (message != null) {
                        context?.let { ctx ->
                            Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
                        }
                    } else {
                        context?.let { ctx ->
                            Toast.makeText(ctx, "Profile updated successfully!", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    findNavController().navigateUp()
                } else {
                    context?.let { ctx ->
                        Toast.makeText(ctx, "Update failed: $message", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}