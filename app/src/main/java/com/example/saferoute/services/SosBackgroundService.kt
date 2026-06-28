package com.example.saferoute.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.saferoute.data.local.AppDatabase
import com.example.saferoute.data.local.EmergencyLog
import com.example.saferoute.data.remote.FirestoreService
import com.example.saferoute.data.repository.EmergencyRepository
import com.example.saferoute.utils.EmergencyType
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.example.saferoute.utils.PermissionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SosBackgroundService : Service() {

    private val CHANNEL_ID = "SosServiceChannel"
    private lateinit var emergencyRepository: EmergencyRepository
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val appDb = AppDatabase.getDatabase(applicationContext)
        val firestoreService = FirestoreService()
        emergencyRepository = EmergencyRepository(appDb.emergencyDao(), firestoreService)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(1, notification)

        if (intent?.action == "TRIGGER_SOS_ACTION") {
            val userId = intent.getStringExtra("USER_ID") ?: FirebaseAuth.getInstance().currentUser?.uid ?: "unknown_user"

            // جلب الأرقام فوراً من الـ Firestore الفرعي لتشغيل استغاثات الحساسات والـ Safe Walk الخلفي
            fetchContactsAndTrigger(userId)
        }

        return START_STICKY
    }

    private fun fetchContactsAndTrigger(userId: String) {
        db.collection("users").document(userId).collection("contacts")
            .get()
            .addOnSuccessListener { documents ->
                val numbers = documents.mapNotNull { it.getString("phone") }
                if (numbers.isNotEmpty()) {
                    runEmergencySequence(userId, numbers)
                }
            }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun runEmergencySequence(userId: String, numbers: List<String>) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (PermissionManager.hasAllPermissions(this)) {
            val locationRequest = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build()

            fusedLocationClient.getCurrentLocation(locationRequest, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val mapsUrl = "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
                        val message = "استغاثة تلقائية من SafeRoute! أنا في خطر، موقعي الحالي: $mapsUrl"

                        try {
                            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                getSystemService(SmsManager::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                SmsManager.getDefault()
                            }
                            for (number in numbers) {
                                smsManager.sendTextMessage(number, null, message, null, null)
                            }
                        } catch (e: Exception) {
                            Log.e("SosService", "فشل إرسال الـ SMS من الخلفية: ${e.message}")
                        }

                        val log = EmergencyLog(
                            userId = userId,
                            type = EmergencyType.SOS,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            timestamp = System.currentTimeMillis(),
                            status = "AUTO"
                        )

                        serviceScope.launch {
                            emergencyRepository.insertLog(log)
                        }
                    }
                }
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SafeRoute في الخدمة حماية الطوارئ نشطة")
            .setContentText("نظام مراقبة الأمان يعمل في الخلفية لحمايتك...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "SafeRoute Service Channel", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}