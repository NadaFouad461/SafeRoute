package com.example.saferoute.ui.history

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.saferoute.R
import com.example.saferoute.data.local.EmergencyLog
import com.example.saferoute.databinding.FragmentHistoryBinding
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HistoryFragment : Fragment(R.layout.fragment_history) {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels()
    private val logAdapter = LogAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHistoryBinding.bind(view)

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
                Toast.makeText(requireContext(), "لا يوجد معرف لهذا البلاغ", Toast.LENGTH_SHORT)
                    .show()
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
                        viewModel.resolveEmergency(firestoreDocId)
                                    Toast.makeText(
                                        requireContext(),
                                        "تم إغلاق البلاغ بنجاح 🎉",
                                        Toast.LENGTH_SHORT
                                    ).show()

                    }
                    .setNegativeButton("إلغاء", null)
                    .show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "خطأ: لا يمكن العثور على معرف البلاغ",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        viewModel.logs.observe(viewLifecycleOwner) { logsList ->
            if (logsList != null) {

                val uniqueLogsMap =
                    LinkedHashMap<String, EmergencyLog>()

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





                val finalFilteredList =
                    uniqueLogsMap.values.toList().sortedByDescending { it.timestamp }

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
                    R.id.nav_home -> {
                        findNavController().navigate(R.id.homeFragment); true
                    }

                    R.id.nav_map -> {
                        findNavController().navigate(R.id.mapFragment); true
                    }

                    R.id.nav_contacts -> {
                        findNavController().navigate(R.id.contactsListFragment); true
                    }

                    R.id.nav_profile -> {
                        findNavController().navigate(R.id.profileFragment2); true
                    }

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
