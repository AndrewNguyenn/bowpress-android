package com.andrewnguyen.bowpress.feature.session.threed

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

/**
 * Guards the singleton listener-cap fix: a `MotionAngleReader` must register
 * its sensor at most once across repeated `start()` calls and unregister at
 * most once across repeated `stop()` calls. If the idempotency guard
 * regresses, the per-process `SensorManager` listener map fills up after
 * enough course-screen entries and the 3D capture screen crashes with
 * `IllegalStateException` ("register failed, the sensor listeners size has
 * exceeded the maximum limit 128").
 */
class MotionAngleReaderTest {

    private fun reader(
        sensorManager: SensorManager?,
        sensor: Sensor? = mockk(relaxed = true),
    ): MotionAngleReader {
        val context = mockk<Context>(relaxed = true)
        every { context.getSystemService(Context.SENSOR_SERVICE) } returns sensorManager
        if (sensorManager != null) {
            every { sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) } returns sensor
            every { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns null
        }
        return MotionAngleReader(context)
    }

    @Test
    fun `start registers the sensor exactly once on the first call`() {
        val sm = mockk<SensorManager>(relaxed = true)
        val reader = reader(sm)

        reader.start()

        verify(exactly = 1) { sm.registerListener(any(), any<Sensor>(), any<Int>()) }
    }

    @Test
    fun `start is idempotent — second call does not re-register`() {
        val sm = mockk<SensorManager>(relaxed = true)
        val reader = reader(sm)

        reader.start()
        reader.start()

        verify(exactly = 1) { sm.registerListener(any(), any<Sensor>(), any<Int>()) }
    }

    @Test
    fun `stop without a prior start is a safe no-op`() {
        val sm = mockk<SensorManager>(relaxed = true)
        val reader = reader(sm)

        reader.stop()

        verify(exactly = 0) { sm.unregisterListener(any<android.hardware.SensorEventListener>()) }
    }

    @Test
    fun `stop is idempotent — second call does not double-unregister`() {
        val sm = mockk<SensorManager>(relaxed = true)
        val reader = reader(sm)

        reader.start()
        reader.stop()
        reader.stop()

        verify(exactly = 1) { sm.unregisterListener(any<android.hardware.SensorEventListener>()) }
    }

    @Test
    fun `start after stop re-registers — singleton can drive a second course`() {
        val sm = mockk<SensorManager>(relaxed = true)
        val reader = reader(sm)

        reader.start()
        reader.stop()
        reader.start()

        verify(exactly = 2) { sm.registerListener(any(), any<Sensor>(), any<Int>()) }
    }

    @Test
    fun `isAvailable is false when no gravity or accelerometer sensor exists`() {
        val sm = mockk<SensorManager>(relaxed = true)
        every { sm.getDefaultSensor(Sensor.TYPE_GRAVITY) } returns null
        every { sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns null
        val context = mockk<Context>(relaxed = true)
        every { context.getSystemService(Context.SENSOR_SERVICE) } returns sm

        val reader = MotionAngleReader(context)

        assertThat(reader.isAvailable).isFalse()
    }

    @Test
    fun `start with no available sensor does not register`() {
        val sm = mockk<SensorManager>(relaxed = true)
        every { sm.getDefaultSensor(Sensor.TYPE_GRAVITY) } returns null
        every { sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns null
        val context = mockk<Context>(relaxed = true)
        every { context.getSystemService(Context.SENSOR_SERVICE) } returns sm

        val reader = MotionAngleReader(context)
        reader.start()

        verify(exactly = 0) { sm.registerListener(any(), any<Sensor>(), any<Int>()) }
    }
}
