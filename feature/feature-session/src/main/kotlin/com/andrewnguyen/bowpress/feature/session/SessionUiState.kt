package com.andrewnguyen.bowpress.feature.session

import com.andrewnguyen.bowpress.core.model.ArrowConfiguration
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.Bow
import com.andrewnguyen.bowpress.core.model.BowConfiguration
import com.andrewnguyen.bowpress.core.model.SessionEnd
import com.andrewnguyen.bowpress.core.model.SessionType
import com.andrewnguyen.bowpress.core.model.ShootingDistance
import com.andrewnguyen.bowpress.core.model.ShootingSession
import com.andrewnguyen.bowpress.core.model.TargetFaceType
import com.andrewnguyen.bowpress.core.model.TargetLayout
import com.andrewnguyen.bowpress.core.model.ThreeDScoringSystem

/**
 * Consolidated UI state for the entire session flow. `SessionHomeScreen` reads
 * `bows/arrowConfigs/selected*`; `ActiveSessionScreen` reads the rest.
 *
 * Deliberately a single state object so a single [SessionViewModel] can drive the home
 * screen, the active session, and the config/end sheets without propagating state
 * through nav args — mirrors the pattern used by `feature-auth` (`AuthUiState`).
 */
data class SessionUiState(
    val isLoading: Boolean = false,
    val error: String? = null,

    // Equipment catalog
    val bows: List<Bow> = emptyList(),
    val arrowConfigs: List<ArrowConfiguration> = emptyList(),
    val bowConfigsByBow: Map<String, List<BowConfiguration>> = emptyMap(),

    // Start-screen selection
    val selectedBow: Bow? = null,
    val selectedArrow: ArrowConfiguration? = null,
    /** Target face the user will shoot at for the session they are about to start. */
    val selectedFaceType: TargetFaceType = TargetFaceType.SIX_RING,
    /** True once the user has manually picked a face — stops the bow-default from overriding. */
    val userOverrodeFace: Boolean = false,
    /** Distance the user will shoot at; null = "not set" (won't appear under any specific-distance analytics filter). */
    val selectedDistance: ShootingDistance? = null,
    /**
     * Multi-spot Vegas layout the user picked on the setup screen. Only
     * meaningful at 20yd + 6-ring; forced to [TargetLayout.SINGLE] off-combo
     * by [SessionViewModel.syncLayoutToCurrentCombo]. Mirrors iOS
     * `SessionView.selectedLayout`.
     */
    val selectedLayout: TargetLayout = TargetLayout.SINGLE,
    /** Practice discipline picked on the setup screen — range vs 3D course. */
    val selectedSessionType: SessionType = SessionType.RANGE,
    /** Scoring system for a 3D course (ignored for range sessions). */
    val selectedScoringSystem: ThreeDScoringSystem = ThreeDScoringSystem.ASA,

    // Active session
    val activeSession: ShootingSession? = null,
    val activeBowConfig: BowConfiguration? = null,
    val activeArrowConfig: ArrowConfiguration? = null,

    // Pending mid-session config change (not committed until next plot or confirm).
    val pendingBowConfig: BowConfiguration? = null,
    val pendingArrowConfig: ArrowConfiguration? = null,

    // Plots for the active session — every plot, in shotAt order. Sliced into
    // ends by `endId` via [endsBreakdown].
    val currentArrows: List<ArrowPlot> = emptyList(),
    // Completed ends for the active session, in endNumber order.
    val completedEnds: List<SessionEnd> = emptyList(),
) {
    val isSessionActive: Boolean get() = activeSession != null
    val hasPendingConfigChange: Boolean
        get() = pendingBowConfig != null || pendingArrowConfig != null

    /**
     * The live end breakdown — completed ends with their arrows plus the
     * in-progress end. The single source of truth the active-session
     * scorecard renders. Mirrors iOS `completedEndsBreakdown` + `currentEndArrows`.
     *
     * Computed once per state instance (a `val` initializer, not a `get()`)
     * so reading it — and `currentEndArrows` / `currentEndNumber` below —
     * doesn't re-slice the whole plot list on every access / recomposition.
     */
    val endsBreakdown: SessionEndsBreakdown =
        SessionEndsBreakdown.from(arrows = currentArrows, ends = completedEnds)

    /** Arrows in the end currently being plotted (no `endId` yet). */
    val currentEndArrows: List<ArrowPlot> get() = endsBreakdown.inProgressArrows

    /** Display number of the end being plotted right now. */
    val currentEndNumber: Int get() = endsBreakdown.currentEndNumber

    /**
     * Face type for the currently-active session (falls back to [selectedFaceType] during
     * setup before a session is active). Screens rendering the target face should prefer
     * this over reading [ShootingSession.targetFaceType] directly.
     */
    val targetFaceType: TargetFaceType
        get() = activeSession?.targetFaceType ?: selectedFaceType

    /**
     * Multi-spot layout for the currently-active session (falls back to
     * [selectedLayout] during setup). Screens rendering the live target
     * should prefer this over reading [ShootingSession.targetLayout] directly.
     */
    val targetLayout: TargetLayout
        get() = activeSession?.targetLayout ?: selectedLayout

    /** The latest bow config for a bow, or null if the bow has none. */
    fun latestConfigFor(bowId: String): BowConfiguration? =
        bowConfigsByBow[bowId]?.maxByOrNull { it.createdAt }
}
