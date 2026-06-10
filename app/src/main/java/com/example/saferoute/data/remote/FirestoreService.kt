package com.example.saferoute.data.remote

import com.google.firebase.firestore.FirebaseFirestore

import com.example.saferoute.data.local.EmergencyLog

class FirestoreService {

    private val db = FirebaseFirestore.getInstance()

    fun sendEmergency(log: EmergencyLog) {

        val data = hashMapOf(
            "type" to log.type,
            "latitude" to log.latitude,
            "longitude" to log.longitude,
            "timestamp" to log.timestamp
        )

        db.collection("emergency_logs")
            .add(data)
    }
}