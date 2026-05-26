package com.andrewnguyen.bowpress.feature.session.threed

import com.andrewnguyen.bowpress.core.designsystem.coursemap.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.CourseStationRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionRepository
import com.andrewnguyen.bowpress.core.data.social.SocialSessionSharer
import com.andrewnguyen.bowpress.core.designsystem.photo.TargetPhotoStore
import com.andrewnguyen.bowpress.core.model.CourseStation
import com.andrewnguyen.bowpress.core.model.SessionType
import com.andrewnguyen.bowpress.core.model.ShareSessionBody
import com.andrewnguyen.bowpress.core.model.ShootingSession
import com.andrewnguyen.bowpress.core.model.SocialVisibility
import com.andrewnguyen.bowpress.core.model.ThreeDScoringSystem
import com.andrewnguyen.bowpress.feature.session.FinishAudience
import com.andrewnguyen.bowpress.feature.session.FinishExtras
import com.andrewnguyen.bowpress.feature.session.ShareOutcome
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
    /**
     * Non-null when the most recent C1 finish-sheet share landed the initial
     * POST but had at least one best-effort step fail. The MainScaffold-level
     * Snackbar renders this string and clears it via
     * [ThreeDCourseViewModel.consumeSharePartialFailure]. Mirrors iOS
     * `ThreeDCourseViewModel.lastSharePartialFailure`.
     */
    val lastSharePartialFailure: String? = null,
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
    private val socialSessionSharer: SocialSessionSharer,
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
                // Clearing `autoEnded` when a session is present stops a stale
                // auto-end flag from a previous course bleeding into the next
                // one (this VM outlives a single course).
                _uiState.update {
                    it.copy(session = session, autoEnded = if (session != null) null else it.autoEnded)
                }
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

    /**
     * C1 finish-sheet entry point — applies title + description + location +
     * photos, runs the course-aware share (or skips on private audience), and
     * publishes any partial-failure hint through [ThreeDCourseUiState.lastSharePartialFailure].
     *
     * 3D scoring doesn't fit the range model's `min(ring, 10)` / `ring == 11
     * ↔ X` rules — an ASA 14 would silently cap to 10, and an IBO 11 (the
     * inner X) would be double-counted as score+X. We pass `totalScore` /
     * `stations.count` / xCount=0 through directly so the post reflects what
     * the archer actually saw on the review screen. Mirrors iOS
     * `ThreeDCourseViewModel.finishCourse(extras:)` +
     * `SessionViewModel.shareCompletedCourse(...)`.
     */
    fun finishCourse(extras: FinishExtras) {
        val session = _uiState.value.session ?: return
        val state = _uiState.value
        // Snapshot the totals up front so a stale reactive emission can't
        // change the numbers between the share POST and any retry. iOS does
        // the same with shareSnapshot / shareArrows.
        val totalScore = state.totalScore
        val stationCount = state.stations.size
        viewModelScope.launch {
            // First photo doubles as the session's local target-paper image
            // — the Log thumbnail listens to TargetPhotoStore so the
            // historical row keeps a recognisable thumbnail. Done before the
            // end-session call so by the time onSessionEnded fires, the file
            // exists. Mirrors iOS FinishSheet.
            extras.photoData.firstOrNull()?.let { bytes ->
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    TargetPhotoStore.save(appContext, bytes, session.id)
                }
            }
            // Persist title + notes (description) + endedAt — same writes
            // the legacy alert path did, just with the extras values.
            val notes = extras.description.trim()
            val trimmedTitle = extras.title.trim().takeIf { it.isNotBlank() }
            runCatching { sessionRepo.setTitle(session.id, trimmedTitle) }
            sessionRepo.endSession(session.id, Instant.now(), notes)

            // Audience gate. Private → skip the share entirely; the session
            // still lands in the archer's log, nobody else sees it.
            if (extras.audience.shouldShare) {
                _uiState.update { it.copy(lastSharePartialFailure = null) }
                val outcome = socialSessionSharer.shareWithExtras(
                    sessionId = session.id,
                    score = totalScore,
                    // xCount=0 for 3D — the scoring systems treat the inner
                    // ring as a value (ASA 14 / IBO 11 / WA3D 10) rather than
                    // a separate X tally, so adding it here would double-
                    // count.
                    xCount = 0,
                    arrowCount = stationCount,
                    distance = null,
                    face = null,
                    title = trimmedTitle ?: session.title,
                    shotAt = session.startedAt,
                    location = extras.location,
                    description = notes,
                    photoData = extras.photoData,
                )
                if (outcome != null && outcome.hasPartialFailure) {
                    val asOutcome = ShareOutcome(
                        sharedSessionId = outcome.sharedSessionId,
                        descriptionSucceeded = outcome.descriptionSucceeded,
                        photosUploaded = outcome.photosUploaded,
                        photosAttempted = outcome.photosAttempted,
                    )
                    _uiState.update { it.copy(lastSharePartialFailure = asOutcome.partialFailureMessage) }
                }
            }
        }
    }

    /** Clear the partial-share failure hint once the user has seen it. */
    fun consumeSharePartialFailure() {
        _uiState.update { it.copy(lastSharePartialFailure = null) }
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
