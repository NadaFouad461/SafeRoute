package com.example.saferoute.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.saferoute.ui.sensors.FallAlertActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow


@AndroidEntryPoint
class SafeWalkService : Service() {

    // الـ Companion Object ده هيخلينا نقدر نقرأ الوقت المتبقي من أي مكان في التطبيق
    companion object {
        const val CHANNEL_ID = "SafeWalkChannel"
        const val NOTIFICATION_ID = 101

        val timeRemaining = MutableStateFlow("00:00")
        val isWalkActive = MutableStateFlow(false)
    }

    private var countDownTimer: CountDownTimer? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val timeInMinutes = intent?.getIntExtra("DURATION_MINUTES", 15) ?: 15
        startSafeWalk(timeInMinutes)

        return START_NOT_STICKY
    }

    private fun startSafeWalk(minutes: Int) {
        isWalkActive.value = true
        val timeInMillis = minutes * 60 * 1000L


        val notification = createNotification("جاري تأمين مسارك...")
        startForeground(NOTIFICATION_ID, notification)


        countDownTimer = object : CountDownTimer(timeInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutesLeft = (millisUntilFinished / 1000) / 60
                val secondsLeft = (millisUntilFinished / 1000) % 60
                val timeString = String.format("%02d:%02d", minutesLeft, secondsLeft)


                timeRemaining.value = timeString
                // تحديث الإشعار فوق
                updateNotification("الوقت المتبقي: $timeString")
            }

            override fun onFinish() {
                isWalkActive.value = false
                timeRemaining.value = "00:00"
                triggerSOS()
            }
        }.start()
    }

    private fun triggerSOS() {
        // 🚨 الكارثة حصلت: الوقت خلص والمستخدم مداسش إنه وصل بالسلامة!
        val sosIntent = Intent(this, FallAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(sosIntent)
        stopSelf() // قفل الخدمة
    }

    private fun createNotification(content: String): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("المشي الآمن 🛡️")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(content: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification(content))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Safe Walk", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        isWalkActive.value = false
    }

    override fun onBind(intent: Intent?): IBinder? = null
}