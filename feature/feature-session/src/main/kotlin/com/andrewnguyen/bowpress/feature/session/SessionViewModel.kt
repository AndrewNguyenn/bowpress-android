package com.andrewnguyen.bowpress.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.ArrowConfigRepository
import com.andrewnguyen.bowpress.core.data.repository.BowConfigRepository
import com.andrewnguyen.bowpress.core.data.repository.BowRepository
import com.andrewnguyen.bowpress.core.data.repository.PlotRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionEndRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionSetupPreferencesRepository
import com.andrewnguyen.bowpress.core.data.social.SocialSessionSharer
import com.andrewnguyen.bowpress.core.model.ArrowConfiguration
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.Bow
import com.andrewnguyen.bowpress.core.model.BowConfiguration
import com.andrewnguyen.bowpress.core.model.SessionEnd
import com.andrewnguyen.bowpress.core.model.SessionLocation
import com.andrewnguyen.bowpress.core.model.SessionType
import com.andrewnguyen.bowpress.core.model.ShootingDistance
import com.andrewnguyen.bowpress.core.model.ShootingSession
import com.andrewnguyen.bowpress.core.model.TargetFaceType
import com.andrewnguyen.bowpress.core.model.TargetLayout
import com.andrewnguyen.bowpress.core.model.ThreeDScoringSystem
import com.andrewnguyen.bowpress.core.model.Zone
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * Drives the whole session flow. Mirrors the iOS `SessionViewModel` structurally but
 * simplified for the Android port — the active session is the source of truth via
 * [SessionRepository.observeActiveSession] (Room-backed), and plots are observed live
 * through [PlotRepository.observeBySession] so the UI updates whenever the background
 * sync writes new plots from the server as well.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val bowRepo: BowRepository,
    private val arrowConfigRepo: ArrowConfigRepository,
    private val bowConfigRepo: BowConfigRepository,
    private val sessionRepo: SessionRepository,
    private val plotRepo: PlotRepository,
    private val sessionEndRepo: SessionEndRepository,
    private val socialSessionSharer: SocialSessionSharer,
    private val sessionSetupPrefs: SessionSetupPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    /**
     * Sticky last multi-spot layout, mirrored from DataStore. The setup
     * screen restores this when the 20yd + 6-ring combo is re-entered;
     * [TargetLayout.SINGLE] means "no sticky pick". Mirrors iOS
     * `@AppStorage("session.lastTargetLayout")`.
     */
    private var lastTargetLayout: TargetLayout = TargetLayout.SINGLE

    init {
        viewModelScope.launch {
            sessionSetupPrefs.lastTargetLayout.collect { lastTargetLayout = it }
        }
        viewModelScope.launch {
            bowRepo.observeBows().collect { bows ->
                _uiState.update { it.copy(bows = bows) }
            }
        }
        viewModelScope.launch {
            arrowConfigRepo.observeAll().collect { configs ->
                _uiState.update { it.copy(arrowConfigs = configs) }
            }
        }
        viewModelScope.launch {
            sessionRepo.observeActiveSession().collect { active ->
                val previousId = _uiState.value.activeSession?.id
                _uiState.update { it.copy(activeSession = active) }
                if (active?.id != previousId && active != null) {
                    // Hydrate active bow/arrow config + the selected bow/arrow handles
                    // whenever a new session becomes active. The selected* fields back
                    // the in-session config banner ("<distance> · <bow> · <arrow>") —
                    // without this hydration step, navigating into an active session
                    // from a fresh VM instance would render "— · — · …".
                    val bowConfig = bowConfigRepo.getById(active.bowConfigId)
                    val arrowConfig = arrowConfigRepo.getById(active.arrowConfigId)
                    val bow = bowRepo.getBow(active.bowId)
                    _uiState.update {
                        it.copy(
                            activeBowConfig = bowConfig,
                            activeArrowConfig = arrowConfig,
                            selectedBow = bow ?: it.selectedBow,
                            selectedArrow = arrowConfig ?: it.selectedArrow,
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            // Observe plots for whichever session is active at any moment.
            sessionRepo.observeActiveSession()
                .flatMapLatest { active ->
                    if (active == null) flowOf(emptyList())
                    else plotRepo.observeBySession(active.id)
                }
                .collect { plots ->
                    _uiState.update { it.copy(currentArrows = plots) }
                }
        }
        viewModelScope.launch {
            // Observe the completed ends for the active session — drives the
            // live ends-history scorecard. Combined with the plot stream
            // above by `endsBreakdown`.
            sessionRepo.observeActiveSession()
                .flatMapLatest { active ->
                    if (active == null) flowOf(emptyList())
                    else sessionEndRepo.observeBySession(active.id)
                }
                .collect { ends ->
                    _uiState.update { it.copy(completedEnds = ends) }
                }
        }
    }

    // ---- Selection (start screen) ----

    fun selectBow(bow: Bow) {
        _uiState.update { state ->
            val latestConfig = state.bowConfigsByBow[bow.id]?.maxByOrNull { it.createdAt }
            // Lazily load configs the first time we select a bow.
            if (latestConfig == null) {
                viewModelScope.launch {
                    val configs = bowConfigRepo.getByBow(bow.id)
                    _uiState.update { cur ->
                        cur.copy(bowConfigsByBow = cur.bowConfigsByBow + (bow.id to configs))
                    }
                }
            }
            // Smart default for the target face whenever the selected bow changes,
            // unless the user has already manually picked a face for this setup.
            val nextFace = if (state.userOverrodeFace) {
                state.selectedFaceType
            } else {
                TargetFaceType.defaultFor(bow.bowType)
            }
            // Route the face change through syncLayoutToCurrentCombo — same as
            // selectFaceType — so a bow-driven face flip (e.g. compound→recurve
            // moving 6-ring→10-ring) collapses a now-off-combo multi-spot
            // layout. iOS gets this for free via .onChange(of: selectedFaceType).
            syncLayoutToCurrentCombo(
                state.copy(selectedBow = bow, selectedFaceType = nextFace),
            )
        }
    }

    fun selectArrow(arrow: ArrowConfiguration) {
        _uiState.update { it.copy(selectedArrow = arrow) }
    }

    /** User manually picked a target face — lock out the smart default until next session start. */
    fun selectFaceType(faceType: TargetFaceType) {
        _uiState.update {
            syncLayoutToCurrentCombo(it.copy(selectedFaceType = faceType, userOverrodeFace = true))
        }
    }

    /** Pick (or clear with `null`) the shooting distance for the next session. */
    fun selectDistance(distance: ShootingDistance?) {
        _uiState.update { syncLayoutToCurrentCombo(it.copy(selectedDistance = distance)) }
    }

    /**
     * User picked a multi-spot Vegas layout from the setup screen's LAYOUT
     * field. Only multi-spot picks are persisted as the sticky default —
     * mirrors iOS `onChange(of: selectedLayout)`.
     */
    fun selectLayout(layout: TargetLayout) {
        _uiState.update { it.copy(selectedLayout = layout) }
        if (layout.isMultiSpot) {
            lastTargetLayout = layout
            viewModelScope.launch { sessionSetupPrefs.setLastTargetLayout(layout) }
        }
    }

    /**
     * Bidirectional sync between the current distance/face combo and the
     * layout selection. Mirrors iOS `syncLayoutToCurrentCombo`:
     *  - When the combo *doesn't* support multi-spot (anything other than
     *    20yd + 6-ring), force [TargetLayout.SINGLE] so a stale Vegas pick
     *    can't silently apply to an outdoor session.
     *  - When the combo *does* support multi-spot and the layout is currently
     *    [TargetLayout.SINGLE] (the implicit fallback), restore the sticky
     *    pick from DataStore — falling back to [TargetLayout.TRIANGLE] as the
     *    canonical Vegas default if nothing is stored.
     */
    private fun syncLayoutToCurrentCombo(state: SessionUiState): SessionUiState {
        val supportsMultiSpot = state.selectedDistance == ShootingDistance.YARDS_20 &&
            state.selectedFaceType == TargetFaceType.SIX_RING
        if (!supportsMultiSpot) {
            return if (state.selectedLayout != TargetLayout.SINGLE) {
                state.copy(selectedLayout = TargetLayout.SINGLE)
            } else {
                state
            }
        }
        if (state.selectedLayout != TargetLayout.SINGLE) return state
        val restored = if (lastTargetLayout.isMultiSpot) lastTargetLayout else TargetLayout.TRIANGLE
        return state.copy(selectedLayout = restored)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ---- Lifecycle ----

    fun selectSessionType(type: SessionType) {
        _uiState.update { it.copy(selectedSessionType = type) }
    }

    fun selectScoringSystem(system: ThreeDScoringSystem) {
        _uiState.update { it.copy(selectedScoringSystem = system) }
    }

    /**
     * Begin a 3D-course session. Mirrors [startSession] but stamps the
     * session `sessionType = THREE_D_COURSE` + the chosen scoring system;
     * [com.andrewnguyen.bowpress.feature.session.threed.ThreeDCourseViewModel]
     * then takes over the live course.
     */
    suspend fun startThreeDCourse(
        bow: Bow,
        arrow: ArrowConfiguration,
        system: ThreeDScoringSystem,
        title: String = "",
        intention: String = "",
    ) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        val existingConfigs = bowConfigRepo.getByBow(bow.id)
        val bowConfig = existingConfigs.maxByOrNull { it.createdAt }
            ?: com.andrewnguyen.bowpress.core.data.config.makeDefaultConfig(bow).also {
                bowConfigRepo.saveConfig(it)
            }
        val session = ShootingSession(
            id = UUID.randomUUID().toString(),
            bowId = bow.id,
            bowConfigId = bowConfig.id,
            arrowConfigId = arrow.id,
            startedAt = Instant.now(),
            sessionType = SessionType.THREE_D_COURSE,
            scoringSystem = system,
            title = title.trim().takeIf { it.isNotEmpty() },
            notes = intention.trim(),
        )
        sessionRepo.saveSession(session)
        _uiState.update {
            it.copy(
                isLoading = false,
                activeSession = session,
                activeBowConfig = bowConfig,
                activeArrowConfig = arrow,
                selectedBow = bow,
                selectedArrow = arrow,
                userOverrodeFace = false,
            )
        }
    }

    suspend fun startSession(
        bow: Bow,
        arrow: ArrowConfiguration,
        title: String = "",
        intention: String = "",
    ) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        // Prefer the most recent saved config for the bow; if none exists (e.g.
        // a bow seeded before iter 20's auto-config), seed a default one and
        // persist it. Mirrors iOS, where SessionView/SessionConfigSheet fall
        // back to `BowConfiguration.makeDefault(for: bow)` rather than erroring.
        val existingConfigs = bowConfigRepo.getByBow(bow.id)
        val bowConfig = existingConfigs.maxByOrNull { it.createdAt }
            ?: com.andrewnguyen.bowpress.core.data.config.makeDefaultConfig(bow).also {
                bowConfigRepo.saveConfig(it)
            }
        val configsForBow = if (existingConfigs.any { it.id == bowConfig.id }) existingConfigs
        else existingConfigs + bowConfig
        val faceType = _uiState.value.selectedFaceType
        val distance = _uiState.value.selectedDistance
        // Layout is only meaningful at 20yd + 6-ring; collapse defensively in
        // case a stale multi-spot pick survived an earlier state. Mirrors iOS
        // `startNewSession`.
        val layout = if (distance == ShootingDistance.YARDS_20 &&
            faceType == TargetFaceType.SIX_RING
        ) {
            _uiState.value.selectedLayout
        } else {
            TargetLayout.SINGLE
        }
        // iOS 4fb5c16/9b104a2/39df3bd: persist title + seed notes from intention
        // so the Log row + session detail show what the user actually typed
        // (previously the fields were decorative — silent data loss).
        val trimmedTitle = title.trim().takeIf { it.isNotEmpty() }
        val trimmedIntention = intention.trim()
        val session = ShootingSession(
            id = UUID.randomUUID().toString(),
            bowId = bow.id,
            bowConfigId = bowConfig.id,
            arrowConfigId = arrow.id,
            startedAt = Instant.now(),
            targetFaceType = faceType,
            targetLayout = layout,
            distance = distance,
            title = trimmedTitle,
            notes = trimmedIntention,
        )
        sessionRepo.saveSession(session)
        _uiState.update {
            it.copy(
                isLoading = false,
                activeSession = session,
                activeBowConfig = bowConfig,
                activeArrowConfig = arrow,
                selectedBow = bow,
                selectedArrow = arrow,
                bowConfigsByBow = it.bowConfigsByBow + (bow.id to configsForBow),
                pendingBowConfig = null,
                pendingArrowConfig = null,
                currentArrows = emptyList(),
                completedEnds = emptyList(),
                // Clear the manual-override flag so the next setup gets a fresh smart default.
                userOverrodeFace = false,
            )
        }
    }

    /** Plot an arrow computed from a [TargetGeometry.Classification]. */
    suspend fun plotArrow(plotX: Double, plotY: Double, ring: Int, zone: Zone) {
        writePlot(plotX = plotX, plotY = plotY, ring = ring, zone = zone)
    }

    /**
     * Plot a miss — a shot that didn't hit the scoring rings at all: ring 0,
     * no plot position. Mirrors iOS `plotArrow(ring: 0, plotX: nil, plotY: nil)`
     * behind the range Miss button.
     */
    suspend fun plotMiss() {
        writePlot(plotX = null, plotY = null, ring = 0, zone = Zone.CENTER)
    }

    /** Build + persist one plot. A miss passes null coordinates. */
    private suspend fun writePlot(plotX: Double?, plotY: Double?, ring: Int, zone: Zone) {
        val state = _uiState.value
        val session = state.activeSession ?: return
        // If a config change is pending, resolve it before writing the plot so the plot
        // carries the new bow/arrow config ids (matches iOS `plotArrow` lazy segment).
        val (activeBowConfig, activeArrowConfig, sessionForPlot) = resolvePendingConfig(
            state = state,
            currentSession = session,
        )
        val plot = ArrowPlot(
            id = UUID.randomUUID().toString(),
            sessionId = sessionForPlot.id,
            bowConfigId = activeBowConfig.id,
            arrowConfigId = activeArrowConfig.id,
            ring = ring,
            zone = zone,
            plotX = plotX,
            plotY = plotY,
            shotAt = Instant.now(),
        )
        plotRepo.savePlot(plot)
    }

    private suspend fun resolvePendingConfig(
        state: SessionUiState,
        currentSession: ShootingSession,
    ): Triple<BowConfiguration, ArrowConfiguration, ShootingSession> {
        val activeBow = state.activeBowConfig
            ?: error("Active bow config missing while plotting arrow")
        val activeArrow = state.activeArrowConfig
            ?: error("Active arrow config missing while plotting arrow")

        if (!state.hasPendingConfigChange) {
            return Triple(activeBow, activeArrow, currentSession)
        }

        val newBowConfig = state.pendingBowConfig ?: activeBow
        val newArrowConfig = state.pendingArrowConfig ?: activeArrow

        // If the pending bow config is brand new to us, persist it.
        if (state.pendingBowConfig != null &&
            state.bowConfigsByBow[newBowConfig.bowId]?.none { it.id == newBowConfig.id } != false
        ) {
            bowConfigRepo.saveConfig(newBowConfig)
        }

        val newSession = currentSession.copy(
            id = UUID.randomUUID().toString(),
            bowConfigId = newBowConfig.id,
            arrowConfigId = newArrowConfig.id,
            startedAt = Instant.now(),
        )
        sessionRepo.saveSession(newSession)
        _uiState.update {
            it.copy(
                activeSession = newSession,
                activeBowConfig = newBowConfig,
                activeArrowConfig = newArrowConfig,
                pendingBowConfig = null,
                pendingArrowConfig = null,
            )
        }
        return Triple(newBowConfig, newArrowConfig, newSession)
    }

    /**
     * Toggle a plot's "flier" flag — excluded plots are persisted through the plot
     * repository so analytics picks up the exclusion on the next aggregation run.
     */
    suspend fun togglePlotExcluded(plotId: String) {
        val state = _uiState.value
        val plot = state.currentArrows.firstOrNull { it.id == plotId } ?: return
        plotRepo.savePlot(plot.copy(excluded = !plot.excluded))
    }

    /**
     * Undo — removes the last arrow of the *in-progress* end. Cannot undo past
     * a completed end: a completed end's arrows are edited via [deleteArrow] /
     * the end-actions sheet, not the undo button. Mirrors iOS `removeLastArrow`.
     */
    suspend fun removeLastArrow() {
        val last = _uiState.value.currentEndArrows.maxByOrNull { it.shotAt } ?: return
        plotRepo.deletePlot(last)
    }

    /**
     * Complete the in-progress end: record a [SessionEnd] with the next end
     * number and stamp every in-progress arrow with the new end's id, so the
     * arrows move out of `currentEndArrows` and into the scorecard. The end
     * starts empty again for the next round. Mirrors iOS `completeEnd`.
     */
    /**
     * Synchronous re-entry guard for [completeEnd]. The `isLoading` flag can't
     * serve this role — it is only flipped after the first suspend point, so a
     * fast double-tap of "Finish End" (or `endSession` racing an in-flight
     * tap) would both pass the `isEmpty()` guard against the *same*
     * `currentEndArrows` and record two `SessionEnd`s. This boolean is
     * checked-and-set on the calling thread before any suspension.
     */
    private var completingEnd = false

    /**
     * Complete the in-progress end. Records a [SessionEnd] and stamps the
     * in-progress arrows with its id. Re-entrant calls are dropped by
     * [completingEnd]. Returns true when an end was recorded.
     */
    suspend fun completeEnd(): Boolean {
        if (completingEnd) return false
        completingEnd = true
        try {
            val state = _uiState.value
            val session = state.activeSession ?: return false
            return completeEndWith(session, state.currentEndArrows, state.currentEndNumber)
        } finally {
            completingEnd = false
        }
    }

    /**
     * The actual end-completion work, against an already-captured arrow
     * snapshot — so [endSession] can complete a trailing end without a second
     * read of the reactive state (and without re-tripping the [completingEnd]
     * guard it already holds). Returns true when an end was recorded.
     */
    private suspend fun completeEndWith(
        session: ShootingSession,
        arrowsInEnd: List<ArrowPlot>,
        endNumber: Int,
    ): Boolean {
        if (arrowsInEnd.isEmpty()) return false
        _uiState.update { it.copy(isLoading = true, error = null) }
        val end = SessionEnd(
            id = UUID.randomUUID().toString(),
            sessionId = session.id,
            endNumber = endNumber,
            completedAt = Instant.now(),
        )
        var recorded = false
        runCatching {
            sessionEndRepo.saveEnd(end)
            // Stamp each in-progress arrow with the new end's id. The plot
            // stream then re-emits and `endsBreakdown` slices them into the
            // completed end. Mirrors the iOS endId-stamp loop.
            arrowsInEnd.forEach { arrow ->
                plotRepo.savePlot(arrow.copy(endId = end.id))
            }
            recorded = true
        }.onFailure { e ->
            _uiState.update { it.copy(error = e.message) }
        }
        _uiState.update { it.copy(isLoading = false) }
        return recorded
    }

    /**
     * Delete a specific arrow by id from Room and remote. Used by the
     * recent-arrows / scorecard edit path to fix a mis-entered score. The
     * arrow leaves whichever end it belonged to; `endsBreakdown` re-slices on
     * the next plot emission. Mirrors iOS `deleteArrow`.
     */
    suspend fun deleteArrow(plotId: String) {
        val plot = _uiState.value.currentArrows.firstOrNull { it.id == plotId } ?: return
        plotRepo.deletePlot(plot)
    }

    // NOTE: iOS `addArrowToEnd` (slotting a recovery arrow into an
    // already-completed end) is intentionally NOT ported yet. It needs a
    // target-tap surface, and the only such edit sheet — ArrowEditSheet —
    // lives in feature-analytics, which already depends on feature-session;
    // importing it back would be a dependency cycle. Rather than leave an
    // unreachable suspend function, the VM method was removed. The live
    // scorecard already supports delete-a-shot-cell + re-plot into the live
    // end as the Android recovery path. Restore addArrowToEnd when a shared
    // edit sheet exists in core-designsystem.

    /**
     * Re-plot an existing arrow at a new (x, y) on the target. Mirrors iOS
     * `APIClient.plotArrow` upsert path used by the session-detail re-plot sheet —
     * the plot id is preserved so end membership and analytics keep their identity.
     */
    suspend fun replotArrow(plotId: String, plotX: Double, plotY: Double, ring: Int, zone: Zone) {
        val plot = _uiState.value.currentArrows.firstOrNull { it.id == plotId } ?: return
        plotRepo.savePlot(
            plot.copy(
                ring = ring,
                zone = zone,
                plotX = plotX,
                plotY = plotY,
            ),
        )
    }

    /**
     * Delete a single completed end (and its arrows) from the active session.
     * Local-first; remote DELETE attempted opportunistically inside the repo.
     */
    fun deleteEnd(endId: String) {
        val sessionId = _uiState.value.activeSession?.id ?: return
        viewModelScope.launch {
            sessionRepo.deleteEnd(sessionId = sessionId, endId = endId)
        }
    }

    /** Apply a pending config change (commits on next plot or session-restart). */
    fun changeConfig(newBowConfigId: String? = null, newArrowConfigId: String? = null) {
        viewModelScope.launch {
            val bowConfig = newBowConfigId?.let { bowConfigRepo.getById(it) }
            val arrowConfig = newArrowConfigId?.let { arrowConfigRepo.getById(it) }
            _uiState.update {
                it.copy(
                    pendingBowConfig = bowConfig ?: it.pendingBowConfig,
                    pendingArrowConfig = arrowConfig ?: it.pendingArrowConfig,
                )
            }
        }
    }

    /**
     * Finalize the active session. [location] is the §18 Instagram-style
     * location tag captured in the end-session sheet — it rides through to the
     * feed share. Null when the archer left the session untagged.
     */
    suspend fun endSession(
        notes: String,
        feelTags: List<String>,
        location: SessionLocation? = null,
    ) {
        val state = _uiState.value
        val session = state.activeSession ?: return
        // Capture the arrow plots + the in-progress end up front, before any
        // suspension. The §15 share's score/X line is computed off this
        // `arrows` snapshot — every plotted arrow, regardless of endId — so
        // it stays correct even though the reactive completedEnds stream
        // hasn't re-emitted the just-auto-completed end yet.
        val arrows = state.currentArrows
        val trailingEnd = state.currentEndArrows
        val trailingEndNumber = state.currentEndNumber
        _uiState.update { it.copy(isLoading = true, error = null) }
        // Auto-complete any in-progress end so the finished session has clean,
        // fully-stamped ends — no trailing batch of endId-less arrows in the
        // log. Goes straight through completeEndWith with the captured
        // snapshot: a no-op when the last end was already finished, and not
        // subject to the `completingEnd` re-entry guard (no second state read).
        completeEndWith(session, trailingEnd, trailingEndNumber)
        sessionRepo.endSession(
            sessionId = session.id,
            endedAt = Instant.now(),
            notes = notes,
        )
        // Persist feel tags onto the session itself — that is where the
        // analytics engine reads them from (ShootingSession.feelTags). The
        // session notes were just written by endSession() above; pass them
        // through unchanged so updateSession doesn't blank them.
        runCatching { sessionRepo.updateSession(session.id, notes, feelTags) }
        _uiState.update {
            it.copy(
                isLoading = false,
                activeSession = null,
                activeBowConfig = null,
                activeArrowConfig = null,
                pendingBowConfig = null,
                pendingArrowConfig = null,
                currentArrows = emptyList(),
                completedEnds = emptyList(),
                userOverrodeFace = false,
            )
        }
        // §15 — publish the saved session to friends' feeds. Fire-and-forget:
        // SocialSessionSharer self-gates on visibility and never throws, so a
        // share failure can't affect the already-persisted save above.
        shareSessionToFeed(session, arrows, location)
    }

    /**
     * Publishes the just-finalized [session] to the friend feed (§15). Scored
     * from the session's [arrows] — score is the ring sum, X count is ring-11
     * hits — counting only non-excluded plots. [location] is the §18 location
     * tag, carried through to the share request.
     */
    private fun shareSessionToFeed(
        session: ShootingSession,
        arrows: List<ArrowPlot>,
        location: SessionLocation?,
    ) {
        val scored = arrows.filterNot { it.excluded }
        viewModelScope.launch {
            socialSessionSharer.shareCompletedSession(
                sessionId = session.id,
                score = scored.sumOf { it.ring },
                xCount = scored.count { it.ring == 11 },
                arrowCount = scored.size,
                distance = session.distance?.label,
                face = session.targetFaceType.label,
                title = session.title?.takeIf { it.isNotBlank() },
                shotAt = session.startedAt,
                location = location,
            )
        }
    }
}
