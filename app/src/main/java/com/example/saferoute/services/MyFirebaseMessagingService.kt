package com.example.saferoute.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.saferoute.MainActivity
import com.example.saferoute.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // فحص شامل لاستخراج النصوص سواء كانت داخل data أو notification
        val title = remoteMessage.data["title"]
            ?: remoteMessage.notification?.title
            ?: "🚨 استغاثة طارئة!"

        val body = remoteMessage.data["body"]
            ?: remoteMessage.notification?.body
            ?: "الرجاء المساعدة فوراً"

        val sosAlertId = remoteMessage.data["SOS_ALERT_ID"] ?: ""
        val senderName = remoteMessage.data["senderName"] ?: "شخص مقرب"

        // إيقاظ الشاشة المظلمة فوراً برمجياً
        wakeUpDeviceScreen()

        // استدعاء دالة بناء الإشعار بمؤثرات الصوت والاهتزاز الكاملة
        sendNotification(title, body, sosAlertId, senderName)
    }

    private fun wakeUpDeviceScreen() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "SafeRoute:EmergencyWakeLock"
        )
        // إيقاظ الشاشة لمدة 5 ثوانٍ كاملة لضمان رؤية البلاغ
        wakeLock.acquire(5000)
    }

    private fun sendNotification(title: String, messageBody: String, sosAlertId: String, senderName: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // اجعلي الـ channelId يطابق ما يرسله السيرفر تماماً
        val channelId = "SafeRoute_SOS_Channel"

        // إعداد الـ Intent لفتح التطبيق والتوجه للهيستوري مباشرة
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("INCOMING_SOS_ID", sosAlertId)
            putExtra("SENDER_NAME", senderName)
            putExtra("NAVIGATE_TO", "HISTORY")
        }

        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3️⃣ إنشاء الـ Channel لأجهزة أندرويد 8 فما فوق وتثبيت أقصى درجات التنبيه
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "🚨 طوارئ Saferoute",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "قناة استقبال استغاثات الطوارئ العاجلة"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(100, 500, 200, 500, 200, 800)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 4️⃣ بناء الإشعار (تم دمج الإعدادات في Builder واحد صحيح لتجنب التكرار والـ Errors)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // استخدام الأيقونة الافتراضية لضمان عدم حدوث Crash لو ملف drawable غير موجود
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL) // يعامل كإشعار مكالمة فائقة الأهمية
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // يظهر محتواه فوق شاشة القفل
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setFullScreenIntent(pendingIntent, true) // يجبر النظام على إظهاره منبثقاً فوراً والشاشة مغلقة
            .setContentIntent(pendingIntent)

        // إرسال الإشعار بمعرف فريد يعتمد على الوقت لمنع التداخل
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}