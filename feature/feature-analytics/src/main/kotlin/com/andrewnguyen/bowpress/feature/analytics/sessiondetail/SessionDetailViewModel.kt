package com.andrewnguyen.bowpress.feature.analytics.sessiondetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.PlotRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionEndRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionRepository
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.Scorecard
import com.andrewnguyen.bowpress.core.model.SessionEnd
import com.andrewnguyen.bowpress.core.model.ShootingDistance
import com.andrewnguyen.bowpress.core.model.TargetFaceType
import com.andrewnguyen.bowpress.core.model.TargetLayout
import com.andrewnguyen.bowpress.core.model.Zone
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for SessionDetailScreen. Mirrors the data iOS `SessionDetailSheet`
 * renders for the Kenrokuen redesign: the title-header strings, the per-end
 * scorecard, and the plotted shot distribution.
 */
data class SessionDetailUiState(
    val isLoading: Boolean = true,
    val sessionId: String = "",
    val arrows: List<ArrowPlot> = emptyList(),
    val ends: List<SessionEnd> = emptyList(),
    val faceType: TargetFaceType = TargetFaceType.TEN_RING,
    val targetLayout: TargetLayout = TargetLayout.SINGLE,
    /** Session title, falling back to the distance label — see iOS `sessionDisplayTitle`. */
    val title: String? = null,
    val distance: ShootingDistance? = null,
    val notes: String = "",
    val feelTags: List<String> = emptyList(),
) {
    val arrowCount: Int get() = arrows.size

    /** Per-end breakdown — every arrow lands in exactly one line (see [Scorecard]). */
    val scorecard: Scorecard get() = Scorecard.build(arrows, ends, sessionId)

    /** Title shown in the header — the session's own name, or its distance. */
    val displayTitle: String
        get() = title?.trim()?.takeIf { it.isNotEmpty() }
            ?: distance?.label
            ?: "Session"
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
            val session = sessionRepo.getById(sessionId)
            _state.update {
                it.copy(
                    isLoading = false,
                    arrows = arrows,
                    ends = ends,
                    faceType = session?.targetFaceType ?: TargetFaceType.TEN_RING,
                    targetLayout = session?.targetLayout ?: TargetLayout.SINGLE,
                    title = session?.title,
                    distance = session?.distance,
                    notes = session?.notes.orEmpty(),
                    feelTags = session?.feelTags.orEmpty(),
                )
            }
        }
    }

    /**
     * Replace an arrow's score/position. Mirrors iOS
     * `HistoricalSessionsView.replotArrow` — keeps the existing id, sessionId,
     * config/end refs, and timestamp; only ring/zone/plotX/plotY change.
     *
     * When [plotX]/[plotY] are passed null (the keypad re-score path with no
     * positional information), snap the plot to the **midline** of the new
     * ring along the existing bearing — without this, precision stats
     * (meanDist, group σ) would compute from a position that doesn't
     * correspond to the score, silently desyncing the heatmap from the
     * displayed ring.
     */
    fun replotArrow(arrowId: String, ring: Int, zone: Zone, plotX: Double?, plotY: Double?) {
        viewModelScope.launch {
            val current = _state.value.arrows.firstOrNull { it.id == arrowId } ?: return@launch
            val (snappedX, snappedY) = if (plotX != null && plotY != null) {
                plotX to plotY
            } else {
                snapToRingMidline(
                    ring = ring,
                    currentX = current.plotX,
                    currentY = current.plotY,
                    faceType = _state.value.faceType,
                )
            }
            val updated = current.copy(
                ring = ring,
                zone = zone,
                plotX = snappedX,
                plotY = snappedY,
            )
            plotRepo.savePlot(updated)
            _state.update { st ->
                st.copy(arrows = st.arrows.map { if (it.id == arrowId) updated else it })
            }
        }
    }

    /**
     * Snap (plotX, plotY) to the geometric midpoint of [ring]'s band along
     * the current bearing. Falls back to a straight-down bearing when the
     * current plot has no position (so the dot lands somewhere visible on
     * the heatmap rather than at origin).
     */
    private fun snapToRingMidline(
        ring: Int,
        currentX: Double?,
        currentY: Double?,
        faceType: TargetFaceType,
    ): Pair<Double, Double> {
        val (inner, outer) = ringBand(ring, faceType)
        val midRadius = (inner + outer) / 2.0
        val curX = currentX ?: 0.0
        val curY = currentY ?: 1.0 // arbitrary south bearing
        val curMag = kotlin.math.hypot(curX, curY)
        return if (curMag < 1e-6) {
            0.0 to midRadius
        } else {
            midRadius * curX / curMag to midRadius * curY / curMag
        }
    }

    /**
     * Inner / outer normalised radius for a given ring on the given face.
     * Numbers come from the same threshold tables `TargetGeometry` uses for
     * classification — so a re-score that lands at the midline classifies
     * back to the same ring on the next render.
     */
    private fun ringBand(ring: Int, faceType: TargetFaceType): Pair<Double, Double> {
        return when (faceType) {
            TargetFaceType.SIX_RING -> when (ring) {
                11 -> 0.0 to 60.0 / 735.0           // X
                10 -> 60.0 / 735.0 to 119.0 / 735.0 // 10 / 9 divider
                9 -> 119.0 / 735.0 to 238.0 / 735.0
                8 -> 238.0 / 735.0 to 357.0 / 735.0
                7 -> 357.0 / 735.0 to 475.0 / 735.0
                6 -> 475.0 / 735.0 to 594.0 / 735.0
                else -> 594.0 / 735.0 to 1.05       // miss — just outside the face
            }
            TargetFaceType.TEN_RING -> when (ring) {
                11 -> 0.0 to 0.05
                10 -> 0.05 to 0.10
                9 -> 0.10 to 0.20
                8 -> 0.20 to 0.30
                7 -> 0.30 to 0.40
                6 -> 0.40 to 0.50
                5 -> 0.50 to 0.60
                4 -> 0.60 to 0.70
                3 -> 0.70 to 0.80
                2 -> 0.80 to 0.90
                1 -> 0.90 to 1.00
                else -> 1.00 to 1.05
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
