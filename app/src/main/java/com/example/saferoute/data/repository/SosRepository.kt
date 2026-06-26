package com.example.saferoute.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import com.example.saferoute.data.local.EmergencyLog
import com.example.saferoute.utils.EmergencyType
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

class SosRepository(
    private val emergencyRepository: EmergencyRepository
) {

    @SuppressLint("MissingPermission")
    suspend fun sendEmergencySos(
        context: Context,
        userId: String,
        emergencyNumbers: List<String>
    ): Result<Unit> {

        return try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            val locationRequest = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build()

            val location = fusedLocationClient
                .getCurrentLocation(locationRequest, null)
                .await()

            if (location != null) {
                // تعديل رابط الخريطة هنا لتجنب الخطأ الإملائي
                val mapsUrl = "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
                val message = "إلحقني! أنا في خطر، ده موقعي الحالي: $mapsUrl"


                val phone = emergencyNumbers.first().removePrefix("+")

                try {
                    // 🟢 1. WhatsApp (Primary)
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(
                        "https://wa.me/$phone?text=${Uri.encode(message)}"
                    )
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)

                } catch (e: Exception) {

                    // 🔴 2. Fallback SMS
                    try {
                        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            context.getSystemService(SmsManager::class.java)
                        } else {
                            SmsManager.getDefault()
                        }

                        emergencyNumbers.forEach { number ->
                            smsManager.sendTextMessage(number, null, message, null, null)
                        }

                    } catch (smsError: Exception) {
                        Log.e("SOS", "Both WhatsApp and SMS failed: ${smsError.message}")
                    }
                }

                val log = EmergencyLog(
                    userId = "test_user",
                    type = EmergencyType.SOS,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = System.currentTimeMillis(),
                    status = "MANUAL"
                )

                emergencyRepository.insertLog(log)
                Result.success(Unit)
            } else {
                Result.failure(Exception("تعذر تحديد الموقع"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}