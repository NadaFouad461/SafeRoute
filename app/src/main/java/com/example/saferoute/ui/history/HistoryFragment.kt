package com.example.saferoute.ui.history

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.saferoute.R
import com.example.saferoute.databinding.FragmentHistoryBinding

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


        viewModel.logs.observe(viewLifecycleOwner) { logsList ->
            if (logsList != null) {
                logAdapter.submitList(logsList)
                binding.tvTotalEventsCount.text = logsList.size.toString()
                calculateSafeDays(logsList)
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

            if (item.itemId == R.id.nav_history) {
                return@setOnItemSelectedListener true
            }

            try {

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
                        findNavController().navigate(R.id.contactsListFragment)
                        true
                    }
                    R.id.nav_profile -> {
                        findNavController().navigate(R.id.profileFragment2)
                        true
                    }
                    else -> false
                }
            } catch (e: Exception) {
                Toast.makeText(context, "مسار التنقل غير مدعوم حالياً", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}