package com.example.saferoute.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.saferoute.data.local.EmergencyLog

@Dao
interface EmergencyDao {

    @Insert
    suspend fun insertLog(log: EmergencyLog)

}