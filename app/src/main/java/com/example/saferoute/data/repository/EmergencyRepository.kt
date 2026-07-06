package com.example.saferoute.data.repository

import androidx.lifecycle.LiveData
import com.example.saferoute.data.EmergencyDao
import com.example.saferoute.data.local.EmergencyLog
import com.example.saferoute.data.remote.FirestoreService

class EmergencyRepository(
    private val emergencyDao: EmergencyDao,
) {


    suspend fun insertLog(log: EmergencyLog) {
        emergencyDao.insertLog(log)
    }

}