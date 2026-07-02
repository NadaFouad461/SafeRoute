package com.example.saferoute.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import org.osmdroid.util.GeoPoint

object LocationRepository {

    private const val TAG = "LocationRepository"

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

    /**
     * Uploads the location using the currently authenticated user's UID.
     * If no user is signed in yet, we sign in anonymously first instead of
     * falling back to a shared constant UID (which caused all unauthenticated
     * users to overwrite each other's location record).
     */
    private fun uploadToFirebase(lat: Double, lon: Double) {
        val currentUid = auth.currentUser?.uid
        if (currentUid != null) {
            writeLocation(currentUid, lat, lon)
            return
        }

        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid != null) {
                    writeLocation(uid, lat, lon)
                } else {
                    Log.w(TAG, "Anonymous sign-in succeeded but returned no user; aborting share")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Anonymous sign-in failed, cannot share location", e)
                isSharingLocation = false
            }
    }

    private fun writeLocation(uid: String, lat: Double, lon: Double) {
        val locationData = mapOf(
            "latitude" to lat,
            "longitude" to lon,
            "timestamp" to System.currentTimeMillis()
        )

        database.getReference("locations/$uid")
            .setValue(locationData)
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to upload location for uid=$uid", e)
            }
    }

    fun stopSharing() {
        isSharingLocation = false
        val uid = auth.currentUser?.uid ?: return
        database.getReference("locations/$uid")
            .removeValue()
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to remove shared location for uid=$uid", e)
            }
    }

    fun getLastKnownLocation(): GeoPoint? = lastKnownLocation

    fun getLastKnownLatLng(): Pair<Double, Double>? {
        val loc = lastKnownLocation ?: return null
        return Pair(loc.latitude, loc.longitude)
    }

    fun hasLocation(): Boolean = lastKnownLocation != null
}