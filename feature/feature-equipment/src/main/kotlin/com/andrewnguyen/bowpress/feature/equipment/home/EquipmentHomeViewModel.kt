package com.andrewnguyen.bowpress.feature.equipment.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.ArrowConfigRepository
import com.andrewnguyen.bowpress.core.data.repository.BowRepository
import com.andrewnguyen.bowpress.core.model.ArrowConfiguration
import com.andrewnguyen.bowpress.core.model.Bow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Collects the live lists of bows + arrows for the equipment home screen. Both
 * lists are already reactive via Room, so we just stitch them together. The
 * screen renders tabs for Bows and Arrows over the combined state.
 *
 * Delete actions flow through here too so the UI doesn't depend on repositories
 * directly.
 */
@HiltViewModel
class EquipmentHomeViewModel @Inject constructor(
    private val bowRepository: BowRepository,
    private val arrowRepository: ArrowConfigRepository,
) : ViewModel() {

    data class UiState(
        val bows: List<Bow> = emptyList(),
        val arrows: List<ArrowConfiguration> = emptyList(),
        val isLoading: Boolean = true,
    )

    val state: StateFlow<UiState> = combine(
        bowRepository.observeBows(),
        arrowRepository.observeAll(),
    ) { bows, arrows ->
        UiState(bows = bows, arrows = arrows, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState(),
    )

    fun deleteBow(id: String) {
        viewModelScope.launch { bowRepository.deleteBow(id) }
    }

    fun deleteArrow(id: String) {
        viewModelScope.launch { arrowRepository.deleteArrowConfig(id) }
    }
}
