package com.example.saferoute.ui.map

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.saferoute.data.repository.LocationRepository
import org.osmdroid.util.GeoPoint

class MapViewModel : ViewModel() {

    private val _currentLocation = MutableLiveData<GeoPoint>()
    val currentLocation: LiveData<GeoPoint> = _currentLocation

    private val _latitude = MutableLiveData<Double>()
    val latitude: LiveData<Double> = _latitude

    private val _longitude = MutableLiveData<Double>()
    val longitude: LiveData<Double> = _longitude

    fun updateLocation(point: GeoPoint) {
        _currentLocation.value = point
        _latitude.value = point.latitude
        _longitude.value = point.longitude

        LocationRepository.updateLocation(
            point.latitude,
            point.longitude
        )
        Log.d(
            "REPOSITORY_TEST",
            LocationRepository.getLastKnownLatLng().toString()
        )
        Log.d("VM_LOCATION", "Updated: ${point.latitude}, ${point.longitude}")
    }

    fun getLastKnownCoordinates(): Pair<Double, Double>? {
        val lat = _latitude.value
        val lon = _longitude.value
        return if (lat != null && lon != null) Pair(lat, lon) else null
    }
}