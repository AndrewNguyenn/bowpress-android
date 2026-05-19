package com.andrewnguyen.bowpress.feature.analytics.sessiondetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.PlotRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionEndRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionRepository
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.SessionEnd
import com.andrewnguyen.bowpress.core.model.ShootingSession
import com.andrewnguyen.bowpress.core.model.TargetFaceType
import com.andrewnguyen.bowpress.core.model.Zone
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for SessionDetailScreen. iOS `SessionDetailSheet` exposes the full
 * shot distribution + group stats; this VM stages the underlying data so
 * future iters can layer in the heatmap / stat chips / replot.
 */
data class SessionDetailUiState(
    val isLoading: Boolean = true,
    val sessionId: String = "",
    val arrows: List<ArrowPlot> = emptyList(),
    val ends: List<SessionEnd> = emptyList(),
    val faceType: TargetFaceType = TargetFaceType.TEN_RING,
) {
    val arrowCount: Int get() = arrows.size
    val endCount: Int get() = ends.size.coerceAtLeast(1)

    /**
     * Precision stats over the session's plotted arrows. Mirrors iOS
     * `PrecisionStats.compute` in HistoricalSessionsView.swift:
     *   meanDistMM = mean(sqrt(x² + y²)) × mmPerNorm
     *   groupSigmaMM = sqrt(mean((x - cx)² + (y - cy)²)) × mmPerNorm
     * where mmPerNorm = 20 / (119 / 735) ≈ 123.53 for the WA 40cm indoor face.
     */
    val precision: PrecisionStats?
        get() = PrecisionStats.from(arrows)
}

data class PrecisionStats(
    val centroidX: Double,
    val centroidY: Double,
    val meanDistMm: Double,
    val groupSigmaMm: Double,
) {
    /**
     * Compass arrow pointing from origin to centroid, matching iOS
     * `directionArrow`. plotY is screen-positive (south), so a positive
     * centroidY means the grouping sits low → ↓.
     */
    val directionArrow: String
        get() {
            val dist = kotlin.math.hypot(centroidX, centroidY)
            if (dist < 0.01) return "⊙"
            val adx = kotlin.math.abs(centroidX)
            val ady = kotlin.math.abs(centroidY)
            if (ady > adx * 2) return if (centroidY > 0) "↓" else "↑"
            if (adx > ady * 2) return if (centroidX > 0) "→" else "←"
            return when {
                centroidX > 0 && centroidY > 0 -> "↘"
                centroidX > 0 && centroidY < 0 -> "↗"
                centroidX < 0 && centroidY > 0 -> "↙"
                else -> "↖"
            }
        }

    companion object {
        const val MM_PER_NORM: Double = 20.0 / (119.0 / 735.0)

        fun from(arrows: List<ArrowPlot>): PrecisionStats? {
            val pts = arrows.mapNotNull { p ->
                val x = p.plotX
                val y = p.plotY
                if (x == null || y == null) null else x to y
            }
            if (pts.isEmpty()) return null
            val cx = pts.sumOf { it.first } / pts.size
            val cy = pts.sumOf { it.second } / pts.size
            val meanDist = kotlin.math.sqrt(
                pts.sumOf { it.first * it.first + it.second * it.second } / pts.size,
            )
            val sigma = kotlin.math.sqrt(
                pts.sumOf {
                    val dx = it.first - cx
                    val dy = it.second - cy
                    dx * dx + dy * dy
                } / pts.size,
            )
            return PrecisionStats(
                centroidX = cx,
                centroidY = cy,
                meanDistMm = meanDist * MM_PER_NORM,
                groupSigmaMm = sigma * MM_PER_NORM,
            )
        }
    }
}

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val plotRepo: PlotRepository,
    private val sessionEndRepo: SessionEndRepository,
    private val sessionRepo: SessionRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val sessionId: String = savedStateHandle["sessionId"] ?: ""

    private val _state = MutableStateFlow(SessionDetailUiState(isLoading = true, sessionId = sessionId))
    val state: StateFlow<SessionDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val arrows = plotRepo.getBySession(sessionId)
            val ends = sessionEndRepo.getBySession(sessionId)
            val faceType = sessionRepo.getById(sessionId)?.targetFaceType ?: TargetFaceType.TEN_RING
            _state.update {
                it.copy(isLoading = false, arrows = arrows, ends = ends, faceType = faceType)
            }
        }
    }

    /**
     * Replace an arrow's score/position. Mirrors iOS
     * `HistoricalSessionsView.replotArrow` — keeps the existing id, sessionId,
     * config/end refs, and timestamp; only ring/zone/plotX/plotY change.
     * The repo writes pendingSync = true so the sync worker propagates.
     */
    fun replotArrow(arrowId: String, ring: Int, zone: Zone, plotX: Double?, plotY: Double?) {
        viewModelScope.launch {
            val current = _state.value.arrows.firstOrNull { it.id == arrowId } ?: return@launch
            val updated = current.copy(ring = ring, zone = zone, plotX = plotX, plotY = plotY)
            plotRepo.savePlot(updated)
            _state.update { st ->
                st.copy(arrows = st.arrows.map { if (it.id == arrowId) updated else it })
            }
        }
    }

    /** Mirrors iOS `deleteArrow(id:)` — local + remote cleanup. */
    fun deleteArrow(arrowId: String) {
        viewModelScope.launch {
            val target = _state.value.arrows.firstOrNull { it.id == arrowId } ?: return@launch
            plotRepo.deletePlot(target)
            // iOS Fix #19: also decrement session.arrowCount so the Log row
            // header and the analytics aggregates don't double-count.
            sessionRepo.getById(sessionId)?.let { s ->
                if (s.arrowCount > 0) {
                    sessionRepo.saveSession(s.copy(arrowCount = s.arrowCount - 1))
                }
            }
            _state.update { st -> st.copy(arrows = st.arrows.filterNot { it.id == arrowId }) }
        }
    }
}
