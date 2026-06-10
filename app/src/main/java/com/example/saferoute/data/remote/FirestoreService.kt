package com.example.saferoute.data.remote

import com.example.saferoute.data.local.EmergencyLog
import com.google.firebase.firestore.FirebaseFirestore


class FirestoreService {

    private val db = FirebaseFirestore.getInstance()

    fun sendEmergency(log: EmergencyLog) {

        val data = hashMapOf(
            "type" to log.type,
            "latitude" to log.latitude,
            "longitude" to log.longitude,
            "timestamp" to log.timestamp,
            "status" to log.status
        )

        db.collection("emergency_logs")
            .add(data)
    }
}