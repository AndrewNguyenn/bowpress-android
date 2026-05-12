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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs [BowDetailScreen]'s read-only header + reactive history list. The bow
 * itself is loaded once on init (it doesn't change shape live); the list of
 * configurations is observed reactively so a freshly-saved config appears in
 * the history immediately if the user returns to this screen. Edit-form state
 * lives in [BowConfigEditViewModel], which the same screen also hosts.
 */
@HiltViewModel
class BowDetailViewModel @Inject constructor(
    private val bowRepository: BowRepository,
    private val bowConfigRepository: BowConfigRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val bowId: String = checkNotNull(savedStateHandle[EquipmentArgs.BOW_ID]) {
        "BowDetailViewModel requires a ${EquipmentArgs.BOW_ID} argument"
    }

    data class UiState(
        val bow: Bow? = null,
        val configurations: List<BowConfiguration> = emptyList(),
        val isLoading: Boolean = true,
        val isDeleted: Boolean = false,
        val errorMessage: String? = null,
    )

    private val _bowFlow = MutableStateFlow<Bow?>(null)

    val state: StateFlow<UiState> = combine(
        _bowFlow,
        bowConfigRepository.observeByBow(bowId),
    ) { bow, configs ->
        UiState(
            bow = bow,
            configurations = configs.sortedByDescending { it.createdAt },
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState(),
    )

    init {
        viewModelScope.launch {
            _bowFlow.value = bowRepository.getBow(bowId)
        }
    }

    /** Delete the bow and everything tied to it. Deletions propagate via Room cascades. */
    fun deleteBow() {
        viewModelScope.launch {
            try {
                bowRepository.deleteBow(bowId)
            } catch (t: Throwable) {
                // no-op — best-effort delete. UI stays on-screen if the API fails locally.
            }
        }
    }

    /** Pin/unpin a configuration as the analytics reference. */
    fun setReference(configId: String, pinned: Boolean) {
        viewModelScope.launch {
            runCatching {
                if (pinned) {
                    bowConfigRepository.setReference(bowId, configId, manuallyPinned = true)
                } else {
                    bowConfigRepository.clearReference(bowId, configId)
                }
            }
        }
    }
}
