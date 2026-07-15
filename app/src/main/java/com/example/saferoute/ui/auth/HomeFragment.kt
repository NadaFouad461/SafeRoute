package com.example.saferoute.ui.auth


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.widget.EditText
import com.example.saferoute.services.SafeWalkService
import com.example.saferoute.R
import com.example.saferoute.data.repository.LocationRepository
import com.example.saferoute.databinding.FragmentHomeBinding
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {
    val currentBatteryLevel: Int
        get() {
            val batteryManager =
                requireContext().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        }

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

        binding.sosBtnCard.setOnClickListener {
            handleSosTrigger()
        }


        binding.actionSafeWalk.setOnClickListener {
            if (SafeWalkService.isWalkActive.value) {
                findNavController().navigate(R.id.action_homeFragment_to_safeWalkFragment)
            } else {
                showSafeWalkDurationDialog()
            }
        }

        binding.actionFakeCall.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_fakeCallFragment)
        }
        binding.actionContacts.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_contactsListFragment)
        }
        binding.actionLiveLocation.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_mapFragment)
        }



        binding.sosBtnCard.setOnLongClickListener {
            handleSosTrigger()
            true
        }

        val isGpsOn = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        binding.gpsStatusTv.text = if (isGpsOn) "GPS: ON" else "GPS: OFF"

        val gpsIcon = if (isGpsOn) {
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_gps_on)
        } else {
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_gps_off)
        }
        binding.gpsStatusTv.setCompoundDrawablesWithIntrinsicBounds(gpsIcon, null, null, null)

        binding.batteryStatusTv.text = "🔋 $currentBatteryLevel%"


    }


    private fun showSafeWalkDurationDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_trip_duration, null)
        val toggleGroup = dialogView.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.durationToggleGroup)
        val customDurationEt = dialogView.findViewById<EditText>(R.id.etManualDuration)
        val btnStart = dialogView.findViewById<View>(R.id.btnStartWalk)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnStart.setOnClickListener {
            val duration = if (customDurationEt.text.isNotEmpty()) {
                customDurationEt.text.toString().toIntOrNull() ?: 20
            } else {
                when (toggleGroup.checkedButtonId) {
                    R.id.btnSmallTrip -> 5
                    R.id.btnMidTrip -> 15
                    R.id.btnLongTrip -> 60
                    else -> 15
                }
            }

            val bundle = Bundle().apply {
                putInt("duration", duration)
            }
            findNavController().navigate(R.id.action_homeFragment_to_safeWalkFragment, bundle)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun handleSosTrigger() {
        val currentUid = auth.currentUser?.uid
        if (currentUid != null) {
            Toast.makeText(
                requireContext(),
                "🚨 Sending Instant Emergency Alert...",
                Toast.LENGTH_SHORT
            ).show()

            val batteryManager =
                requireContext().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val currentBatteryLevel =
                batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

            fetchLocationAndTriggerSOS(currentUid, currentUserName, currentBatteryLevel)
        } else {
            Toast.makeText(context, "User not logged in!", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * بيجيب موقع حقيقي قبل إرسال SOS بدل الإحداثيات الثابتة القديمة.
     * أولوية الاستخدام:
     * 1) آخر موقع محفوظ في LocationRepository (لو الماب اتفتحت قبل كده) - أسرع حل.
     * 2) موقع فعلي طازج من FusedLocationProviderClient.
     * 3) لو مفيش صلاحية أو فشل الجلب، بيتبعت 0.0/0.0 مع تنبيه للمستخدم بدل ما يوهم إنه بعت مكان حقيقي غلط.
     */
    private fun fetchLocationAndTriggerSOS(userId: String, userName: String, batteryLevel: Int) {
        LocationRepository.getLastKnownLatLng()?.let { (lat, lon) ->
            triggerDirectSOS(userId, userName, batteryLevel, lat, lon)
            return
        }

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                requireContext(),
                "⚠️ صلاحية الموقع مش متاحة، هيتبعت بلاغ بدون موقع دقيق!",
                Toast.LENGTH_LONG
            ).show()
            triggerDirectSOS(userId, userName, batteryLevel, 0.0, 0.0)
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (_binding == null || !isAdded) return@addOnSuccessListener
                    if (location != null) {
                        LocationRepository.updateLocation(location.latitude, location.longitude)
                        triggerDirectSOS(
                            userId,
                            userName,
                            batteryLevel,
                            location.latitude,
                            location.longitude
                        )
                    } else {
                        triggerDirectSOS(userId, userName, batteryLevel, 0.0, 0.0)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("HomeFragment", "Failed to fetch location for SOS: ${e.message}")
                    if (_binding != null && isAdded) {
                        triggerDirectSOS(userId, userName, batteryLevel, 0.0, 0.0)
                    }
                }
        } catch (e: SecurityException) {
            Log.e("HomeFragment", "Location permission missing: ${e.message}")
            triggerDirectSOS(userId, userName, batteryLevel, 0.0, 0.0)
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

                        startListeningForIncomingSos(currentUid)
                    }
                }
                .addOnFailureListener {
                    if (_binding != null && isAdded) {
                        binding.welcomeTv.text = "Good Evening, Sara"
                    }
                }
        }
    }


    private fun startListeningForIncomingSos(currentUserId: String) {
        db.collection("emergency_logs")
            .whereEqualTo("status", "triggered")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("HomeFragment", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    for (change in snapshots.documentChanges) {
                        if (change.type == DocumentChange.Type.ADDED) {
                            val doc = change.document
                            val sosAlertId = doc.id
                            val girlUserId = doc.getString("userId") ?: ""

                            if (girlUserId == currentUserId) continue

                            showEmergencyDialog(sosAlertId)
                        }
                    }
                }
            }
    }

    private fun showEmergencyDialog(sosAlertId: String) {
        if (_binding == null || !isAdded) return


        val sharedPrefs =
            requireContext().getSharedPreferences("saferoute_prefs", Context.MODE_PRIVATE)
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


    private fun triggerDirectSOS(
        userId: String,
        userName: String,
        batteryLevel: Int,
        latitude: Double,
        longitude: Double
    ) {
        val emergencyData = hashMapOf(
            "userId" to userId,
            "userName" to userName,
            "status" to "triggered",
            "batteryLevel" to batteryLevel,
            "latitude" to latitude,
            "longitude" to longitude,
            "timestamp" to com.google.firebase.Timestamp.now(),
            "alertedContacts" to listOf<String>()
        )

        db.collection("emergency_logs").add(emergencyData)
            .addOnSuccessListener { documentReference ->
                if (_binding != null && isAdded) {
                    Toast.makeText(requireContext(), "🚨 SOS Saved to Database!", Toast.LENGTH_SHORT)
                        .show()

                    val bundle = Bundle().apply {
                        putString("sosAlertId", documentReference.id)
                        putInt("batteryLevel", batteryLevel)
                    }

                    try {
                        findNavController().navigate(
                            R.id.action_homeFragment_to_sosFragment,
                            bundle
                        )
                    } catch (e: Exception) {
                        try {
                            findNavController().navigate(R.id.sosFragment, bundle)
                        } catch (navError: Exception) {
                            Toast.makeText(
                                requireContext(),
                                "Nav Error: ${navError.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                if (_binding != null && isAdded) {
                    Toast.makeText(
                        requireContext(),
                        "Failed to trigger SOS: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
