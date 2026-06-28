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

            val location = fusedLocationClient.getCurrentLocation(locationRequest, null).await()

            if (location != null) {
                val mapsUrl = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
                val message = "إلحقني! أنا في خطر، ده موقعي الحالي على الخريطة: $mapsUrl"

                // 1️⃣ أولاً: إرسال SMS تلقائي لجميع الأرقام المتواجدة في القائمة
                try {
                    val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.getSystemService(SmsManager::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        SmsManager.getDefault()
                    }

                    for (number in emergencyNumbers) {
                        smsManager.sendTextMessage(number, null, message, null, null)
                        Log.d("SOS_SYSTEM", "SMS sent to: $number")
                    }
                } catch (smsError: Exception) {
                    Log.e("SOS_SYSTEM", "SMS sending failed: ${smsError.message}")
                }

                // 2️⃣ ثانياً: فتح الواتساب كخطوة إضافية تفاعلية لأول رقم في القائمة
                if (emergencyNumbers.isNotEmpty()) {
                    val primaryPhone = emergencyNumbers.first().removePrefix("+")
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://wa.me/$primaryPhone?text=${Uri.encode(message)}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("SOS_SYSTEM", "WhatsApp app not found on device.")
                    }
                }

                // حفظ اللوج محلياً وسحابياً
                val log = EmergencyLog(
                    userId = userId,
                    type = EmergencyType.SOS,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = System.currentTimeMillis(),
                    status = "MANUAL"
                )
                emergencyRepository.insertLog(log)

                Result.success(Unit)
            } else {
                Result.failure(Exception("تعذر الحصول على إحداثيات الموقع"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}