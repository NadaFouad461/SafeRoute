package com.example.saferoute.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.saferoute.R
import com.example.saferoute.databinding.FragmentHomeBinding
import com.example.saferoute.ui.auth.HomeItem



import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)


        setupRecentActivityRecyclerView()


        fetchUserDataAndGreet()


//        binding.actionContacts.setOnClickListener {
//            findNavController().navigate(R.id.action_homeFragment_to_addContactFragment)
//        }

        binding.actionSafeWalk.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_safeWalkFragment)
        }

        binding.actionFakeCall.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_fakeCallFragment)
        }


//        binding.fabEmergency.setOnClickListener {
//            Toast.makeText(context, "🚨 Instant Emergency Alert Triggered via FAB!", Toast.LENGTH_SHORT).show()
//        }

        binding.sosBtnCard.setOnLongClickListener {
            Toast.makeText(context, "⚡ SOS Background Monitoring Activated!", Toast.LENGTH_LONG).show()
            true
        }

//        binding.bottomNavigationView.setOnItemSelectedListener { item ->
//            when (item.itemId) {
//                R.id.nav_home -> true
//
//                else -> false
//            }
//        }
    }

    private fun setupRecentActivityRecyclerView() {
        val activityLogList = listOf(
            HomeItem("1", "🚨 SOS Alert Triggered (SMS Sent)", "Just now", "sos"),
            HomeItem("2", "📞 Fake Call Utility Executed", "15 mins ago", "fake_call"),
            HomeItem("3", "👟 Safe Walk Navigation Completed", "2 hours ago", "safe_walk"),
            HomeItem("4", "📞 Fake Call Scheduled & Received", "Yesterday", "fake_call"),
            HomeItem("5", "🚨 Fall Detection SOS Auto-Triggered", "3 days ago", "sos")
        )

        binding.recentActivityRv.layoutManager = LinearLayoutManager(context)

        binding.recentActivityRv.adapter = HomeAdapter(activityLogList)
    }

    private fun fetchUserDataAndGreet() {
        val currentUid = auth.currentUser?.uid
        if (currentUid != null) {
            db.collection("users").document(currentUid).get()
                .addOnSuccessListener { documentSnapshot ->

                    if (_binding != null && isAdded && documentSnapshot != null && documentSnapshot.exists()) {
                        val userName = documentSnapshot.getString("name") ?: "User"
                        binding.welcomeTv.text = "Good Evening, $userName"
                    }
                }
                .addOnFailureListener {

                    if (_binding != null && isAdded) {
                        binding.welcomeTv.text = "Good Evening, Sara"
                    }
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}