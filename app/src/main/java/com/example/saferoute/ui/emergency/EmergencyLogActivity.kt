package com.example.saferoute.ui.emergency

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.saferoute.data.local.EmergencyLog
import com.example.saferoute.data.local.AppDatabase
import com.example.saferoute.data.remote.FirestoreService
import com.example.saferoute.data.repository.EmergencyRepository
import com.example.saferoute.databinding.ActivityEmergencyLogBinding

class EmergencyLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmergencyLogBinding
    private lateinit var adapter: EmergencyAdapter
    private lateinit var viewModel: EmergencyViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmergencyLogBinding.inflate(layoutInflater)
        setContentView(binding.root)


        adapter = EmergencyAdapter(emptyList())
        binding.recyclerLogs.layoutManager = LinearLayoutManager(this)
        binding.recyclerLogs.adapter = adapter

        val db = AppDatabase.getDatabase(this)
        val firestore = FirestoreService()

        val repository = EmergencyRepository(
            db.emergencyDao(),
            firestore
        )


        viewModel = ViewModelProvider(
            this,
            EmergencyViewModelFactory(repository)
        )[EmergencyViewModel::class.java]


        viewModel.logs.observe(this) { list ->
            adapter.updateData(list)
        }


        binding.btnTest.setOnClickListener {
            viewModel.saveLog(
                EmergencyLog(
                    type = "SOS",
                    latitude = 31.2001,
                    longitude = 29.9187,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
}