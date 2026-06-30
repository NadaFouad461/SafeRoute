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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SosRepository(
    private val emergencyRepository: EmergencyRepository
) {

    private val db = FirebaseFirestore.getInstance()

    suspend fun getEmergencyContacts(
        userId: String
    ): List<String> {

        val result =
            db.collection("users")
                .document(userId)
                .collection("contacts")
                .get()
                .await()

        return result.documents.mapNotNull {
            it.getString("phone")
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun sendEmergencySos(
        context: Context,
        userId: String
    ): Result<Unit> {

        return try {

            val emergencyNumbers =
                getEmergencyContacts(userId)

            if (emergencyNumbers.isEmpty()) {

                return Result.failure(
                    Exception("لا يوجد جهات اتصال للطوارئ")
                )
            }

            val fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(context)

            val locationRequest =
                CurrentLocationRequest.Builder()
                    .setPriority(
                        Priority.PRIORITY_HIGH_ACCURACY
                    )
                    .build()

            val location =
                fusedLocationClient
                    .getCurrentLocation(
                        locationRequest,
                        null
                    )
                    .await()

            if (location != null) {

                val mapsUrl =
                    "https://maps.google.com/?q=${location.latitude},${location.longitude}"

                val message =
                    "🚨 أنا في خطر، موقعي الحالي: $mapsUrl"

                try {

                    val smsManager =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                            context.getSystemService(
                                SmsManager::class.java
                            )

                        } else {

                            @Suppress("DEPRECATION")
                            SmsManager.getDefault()
                        }

                    for (number in emergencyNumbers) {

                        smsManager.sendTextMessage(
                            number,
                            null,
                            message,
                            null,
                            null
                        )
                    }

                } catch (e: Exception) {

                    Log.e(
                        "SOS_SYSTEM",
                        e.message ?: ""
                    )
                }

                if (emergencyNumbers.isNotEmpty()) {

                    try {

                        val phone =
                            emergencyNumbers.first()

                        val intent =
                            Intent(Intent.ACTION_VIEW)

                        intent.data =
                            Uri.parse(
                                "https://wa.me/$phone?text=${Uri.encode(message)}"
                            )

                        intent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                        )

                        context.startActivity(intent)

                    } catch (e: Exception) {

                        Log.e(
                            "WHATSAPP",
                            e.message ?: ""
                        )
                    }
                }

                val log =
                    EmergencyLog(
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

                Result.failure(
                    Exception("تعذر الحصول على الموقع")
                )
            }

        } catch (e: Exception) {

            Result.failure(e)
        }
    }
}