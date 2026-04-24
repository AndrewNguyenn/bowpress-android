package com.andrewnguyen.bowpress.feature.equipment.bow

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.BowConfigRepository
import com.andrewnguyen.bowpress.core.data.repository.BowRepository
import com.andrewnguyen.bowpress.core.model.Bow
import com.andrewnguyen.bowpress.core.model.BowConfiguration
import com.andrewnguyen.bowpress.feature.equipment.nav.EquipmentArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Loads a single [BowConfiguration] + its [Bow] for read-only display.
 */
@HiltViewModel
class BowConfigDetailViewModel @Inject constructor(
    private val bowRepository: BowRepository,
    private val bowConfigRepository: BowConfigRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val bowId: String = checkNotNull(savedStateHandle[EquipmentArgs.BOW_ID])
    private val configId: String = checkNotNull(savedStateHandle[EquipmentArgs.CONFIG_ID])

    data class UiState(
        val bow: Bow? = null,
        val config: BowConfiguration? = null,
        val isLoading: Boolean = true,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val bow = bowRepository.getBow(bowId)
            val config = bowConfigRepository.getById(configId)
            _state.update { it.copy(bow = bow, config = config, isLoading = false) }
        }
    }

    /** Pin this config as the reference (manual) or unpin it if already referenced. */
    fun toggleReference() {
        val current = _state.value.config ?: return
        viewModelScope.launch {
            runCatching {
                if (current.isReference == true) {
                    bowConfigRepository.clearReference(bowId, current.id)
                } else {
                    bowConfigRepository.setReference(bowId, current.id, manuallyPinned = true)
                }
            }
            val refreshed = bowConfigRepository.getById(configId)
            _state.update { it.copy(config = refreshed) }
        }
    }
}
