package com.example.saferoute.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat

class LocationTrackingService(
    private val context: Context,
    private val onLocationUpdate: (lat: Double, lon: Double) -> Unit
) {

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val listener = object : LocationListener {

        override fun onLocationChanged(location: Location) {
            Log.d("LocationService", "New location: ${location.latitude}, ${location.longitude}")
            onLocationUpdate(location.latitude, location.longitude)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    fun start() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("LocationService", "Permission not granted, cannot start tracking")
            return
        }

        //  GPS Provider
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

        Log.d("LocationService", "Tracking started")
    }

    fun stop() {
        locationManager.removeUpdates(listener)
        Log.d("LocationService", "Tracking stopped")
    }
}