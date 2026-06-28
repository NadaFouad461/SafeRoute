package com.saferoute.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.saferoute.databinding.FragmentAddContactBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class AddContactFragment : Fragment() {

    private var _binding: FragmentAddContactBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated( view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // إعداد الـ Spinner الخاص بصلة القرابة كما في التصميم
        val relationships = arrayOf("Family", "Friend", "Partner", "Work")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, relationships)
        binding.spinnerRelationship.adapter = adapter

        // عند الضغط على زر الحفظ
        binding.btnSaveContact.setOnClickListener {
            saveContactToFirestore()
        }
    }

    private fun saveContactToFirestore() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(context, "User not authenticated!", Toast.LENGTH_SHORT).show()
            return
        }

        val name = binding.etContactName.text.toString().trim()
        val phone = binding.etContactPhone.text.toString().trim()
        val relationship = binding.spinnerRelationship.selectedItem.toString()
        val isPriority = binding.switchPriority.isChecked

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // تجهيز بيانات جهة الاتصال
        val contactData = hashMapOf(
            "name" to name,
            "phone" to phone,
            "relationship" to relationship,
            "isPriority" to isPriority,
            "fcmToken" to "" // هيتم تحديثه لاحقاً لما نربط الـ Cloud Functions
        )

        // الحفظ داخل الـ subcollection الخاصة بالمستخدم الحالي
        db.collection("users")
            .document(currentUserId)
            .collection("contacts")
            .document() // Firestore هيعمل Auto-generated ID للـ contact تلقائياً
            .set(contactData)
            .addOnSuccessListener {
                Toast.makeText(context, "Contact added successfully! 🎉", Toast.LENGTH_SHORT).show()
                // هنا ممكن ترجعي للشاشة السابقة (مثلاً الـ Dashboard أو قائمة الجهات)
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error saving contact: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}