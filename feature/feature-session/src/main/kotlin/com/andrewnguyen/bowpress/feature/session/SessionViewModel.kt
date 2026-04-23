package com.andrewnguyen.bowpress.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.ArrowConfigRepository
import com.andrewnguyen.bowpress.core.data.repository.BowConfigRepository
import com.andrewnguyen.bowpress.core.data.repository.BowRepository
import com.andrewnguyen.bowpress.core.data.repository.PlotRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionEndRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionRepository
import com.andrewnguyen.bowpress.core.model.ArrowConfiguration
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.Bow
import com.andrewnguyen.bowpress.core.model.BowConfiguration
import com.andrewnguyen.bowpress.core.model.SessionEnd
import com.andrewnguyen.bowpress.core.model.ShootingSession
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    init {
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
                    // Hydrate active bow/arrow config whenever a new session becomes active.
                    val bowConfig = bowConfigRepo.getById(active.bowConfigId)
                    val arrowConfig = arrowConfigRepo.getById(active.arrowConfigId)
                    _uiState.update {
                        it.copy(
                            activeBowConfig = bowConfig,
                            activeArrowConfig = arrowConfig,
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
            state.copy(selectedBow = bow)
        }
    }

    fun selectArrow(arrow: ArrowConfiguration) {
        _uiState.update { it.copy(selectedArrow = arrow) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ---- Lifecycle ----

    suspend fun startSession(bow: Bow, arrow: ArrowConfiguration) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        // Prefer the most recent saved config for the bow; surface an error if the
        // caller launched without equipment being configured in feature-equipment.
        val existingConfigs = bowConfigRepo.getByBow(bow.id)
        val bowConfig = existingConfigs.maxByOrNull { it.createdAt }
        if (bowConfig == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "This bow has no saved configuration. Add one in Equipment first.",
                )
            }
            return
        }
        val session = ShootingSession(
            id = UUID.randomUUID().toString(),
            bowId = bow.id,
            bowConfigId = bowConfig.id,
            arrowConfigId = arrow.id,
            startedAt = Instant.now(),
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
                bowConfigsByBow = it.bowConfigsByBow + (bow.id to existingConfigs),
                pendingBowConfig = null,
                pendingArrowConfig = null,
                currentArrows = emptyList(),
            )
        }
    }

    /** Plot an arrow computed from a [TargetGeometry.Classification]. */
    suspend fun plotArrow(plotX: Double, plotY: Double, ring: Int, zone: Zone) {
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

    suspend fun endSession(notes: String, feelTags: List<String>) {
        val session = _uiState.value.activeSession ?: return
        _uiState.update { it.copy(isLoading = true, error = null) }
        sessionRepo.endSession(
            sessionId = session.id,
            endedAt = Instant.now(),
            notes = notes,
        )
        // Persist the closing end (feel tags live on the end in the Android schema).
        val end = SessionEnd(
            id = UUID.randomUUID().toString(),
            sessionId = session.id,
            endNumber = 1,
            notes = if (feelTags.isEmpty()) null else feelTags.joinToString(","),
            completedAt = Instant.now(),
        )
        runCatching { sessionEndRepo.saveEnd(end) }
        _uiState.update {
            it.copy(
                isLoading = false,
                activeSession = null,
                activeBowConfig = null,
                activeArrowConfig = null,
                pendingBowConfig = null,
                pendingArrowConfig = null,
                currentArrows = emptyList(),
            )
        }
    }
}
