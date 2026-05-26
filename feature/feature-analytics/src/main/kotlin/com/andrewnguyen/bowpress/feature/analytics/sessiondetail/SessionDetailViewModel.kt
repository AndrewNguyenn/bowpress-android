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
                    distance = _state.value.distance,
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
        distance: ShootingDistance?,
    ): Pair<Double, Double> {
        val (inner, outer) = ringBand(ring, faceType, distance)
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
    private fun ringBand(
        ring: Int,
        faceType: TargetFaceType,
        distance: ShootingDistance?,
    ): Pair<Double, Double> {
        // §B3 — route through the distance-aware geometry so a 50/70m
        // sixRing session snaps to the 7-zone (Outdoor80) thresholds and
        // ring 5 lands at the right midline, instead of being clamped to
        // the Vegas 6-ring outer edge.
        val geometry = com.andrewnguyen.bowpress.feature.session.TargetGeometry
            .forFace(faceType, distance)
        return ringBandFromGeometry(ring, geometry)
    }

    /**
     * Derive the inner/outer band for a numbered scoring ring on a given
     * geometry. Walks the geometry's [thresholds] (innermost-first: X, then
     * the inner-most numeric ring, ..., outer-most). The same code path
     * correctly handles 6-zone, 7-zone, and 10-zone faces from a single
     * source — mirrors the `band(forRing:)` helper iOS uses (commit b53b748).
     */
    private fun ringBandFromGeometry(
        ring: Int,
        geometry: com.andrewnguyen.bowpress.feature.session.TargetGeometry,
    ): Pair<Double, Double> {
        if (ring >= 11) return 0.0 to geometry.thresholds[0]
        val innermost = geometry.innermostNumericRing
        val outermost = geometry.outerRingValue
        if (ring !in outermost..innermost) {
            // Out of range for this face — treat as a miss. Park the dot
            // just past the FULL canvas edge (1.0..1.05), not just past
            // the outer scoring band — matches the legacy ringBand
            // behavior, which always landed misses at the canvas edge
            // regardless of where the outer scoring ring sat (Vegas's
            // outer ring 6 sits at 0.808, well inside 1.0).
            return 1.0 to 1.05
        }
        // ring = innermost - (i - 1) ⇒ i = innermost - ring + 1.
        val i = innermost - ring + 1
        val inner = geometry.thresholds[i - 1]
        val outer = geometry.thresholds[i]
        return inner to outer
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
