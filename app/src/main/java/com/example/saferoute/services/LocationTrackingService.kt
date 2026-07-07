package com.example.saferoute.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.saferoute.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint

class LocationTrackingService : Service() {

    private lateinit var locationManager: LocationManager
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "location_tracking_channel"

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.d("LocationService", "New location: ${location.latitude}, ${location.longitude}")

            val intent = Intent("LocationUpdateIntent").apply {
                putExtra("lat", location.latitude)
                putExtra("lon", location.longitude)
            }
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForegroundServiceWithNotification()
        startTracking()
        return START_STICKY
    }

    private fun startTracking() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }

        val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val lastNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        val initialLocation = lastGps ?: lastNet
        initialLocation?.let {
            val intent = Intent("LocationUpdateIntent").apply {
                putExtra("lat", it.latitude)
                putExtra("lon", it.longitude)
            }
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
        }

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                0f,
                listener
            )
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                1000L,
                0f,
                listener
            )
        }
    }

    private fun startForegroundServiceWithNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("تتبع المسار الآمن نشط")
            .setContentText("يتم الآن تحديث موقعك لضمان سلامتك...")
            .setSmallIcon(R.drawable.ic_map_marker)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "خدمة تتبع الموقع",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        locationManager.removeUpdates(listener)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}