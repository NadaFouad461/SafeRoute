package com.example.saferoute.ui.start

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.saferoute.services.FallDetectionService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject


@HiltViewModel
class PermissionsViewModel@Inject constructor(
    @ApplicationContext private val context: Context,
    private val sharedPrefs: SharedPreferences
): ViewModel() {

    private val _permissionsGranted = MutableLiveData<Boolean>()
    val permissionsGranted: LiveData<Boolean> = _permissionsGranted


    init {

        val isSavedActive = sharedPrefs.getBoolean("IS_FALL_DETECTION_ACTIVE", false)
        _permissionsGranted.value = isSavedActive

        if (isSavedActive) {
            startServiceIntent()
        }
    }

    fun startDetection() {
        _permissionsGranted.value = true

        sharedPrefs.edit().putBoolean("IS_FALL_DETECTION_ACTIVE", true).apply()
        startServiceIntent()
    }

    fun stopDetection() {
        _permissionsGranted.value = false

        sharedPrefs.edit().putBoolean("IS_FALL_DETECTION_ACTIVE", false).apply()
        context.stopService(Intent(context, FallDetectionService::class.java))
    }

    // Helper method to start the FallDetectionService
    private fun startServiceIntent() {
        val intent = Intent(context, FallDetectionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}