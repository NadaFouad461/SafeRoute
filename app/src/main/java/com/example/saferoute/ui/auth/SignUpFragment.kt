package com.example.saferoute.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.saferoute.R
import com.example.saferoute.databinding.FragmentSignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpFragment : Fragment(R.layout.fragment_sign_up) {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSignUpBinding.bind(view)

        binding.goToLoginTv.setOnClickListener {
            findNavController().navigate(R.id.action_signUpFragment_to_loginFragment)
        }

        binding.signupBtn.setOnClickListener {
            val name = binding.nameEt.text.toString().trim()
            val phone = binding.phoneEt.text.toString().trim()
            val email = binding.emailEt.text.toString().trim()
            val pass = binding.passwordEt.text.toString().trim()
            val confirmPass = binding.confirmPasswordEt.text.toString().trim()


            if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Invalid email format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass.length < 6) {
                Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != confirmPass) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.signupBtn.isEnabled = false

            auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser!!.uid

                        FirebaseMessaging.getInstance().token
                            .addOnSuccessListener { token ->
                                val userData = hashMapOf(
                                    "name" to name,
                                    "email" to email,
                                    "phone" to phone,
                                    "fcmToken" to token

                                )

                                db.collection("users")
                                    .document(uid)
                                    .set(userData)
                                    .addOnSuccessListener {
                                        binding.signupBtn.isEnabled = true
                                        context?.let { ctx ->
                                            Toast.makeText(ctx, "Account created! 🎉", Toast.LENGTH_SHORT).show()
                                        }
                                        findNavController().navigate(R.id.action_signUpFragment_to_loginFragment)
                                    }
                                    .addOnFailureListener { e ->
                                        binding.signupBtn.isEnabled = true
                                        context?.let { ctx ->
                                            Toast.makeText(ctx, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }
                    } else {
                        binding.signupBtn.isEnabled = true
                        context?.let { ctx ->
                            Toast.makeText(ctx, task.exception?.message, Toast.LENGTH_SHORT).show()
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