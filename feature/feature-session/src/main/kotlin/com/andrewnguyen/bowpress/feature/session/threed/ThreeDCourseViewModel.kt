package com.andrewnguyen.bowpress.feature.session.threed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.CourseStationRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionRepository
import com.andrewnguyen.bowpress.core.model.CourseStation
import com.andrewnguyen.bowpress.core.model.SessionType
import com.andrewnguyen.bowpress.core.model.ShootingSession
import com.andrewnguyen.bowpress.core.model.ThreeDScoringSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/** UI state for a live 3D course. */
data class ThreeDCourseUiState(
    val session: ShootingSession? = null,
    val stations: List<CourseStation> = emptyList(),
    val elevationGrid: ElevationGrid? = null,
    val autoEnded: AutoEndReason? = null,
) {
    val isCourseActive: Boolean get() = session != null
    val scoringSystem: ThreeDScoringSystem get() = session?.scoringSystem ?: ThreeDScoringSystem.ASA
    val nextStationNumber: Int get() = stations.size + 1
    val totalScore: Int get() = stations.sumOf { it.ring }
    val averageScore: Double
        get() = if (stations.isEmpty()) 0.0 else totalScore.toDouble() / stations.size
    val killCount: Int
        get() {
            val top = scoringSystem.rings.take(2).toSet()
            return stations.count { it.ring in top }
        }
    val isCleanRound: Boolean get() = stations.isNotEmpty() && stations.all { it.ring > 5 }
}

/** Why a course was auto-ended — mirrors iOS `ThreeDCourseViewModel.AutoEndReason`. */
enum class AutoEndReason(val note: String) {
    DURATION("Course auto-ended — 18-hour session limit reached."),
    DISTANCE("Course auto-ended — 26-mile distance limit reached."),
}

