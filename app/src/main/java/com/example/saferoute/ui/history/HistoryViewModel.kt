package com.example.saferoute.ui.history

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.saferoute.data.local.EmergencyLog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val currentUserId = auth.currentUser?.uid ?: ""

    private val _logs = MutableLiveData<List<EmergencyLog>>()
    val logs: LiveData<List<EmergencyLog>> get() = _logs

    fun resolveEmergency(alertId: String) {
        db.collection("emergency_logs").document(alertId)
            .update("status", "Resolved")
            .addOnFailureListener {
                Log.e("HistoryViewModel", "فشل تحديث الحالة: ${it.message}")
            }
    }

    fun listenToEmergencyLogs(incomingSosId: String? = null) {
        if (currentUserId.isEmpty()) return

        db.collection("emergency_logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener

                val logsList = mutableListOf<EmergencyLog>()

                for (doc in snapshots) {
                    val userIdInDoc = doc.getString("userId") ?: ""
                    val status = doc.getString("status") ?: "Emergency Dispatched"

                    if (status.equals("canceled", ignoreCase = true)) {
                        continue
                    }

                    val sharedWith = doc.get("sharedWith") as? List<*> ?: emptyList<Any>()
                    val isFromMe = userIdInDoc == currentUserId
                    val isForMe = sharedWith.contains(currentUserId) || doc.id == incomingSosId

                    if (isFromMe || isForMe) {
                        val realBattery = doc.getLong("batteryLevel")?.toInt() ?: 100
                        val userName = doc.getString("userName") ?: "شخص مقرب"
                        val logType = if (isFromMe) "SOS" else "SOS_INCOMING"

                        val customStatus = when {
                            status.contains("Resolved", ignoreCase = true) || status == "safe" -> {
                                if (isFromMe) "Resolved" else "✅ $userName Is Safe Now"
                            }
                            status.contains("Dispatched", ignoreCase = true) || status == "triggered" -> {
                                if (isFromMe) "Emergency Dispatched" else "⚠️ $userName Needs Help!"
                            }
                            else -> status
                        }

                        val finalTimestamp: Long = when (val rawTime = doc.get("timestamp")) {
                            is Timestamp -> rawTime.toDate().time
                            is Long -> rawTime
                            else -> System.currentTimeMillis()
                        }

                        val log = EmergencyLog(
                            id = doc.id.hashCode(),
                            userId = userIdInDoc,
                            type = "$logType|${doc.id}",
                            latitude = doc.getDouble("latitude") ?: 0.0,
                            longitude = doc.getDouble("longitude") ?: 0.0,
                            timestamp = finalTimestamp,
                            status = "$customStatus|${sharedWith.size.coerceAtLeast(1)}",
                            batteryLevel = realBattery
                        )

                        logsList.add(log)
                    }
                }
                _logs.postValue(logsList)
            }
    }
}