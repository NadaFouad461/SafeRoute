package com.example.saferoute.data.repository

import org.osmdroid.util.GeoPoint

object LocationRepository {

    private var lastKnownLocation: GeoPoint? = null

    fun updateLocation(lat: Double, lon: Double) {
        lastKnownLocation = GeoPoint(lat, lon)
    }

    fun getLastKnownLocation(): GeoPoint? {
        return lastKnownLocation
    }

    fun getLastKnownLatLng(): Pair<Double, Double>? {
        val loc = lastKnownLocation ?: return null
        return Pair(loc.latitude, loc.longitude)
    }

    fun hasLocation(): Boolean = lastKnownLocation != null
}