package com.example.saferoute.ui.contacts

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.saferoute.R
import com.example.saferoute.databinding.FragmentContactsListBinding
import com.example.saferoute.models.ContactItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ContactsListFragment : Fragment(R.layout.fragment_contacts_list) {

    private var _binding: FragmentContactsListBinding? = null
    private val binding get() = _binding!!

    private val emergencyContactsList = mutableListOf<ContactItem>()
    private lateinit var contactsAdapter: ContactsAdapter

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentContactsListBinding.bind(view)

        setupRecyclerView()
        loadContacts()

        binding.fabAddContact.setOnClickListener {
            findNavController().navigate(R.id.action_contactsListFragment_to_addContactFragment)
        }

        binding.filterBtn.setOnClickListener {
            Toast.makeText(context, "Filtering contacts...", Toast.LENGTH_SHORT).show()
        }

        setupBottomNavigation()
    }

    private fun setupRecyclerView() {

        contactsAdapter = ContactsAdapter(
            emergencyContactsList,
            onEditClick = { contact ->

                val bundle = Bundle().apply {
                    putString("contactId", contact.id)
                    putString("contactName", contact.name)
                    putString("contactPhone", contact.phone)
                    putString("contactRelation", contact.relationship)
                    putBoolean("isPriority", contact.isPriority)
                }

                findNavController().navigate(R.id.action_contactsListFragment_to_addContactFragment, bundle)
            },
            onDeleteClick = { contact ->

                deleteContactFromFirestore(contact)
            }
        )

        binding.emergencyContactsRv.layoutManager = LinearLayoutManager(context)
        binding.emergencyContactsRv.adapter = contactsAdapter
    }

    private fun loadContacts() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .collection("contacts")
            .get()
            .addOnSuccessListener { result ->
                emergencyContactsList.clear()
                for (doc in result) {
                    val contact = ContactItem(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        phone = doc.getString("phone") ?: "",
                        relationship = doc.getString("relationship") ?: "",
                        isPriority = doc.getBoolean("isPriority") ?: false,
                        fcmToken = doc.getString("fcmToken") ?: ""
                    )
                    emergencyContactsList.add(contact)
                }
                contactsAdapter.notifyDataSetChanged()
                updateContactsCount()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load contacts", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteContactFromFirestore(contact: ContactItem) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .collection("contacts")
            .document(contact.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "${contact.name} deleted successfully", Toast.LENGTH_SHORT).show()
                emergencyContactsList.remove(contact)
                contactsAdapter.notifyDataSetChanged()
                updateContactsCount()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error deleting contact: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateContactsCount() {
        val count = emergencyContactsList.size
        binding.contactsCountTv.text = "$count Contacts active"
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.selectedItemId = R.id.nav_contacts

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    findNavController().navigate(R.id.homeFragment)
                    true
                }
                R.id.nav_map -> {
                    findNavController().navigate(R.id.mapFragment)
                    true
                }
                R.id.nav_contacts -> true
                R.id.nav_history -> {
                    findNavController().navigate(R.id.historyFragment)
                    true
                }
                R.id.nav_profile -> {
                    findNavController().navigate(R.id.profileFragment2)
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}