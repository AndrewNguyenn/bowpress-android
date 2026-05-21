package com.andrewnguyen.bowpress.feature.session.threed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.CourseStationRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionRepository
import com.andrewnguyen.bowpress.core.model.CourseStation
import com.andrewnguyen.bowpress.core.model.ShootingSession
import com.andrewnguyen.bowpress.core.model.ThreeDScoringSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Read-only detail of a finished 3D course, shown from the Log. */
data class ThreeDLogDetailUiState(
    val isLoading: Boolean = true,
    val session: ShootingSession? = null,
    val stations: List<CourseStation> = emptyList(),
    val elevationGrid: ElevationGrid? = null,
) {
    val scoringSystem: ThreeDScoringSystem get() = session?.scoringSystem ?: ThreeDScoringSystem.ASA
    val totalScore: Int get() = stations.sumOf { it.ring }
    val averageScore: Double
        get() = if (stations.isEmpty()) 0.0 else totalScore.toDouble() / stations.size
    val killCount: Int
        get() = scoringSystem.rings.take(2).toSet().let { top -> stations.count { it.ring in top } }
    val isCleanRound: Boolean get() = stations.isNotEmpty() && stations.all { it.ring > 5 }
}

/**
 * Backs `ThreeDLogDetailScreen`. Hydrates `course_stations` from the server
 * first (the row may have been walked on another device), then observes the
 * local table — the 3D counterpart of how the range detail reads plots.
 */
@HiltViewModel
class ThreeDLogDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepo: SessionRepository,
    private val courseStationRepo: CourseStationRepository,
) : ViewModel() {

    private val sessionId: String = savedStateHandle["sessionId"] ?: ""

    private val _uiState = MutableStateFlow(ThreeDLogDetailUiState())
    val uiState: StateFlow<ThreeDLogDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val session = sessionRepo.getById(sessionId)
            _uiState.update { it.copy(session = session, isLoading = false) }
        }
        // Pull any stations logged on another device before observing locally.
        viewModelScope.launch {
            runCatching { courseStationRepo.refreshForSession(sessionId) }
        }
        viewModelScope.launch {
            courseStationRepo.observeBySession(sessionId).collect { stations ->
                _uiState.update { it.copy(stations = stations) }
                // Best-effort terrain contours, anchored at the first station.
                if (_uiState.value.elevationGrid == null) {
                    stations.firstOrNull { it.latitude != null && it.longitude != null }?.let { s ->
                        val grid = ElevationService.fetchGrid(s.latitude!!, s.longitude!!)
                        if (grid != null) _uiState.update { it.copy(elevationGrid = grid) }
                    }
                }
            }
        }
    }
}
