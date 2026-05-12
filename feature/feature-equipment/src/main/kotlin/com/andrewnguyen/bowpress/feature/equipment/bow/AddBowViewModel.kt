package com.andrewnguyen.bowpress.feature.equipment.bow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.BowConfigRepository
import com.andrewnguyen.bowpress.core.data.repository.BowRepository
import com.andrewnguyen.bowpress.core.model.Bow
import com.andrewnguyen.bowpress.core.model.BowType
import com.andrewnguyen.bowpress.core.data.config.makeDefaultConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddBowViewModel @Inject constructor(
    private val bowRepository: BowRepository,
    private val bowConfigRepository: BowConfigRepository,
) : ViewModel() {

    data class UiState(
        val name: String = "",
        val bowType: BowType = BowType.COMPOUND,
        val isSaving: Boolean = false,
        val errorMessage: String? = null,
        val savedBow: Bow? = null,
    ) {
        val canSave: Boolean get() = name.trim().isNotEmpty() && !isSaving
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun updateName(name: String) = _state.update { it.copy(name = name) }

    fun updateBowType(type: BowType) = _state.update { it.copy(bowType = type) }

    fun dismissError() = _state.update { it.copy(errorMessage = null) }

    /**
     * Persist the new bow + seed its initial configuration. Mirrors iOS
     * `AddBowView.save()`: bow + v1 BowConfiguration written locally so a
     * session started immediately has something to anchor to. iOS saves
     * brand/model as empty strings.
     */
    fun save(userId: String) {
        val current = _state.value
        if (!current.canSave) return

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val newBow = Bow(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    name = current.name.trim(),
                    bowType = current.bowType,
                    brand = "",
                    model = "",
                    createdAt = Instant.now(),
                )
                bowRepository.saveBow(newBow)
                bowConfigRepository.saveConfig(makeDefaultConfig(newBow))
                _state.update { it.copy(isSaving = false, savedBow = newBow) }
            } catch (t: Throwable) {
                _state.update { it.copy(isSaving = false, errorMessage = t.message ?: "Failed to save bow") }
            }
        }
    }
}
