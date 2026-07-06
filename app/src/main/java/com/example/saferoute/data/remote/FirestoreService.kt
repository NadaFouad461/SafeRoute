package com.example.saferoute.data.remote

import com.example.saferoute.data.local.EmergencyLog
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreService @Inject constructor(
    private val db: FirebaseFirestore
) {

    fun getUserData(userId: String, onResult: (Boolean, Map<String, Any>?, String?) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    onResult(true, document.data, null)
                } else {
                    onResult(false, null, "User document does not exist")
                }
            }
            .addOnFailureListener { exception ->
                onResult(false, null, exception.message)
            }
    }


    fun updateUserData(userId: String, name: String, email: String, onResult: (Boolean, String?) -> Unit) {
        val updatedData = hashMapOf<String, Any>(
            "name" to name,
            "email" to email
        )

        db.collection("users").document(userId)
            .update(updatedData)
            .addOnCompleteListener { task ->
                onResult(task.isSuccessful, task.exception?.message)
            }
    }

    fun sendEmergency(log: EmergencyLog) {
        val data = hashMapOf(
            "userId" to log.userId,
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
