package com.example.saferoute.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.saferoute.R
import com.example.saferoute.data.remote.FirebaseService
import com.example.saferoute.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseService.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        val user = auth.currentUser

        binding.logoutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            findNavController().navigate(R.id.action_homeFragment_to_loginFragment)
        }

        if (user != null) {
            binding.welcomeTv.text = "Welcome ${user.email}"

            // 1. جلب بيانات المستخدم الأساسية وعرضها
            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name")
                        val phone = document.getString("phone")

                        binding.welcomeTv.text = "Welcome $name"
                        binding.phoneTv.text = "Phone: $phone"
                    }

                    // 2. جلب جهات الاتصال ديناميكياً وعرض عددها النشط بناءً على التصميم الاحترافي
                    db.collection("users").document(user.uid)
                        .collection("contacts")
                        .get()
                        .addOnSuccessListener { contactsSnapshot ->
                            val contactsCount = contactsSnapshot.size()
                            // هنا بتعرضي عدد جهات الاتصال النشطة في الـ TextView الخاص بـ Active Guardians
                            binding.dadTv.text = "Active Emergency Contacts: $contactsCount"
                            // مسحنا binding.momTv لأن العرض بقا مركزي وتجميعي لكل الـ Contacts
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}