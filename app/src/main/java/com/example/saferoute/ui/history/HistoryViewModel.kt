package com.example.saferoute.ui.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.saferoute.data.local.EmergencyLog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HistoryViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _logs = MutableLiveData<List<EmergencyLog>>()
    val logs: LiveData<List<EmergencyLog>> get() = _logs


    fun listenToEmergencyLogs() {
        if (currentUserId.isEmpty()) return

        db.collection("emergency_logs")
            .whereEqualTo("userId", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) {
                    return@addSnapshotListener
                }

                val logsList = mutableListOf<EmergencyLog>()
                for (doc in snapshots) {

                    val alertedContacts = doc.get("alertedContacts") as? List<String> ?: emptyList()


                    val log = EmergencyLog(
                        id = doc.id.hashCode(),
                        userId = doc.getString("userId") ?: "",
                        type = doc.getString("type") ?: "SOS",
                        latitude = doc.getDouble("latitude") ?: 0.0,
                        longitude = doc.getDouble("longitude") ?: 0.0,
                        timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis(),
                        status = doc.getString("status") ?: "Dispatched",

                        batteryLevel = alertedContacts.size
                    )
                    logsList.add(log)
                }
                _logs.postValue(logsList)
            }
    }
}