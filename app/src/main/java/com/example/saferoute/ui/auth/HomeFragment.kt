package com.example.saferoute.ui.auth

import android.content.Context
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.saferoute.R
import com.example.saferoute.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var currentUserName: String = "SafeRoute User"
    private var currentUserPhone: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)


        setupRecentActivityRecyclerView()


        fetchUserDataAndGreet()


        binding.actionContacts.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_contactsListFragment)
        }
        binding.actionLiveLocation.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_mapFragment)
        }


        binding.fabEmergency.setOnClickListener {
            handleSosTrigger()
        }


        binding.sosBtnCard.setOnClickListener {
            handleSosTrigger()
        }


        binding.sosBtnCard.setOnLongClickListener {
            handleSosTrigger()
            true
        }


        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true

                R.id.nav_map -> {
                    findNavController().navigate(R.id.action_homeFragment_to_mapFragment)
                    true
                }
                R.id.nav_contacts -> {
                    findNavController().navigate(R.id.action_homeFragment_to_contactsListFragment)
                    true
                }

                R.id.nav_history -> {
                    findNavController().navigate(R.id.action_homeFragment_to_historyFragment)
                    true
                }

                R.id.nav_profile -> {
                    findNavController().navigate(R.id.action_homeFragment_to_profileFragment2)
                    true
                }

                else -> false
            }
        }
    }


    private fun handleSosTrigger() {
        val currentUid = auth.currentUser?.uid
        if (currentUid != null) {
            findNavController().navigate(R.id.action_homeFragment_to_sosFragment)
        } else {
            Toast.makeText(context, "User not logged in!", Toast.LENGTH_SHORT).show()
        }
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
                        currentUserName = documentSnapshot.getString("name") ?: "User"
                        currentUserPhone = documentSnapshot.getString("phone") ?: ""
                        binding.welcomeTv.text = "Good Evening, $currentUserName"

                    }
                }
                .addOnFailureListener {
                    if (_binding != null && isAdded) {
                        binding.welcomeTv.text = "Good Evening, Sara"
                    }
                }
        }
    }




    private fun showEmergencyDialog(sosAlertId: String) {
        if (_binding == null || !isAdded) return


        val sharedPrefs = requireContext().getSharedPreferences("saferoute_prefs", Context.MODE_PRIVATE)
        val isDismissedBefore = sharedPrefs.getBoolean("dismissed_$sosAlertId", false)
        if (isDismissedBefore) return

        AlertDialog.Builder(requireContext())
            .setTitle("🚨 بلاغ استغاثة طارئ SOS!")
            .setMessage("هناك خطر يواجه أحد جهات اتصالك المقربة الآن! اضغطي للانتقال للسجل ومتابعة الحالة.")
            .setCancelable(false)
            .setPositiveButton("الانتقال للسجل (History)") { _, _ ->
                val bundle = Bundle().apply {
                    putString("incomingSosId", sosAlertId)
                    putBoolean("isFromSomeoneElse", true)
                }

                val navController = findNavController()
                if (navController.currentDestination?.id == R.id.homeFragment) {
                    navController.navigate(R.id.action_homeFragment_to_historyFragment, bundle)
                }
            }
            .setNegativeButton("إغلاق") { dialog, _ ->

                sharedPrefs.edit().putBoolean("dismissed_$sosAlertId", true).apply()
                dialog.dismiss()
            }
            .show()
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}