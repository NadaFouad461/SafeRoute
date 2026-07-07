package com.example.saferoute.ui.sos

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class SosAlertState(
    val senderName: String = "",
    val relation: String = "",
    val avatarUrl: String = "",
    val distance: Double = 0.0,
    val batteryLevel: Int = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val timeAgo: String = "",
    val phoneNumber: String = ""
)


@HiltViewModel
class SosAlertViewModel @Inject constructor() : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _alertState = MutableStateFlow<SosAlertState?>(null)
    val alertState: StateFlow<SosAlertState?> = _alertState

    fun listenToSosAlert(sosAlertId: String) {
        _alertState.value = null

        if (sosAlertId.isEmpty() || sosAlertId == "test_id") return

        firestore.collection("emergency_logs").document(sosAlertId).get()
            .addOnSuccessListener { logSnapshot ->
                if (logSnapshot == null || !logSnapshot.exists()) return@addOnSuccessListener

                val lat = logSnapshot.getDouble("latitude") ?: 0.0
                val lng = logSnapshot.getDouble("longitude") ?: 0.0
                val battery = logSnapshot.getLong("batteryLevel")?.toInt() ?: 100
                val girlUserId =
                    logSnapshot.getString("userId") ?: logSnapshot.getString("girlUserId") ?: ""

                val alertedContacts =
                    logSnapshot.get("alertedContacts") as? List<*> ?: emptyList<Any>()
                val totalAlerted = alertedContacts.size


                var contactRelation =
                    if (totalAlerted > 0) "$totalAlerted Contacts Notified" else "Direct SOS Alert"

                if (girlUserId.isNotEmpty()) {

                    firestore.collection("users").document(girlUserId).get()
                        .addOnSuccessListener { girlSnapshot ->
                            if (!girlSnapshot.exists()) return@addOnSuccessListener

                            val girlRealName = girlSnapshot.getString("name") ?: "مستخدم SafeRoute"
                            val girlPhone = girlSnapshot.getString("phone") ?: ""


                            if (currentUserId.isNotEmpty() && girlPhone.isNotEmpty()) {
                                firestore.collection("users").document(currentUserId)
                                    .collection("contacts")
                                    .whereEqualTo("phone", girlPhone)
                                    .get()
                                    .addOnSuccessListener { contactSnapshots ->
                                        var finalDisplayName = girlRealName


                                        if (!contactSnapshots.isEmpty) {
                                            val contactDoc = contactSnapshots.documents.first()

                                            finalDisplayName =
                                                contactDoc.getString("name") ?: girlRealName

                                            contactRelation = contactDoc.getString("relation")
                                                ?: contactDoc.getString("relationship")
                                                        ?: contactRelation
                                        }


                                        _alertState.value = SosAlertState(
                                            senderName = finalDisplayName,
                                            relation = contactRelation,
                                            avatarUrl = "",
                                            distance = 0.0,
                                            batteryLevel = battery,
                                            latitude = lat,
                                            longitude = lng,
                                            address = "مصر، الموقع الحالي للبلاغ",
                                            timeAgo = "نشط الآن",
                                            phoneNumber = girlPhone
                                        )
                                    }
                            }
                        }
                }
            }
    }
}