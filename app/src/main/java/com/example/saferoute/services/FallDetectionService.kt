package com.example.saferoute.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.saferoute.utils.FallDetector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FallDetectionService: Service() {

    @Inject
    lateinit var fallDetector: FallDetector
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)



    override fun onCreate() {
        super.onCreate()
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

    private fun observeFallEvents() {
       serviceScope.launch {
           fallDetector.fallEvent.collect {
               Log.d("FallDetection", "🚨 تم اكتشاف سقوط حقيقي! يتم الآن معالجة الطوارئ...")
           }
       }
    }

    private fun startForegroundService() {
        val channelId = "SafeRoute_Fall_Detection"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "مراقبة السقوط",
                NotificationManager.IMPORTANCE_LOW // Low عشان ميعملش صوت كل شوية
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("درع SafeRoute مفعل 🛡️")
            .setContentText("يتم الآن مراقبة حركتك لحمايتك...")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // غيرها بأيقونة التطبيق بتاعك بعدين
            .setOngoing(true) // يمنع المستخدم إنه يمسح الإشعار بالغلط
            .build()

        startForeground(1, notification)
    }


    override fun onBind(p0: Intent?): IBinder? = null
}