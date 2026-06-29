package com.example.saferoute.ui.history

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.saferoute.R
import com.example.saferoute.data.local.AppDatabase
import com.example.saferoute.data.remote.FirestoreService
import com.example.saferoute.data.repository.EmergencyRepository
import com.example.saferoute.databinding.FragmentHistoryBinding
import com.example.saferoute.ui.emergency.EmergencyViewModel
import com.example.saferoute.ui.emergency.EmergencyViewModelFactory

class HistoryFragment : Fragment(R.layout.fragment_history) {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: EmergencyViewModel
    private val logAdapter = LogAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHistoryBinding.bind(view)


        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = logAdapter
        }


        val database = AppDatabase.getDatabase(requireContext().applicationContext)
        val emergencyDao = database.emergencyDao()
        val repository = EmergencyRepository(emergencyDao, FirestoreService())
        val factory = EmergencyViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[EmergencyViewModel::class.java]


        viewModel.logs.observe(viewLifecycleOwner) { logsList ->
            if (logsList != null) {
                logAdapter.submitList(logsList)
                binding.tvTotalEventsCount.text = logsList.size.toString()
            }
        }


        binding.bottomNavigationHistory.selectedItemId = R.id.historyFragment


        binding.bottomNavigationHistory.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    findNavController().navigate(R.id.homeFragment)
                    true
                }
                R.id.mapFragment -> {
                    findNavController().navigate(R.id.mapFragment)
                    true
                }
                R.id.profileFragment2 -> {
                    findNavController().navigate(R.id.profileFragment2)
                    true
                }
                R.id.historyFragment -> true
                else -> false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}