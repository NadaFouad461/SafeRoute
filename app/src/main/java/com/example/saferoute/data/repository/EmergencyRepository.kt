package com.example.saferoute.data.repository

import com.example.saferoute.data.EmergencyDao
import com.example.saferoute.data.remote.FirestoreService
import com.example.saferoute.models.ContactItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmergencyRepository @Inject constructor(

    private val emergencyDao: EmergencyDao,
    private val firestoreService: FirestoreService

) {

    suspend fun insertLog(log: com.example.saferoute.data.local.EmergencyLog) {
        emergencyDao.insertLog(log)
    }

    fun loadEmergencyContacts(

        currentUserId: String,
        onSuccess: (MutableList<ContactItem>) -> Unit,
        onFailure: (String) -> Unit

    ) {

        firestoreService.loadEmergencyContacts(

            currentUserId,
            onSuccess,
            onFailure

        )

    }

    fun createEmergency(

        emergencyData: HashMap<String, Any>,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit

    ) {

        firestoreService.createEmergency(

            emergencyData,
            onSuccess,
            onFailure

        )

    }

    fun updateEmergency(

        documentId: String,
        updateData: HashMap<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit

    ) {

        firestoreService.updateEmergency(

            documentId,
            updateData,
            onSuccess,
            onFailure

        )

    }

    fun cancelEmergency(

        documentId: String

    ) {

        firestoreService.cancelEmergency(documentId)

    }

    fun queueNotification(

        notification: HashMap<String, Any>

    ) {

        firestoreService.queueNotification(notification)

    }
    fun getUserData(
        userId: String,
        onResult: (Boolean, Map<String, Any>?, String?) -> Unit
    ) {

        firestoreService.getUserData(
            userId,
            onResult
        )

    }

}