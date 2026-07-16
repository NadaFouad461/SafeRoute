package com.example.saferoute.data.remote

import android.util.Log
import com.example.saferoute.models.ContactItem
import com.example.saferoute.ui.auth.HomeItem
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query

@Singleton
class FirestoreService @Inject constructor(
    private val db: FirebaseFirestore
) {

    fun getUserData(
        userId: String,
        onResult: (Boolean, Map<String, Any>?, String?) -> Unit
    ) {

        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {
                    onResult(true, document.data, null)
                } else {
                    onResult(false, null, "User not found")
                }

            }
            .addOnFailureListener {

                onResult(false, null, it.message)

            }
    }

    fun updateUserData(
        userId: String,
        name: String,
        email: String,
        onResult: (Boolean, String?) -> Unit
    ) {

        val updatedData = hashMapOf<String, Any>(
            "name" to name,
            "email" to email
        )

        db.collection("users")
            .document(userId)
            .update(updatedData)
            .addOnCompleteListener {

                onResult(
                    it.isSuccessful,
                    it.exception?.message
                )

            }

    }

    fun loadEmergencyContacts(

        currentUserId: String,
        onSuccess: (MutableList<ContactItem>) -> Unit,
        onFailure: (String) -> Unit

    ) {

        val contacts = mutableListOf<ContactItem>()

        db.collection("users")
            .document(currentUserId)
            .collection("contacts")
            .get()
            .addOnSuccessListener { documents ->

                if (documents.isEmpty) {
                    onSuccess(contacts)
                    return@addOnSuccessListener
                }

                var finished = 0

                documents.forEach { doc ->

                    val phone = doc.getString("phone") ?: ""
                    val name = doc.getString("name") ?: ""
                    val relationship = doc.getString("relationship") ?: ""
                    val priority = doc.getBoolean("isPriority") ?: false

                    db.collection("users")
                        .whereEqualTo("phone", phone)
                        .get()
                        .addOnSuccessListener { result ->

                            var token = ""
                            var uid = ""

                            if (!result.isEmpty) {
                                token = result.documents.first().getString("fcmToken") ?: ""
                                uid = result.documents.first().id
                            }

                            contacts.add(
                                ContactItem(
                                    id = doc.id,
                                    name = name,
                                    phone = phone,
                                    relationship = relationship,
                                    isPriority = priority,
                                    fcmToken = token,
                                    userUid = uid
                                )
                            )

                            finished++

                            if (finished == documents.size()) {

                                onSuccess(contacts)

                            }

                        }
                        .addOnFailureListener {

                            finished++

                            contacts.add(
                                ContactItem(
                                    id = doc.id,
                                    name = name,
                                    phone = phone,
                                    relationship = relationship,
                                    isPriority = priority
                                )
                            )

                            if (finished == documents.size()) {

                                onSuccess(contacts)

                            }

                        }

                }

            }
            .addOnFailureListener {

                onFailure(it.message ?: "Unknown Error")

            }

    }

    fun updateEmergency(

        documentId: String,
        updateData: HashMap<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit

    ) {

        db.collection("emergency_logs")
            .document(documentId)
            .update(updateData)
            .addOnSuccessListener {

                onSuccess()

            }
            .addOnFailureListener {

                onFailure(it.message ?: "")

            }

    }

    fun createEmergency(

        emergencyData: HashMap<String, Any>,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit

    ) {

        db.collection("emergency_logs")
            .add(emergencyData)
            .addOnSuccessListener {

                onSuccess(it.id)

            }
            .addOnFailureListener {

                onFailure(it.message ?: "")

            }

    }

    fun cancelEmergency(documentId: String) {
        db.collection("emergency_logs")
            .document(documentId)
            .delete()
            .addOnSuccessListener {
                Log.d("SOS_DEBUG", "تم حذف البلاغ الملغي نهائياً")
            }
            .addOnFailureListener { e ->
                Log.e("SOS_DEBUG", "خطأ في حذف البلاغ: ${e.message}")
            }
    }

    fun queueNotification(

        notification: HashMap<String, Any>

    ) {

        db.collection("notifications_queue")
            .add(notification)

    }
    fun getRecentEmergencies(

        userId: String,
        onSuccess: (List<HomeItem>) -> Unit,
        onFailure: (String) -> Unit

    ) {

        db.collection("emergency_logs")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { result ->

                val recentList = mutableListOf<HomeItem>()

                result.documents.forEach { doc ->

                    val status = doc.getString("status") ?: "triggered"

                    val type =
                        if (status.equals("Resolved", ignoreCase = true)) {
                            "resolved"
                        } else {
                            "sos"
                        }

                    val timestamp = doc.getTimestamp("timestamp")

                    val timeText =
                        if (timestamp != null) {
                            android.text.format.DateFormat.format(
                                "dd/MM/yyyy HH:mm",
                                timestamp.toDate()
                            ).toString()
                        } else {
                            "Unknown"
                        }

                    recentList.add(

                        HomeItem(

                            id = doc.id,

                            title = "🚨 $type Alert",

                            timestamp = timeText,

                            type = type.lowercase()

                        )

                    )
                }

                onSuccess(recentList)

            }
            .addOnFailureListener {

                onFailure(it.message ?: "Failed to load recent activities")

            }

    }

}