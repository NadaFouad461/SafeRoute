package com.example.saferoute.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import org.osmdroid.util.GeoPoint

object LocationRepository {

    private var lastKnownLocation: GeoPoint? = null
    var isSharingLocation: Boolean = false

    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun updateLocation(lat: Double, lon: Double) {
        lastKnownLocation = GeoPoint(lat, lon)
        if (isSharingLocation) {
            uploadToFirebase(lat, lon)
        }
    }

    private fun uploadToFirebase(lat: Double, lon: Double) {
        val uid = auth.currentUser?.uid ?: "test_user_123"

        val locationData = mapOf(
            "latitude" to lat,
            "longitude" to lon,
            "timestamp" to System.currentTimeMillis()
        )

        database.getReference("locations/$uid")
            .setValue(locationData)
    }

    fun stopSharing() {
        isSharingLocation = false
        val uid = auth.currentUser?.uid ?: return
        database.getReference("locations/$uid").removeValue()
    }

    fun getLastKnownLocation(): GeoPoint? = lastKnownLocation

    fun getLastKnownLatLng(): Pair<Double, Double>? {
        val loc = lastKnownLocation ?: return null
        return Pair(loc.latitude, loc.longitude)
    }

    fun hasLocation(): Boolean = lastKnownLocation != null
}