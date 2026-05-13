package com.andrewnguyen.bowpress.feature.equipment.bow

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.BowConfigRepository
import com.andrewnguyen.bowpress.core.data.repository.BowRepository
import com.andrewnguyen.bowpress.core.model.Bow
import com.andrewnguyen.bowpress.core.model.BowConfiguration
import com.andrewnguyen.bowpress.core.model.BowType
import com.andrewnguyen.bowpress.core.model.RearStabSide
import com.andrewnguyen.bowpress.feature.equipment.nav.EquipmentArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * Backs the conditional bow-config edit form. Init has three outcomes:
 *
 *   1. Explicit `configId` nav arg → seed from that config (history-entry edit
 *      or "Log new tuning from this snapshot" flow).
 *   2. No `configId`, bow has ≥1 config → seed from the bow's latest config.
 *      This is the BowDetailScreen inline-form path that matches iOS.
 *   3. No `configId`, bow has zero configs → seed from type-aware defaults so
 *      the form is still usable (defensive; new bows are normally created with
 *      an initial config attached).
 *
 * The form itself is gated by [EquipmentFieldRules][com.andrewnguyen.bowpress.feature.equipment.EquipmentFieldRules].
 * This ViewModel only carries raw values; whether a field renders is a pure
 * function of `bow.bowType` + the current `rearStabSide`.
 */
