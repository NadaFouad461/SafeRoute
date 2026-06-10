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
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.example.saferoute.utils.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SosBackgroundService : Service() {

    private val CHANNEL_ID = "SosServiceChannel"
    private lateinit var emergencyRepository: EmergencyRepository
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // تهيئة الريبوزيتوري داخل الخدمة لضمان حفظ البيانات محلياً وسحابياً
        val db = AppDatabase.getDatabase(applicationContext)
        val firestoreService = FirestoreService()
        emergencyRepository = EmergencyRepository(db.emergencyDao(), firestoreService)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(1, notification)

        if (intent?.action == "TRIGGER_SOS_ACTION") {
            val userId = intent.getStringExtra("USER_ID") ?: "unknown_user"
            val emergencyNumbers = listOf("01000000000", "01200000000") // يفضل تمريرها عبر الـ Intent مستقبلاً

            runEmergencySequence(userId, emergencyNumbers)
        }

        return START_STICKY
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun runEmergencySequence(userId: String, numbers: List<String>) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (PermissionManager.hasAllPermissions(this)) {

            val locationRequest = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build()

            val cancellationToken = com.google.android.gms.tasks.CancellationTokenSource().token

            fusedLocationClient.getCurrentLocation(locationRequest, cancellationToken)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val mapsUrl = "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
                        val message = "إلحقني! أنا في خطر، ده موقعي الحالي: $mapsUrl"

                        try {
                            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                getSystemService(SmsManager::class.java)
                            } else {
                                SmsManager.getDefault()
                            }
                            for (number in numbers) {
                                smsManager.sendTextMessage(number, null, message, null, null)
                            }
                            Log.d("SosService", "تم إرسال رسائل الاستغاثة من الخلفية بنجاح.")
                        } catch (e: Exception) {
                            Log.e("SosService", "فشل إرسال الـ SMS: ${e.message}")
                        }

                        // حفظ البيانات من خلال الـ Repository ليتم تخزينها في Room و Firebase معاً
                        val log = EmergencyLog(
                            type = "SOS_BACKGROUND",
                            latitude = location.latitude,
                            longitude = location.longitude,
                            timestamp = System.currentTimeMillis(),
                            status = "ACTIVE_BACKGROUND"
                        )

                        serviceScope.launch {
                            emergencyRepository.insertLog(log)
                        }
                    } else {
                        Log.e("SosService", "تعذر جلب الموقع")
                    }
                }
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SafeRoute في الخدمة")
            .setContentText("نظام مراقبة الأمان يعمل في الخلفية لحمايتك...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "SafeRoute Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}