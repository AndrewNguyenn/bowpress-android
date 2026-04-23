package com.andrewnguyen.bowpress.feature.equipment.bow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.catalog.BowCatalogRepository
import com.andrewnguyen.bowpress.core.data.catalog.CatalogManufacturer
import com.andrewnguyen.bowpress.core.data.catalog.CatalogModel
import com.andrewnguyen.bowpress.core.data.repository.BowConfigRepository
import com.andrewnguyen.bowpress.core.data.repository.BowRepository
import com.andrewnguyen.bowpress.core.model.Bow
import com.andrewnguyen.bowpress.core.model.BowType
import com.andrewnguyen.bowpress.feature.equipment.components.makeDefaultConfig
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
    private val catalogRepository: BowCatalogRepository,
) : ViewModel() {

    /**
     * Screen state.
     *
     * [manufacturers] is loaded once from assets on init. [selectedManufacturerId]
     * drives the cascading model picker — switching brand clears the model.
     */
    data class UiState(
        val name: String = "",
        val bowType: BowType = BowType.COMPOUND,
        val manufacturers: List<CatalogManufacturer> = emptyList(),
        val selectedManufacturerId: String? = null,
        val selectedModelId: String? = null,
        val isSaving: Boolean = false,
        val errorMessage: String? = null,
        val savedBow: Bow? = null,
    ) {
        /** Models for the current brand, empty list if no brand picked. */
        val availableModels: List<CatalogModel>
            get() = manufacturers.firstOrNull { it.id == selectedManufacturerId }?.models.orEmpty()

        /** Name required; brand + model are optional, matching iOS behaviour. */
        val canSave: Boolean get() = name.trim().isNotEmpty() && !isSaving
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val manufacturers = catalogRepository.manufacturers()
            _state.update { it.copy(manufacturers = manufacturers) }
        }
    }

    fun updateName(name: String) = _state.update { it.copy(name = name) }

    fun updateBowType(type: BowType) = _state.update { it.copy(bowType = type) }

    fun selectManufacturer(id: String?) = _state.update {
        // Changing brand resets the model — the iOS picker does the same.
        it.copy(selectedManufacturerId = id, selectedModelId = null)
    }

    fun selectModel(id: String?) = _state.update { it.copy(selectedModelId = id) }

    fun dismissError() = _state.update { it.copy(errorMessage = null) }

    /**
     * Persist the new bow + seed its initial configuration. Mirrors iOS
     * `AddBowView.save()`: both the bow and a v1 [BowConfiguration] are written
     * locally so a session started immediately after has something to anchor to.
     */
    fun save(userId: String) {
        val current = _state.value
        if (!current.canSave) return

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val manufacturer = current.manufacturers.firstOrNull { it.id == current.selectedManufacturerId }
                val model = manufacturer?.models?.firstOrNull { it.id == current.selectedModelId }
                val newBow = Bow(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    name = current.name.trim(),
                    bowType = current.bowType,
                    brand = manufacturer?.name.orEmpty(),
                    model = model?.name.orEmpty(),
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