/**
 * Mirrors iOS `ThreeDCourseViewModel`. Drives a live 3D course — observes the
 * active 3D session row and its [CourseStation]s, owns the GPS + motion
 * trackers, fetches the terrain grid, and applies the auto-end safety caps.
 *
 * The session row itself is created by `SessionViewModel.startThreeDCourse`;
 * this view model takes over once a 3D session is active.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ThreeDCourseViewModel @Inject constructor(
    @ApplicationContext private val appContext: android.content.Context,
    private val sessionRepo: SessionRepository,
    private val courseStationRepo: CourseStationRepository,
) : ViewModel() {

    val locationTracker = CourseLocationTracker(appContext)
    val motionReader = MotionAngleReader(appContext)

    private val _uiState = MutableStateFlow(ThreeDCourseUiState())
    val uiState: StateFlow<ThreeDCourseUiState> = _uiState.asStateFlow()

    private var trackersRunning = false

    init {
        val active3d = sessionRepo.observeActiveSession()
            .map { it?.takeIf { s -> s.sessionType == SessionType.THREE_D_COURSE } }
            .distinctUntilChanged()

        viewModelScope.launch {
            active3d.collect { session ->
                _uiState.update { it.copy(session = session) }
                if (session != null && !trackersRunning) {
                    trackersRunning = true
                    locationTracker.start()
                    motionReader.start()
                } else if (session == null && trackersRunning) {
                    trackersRunning = false
                    locationTracker.stop()
                    motionReader.stop()
                }
            }
        }

        viewModelScope.launch {
            active3d.flatMapLatest { session ->
                if (session != null) courseStationRepo.observeBySession(session.id)
                else flowOf(emptyList())
            }.collect { stations ->
                _uiState.update { it.copy(stations = stations) }
            }
        }

        // First GPS fix → fetch the terrain elevation grid for the contours.
        viewModelScope.launch {
            locationTracker.hasFix.collect { fix ->
                if (fix && _uiState.value.elevationGrid == null && _uiState.value.isCourseActive) {
                    locationTracker.current.value?.let { p ->
                        val grid = ElevationService.fetchGrid(p.latitude, p.longitude)
                        if (grid != null) _uiState.update { it.copy(elevationGrid = grid) }
                    }
                }
            }
        }

        // Auto-end safety caps — checked once a second while a course is live.
        viewModelScope.launch {
            while (true) {
                val s = _uiState.value
                if (s.isCourseActive && s.autoEnded == null) {
                    val elapsed = s.session?.startedAt?.let {
                        Instant.now().epochSecond - it.epochSecond
                    } ?: 0L
                    val reason = autoEndReason(elapsed, locationTracker.distanceMeters.value)
                    if (reason != null) {
                        _uiState.update { it.copy(autoEnded = reason) }
                        finishCourse(reason.note)
                    }
                }
                delay(1_000)
            }
        }
    }

    /** The cap that's been crossed, or null while both are within limits. */
    fun autoEndReason(elapsedSeconds: Long, distanceMeters: Double): AutoEndReason? = when {
        elapsedSeconds >= MAX_COURSE_DURATION_SECONDS -> AutoEndReason.DURATION
        distanceMeters >= MAX_COURSE_DISTANCE_METERS -> AutoEndReason.DISTANCE
        else -> null
    }

    /** A fresh station id — generated when the archer taps Shoot. */
    fun newStationId(): String = UUID.randomUUID().toString()

    /** Persist a freshly-shot station. */
    fun commitStation(
        id: String,
        estimatedDistance: Double?,
        distanceUnit: String?,
        angleDegrees: Double,
        bearingDegrees: Double?,
        latitude: Double?,
        longitude: Double?,
        ring: Int,
        plotX: Double?,
        plotY: Double?,
        hasScenePhoto: Boolean,
        hasArrowPhoto: Boolean,
    ) {
        val session = _uiState.value.session ?: return
        val station = CourseStation(
            id = id,
            sessionId = session.id,
            stationNumber = _uiState.value.nextStationNumber,
            estimatedDistance = estimatedDistance,
            distanceUnit = distanceUnit,
            angleDegrees = angleDegrees,
            bearingDegrees = bearingDegrees,
            latitude = latitude,
            longitude = longitude,
            ring = ring,
            plotX = plotX,
            plotY = plotY,
            hasScenePhoto = hasScenePhoto,
            hasArrowPhoto = hasArrowPhoto,
            shotAt = Instant.now(),
        )
        viewModelScope.launch { courseStationRepo.saveStation(station) }
    }

    /** Re-score / re-photo an already-logged station. */
    fun updateStation(
        existing: CourseStation,
        ring: Int,
        plotX: Double?,
        plotY: Double?,
        hasScenePhoto: Boolean,
        hasArrowPhoto: Boolean,
    ) {
        val updated = existing.copy(
            ring = ring, plotX = plotX, plotY = plotY,
            hasScenePhoto = hasScenePhoto, hasArrowPhoto = hasArrowPhoto,
        )
        viewModelScope.launch { courseStationRepo.saveStation(updated) }
    }

    /** Remove a station and renumber the rest contiguously. */
    fun deleteStation(station: CourseStation) {
        viewModelScope.launch {
            courseStationRepo.deleteStation(station)
            CourseStationPhotoStore.deleteAll(appContext, station.id)
            val remaining = _uiState.value.stations
                .filter { it.id != station.id }
                .sortedBy { it.stationNumber }
            remaining.forEachIndexed { index, s ->
                if (s.stationNumber != index + 1) {
                    courseStationRepo.saveStation(s.copy(stationNumber = index + 1))
                }
            }
        }
    }

    /** Sign the course off — the completed row flows to the Log. */
    fun finishCourse(notes: String = "") {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            sessionRepo.endSession(session.id, Instant.now(), notes)
        }
    }

    /** Abandon the course — delete the session row and its stations. */
    fun discardCourse() {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            _uiState.value.stations.forEach {
                CourseStationPhotoStore.deleteAll(appContext, it.id)
            }
            sessionRepo.deleteSession(session.id)
        }
    }

    override fun onCleared() {
        locationTracker.stop()
        motionReader.stop()
    }

    companion object {
        const val MAX_COURSE_DURATION_SECONDS = 18L * 60 * 60
        const val MAX_COURSE_DISTANCE_METERS = 26.0 * 1609.344
    }
}
