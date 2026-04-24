package com.andrewnguyen.bowpress.feature.session

import com.andrewnguyen.bowpress.core.model.ArrowConfiguration
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.Bow
import com.andrewnguyen.bowpress.core.model.BowConfiguration
import com.andrewnguyen.bowpress.core.model.ShootingSession
import com.andrewnguyen.bowpress.core.model.TargetFaceType

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

    // Active session
    val activeSession: ShootingSession? = null,
    val activeBowConfig: BowConfiguration? = null,
    val activeArrowConfig: ArrowConfiguration? = null,

    // Pending mid-session config change (not committed until next plot or confirm).
    val pendingBowConfig: BowConfiguration? = null,
    val pendingArrowConfig: ArrowConfiguration? = null,

    // Plots for the active session
    val currentArrows: List<ArrowPlot> = emptyList(),
) {
    val isSessionActive: Boolean get() = activeSession != null
    val hasPendingConfigChange: Boolean
        get() = pendingBowConfig != null || pendingArrowConfig != null

    /**
     * Face type for the currently-active session (falls back to [selectedFaceType] during
     * setup before a session is active). Screens rendering the target face should prefer
     * this over reading [ShootingSession.targetFaceType] directly.
     */
    val targetFaceType: TargetFaceType
        get() = activeSession?.targetFaceType ?: selectedFaceType

    /** The latest bow config for a bow, or null if the bow has none. */
    fun latestConfigFor(bowId: String): BowConfiguration? =
        bowConfigsByBow[bowId]?.maxByOrNull { it.createdAt }
}
