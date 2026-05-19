package com.andrewnguyen.bowpress.feature.equipment.sightmarks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.SightMarkRepository
import com.andrewnguyen.bowpress.core.model.DistanceUnit
import com.andrewnguyen.bowpress.core.model.SightMark
import com.andrewnguyen.bowpress.core.model.SightMarkSuggester
import com.andrewnguyen.bowpress.feature.equipment.nav.EquipmentArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * Backs `SightMarksListScreen`. Wraps a single-bow scoped flow over the
 * SightMark repository + exposes save/delete + a sight-mark suggester
 * for arbitrary distances. Mirrors iOS `SightMarksListView` state.
 */
@HiltViewModel
class SightMarksViewModel @Inject constructor(
    private val repository: SightMarkRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val bowId: String = savedStateHandle[EquipmentArgs.BOW_ID] ?: ""

    /** All marks for the bow, distance-sorted. */
    val marks: StateFlow<List<SightMark>> = repository.observeByBow(bowId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Measured-only filter — synthesized suggestions don't refit. */
    val measuredMarks: StateFlow<List<SightMark>> = repository.observeByBow(bowId)
        .map { all -> all.filter { !it.isSuggestion } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(
        distance: Double,
        unit: DistanceUnit,
        mark: Double,
        note: String?,
        userId: String,
    ) {
        val now = Instant.now()
        viewModelScope.launch {
            repository.save(
                SightMark(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    bowId = bowId,
                    distance = distance,
                    distanceUnit = unit,
                    mark = mark,
                    note = note?.trim()?.takeIf { it.isNotEmpty() },
                    isSuggestion = false,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }

    fun update(mark: SightMark) {
        viewModelScope.launch {
            repository.save(mark.copy(updatedAt = Instant.now()))
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    /**
     * Live suggestion for [distance] in [unit] given the current measured
     * marks. Returns null while gates fail (not enough marks, spread too
     * small, distance out of range). Mirrors iOS SightMarkSuggester.suggest.
     */
    fun suggestionFor(distance: Double, unit: DistanceUnit): SightMarkSuggester.Outcome =
        SightMarkSuggester.suggest(distance, unit, measuredMarks.value)
}