@HiltViewModel
class BowConfigEditViewModel @Inject constructor(
    private val bowRepository: BowRepository,
    private val bowConfigRepository: BowConfigRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val bowId: String = checkNotNull(savedStateHandle[EquipmentArgs.BOW_ID])
    // Optional: missing on the bow-detail route (which seeds from the latest config).
    private val configId: String? = savedStateHandle[EquipmentArgs.CONFIG_ID]

    data class UiState(
        val bow: Bow? = null,
        val baseConfig: BowConfiguration? = null,
        val isLoading: Boolean = true,

        val label: String = "",

        // Shared
        val drawLength: Double = 28.0,
        val restVertical: Int = 0,
        val restHorizontal: Int = 0,
        val restDepth: Double = 0.0,
        val sightPosition: Int = 0,
        val gripAngle: Double = 0.0,
        val nockingHeight: Int = 0,

        // Compound
        val letOffPct: Double = 80.0,
        val peepHeight: Double = 9.0,
        val dLoopLength: Double = 2.0,
        val topCableTwists: Int = 0,
        val bottomCableTwists: Int = 0,
        val mainStringTopTwists: Int = 0,
        val mainStringBottomTwists: Int = 0,
        val topLimbTurns: Double = 0.0,
        val bottomLimbTurns: Double = 0.0,
        val frontStabWeight: Double = 0.0,
        val frontStabAngle: Double = 0.0,
        val rearStabSide: RearStabSide = RearStabSide.NONE,
        val rearStabWeight: Double = 0.0,
        val rearStabVertAngle: Double = 0.0,
        val rearStabHorizAngle: Double = 0.0,

        // Recurve / barebow
        val braceHeight: Double = 8.5,
        val tillerTop: Double = 0.0,
        val tillerBottom: Double = 0.0,
        val plungerTension: Int = 12,
        val clickerPosition: Double = 0.0,
        val rearStabLeftWeight: Double = 6.0,
        val rearStabRightWeight: Double = 6.0,
        // Free-text grip / limb identifiers — recurve + barebow only.
        val specificGrip: String = "",
        val specificLimbs: String = "",

        val isSaving: Boolean = false,
        val savedConfigId: String? = null,
        val errorMessage: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /**
     * Shared upstream so `gripSuggestions` and `limbSuggestions` don't open
     * two independent Room cursors over the same query. `WhileSubscribed`
     * because the suggestions only matter while the form is on screen.
     */
    private val allConfigs: StateFlow<List<BowConfiguration>> =
        bowConfigRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * De-duplicated catalog of previously-entered specific-grip values across
     * every bow's configs. Mirrors iOS `BowConfiguration.suggestions(from:keyPath:)`.
     */
    val gripSuggestions: StateFlow<List<String>> = allConfigs
        .map { configs -> distinctNonEmpty(configs.map { it.specificGrip }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** De-duplicated catalog of previously-entered specific-limb values. */
    val limbSuggestions: StateFlow<List<String>> = allConfigs
        .map { configs -> distinctNonEmpty(configs.map { it.specificLimbs }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun distinctNonEmpty(raw: List<String?>): List<String> {
        val seen = mutableSetOf<String>()
        val out = mutableListOf<String>()
        for (value in raw) {
            val trimmed = value?.trim().orEmpty()
            if (trimmed.isEmpty()) continue
            val key = trimmed.lowercase()
            if (seen.add(key)) out += trimmed
        }
        return out
    }

    init {
        viewModelScope.launch {
            val bow = bowRepository.getBow(bowId)
            val base = when {
                configId != null -> bowConfigRepository.getById(configId)
                // Bow-detail inline-form path: take the latest config from the live
                // configs flow. `first()` resolves once the repository has emitted
                // its initial value, so we don't return stale state.
                else -> bowConfigRepository.observeByBow(bowId).first()
                    .maxByOrNull { it.createdAt }
            }
            if (bow != null && base != null) {
                _state.value = seedFromBase(bow, base)
            } else if (bow != null) {
                // Bow exists but no configs yet (new bow path) — show defaults so
                // the inline form is still usable.
                _state.value = UiState(bow = bow, baseConfig = null, isLoading = false)
            } else {
                _state.value = UiState(isLoading = false, errorMessage = "Bow or config not found")
            }
        }
    }

    // Mutators — each one just copies state; mirror the iOS `@State` setters.
    fun updateLabel(v: String) = _state.update { it.copy(label = v) }
    fun updateDrawLength(v: Double) = _state.update { it.copy(drawLength = v.coerceIn(17.0, 37.0)) }
    fun updateRestVertical(v: Int) = _state.update { it.copy(restVertical = v.coerceIn(-16, 16)) }
    fun updateRestHorizontal(v: Int) = _state.update { it.copy(restHorizontal = v.coerceIn(-16, 16)) }
    fun updateRestDepth(v: Double) = _state.update { it.copy(restDepth = v.coerceIn(-5.0, 5.0)) }
    fun updateSightPosition(v: Int) = _state.update { it.copy(sightPosition = v.coerceIn(-15, 15)) }
    fun updateGripAngle(v: Double) = _state.update { it.copy(gripAngle = v.coerceIn(0.0, 90.0)) }
    fun updateNockingHeight(v: Int) = _state.update { it.copy(nockingHeight = v.coerceIn(-80, 80)) }
    fun updateLetOff(v: Double) = _state.update { it.copy(letOffPct = v.coerceIn(40.0, 99.0)) }
    fun updatePeepHeight(v: Double) = _state.update { it.copy(peepHeight = v.coerceIn(3.0, 17.0)) }
    fun updateDLoop(v: Double) = _state.update { it.copy(dLoopLength = v.coerceIn(0.1, 5.0)) }
    fun updateTopCable(v: Int) = _state.update { it.copy(topCableTwists = v.coerceIn(-10, 10)) }
    fun updateBottomCable(v: Int) = _state.update { it.copy(bottomCableTwists = v.coerceIn(-10, 10)) }
    fun updateMainStringTop(v: Int) = _state.update { it.copy(mainStringTopTwists = v.coerceIn(-10, 10)) }
    fun updateMainStringBottom(v: Int) = _state.update { it.copy(mainStringBottomTwists = v.coerceIn(-10, 10)) }
    fun updateTopLimb(v: Double) = _state.update { it.copy(topLimbTurns = v.coerceIn(-10.0, 10.0)) }
    fun updateBottomLimb(v: Double) = _state.update { it.copy(bottomLimbTurns = v.coerceIn(-10.0, 10.0)) }
    fun updateFrontStabWeight(v: Double) = _state.update { it.copy(frontStabWeight = v.coerceIn(0.0, 60.0)) }
    fun updateFrontStabAngle(v: Double) = _state.update { it.copy(frontStabAngle = v.coerceIn(0.0, 10.0)) }
    fun updateRearStabSide(v: RearStabSide) = _state.update { it.copy(rearStabSide = v) }
    fun updateRearStabWeight(v: Double) = _state.update { it.copy(rearStabWeight = v.coerceIn(0.0, 60.0)) }
    fun updateRearStabVertAngle(v: Double) = _state.update { it.copy(rearStabVertAngle = v.coerceIn(-90.0, 90.0)) }
    fun updateRearStabHorizAngle(v: Double) = _state.update { it.copy(rearStabHorizAngle = v.coerceIn(0.0, 90.0)) }
    fun updateBraceHeight(v: Double) = _state.update { it.copy(braceHeight = v.coerceIn(5.0, 12.0)) }
    fun updateTillerTop(v: Double) = _state.update { it.copy(tillerTop = v.coerceIn(-10.0, 10.0)) }
    fun updateTillerBottom(v: Double) = _state.update { it.copy(tillerBottom = v.coerceIn(-10.0, 10.0)) }
    fun updatePlungerTension(v: Int) = _state.update { it.copy(plungerTension = v.coerceIn(0, 30)) }
    fun updateClickerPosition(v: Double) = _state.update { it.copy(clickerPosition = v.coerceIn(-50.0, 50.0)) }
    fun updateRearStabLeftWeight(v: Double) = _state.update { it.copy(rearStabLeftWeight = v.coerceIn(0.0, 30.0)) }
    fun updateRearStabRightWeight(v: Double) = _state.update { it.copy(rearStabRightWeight = v.coerceIn(0.0, 30.0)) }
    fun updateSpecificGrip(v: String) = _state.update { it.copy(specificGrip = v) }
    fun updateSpecificLimbs(v: String) = _state.update { it.copy(specificLimbs = v) }

    fun dismissError() = _state.update { it.copy(errorMessage = null) }

    /** Clear the one-shot `savedConfigId` after a navigation callback has consumed it. */
    fun acknowledgeSaved() = _state.update { it.copy(savedConfigId = null) }

    /**
     * Snapshot the current form into a brand-new [BowConfiguration] and persist.
     * Matches iOS `BowConfigEditView.save()` — the new row's type-specific fields
     * only get populated when the bow's type requires them; the rest stay null.
     */
    fun save() {
        val current = _state.value
        val bow = current.bow ?: return
        if (current.isSaving) return

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val newConfig = buildConfig(bow, current)
                bowConfigRepository.saveConfig(newConfig)
                _state.update { it.copy(isSaving = false, savedConfigId = newConfig.id) }
            } catch (t: Throwable) {
                _state.update { it.copy(isSaving = false, errorMessage = t.message ?: "Failed to save configuration") }
            }
        }
    }

    private fun buildConfig(bow: Bow, s: UiState): BowConfiguration {
        val trimmedLabel = s.label.trim().takeIf { it.isNotEmpty() }
        val base = BowConfiguration(
            id = UUID.randomUUID().toString(),
            bowId = bow.id,
            createdAt = Instant.now(),
            label = trimmedLabel,
            drawLength = s.drawLength,
            restVertical = s.restVertical,
            restHorizontal = s.restHorizontal,
            restDepth = s.restDepth,
            gripAngle = s.gripAngle,
            nockingHeight = s.nockingHeight,
        )
        return when (bow.bowType) {
            BowType.COMPOUND -> base.copy(
                sightPosition = s.sightPosition,
                letOffPct = s.letOffPct,
                peepHeight = s.peepHeight,
                dLoopLength = s.dLoopLength,
                topCableTwists = s.topCableTwists,
                bottomCableTwists = s.bottomCableTwists,
                mainStringTopTwists = s.mainStringTopTwists,
                mainStringBottomTwists = s.mainStringBottomTwists,
                topLimbTurns = s.topLimbTurns,
                bottomLimbTurns = s.bottomLimbTurns,
                frontStabWeight = s.frontStabWeight,
                frontStabAngle = s.frontStabAngle,
                rearStabSide = s.rearStabSide,
                // BOTH splits the weight into L/R; LEFT/RIGHT keep the single weight; NONE clears all.
                rearStabWeight = if (s.rearStabSide == RearStabSide.LEFT || s.rearStabSide == RearStabSide.RIGHT) s.rearStabWeight else null,
                rearStabLeftWeight = if (s.rearStabSide == RearStabSide.BOTH) s.rearStabLeftWeight else null,
                rearStabRightWeight = if (s.rearStabSide == RearStabSide.BOTH) s.rearStabRightWeight else null,
                rearStabVertAngle = s.rearStabVertAngle,
                rearStabHorizAngle = s.rearStabHorizAngle,
            )
            BowType.RECURVE -> base.copy(
                braceHeight = s.braceHeight,
                tillerTop = s.tillerTop,
                tillerBottom = s.tillerBottom,
                plungerTension = s.plungerTension,
                clickerPosition = s.clickerPosition,
                frontStabWeight = s.frontStabWeight,
                frontStabAngle = s.frontStabAngle,
                rearStabLeftWeight = s.rearStabLeftWeight,
                rearStabRightWeight = s.rearStabRightWeight,
                rearStabVertAngle = s.rearStabVertAngle,
                rearStabHorizAngle = s.rearStabHorizAngle,
                specificGrip = s.specificGrip.trim().takeIf { it.isNotEmpty() },
                specificLimbs = s.specificLimbs.trim().takeIf { it.isNotEmpty() },
            )
            BowType.BAREBOW -> base.copy(
                braceHeight = s.braceHeight,
                tillerTop = s.tillerTop,
                tillerBottom = s.tillerBottom,
                plungerTension = s.plungerTension,
                specificGrip = s.specificGrip.trim().takeIf { it.isNotEmpty() },
                specificLimbs = s.specificLimbs.trim().takeIf { it.isNotEmpty() },
            )
        }
    }

    /** Seed new-form state from the provided [base]. Mirrors iOS `seedFromBase`. */
    private fun seedFromBase(bow: Bow, base: BowConfiguration): UiState {
        // Shared — always start tuning rest/sight at zero (iOS seeds 0 here too).
        val seed = UiState(
            bow = bow,
            baseConfig = base,
            isLoading = false,
            label = "",
            drawLength = base.drawLength,
            restVertical = 0,
            restHorizontal = 0,
            restDepth = 0.0,
            sightPosition = 0,
            gripAngle = 0.0,
            nockingHeight = 0,
        )

        return when (bow.bowType) {
            BowType.COMPOUND -> seed.copy(
                letOffPct = base.letOffPct ?: 80.0,
                peepHeight = base.peepHeight ?: 9.0,
                dLoopLength = base.dLoopLength ?: 2.0,
                frontStabWeight = base.frontStabWeight ?: 0.0,
                frontStabAngle = base.frontStabAngle ?: 0.0,
                rearStabSide = base.rearStabSide ?: RearStabSide.NONE,
                rearStabWeight = base.rearStabWeight ?: 0.0,
                rearStabLeftWeight = base.rearStabLeftWeight ?: 0.0,
                rearStabRightWeight = base.rearStabRightWeight ?: 0.0,
                rearStabVertAngle = base.rearStabVertAngle ?: 0.0,
                rearStabHorizAngle = base.rearStabHorizAngle ?: 0.0,
            )
            BowType.RECURVE -> seed.copy(
                braceHeight = base.braceHeight ?: 8.5,
                tillerTop = base.tillerTop ?: 0.0,
                tillerBottom = base.tillerBottom ?: 0.0,
                plungerTension = base.plungerTension ?: 12,
                clickerPosition = base.clickerPosition ?: 0.0,
                frontStabWeight = base.frontStabWeight ?: 6.0,
                frontStabAngle = base.frontStabAngle ?: 0.0,
                rearStabLeftWeight = base.rearStabLeftWeight ?: 6.0,
                rearStabRightWeight = base.rearStabRightWeight ?: 6.0,
                rearStabVertAngle = base.rearStabVertAngle ?: 0.0,
                rearStabHorizAngle = base.rearStabHorizAngle ?: 0.0,
                specificGrip = base.specificGrip.orEmpty(),
                specificLimbs = base.specificLimbs.orEmpty(),
            )
            BowType.BAREBOW -> seed.copy(
                braceHeight = base.braceHeight ?: 8.5,
                tillerTop = base.tillerTop ?: 0.0,
                tillerBottom = base.tillerBottom ?: 0.0,
                plungerTension = base.plungerTension ?: 12,
                specificGrip = base.specificGrip.orEmpty(),
                specificLimbs = base.specificLimbs.orEmpty(),
            )
        }
    }
}
