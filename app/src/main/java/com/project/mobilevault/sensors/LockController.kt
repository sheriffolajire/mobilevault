package com.project.mobilevault.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlin.math.sqrt

/**
 * Listens to accelerometer to trigger an app lock when:
 * - Device is placed face-down (z acceleration below threshold), or
 * - Device has been still for a configured duration.
 */
class LockController(
    context: Context,
    private val onLock: () -> Unit,
    private val stillnessMs: Long = 30_000L,
    private val faceDownZ: Float = -7.0f
) : SensorEventListener {

    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accel: Sensor? = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    @Volatile private var lastActive = SystemClock.elapsedRealtime()

    fun start() {
        accel?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        lastActive = SystemClock.elapsedRealtime()
    }

    fun stop() {
        sm.unregisterListener(this)
    }

    fun notifyUserInteraction() {
        lastActive = SystemClock.elapsedRealtime()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val now = SystemClock.elapsedRealtime()

        // Face-down quick lock
        if (z < faceDownZ) {
            onLock()
            return
        }

        // Stillness detection – update lastActive if we see meaningful motion
        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        // Gravity ~9.81; significant deviation implies movement
        if (magnitude > 10.3f || magnitude < 9.3f) {
            lastActive = now
        }

        // Extended stillness → lock
        if (now - lastActive > stillnessMs) {
            onLock()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* no-op */ }
}