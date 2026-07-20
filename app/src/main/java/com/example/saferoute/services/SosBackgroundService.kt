package com.example.saferoute.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.BatteryManager
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
import com.example.saferoute.utils.PermissionManager
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint

class SosBackgroundService : Service() {

    private val CHANNEL_ID = "SosServiceChannel"
    private lateinit var emergencyRepository: EmergencyRepository
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // البدء كخدمة أمامية فوراً لتجنب الـ Crash
        startForegroundService()

        val appDb = AppDatabase.getDatabase(applicationContext)
        val firestoreService = FirestoreService(db)
        emergencyRepository = EmergencyRepository(appDb.emergencyDao(), firestoreService)
    }

    private fun startForegroundService() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1002, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1002, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val userId = intent?.getStringExtra("USER_ID") ?: FirebaseAuth.getInstance().currentUser?.uid

        when (intent?.action) {
            "TRIGGER_SOS_ACTION" -> {
                if (userId != null) {
                    fetchContactsAndTrigger(userId)
                } else {
                    Log.e("SosService", "لم يتم العثور على User ID")
                }
            }
            "TRIGGER_DANGER_ZONE_ACTION" -> {
                val zoneDescription = intent.getStringExtra("ZONE_DESCRIPTION") ?: "منطقة خطر"
                if (userId != null) {
                    fetchContactsAndTriggerZoneAlert(userId, zoneDescription)
                } else {
                    Log.e("SosService", "لم يتم العثور على User ID")
                }
            }
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
                } else {
                    Log.e("SosService", "لا يوجد جهات اتصال مسجلة!")
                }
            }
    }

    private fun fetchContactsAndTriggerZoneAlert(userId: String, zoneDescription: String) {
        db.collection("users").document(userId).collection("contacts")
            .get()
            .addOnSuccessListener { documents ->
                val numbers = documents.mapNotNull { it.getString("phone") }
                if (numbers.isNotEmpty()) {
                    runDangerZoneAlertSequence(userId, numbers, zoneDescription)
                } else {
                    Log.e("SosService", "لا يوجد جهات اتصال مسجلة! (تنبيه منطقة خطر)")
                }
            }
    }


    @android.annotation.SuppressLint("MissingPermission")
    private fun runDangerZoneAlertSequence(userId: String, numbers: List<String>, zoneDescription: String) {

        if (!PermissionManager.hasAllPermissions(this)) {
            Log.e("SosService", "لا توجد صلاحيات كافية لإرسال تنبيه منطقة الخطر")
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val locationRequest = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        fusedLocationClient.getCurrentLocation(locationRequest, null)
            .addOnSuccessListener { location ->
                val message = if (location != null) {
                    val mapsUrl =
                        "https://maps.google.com/?q=${location.latitude},${location.longitude}"
                    "⚠️ تنبيه SafeRoute: دخل صديقك منطقة عالية الخطورة ($zoneDescription).\nموقعه الحالي: $mapsUrl"
                } else {
                    "⚠️ تنبيه SafeRoute: دخل صديقك منطقة عالية الخطورة ($zoneDescription) (تعذر تحديد الموقع بدقة الآن)."
                }

                // إرسال صامت فقط - بدون فتح تطبيق الرسائل
                sendSmsToContacts(numbers, message)

                saveToHistory(
                    userId,
                    location?.latitude ?: 0.0,
                    location?.longitude ?: 0.0,
                    numbers,
                    type = "DANGER_ZONE",
                    status = "Danger Zone Alert: $zoneDescription"
                )
            }
            .addOnFailureListener {
                val message =
                    "⚠️ تنبيه SafeRoute: دخل صديقك منطقة عالية الخطورة ($zoneDescription) (الـ GPS لا يستجيب)."
                sendSmsToContacts(numbers, message)
                saveToHistory(
                    userId, 0.0, 0.0, numbers,
                    type = "DANGER_ZONE",
                    status = "Danger Zone Alert: $zoneDescription"
                )
            }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun runEmergencySequence(userId: String, numbers: List<String>) {

        if (!PermissionManager.hasAllPermissions(this)) {
            val fallbackMsg =
                "استغاثة من SafeRoute! أنا في خطر (موقعي غير متاح بسبب نقص الصلاحيات)."
            sendSmsToContacts(numbers, fallbackMsg)
            openSmsApp(numbers, fallbackMsg)
            saveToHistory(userId, 0.0, 0.0, numbers)
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val locationRequest = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        fusedLocationClient.getCurrentLocation(locationRequest, null)
            .addOnSuccessListener { location ->
                val message = if (location != null) {
                    val mapsUrl =
                        "https://maps.google.com/?q=${location.latitude},${location.longitude}"
                    "استغاثة تلقائية من SafeRoute! أنا في خطر، موقعي: $mapsUrl"
                } else {
                    "استغاثة تلقائية من SafeRoute! أنا في خطر (تعذر تحديد موقعي بدقة الآن)."
                }

                // 1. إرسال الرسائل الصامتة
                sendSmsToContacts(numbers, message)

                // 2. فتح تطبيق الرسائل
                openSmsApp(numbers, message)

                // 3. الحفظ في الهيستوري (Firestore & Room)
                saveToHistory(
                    userId,
                    location?.latitude ?: 0.0,
                    location?.longitude ?: 0.0,
                    numbers
                )
            }
            .addOnFailureListener {
                val message = "استغاثة تلقائية من SafeRoute! أنا في خطر (الـ GPS لا يستجيب)."
                sendSmsToContacts(numbers, message)
                openSmsApp(numbers, message)
                saveToHistory(userId, 0.0, 0.0, numbers)
            }
    }

    private fun sendSmsToContacts(numbers: List<String>, message: String) {
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
            Log.e("SosService", "فشل إرسال الـ SMS صامتاً: ${e.message}")
        }
    }

    private fun openSmsApp(numbers: List<String>, message: String) {
        try {
            val separator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) "," else ";"
            val allNumbers = numbers.joinToString(separator)

            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$allNumbers")
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(smsIntent)
            Log.d("SosService", "تم فتح تطبيق الرسائل بنجاح")
        } catch (e: Exception) {
            Log.e("SosService", "فشل فتح تطبيق الرسائل: ${e.message}")
        }
    }

    private fun saveToHistory(
        userId: String,
        lat: Double,
        lng: Double,
        numbers: List<String>,
        type: String = "SOS",
        status: String = "Emergency Dispatched"
    ) {
        db.collection("users").document(userId).get().addOnSuccessListener { doc ->
            val userName = doc.getString("name") ?: "مستخدم SafeRoute"

            val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
            val battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

            // 1. الحفظ في فايربيز (مرة واحدة فقط بالبيانات الكاملة)
            val emergencyData = hashMapOf(
                "userId" to userId,
                "userName" to userName,
                "status" to status,
                "type" to type,
                "latitude" to lat,
                "longitude" to lng,
                "batteryLevel" to battery,
                "alertedContacts" to numbers,
                "timestamp" to com.google.firebase.Timestamp.now()
            )

            db.collection("emergency_logs").add(emergencyData).addOnSuccessListener {
                Log.d("SosService", "تم الحفظ في History بنجاح")
            }

            // 2. الحفظ المحلي في (Room) فقط! بدون رفع نسخة تانية للفايربيز
            val log = EmergencyLog(
                userId = userId,
                type = type,
                latitude = lat,
                longitude = lng,
                timestamp = System.currentTimeMillis(),
                status = status,
                batteryLevel = battery
            )
            serviceScope.launch {
                val appDb = AppDatabase.getDatabase(applicationContext)
                appDb.emergencyDao().insertLog(log)
            }
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SafeRoute حماية الطوارئ نشطة")
            .setContentText("نظام مراقبة الأمان يعمل في الخلفية لحمايتك...")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
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