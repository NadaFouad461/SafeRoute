package com.example.saferoute.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.saferoute.R
import com.example.saferoute.databinding.FragmentForgetBinding
import com.google.firebase.auth.FirebaseAuth

class ForgetFragment : Fragment(R.layout.fragment_forget) {

    private var _binding: FragmentForgetBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentForgetBinding.bind(view)

        binding.resetBtn.setOnClickListener {

            val email =
                binding.emailEt.text.toString().trim()

            if (email.isEmpty()) {

                Toast.makeText(
                    context,
                    "Enter your email",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->

                    if (task.isSuccessful) {

                        Toast.makeText(
                            context,
                            "Reset email sent!",
                            Toast.LENGTH_SHORT
                        ).show()

                        findNavController().navigate(
                            R.id.action_forgetFragment_to_loginFragment
                        )

                    } else {

                        Toast.makeText(
                            context,
                            task.exception?.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        binding.backToLoginTv.setOnClickListener {

            findNavController().navigate(
                R.id.action_forgetFragment_to_loginFragment
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}