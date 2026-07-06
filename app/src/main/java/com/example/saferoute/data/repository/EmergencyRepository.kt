package com.example.saferoute.data.repository

import androidx.lifecycle.LiveData
import com.example.saferoute.data.EmergencyDao
import com.example.saferoute.data.local.EmergencyLog
import com.example.saferoute.data.remote.FirestoreService

class EmergencyRepository(
    private val emergencyDao: EmergencyDao,
    private val firestoreService: FirestoreService
) {


    suspend fun insertLog(log: EmergencyLog) {
        emergencyDao.insertLog(log)
    }

    fun getLogsByUser(
        userId: String
    ): LiveData<List<EmergencyLog>> {
        return emergencyDao.getLogsByUser(userId)
    }
}