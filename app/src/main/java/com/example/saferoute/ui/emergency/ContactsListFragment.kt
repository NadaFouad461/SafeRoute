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

class ContactsListFragment : Fragment(R.layout.fragment_contacts_list) {

    private var _binding: FragmentContactsListBinding? = null
    private val binding get() = _binding!!


    private val emergencyContactsList = mutableListOf<ContactItem>()
    private lateinit var contactsAdapter: ContactsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentContactsListBinding.bind(view)


        setupRecyclerView()


        updateContactsCount()


        binding.fabAddContact.setOnClickListener {

            findNavController().navigate(R.id.action_contactsListFragment_to_addContactFragment)
        }


        binding.filterBtn.setOnClickListener {
            Toast.makeText(context, "Filtering contacts...", Toast.LENGTH_SHORT).show()
        }


        setupBottomNavigation()
    }

    private fun setupRecyclerView() {
        contactsAdapter = ContactsAdapter(emergencyContactsList)
        binding.emergencyContactsRv.layoutManager = LinearLayoutManager(context)
        binding.emergencyContactsRv.adapter = contactsAdapter
    }

    private fun updateContactsCount() {
        val count = emergencyContactsList.size
        binding.contactsCountTv.text = "$count Contacts active"
    }

    private fun setupBottomNavigation() {

        binding.bottomNavigationView.selectedItemId = R.id.nav_contacts

         fun setupBottomNavigation() {

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

                    R.id.nav_contacts -> {
                        true
                    }

                    R.id.nav_history -> {
                        Toast.makeText(
                            context,
                            "Opening History...",
                            Toast.LENGTH_SHORT
                        ).show()
                        true
                    }

                    R.id.nav_profile -> {
                        Toast.makeText(
                            context,
                            "Opening Profile...",
                            Toast.LENGTH_SHORT
                        ).show()
                        true
                    }

                    else -> false
                }
            }
        }


    fun addNewContact(contact: ContactItem) {
        emergencyContactsList.add(contact)
        contactsAdapter.notifyItemInserted(emergencyContactsList.size - 1)
        updateContactsCount()
    }


}
}