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
        emergencyDao.insertLog(log)    // حفظ محلي في Room
        firestoreService.sendEmergency(log) // رفع ومزامنة على السحاب في Firestore
    }

    fun getAllLogs(): LiveData<List<EmergencyLog>> {
        return emergencyDao.getAllLogs()
    }
}