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
                    val sharedWith = doc.get("sharedWith") as? List<*> ?: emptyList<Any>()

                    // 🎯 حماية صارمة: تخطي البلاغات الملغية أو البلاغات الأولية التي لم تكتمل بعد
                    if (status.equals("canceled", ignoreCase = true) || status.equals("triggered", ignoreCase = true)) {
                        continue
                    }

                    val isFromMe = userIdInDoc == currentUserId
                    val isForMe = sharedWith.contains(currentUserId) || doc.id == incomingSosId || (!isFromMe && status != "canceled")

                    if (isFromMe || isForMe) {
                        val alertedContacts = doc.get("alertedContacts") as? List<*> ?: emptyList<Any>()
                        val realBattery = doc.getLong("batteryLevel")?.toInt() ?: 100
                        val userName = doc.getString("userName") ?: "شخص مقرب"
                        val logType = if (isFromMe) "SOS" else "SOS_INCOMING"

                        // في HistoryViewModel.kt
                        val customStatus = when {
                            status.contains("Resolved", ignoreCase = true) || status == "safe" -> {
                                if (isFromMe) "Resolved" else "✅ $userName Is Safe Now"
                            }
                            // اجعلي الـ triggered ظاهرة في الهيستوري، لأنها استغاثة حقيقية بدأت بالفعل
                            status.contains("Dispatched", ignoreCase = true) || status == "triggered" -> {
                                if (isFromMe) "Emergency Dispatched" else "⚠️ $userName Needs Help!"
                            }
                            else -> status
                        }

                        val finalTimestamp: Long = when (val rawTime = doc.get("timestamp")) {
                            is com.google.firebase.Timestamp -> rawTime.toDate().time
                            is Long -> rawTime
                            else -> System.currentTimeMillis()
                        }

                        // عدلي دالة الـ for loop داخل listenToEmergencyLogs
                        val log = EmergencyLog(
                            id = doc.id.hashCode(),
                            userId = doc.id,
                            // التعديل هنا: ندمج المعرف الحقيقي في الـ type ليتم استخراجه لاحقاً
                            type = "$logType|${doc.id}",
                            latitude = doc.getDouble("latitude") ?: 0.0,
                            longitude = doc.getDouble("longitude") ?: 0.0,
                            timestamp = finalTimestamp,
                            status = "$customStatus|${alertedContacts.size.coerceAtLeast(1)}",
                            batteryLevel = realBattery
                        )

                        logsList.add(log)
                    }
                }
                _logs.postValue(logsList)
            }
    }
}