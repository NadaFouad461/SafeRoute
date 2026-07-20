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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint

class LocationTrackingService : Service() {

    private lateinit var locationManager: LocationManager
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "location_tracking_channel"


    private val LOCATION_UPDATE_INTERVAL_MS = 5 * 60 * 1000L

    // ==== كشف الدخول التلقائي لمناطق الخطر ====
    private data class DangerZone(val id: String, val point: Location, val description: String)

    private val dangerZones = mutableListOf<DangerZone>()
    private var dangerZonesListener: ValueEventListener? = null
    private val dangerZonesRef = FirebaseDatabase.getInstance().getReference("dangerZones")

    private val insideZoneIds = mutableSetOf<String>()

    private val DANGER_ZONE_ENTER_RADIUS_METERS = 150f
    private val DANGER_ZONE_EXIT_RADIUS_METERS = 200f

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.d("LocationService", "New location: ${location.latitude}, ${location.longitude}")

            val intent = Intent("LocationUpdateIntent").apply {
                putExtra("lat", location.latitude)
                putExtra("lon", location.longitude)
            }
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)

            checkDangerZoneEntry(location)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // استدعاء startForeground فوراً في onCreate لتجنب الـ Crash في أندرويد 12+
        createNotificationChannel()
        startForegroundServiceWithNotification()
        startListeningToDangerZones()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
            checkDangerZoneEntry(it)
        }

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LOCATION_UPDATE_INTERVAL_MS,
                0f,
                listener
            )
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                LOCATION_UPDATE_INTERVAL_MS,
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

    // ==== منطق مناطق الخطر ====

    private fun startListeningToDangerZones() {
        if (dangerZonesListener != null) return
        val zoneListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                dangerZones.clear()
                for (zoneSnapshot in snapshot.children) {
                    val lat = zoneSnapshot.child("latitude").getValue(Double::class.java) ?: continue
                    val lon = zoneSnapshot.child("longitude").getValue(Double::class.java) ?: continue
                    val desc = zoneSnapshot.child("description").getValue(String::class.java)
                        ?: "منطقة خطر"

                    val zoneLocation = Location("dangerZone").apply {
                        latitude = lat
                        longitude = lon
                    }
                    dangerZones.add(DangerZone(zoneSnapshot.key ?: "$lat,$lon", zoneLocation, desc))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LocationService", "فشل تحميل مناطق الخطر: ${error.message}")
            }
        }
        dangerZonesListener = zoneListener
        dangerZonesRef.addValueEventListener(zoneListener)
    }

    private fun checkDangerZoneEntry(currentLocation: Location) {
        if (dangerZones.isEmpty()) return

        for (zone in dangerZones) {
            val distance = currentLocation.distanceTo(zone.point)
            val alreadyInside = insideZoneIds.contains(zone.id)

            if (!alreadyInside && distance <= DANGER_ZONE_ENTER_RADIUS_METERS) {
                // دخل المستخدم منطقة خطر جديدة - ابعت تنبيه تلقائي
                insideZoneIds.add(zone.id)
                triggerAutoDangerZoneAlert(zone.description)
            } else if (alreadyInside && distance > DANGER_ZONE_EXIT_RADIUS_METERS) {
                // المستخدم خرج من المنطقة - يقدر ياخد تنبيه تاني لو رجع دخلها
                insideZoneIds.remove(zone.id)
            }
        }
    }

    private fun triggerAutoDangerZoneAlert(zoneDescription: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("LocationService", "تعذر إرسال تنبيه منطقة الخطر: لا يوجد مستخدم مسجل دخول")
            return
        }

        val serviceIntent = Intent(this, SosBackgroundService::class.java).apply {
            action = "TRIGGER_DANGER_ZONE_ACTION"
            putExtra("USER_ID", userId)
            putExtra("ZONE_DESCRIPTION", zoneDescription)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        Log.d("LocationService", "تم إرسال تنبيه دخول منطقة خطر: $zoneDescription")
    }

    override fun onDestroy() {
        locationManager.removeUpdates(listener)
        dangerZonesListener?.let { dangerZonesRef.removeEventListener(it) }
        dangerZonesListener = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}