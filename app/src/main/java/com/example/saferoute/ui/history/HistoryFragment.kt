package com.example.saferoute.ui.history

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.saferoute.R
import com.example.saferoute.databinding.FragmentHistoryBinding
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.log

class HistoryFragment : Fragment(R.layout.fragment_history) {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HistoryViewModel
    private val logAdapter = LogAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHistoryBinding.bind(view)

        viewModel = ViewModelProvider(this)[HistoryViewModel::class.java]

        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = logAdapter
        }


        logAdapter.setOnItemClickListener { log ->
            val typeParts = log.type.split("|")
            val firestoreDocId = typeParts.getOrNull(1)

            if (!firestoreDocId.isNullOrEmpty()) {
                val bundle = Bundle().apply {
                    putString("SOS_ALERT_ID", firestoreDocId)
                }
                findNavController().navigate(R.id.emergencyNotificationFragment, bundle)
            } else {
                Toast.makeText(requireContext(), "لا يوجد معرف لهذا البلاغ", Toast.LENGTH_SHORT).show()
            }
        }


        logAdapter.setOnSafeClickListener { log ->
            val typeParts = log.type.split("|")
            val firestoreDocId = typeParts.getOrNull(1)

            if (!firestoreDocId.isNullOrEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setTitle("تأكيد حالة الأمان")
                    .setMessage("هل أنتِ بخير وتريدين إنهاء حالة الاستغاثة الحالية؟")
                    .setPositiveButton("نعم") { _, _ ->
                        FirebaseFirestore.getInstance().collection("emergency_logs")
                            .document(firestoreDocId)
                            .update("status", "Resolved")
                            .addOnSuccessListener {
                                context?.let { ctx ->
                                    Toast.makeText(ctx, "تم إغلاق البلاغ بنجاح 🎉", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener {
                                context?.let { ctx ->
                                    Toast.makeText(ctx, "فشل التحديث", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                    .setNegativeButton("إلغاء", null)
                    .show()
            } else {
                Toast.makeText(requireContext(), "خطأ: لا يمكن العثور على معرف البلاغ", Toast.LENGTH_SHORT).show()
            }
        }


        viewModel.logs.observe(viewLifecycleOwner) { logsList ->
            if (logsList != null) {
                val uniqueLogsMap = LinkedHashMap<String, com.example.saferoute.data.local.EmergencyLog>()

                logsList.forEach { log ->
                    if (!log.userId.isNullOrEmpty()) {

                        val timeKey = log.timestamp / 60000
                        val uniqueKey = "${log.userId}_$timeKey"

                        if (!uniqueLogsMap.containsKey(uniqueKey)) {
                            uniqueLogsMap[uniqueKey] = log
                        } else {

                            if (log.status.contains("Resolved")) {
                                uniqueLogsMap[uniqueKey] = log
                            }
                        }
                    }
                }


                val isFromSomeoneElse = arguments?.getBoolean("IS_FROM_SOMEONE_ELSE", false) ?: false
                val incomingSosId = arguments?.getString("INCOMING_SOS_ID") ?: ""
                val senderName = arguments?.getString("SENDER_NAME") ?: "ابنتكِ"

                if (isFromSomeoneElse && incomingSosId.isNotEmpty()) {
                    val timeKey = System.currentTimeMillis() / 60000
                    val externalUniqueKey = "${incomingSosId}_$timeKey"
                    val existingLog = uniqueLogsMap[externalUniqueKey]

                    if (existingLog == null || !existingLog.status.contains("Resolved", ignoreCase = true)) {
                        val externalLog = com.example.saferoute.data.local.EmergencyLog(
                            id = incomingSosId.hashCode(),
                            userId = incomingSosId,
                            type = "SOS|$incomingSosId",
                            latitude = arguments?.getDouble("LAT", 0.0) ?: 0.0,
                            longitude = arguments?.getDouble("LNG", 0.0) ?: 0.0,
                            timestamp = System.currentTimeMillis(),
                            status = "Incoming_SOS|$senderName",
                            batteryLevel = 100
                        )
                        uniqueLogsMap[externalUniqueKey] = externalLog
                    }
                }


                val finalFilteredList = uniqueLogsMap.values.toList().sortedByDescending { it.timestamp }

                logAdapter.submitList(finalFilteredList)
                binding.tvTotalEventsCount.text = finalFilteredList.size.toString()
                calculateSafeDays(finalFilteredList)
            }
        }

        viewModel.listenToEmergencyLogs()
        setupBottomNavigation()
    }

    private fun calculateSafeDays(logsList: List<com.example.saferoute.data.local.EmergencyLog>) {
        if (logsList.isEmpty()) {
            binding.tvSafeDaysCount.text = "30"
            return
        }
        val lastLogTimestamp = logsList.first().timestamp
        val currentTimestamp = System.currentTimeMillis()
        val diffInMs = currentTimestamp - lastLogTimestamp
        val diffInDays = (diffInMs / (1000 * 60 * 60 * 24)).toInt()
        val safeDays = if (diffInDays > 30) 30 else diffInDays
        binding.tvSafeDaysCount.text = safeDays.toString()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationHistory.selectedItemId = R.id.nav_history
        binding.bottomNavigationHistory.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.nav_history) return@setOnItemSelectedListener true
            try {
                when (item.itemId) {
                    R.id.nav_home -> { findNavController().navigate(R.id.homeFragment); true }
                    R.id.nav_map -> { findNavController().navigate(R.id.mapFragment); true }
                    R.id.nav_contacts -> { findNavController().navigate(R.id.contactsListFragment); true }
                    R.id.nav_profile -> { findNavController().navigate(R.id.profileFragment2); true }
                    else -> false
                }
            } catch (e: Exception) {
                context?.let { ctx ->
                    Toast.makeText(ctx, "مسار التنقل غير مدعوم حالياً", Toast.LENGTH_SHORT).show()
                }
                false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}