package com.example.saferoute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emergency_logs")
data class EmergencyLog(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val userId: String,

    val type: String,

    val latitude: Double,

    val longitude: Double,

    val timestamp: Long,

    val status: String = "ACTIVE",

    // 🔋 نسبة البطارية ديناميكية وقت البلاغ (تُمرر عند إنشاء البلاغ)
    val batteryLevel: Int = 100
)