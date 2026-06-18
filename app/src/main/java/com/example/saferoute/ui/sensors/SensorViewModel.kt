package com.example.saferoute.ui.sensors

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.saferoute.services.FallDetectionService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject


@HiltViewModel
class SensorViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _isDetectionActive = MutableLiveData(false)
    val isDetectionActive: LiveData<Boolean> = _isDetectionActive

    fun startDetection() {
        _isDetectionActive.value = true
        val intent = Intent(context, FallDetectionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopDetection() {
        _isDetectionActive.value = false
        context.stopService(Intent(context, FallDetectionService::class.java))
    }
}