package com.andrewnguyen.bowpress.feature.analytics.sessiondetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.ArrowConfigRepository
import com.andrewnguyen.bowpress.core.data.repository.BowRepository
import com.andrewnguyen.bowpress.core.data.repository.PlotRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionEndRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionRepository
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.Scorecard
import com.andrewnguyen.bowpress.core.model.SessionEnd
import com.andrewnguyen.bowpress.core.model.ShootingDistance
import com.andrewnguyen.bowpress.core.model.TargetFaceType
import com.andrewnguyen.bowpress.core.model.TargetLayout
import com.andrewnguyen.bowpress.feature.session.TargetGeometry
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
    /**
     * Shaft outside diameter (mm) of the arrow config the session was shot
     * with — drives the plot-dot size on the multi-spot `BPPlottedTarget`
     * render so the dot represents the real shaft against the printed Vegas
     * spot. Null when the session has no arrow config, or the config has
     * no shaft diameter recorded → BPPlottedTarget falls back to 6mm.
     */
    val arrowDiameterMm: Double? = null,
    /**
     * Equipment-inline strip — bow name + canonical [com.andrewnguyen.bowpress.core.model.BowType]
     * + arrow set name, resolved off local bow + arrow_configurations
     * rows. Each is independently nullable; the rendered strip drops
     * missing fields along with their separator.
     */
    val bowName: String? = null,
    val bowType: com.andrewnguyen.bowpress.core.model.BowType? = null,
    val arrowName: String? = null,
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
    private val bowRepo: BowRepository,
    private val arrowConfigRepo: ArrowConfigRepository,
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
            // Equipment-inline strip — resolve the bow + arrow_config
            // rows the session was shot on. Either lookup is a quick
            // local DAO read; null when the row was deleted or the id
            // points nowhere, in which case the strip drops the span.
            // The arrow_config doubles as the source of the shaft
            // diameter that drives BPPlottedTarget's per-shaft dot size.
            //
            // NOTE: one-shot read — matches the surrounding fields
            // (notes, feel tags, etc.). A bow / arrow rename in
            // Equipment while this screen is open won't reflect until
            // the screen is re-entered. Future refactor: move all of
            // this off `init { launch { once } }` onto an observed
            // combine() and the strip updates live.
            val bow = session?.bowId?.let { bowRepo.getBow(it) }
            val arrowConfig = session?.arrowConfigId?.let { arrowConfigRepo.getById(it) }
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
                    arrowDiameterMm = arrowConfig?.shaftDiameter,
                    bowName = bow?.name,
                    bowType = bow?.bowType,
                    arrowName = arrowConfig?.label,
                )
            }
        }
    }

    /**
     * Replace an arrow's score/position. Mirrors iOS
     * `HistoricalSessionsView.replotArrow` — keeps the existing id, sessionId,
     * config/end refs, and timestamp; only ring/zone/plotX/plotY change.
     *
     * Keypad re-score path (null [plotX]/[plotY]):
     *  - If the existing position is already inside the new ring's band,
     *    the dot is preserved verbatim — the archer's drop position
     *    doesn't move when the keypad pick agrees with where the dot
     *    already sat (iOS issue #3 / troy.jpeg).
     *  - Otherwise snap to the midline of the new ring along the existing
     *    bearing so the persisted position classifies back to the new
     *    ring on the next render.
     *
     * Positional path (non-null [plotX]/[plotY]): use those coordinates.
     *
     * Zone is always derived from the final coordinates so the persisted
     * zone tag and (plotX, plotY) stay consistent (iOS issue #13).
     */
    fun replotArrow(arrowId: String, ring: Int, plotX: Double?, plotY: Double?) {
        viewModelScope.launch {
            val current = _state.value.arrows.firstOrNull { it.id == arrowId } ?: return@launch
            // §B3 — distance-aware geometry so 50/70m sixRing routes through
            // the 7-zone (Outdoor80) thresholds, not the indoor Vegas
            // thresholds.
            val geometry = TargetGeometry.forFace(_state.value.faceType, _state.value.distance)
            val (finalX, finalY, finalZone) = if (plotX != null && plotY != null) {
                Triple(plotX, plotY, geometry.classify(plotX, plotY).zone)
            } else {
                val snapped = geometry.snapToRingMidline(ring, current.plotX, current.plotY)
                if (snapped == null) {
                    Triple(current.plotX, current.plotY, current.zone)
                } else {
                    Triple(
                        snapped.first,
                        snapped.second,
                        geometry.classify(snapped.first, snapped.second).zone,
                    )
                }
            }
            val updated = current.copy(
                ring = ring,
                zone = finalZone,
                plotX = finalX,
                plotY = finalY,
            )
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
