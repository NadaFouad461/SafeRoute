package com.example.saferoute.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.saferoute.ui.sensors.FallAlertActivity
import com.example.saferoute.utils.FallDetector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FallDetectionService : Service() {

    @Inject
    lateinit var fallDetector: FallDetector

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val FOREGROUND_CHANNEL_ID = "SafeRoute_Fall_Detection"
        const val ALERT_CHANNEL_ID = "SafeRoute_Fall_Alert"
        const val FALL_ALERT_NOTIFICATION_ID = 99
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startForegroundService()
        fallDetector.startEventListeners()
        observeFallEvents()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        fallDetector.stopEventListeners()
        serviceScope.cancel()
    }

    // ═══ الـ Channels ═══

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)


            val foregroundChannel = NotificationChannel(
                FOREGROUND_CHANNEL_ID, "مراقبة السقوط", NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(foregroundChannel)

            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "تنبيه سقوط طارئ",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    // ═══ مراقبة السقوط ═══

    private fun observeFallEvents() {
        serviceScope.launch {
            fallDetector.fallEvent.collect {
                Log.d("FallDetection", "🚨 سقوط مكتشف!")

                // ✅ افتح الـ Activity مباشرة
                val intent =
                    Intent(this@FallDetectionService, FallAlertActivity::class.java).apply {
                        flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                startActivity(intent)
                showFallAlertNotification()
            }
        }
    }

    // ═══ Notification تفتح الـ FallAlertActivity ═══

    private fun showFallAlertNotification() {
        val fullScreenIntent = Intent(this, FallAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle("⚠️ هل أنت بخير؟")
            .setContentText("تم اكتشاف سقوط محتمل — اضغط للرد")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true).setOngoing(false)
            .setFullScreenIntent(fullScreenPendingIntent, true).build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(FALL_ALERT_NOTIFICATION_ID, notification)
        try {
            startActivity(fullScreenIntent)
        } catch (e: Exception) {
            Log.e("FallDetection", "Failed to start activity directly: ${e.message}")
        }
    }

    // ═══ الـ Foreground Notification الثابتة ═══

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setContentTitle("درع SafeRoute مفعل 🛡️")
            .setContentText("يتم الآن مراقبة حركتك لحمايتك...")
            .setSmallIcon(android.R.drawable.ic_dialog_info).setOngoing(true).build()

        startForeground(1, notification)
    }

    override fun onBind(p0: Intent?): IBinder? = null
}