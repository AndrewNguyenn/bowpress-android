package com.andrewnguyen.bowpress.feature.session.threed

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mirrors iOS `CourseLocationTracker`. Continuous GPS tracker for a live 3D
 * course — accumulates a downsampled breadcrumb of the walked route and the
 * live compass heading. Tuned for a multi-hour wooded walk: coarse accuracy,
 * a small distance filter.
 *
 * On an emulator without a simulated location, `hasFix` stays false and the
 * course map falls back to a synthesized layout.
 */
class CourseLocationTracker(private val context: Context) : LocationListener, SensorEventListener {

    private val _breadcrumb = MutableStateFlow<List<GeoPoint>>(emptyList())
    val breadcrumb: StateFlow<List<GeoPoint>> = _breadcrumb.asStateFlow()

    private val _current = MutableStateFlow<GeoPoint?>(null)
    val current: StateFlow<GeoPoint?> = _current.asStateFlow()

    private val _hasFix = MutableStateFlow(false)
    val hasFix: StateFlow<Boolean> = _hasFix.asStateFlow()

    /** Live compass heading, 0…360 clockwise from true north. Null until known. */
    private val _heading = MutableStateFlow<Double?>(null)
    val heading: StateFlow<Double?> = _heading.asStateFlow()

    /** Cumulative distance walked, in metres. */
    private val _distanceMeters = MutableStateFlow(0.0)
    val distanceMeters: StateFlow<Double> = _distanceMeters.asStateFlow()

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private var lastRawLocation: Location? = null
    private var tracking = false

    /** A breadcrumb point is appended only after moving this far (metres). */
    private val minBreadcrumbSpacing = 3.0

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun start() {
        if (tracking) return
        tracking = true
        if (hasLocationPermission()) {
            runCatching {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 1_000L, 5f, this,
                )
            }
        }
        val rotation = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotation != null) {
            sensorManager.registerListener(this, rotation, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        tracking = false
        runCatching { locationManager?.removeUpdates(this) }
        sensorManager?.unregisterListener(this)
    }

    // ---- LocationListener ----

    override fun onLocationChanged(location: Location) {
        if (location.hasAccuracy() && location.accuracy >= 100f) return
        val point = GeoPoint(location.latitude, location.longitude)
        _current.value = point
        _hasFix.value = true
        val last = lastRawLocation
        if (last != null) {
            val step = location.distanceTo(last)
            if (step >= minBreadcrumbSpacing) {
                _distanceMeters.value += step
                _breadcrumb.value = _breadcrumb.value + point
                lastRawLocation = location
            }
        } else {
            _breadcrumb.value = _breadcrumb.value + point
            lastRawLocation = location
        }
    }

    @Deprecated("Required by the LocationListener interface")
    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
    override fun onProviderEnabled(provider: String) = Unit
    override fun onProviderDisabled(provider: String) = Unit

    // ---- SensorEventListener (compass) ----

    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientation)
        // azimuth is radians, -π…π counter-clockwise from north → 0…360 cw.
        val degrees = (Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0
        _heading.value = degrees
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
