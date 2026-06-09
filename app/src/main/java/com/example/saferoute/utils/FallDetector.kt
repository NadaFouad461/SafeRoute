package com.example.saferoute.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import kotlin.math.sqrt

class FallDetector@Inject constructor(
   @ApplicationContext private val context: Context
): SensorEventListener {

    private  val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private  val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _fallEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val fallEvent = _fallEvent.asSharedFlow()

    private var FALL_THRESHOLD = 3.0f

    private var lastFallTime: Long = 0
    private val COOLDOWN_TIME_MS = 10000

    fun startEventListeners() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

    }

    fun stopEventListeners() {
        sensorManager.unregisterListener(this)
    }


    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val magnitude = sqrt((x * x + y * y + z * z).toDouble())
            val gForce = magnitude.toFloat() / SensorManager.GRAVITY_EARTH

            if (gForce > FALL_THRESHOLD && System.currentTimeMillis() - lastFallTime > COOLDOWN_TIME_MS) {
                lastFallTime = System.currentTimeMillis()
                _fallEvent.tryEmit(Unit)
            }
        }
    }
}