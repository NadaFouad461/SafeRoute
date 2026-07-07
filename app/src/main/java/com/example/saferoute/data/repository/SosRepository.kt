package com.example.saferoute.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SosRepository @Inject constructor(
    private val emergencyRepository: EmergencyRepository
) {

    private val db = FirebaseFirestore.getInstance()

    suspend fun getEmergencyContacts(
        userId: String
    ): List<String> {
        val result = db.collection("users")
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
        userId: String,
        selectedNumbers: List<String>
    ): String {

        try {
            if (selectedNumbers.isEmpty()) {
                return ""
            }

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val locationRequest = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build()

            val location = fusedLocationClient.getCurrentLocation(locationRequest, null).await()

            if (location != null) {
                val message =
                    "🚨 emergency SOS! I am in danger. Please help me. My current location coordinates are: ${location.latitude} , ${location.longitude}"


                val batteryManager =
                    context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
                val currentBattery =
                    batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)


                val emergencyData = hashMapOf(
                    "userId" to userId,
                    "girlUserId" to userId,
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "batteryLevel" to currentBattery, // البطارية الحقيقية لإنهاء مشكلة الـ 100%
                    "type" to "SOS",
                    "status" to "MANUAL",
                    "alertedContacts" to selectedNumbers
                )


                val firestoreResult = db.collection("emergency_logs").add(emergencyData).await()
                val firestoreDocId = firestoreResult.id

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context.applicationContext,
                        "وضع الفحص: جاري الانتقال للواتساب مباشرة... 🟢",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                delay(100)


                var activeWhatsAppNumber = ""
                try {
                    activeWhatsAppNumber = selectedNumbers.first()
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data =
                            Uri.parse("https://wa.me/$activeWhatsAppNumber?text=${Uri.encode(message)}")
                        // 🎯 🔥 الـ Flags دي بتجبر الأندرويد يسيب تطبيقك شغال في الخلفية بالـ Timer بتاعه وميعملش ريستارت لما ترجعي
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e("WHATSAPP", "WhatsApp Error: ${e.message}")
                }



                return firestoreDocId

            } else {
                Log.e("SOS_SYSTEM", "فشل جلب الموقع الجغرافي (Location is null)")
                return ""
            }
        } catch (e: Exception) {
            Log.e("SOS_SYSTEM", "خطأ عام في الـ Repository: ${e.message}")
            return ""
        }
    }
}