package com.andrewnguyen.bowpress.feature.session.threed

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.andrewnguyen.bowpress.core.designsystem.coursemap.GeoPoint
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Guards the singleton listener-cap fix and the start-time state reset.
 * Same motivation as [MotionAngleReaderTest] — without idempotent start/stop
 * a per-VM tracker leaks `SensorEventListener` entries against the 128-cap
 * system map. Also covers that a singleton `start()` clears prior-course
 * breadcrumb / heading / distance so a second course doesn't inherit the
 * first one's route.
 */
class CourseLocationTrackerTest {

    @Before
    fun stubPermissionCheck() {
        // CourseLocationTracker.hasLocationPermission() calls
        // ContextCompat.checkSelfPermission — stub it so tests don't depend
        // on the JVM unit-test stub returning a fixed value.
        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(any(), any())
        } returns PackageManager.PERMISSION_DENIED
    }

    @After
    fun unstub() {
        unmockkStatic(ContextCompat::class)
    }

    private fun tracker(
        sensorManager: SensorManager? = mockk(relaxed = true),
        locationManager: LocationManager? = mockk(relaxed = true),
        rotationSensor: Sensor? = mockk(relaxed = true),
    ): CourseLocationTracker {
        val context = mockk<Context>(relaxed = true)
        every { context.getSystemService(Context.SENSOR_SERVICE) } returns sensorManager
        every { context.getSystemService(Context.LOCATION_SERVICE) } returns locationManager
        if (sensorManager != null) {
            every { sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) } returns rotationSensor
        }
        return CourseLocationTracker(context)
    }

    @Test
    fun `start registers the rotation sensor exactly once`() {
        val sm = mockk<SensorManager>(relaxed = true)
        every { sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) } returns mockk(relaxed = true)
        val tracker = tracker(sensorManager = sm)

        tracker.start()

        verify(exactly = 1) { sm.registerListener(any(), any<Sensor>(), any<Int>()) }
    }

    @Test
    fun `start is idempotent — second call does not re-register`() {
        val sm = mockk<SensorManager>(relaxed = true)
        every { sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) } returns mockk(relaxed = true)
        val tracker = tracker(sensorManager = sm)

        tracker.start()
        tracker.start()

        verify(exactly = 1) { sm.registerListener(any(), any<Sensor>(), any<Int>()) }
    }

    @Test
    fun `stop without a prior start is a safe no-op against the sensor manager`() {
        val sm = mockk<SensorManager>(relaxed = true)
        every { sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) } returns mockk(relaxed = true)
        val tracker = tracker(sensorManager = sm)

        tracker.stop()

        // unregisterListener fires unconditionally — that's harmless on an
        // unregistered listener, but the sensorsRegistered flag must stay
        // false so a later start() re-arms cleanly.
        tracker.start()
        verify(exactly = 1) { sm.registerListener(any(), any<Sensor>(), any<Int>()) }
    }

    @Test
    fun `start after stop re-registers — singleton can drive a second course`() {
        val sm = mockk<SensorManager>(relaxed = true)
        every { sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) } returns mockk(relaxed = true)
        val tracker = tracker(sensorManager = sm)

        tracker.start()
        tracker.stop()
        tracker.start()

        verify(exactly = 2) { sm.registerListener(any(), any<Sensor>(), any<Int>()) }
    }

    @Test
    fun `start wipes every prior-course state field so a fresh course starts empty`() {
        val tracker = tracker()

        // Simulate accumulated state from a previous course's run.
        val seedCrumb = listOf(GeoPoint(10.0, 20.0), GeoPoint(11.0, 21.0))
        tracker.seedBreadcrumb(seedCrumb)
        tracker.seedCurrent(GeoPoint(10.0, 20.0))
        tracker.seedHasFix(true)
        tracker.seedHeading(123.4)
        tracker.seedDistance(8_000.0)
        assertThat(tracker.breadcrumb.value).isEqualTo(seedCrumb)
        assertThat(tracker.distanceMeters.value).isEqualTo(8_000.0)

        tracker.start()

        assertThat(tracker.breadcrumb.value).isEmpty()
        assertThat(tracker.current.value).isNull()
        assertThat(tracker.hasFix.value).isFalse()
        assertThat(tracker.heading.value).isNull()
        // `distanceMeters` matters specifically because `ThreeDCourseViewModel`
        // reads it for the 26-mile auto-end safety cap — a non-zero carryover
        // would spuriously auto-end a fresh course.
        assertThat(tracker.distanceMeters.value).isEqualTo(0.0)
    }

    @Test
    fun `start does not wipe state when already tracking — defensive re-call preserves the route`() {
        val tracker = tracker()
        tracker.start()

        val mid = listOf(GeoPoint(1.0, 2.0))
        tracker.seedBreadcrumb(mid)
        tracker.seedDistance(150.0)

        // A second start() while tracking is already true must not wipe.
        tracker.start()

        assertThat(tracker.breadcrumb.value).isEqualTo(mid)
        assertThat(tracker.distanceMeters.value).isEqualTo(150.0)
    }

    // -- Reflective state seeders --
    // The tracker's StateFlows are written only by the LocationListener /
    // SensorEventListener callbacks; reflection is the cheapest way to
    // simulate accumulated state without faking the platform APIs. Matches
    // the project pattern (see `FeedViewModelTest`).

    private fun CourseLocationTracker.seedBreadcrumb(points: List<GeoPoint>) {
        seedFlow<List<GeoPoint>>("_breadcrumb", points)
    }

    private fun CourseLocationTracker.seedCurrent(point: GeoPoint?) {
        seedFlow<GeoPoint?>("_current", point)
    }

    private fun CourseLocationTracker.seedHasFix(value: Boolean) {
        seedFlow<Boolean>("_hasFix", value)
    }

    private fun CourseLocationTracker.seedHeading(value: Double?) {
        seedFlow<Double?>("_heading", value)
    }

    private fun CourseLocationTracker.seedDistance(meters: Double) {
        seedFlow<Double>("_distanceMeters", meters)
    }

    private fun <T> CourseLocationTracker.seedFlow(fieldName: String, value: T) {
        val field = CourseLocationTracker::class.java
            .getDeclaredField(fieldName).apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(this) as kotlinx.coroutines.flow.MutableStateFlow<T>
        flow.value = value
    }
}
