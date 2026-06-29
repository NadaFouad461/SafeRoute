package com.example.saferoute.ui.contact

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.saferoute.R
import com.example.saferoute.databinding.FragmentAddContactBinding
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddContactFragment : Fragment(R.layout.fragment_add_contact) {

    private var _binding: FragmentAddContactBinding? = null
    private val binding get() = _binding!!


    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddContactBinding.bind(view)


        binding.backBtn.setOnClickListener {
            findNavController().navigateUp()
        }


        binding.addContactBtn.setOnClickListener {
            saveEmergencyContactToFirestore()
        }
    }

    private fun saveEmergencyContactToFirestore() {

        val fullName = binding.contactNameEt.text.toString().trim()
        val phoneNumber = binding.contactPhoneEt.text.toString().trim()
        val isPriority = binding.prioritySwitch.isChecked

        val selectedChipId = binding.relationshipChipGroup.checkedChipId
        val relationship = if (selectedChipId != View.NO_ID) {
            view?.findViewById<Chip>(selectedChipId)?.text.toString()
        } else {
            "Family"
        }


        if (fullName.isEmpty()) {
            binding.contactNameEt.error = "Full name is required"
            binding.contactNameEt.requestFocus()
            return
        }

        if (phoneNumber.isEmpty()) {
            binding.contactPhoneEt.error = "Phone number is required"
            binding.contactPhoneEt.requestFocus()
            return
        }


        val currentUserUid = auth.currentUser?.uid
        if (currentUserUid == null) {
            Toast.makeText(context, "User session expired. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }


        binding.addContactBtn.isEnabled = false


        val contactMap = hashMapOf(
            "name" to fullName,
            "phone" to phoneNumber,
            "relationship" to relationship,
            "isPriority" to isPriority,
            "createdAt" to com.google.firebase.Timestamp.now()
        )


        db.collection("users")
            .document(currentUserUid)
            .collection("contacts")
            .add(contactMap)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(context, "Contact saved successfully! 🎉", Toast.LENGTH_LONG).show()


                binding.addContactBtn.isEnabled = true
                clearFields()


                findNavController().navigateUp()
            }
            .addOnFailureListener { exception ->
                binding.addContactBtn.isEnabled = true
                Toast.makeText(context, "Failed to save contact: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun clearFields() {
        binding.contactNameEt.text?.clear()
        binding.contactPhoneEt.text?.clear()
        binding.prioritySwitch.isChecked = false
        binding.relationshipChipGroup.clearCheck()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}