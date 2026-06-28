package com.example.saferoute.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.saferoute.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService :
    FirebaseMessagingService() {

    override fun onNewToken(token: String) {

        super.onNewToken(token)

        android.util.Log.d("FCM_TOKEN", "TOKEN = $token")
    }

    override fun onMessageReceived(
        remoteMessage: RemoteMessage
    ) {

        super.onMessageReceived(remoteMessage)

        showNotification(
            remoteMessage.notification?.title ?: "SOS Alert",
            remoteMessage.notification?.body ?: ""
        )
    }

    private fun showNotification(
        title: String,
        message: String
    ) {

        val manager =
            getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel =
                NotificationChannel(
                    "sos_channel",
                    "SOS Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                )

            manager.createNotificationChannel(channel)
        }

        val notification =
            NotificationCompat.Builder(
                this,
                "sos_channel"
            )
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()

        manager.notify(
            1,
            notification
        )
    }
}