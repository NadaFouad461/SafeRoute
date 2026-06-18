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

class FallDetector @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorEventListener {

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _fallEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val fallEvent = _fallEvent.asSharedFlow()

    // ✅ الـ Thresholds الصح
    private val FALL_THRESHOLD = 3.5f      // gForce — اللحظة اللي بيسقط فيها
    private val IMPACT_THRESHOLD = 1.5f    // gForce — بعد ما يستقر على الأرض
    private val COOLDOWN_TIME_MS = 1_000L // 10 ثواني بين كل كشف وتاني

    // Two-Phase Detection
    private var fallDetectedTime: Long = 0
    private var waitingForImpact = false
    private val IMPACT_WINDOW_MS = 1500L   // 1.5 ثانية عشان يستقر

    private var lastFallTime: Long = 0

    fun startEventListeners() {
        accelerometer?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    fun stopEventListeners() {
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val magnitude = sqrt((x * x + y * y + z * z).toDouble())
        val gForce = (magnitude / SensorManager.GRAVITY_EARTH).toFloat()

        val now = System.currentTimeMillis()

        // ✅ المرحلة الأولى — اكتشاف الـ Spike
        if (!waitingForImpact && gForce > FALL_THRESHOLD) {
            if (now - lastFallTime > COOLDOWN_TIME_MS) {
                waitingForImpact = true
                fallDetectedTime = now
            }
            return
        }

        // ✅ المرحلة الثانية — تأكيد السقوط بعد الاستقرار
        if (waitingForImpact) {
            val timeSinceFall = now - fallDetectedTime

            // لو في الـ window وقيمة الـ gForce واطية = استقر على الأرض
            if (timeSinceFall in 300..IMPACT_WINDOW_MS.toInt() && gForce < IMPACT_THRESHOLD) {
                waitingForImpact = false
                lastFallTime = now
                _fallEvent.tryEmit(Unit) // ✅ سقوط حقيقي مؤكد
                return
            }

            // لو الـ window خلصت ومش متأكدين = مش سقوط حقيقي
            if (timeSinceFall > IMPACT_WINDOW_MS) {
                waitingForImpact = false
            }
        }
    }
}