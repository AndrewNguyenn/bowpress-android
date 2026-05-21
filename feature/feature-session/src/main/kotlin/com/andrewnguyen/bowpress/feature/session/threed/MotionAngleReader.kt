package com.andrewnguyen.bowpress.feature.session.threed

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mirrors iOS `MotionAngleReader`. Live shot-incline reader for the 3D
 * capture screen — the elevation of the back camera's optical axis above
 * (positive) or below (negative) horizontal, derived from the gravity vector.
 *
 * On hardware without a gravity sensor `isAvailable` is false and the capture
 * screen falls back to a manual angle stepper.
 */
class MotionAngleReader(context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gravitySensor: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val isAvailable: Boolean = gravitySensor != null

    /** Current shot incline in degrees: positive uphill, negative downhill. */
    private val _angleDegrees = MutableStateFlow(0.0)
    val angleDegrees: StateFlow<Double> = _angleDegrees.asStateFlow()

    val roundedAngle: Int get() = Math.round(_angleDegrees.value).toInt()

    fun start() {
        val sensor = gravitySensor ?: return
        sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val elevation = ThreeDGeometry.elevationFromGravity(
            event.values[0], event.values[1], event.values[2],
        )
        // Light smoothing so the readout doesn't jitter on a held phone.
        _angleDegrees.value = _angleDegrees.value * 0.8 + elevation * 0.2
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
