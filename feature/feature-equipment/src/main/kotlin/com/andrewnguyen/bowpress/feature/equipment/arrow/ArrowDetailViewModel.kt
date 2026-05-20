package com.andrewnguyen.bowpress.feature.equipment.arrow

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.ArrowConfigRepository
import com.andrewnguyen.bowpress.core.data.repository.UnitPreferencesRepository
import com.andrewnguyen.bowpress.core.model.ArrowConfiguration
import com.andrewnguyen.bowpress.core.model.FletchingType
import com.andrewnguyen.bowpress.core.model.ShaftDiameterValidation
import com.andrewnguyen.bowpress.core.model.UnitFormatting
import com.andrewnguyen.bowpress.core.model.UnitSystem
import com.andrewnguyen.bowpress.feature.equipment.nav.EquipmentArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Loads an existing arrow for editing. Shares its [UiState] shape with
 * [AddArrowViewModel] by re-implementing the same fields — keeping them
 * independent avoids entangling creation vs. update flows.
 */
@HiltViewModel
class ArrowDetailViewModel @Inject constructor(
    private val repository: ArrowConfigRepository,
    private val unitPrefs: UnitPreferencesRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val arrowId: String = checkNotNull(savedStateHandle[EquipmentArgs.ARROW_ID])

    data class UiState(
        val arrow: ArrowConfiguration? = null,
        val label: String = "",
        val brand: String = "",
        val model: String = "",
        val length: Double = 28.0,
        val pointWeight: Int = 100,
        val fletchingType: FletchingType = FletchingType.VANE,
        val fletchingLength: Double = 2.0,
        val fletchingOffset: Double = 1.5,
        val nockType: String = "",
        val totalWeightText: String = "",
        // Free-input shaft diameter — parsed/validated on save. See UnitFormatting.
        val shaftDiameterText: String = "",
        val notes: String = "",
        val isLoading: Boolean = true,
        val isSaving: Boolean = false,
        val showSavedBanner: Boolean = false,
        val isDeleted: Boolean = false,
        val errorMessage: String? = null,
    ) {
        val canSave: Boolean get() = arrow != null && label.trim().isNotEmpty() && !isSaving
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val arrow = repository.getById(arrowId)
            val system = unitPrefs.unitSystem.first()
            if (arrow == null) {
                _state.update { it.copy(isLoading = false, errorMessage = "Arrow not found") }
            } else {
                _state.value = UiState(
                    arrow = arrow,
                    label = arrow.label,
                    brand = arrow.brand.orEmpty(),
                    model = arrow.model.orEmpty(),
                    length = arrow.length,
                    pointWeight = arrow.pointWeight,
                    fletchingType = arrow.fletchingType,
                    fletchingLength = arrow.fletchingLength,
                    fletchingOffset = arrow.fletchingOffset,
                    nockType = arrow.nockType.orEmpty(),
                    totalWeightText = arrow.totalWeight?.toString().orEmpty(),
                    shaftDiameterText = arrow.shaftDiameter
                        ?.let { UnitFormatting.shaftDiameterValue(it, system) }.orEmpty(),
                    notes = arrow.notes.orEmpty(),
                    isLoading = false,
                )
            }
        }
        // Re-render the in-flight diameter text on unit-system changes.
        viewModelScope.launch {
            var prev: UnitSystem? = null
            unitPrefs.unitSystem.collect { system ->
                val from = prev
                if (from != null && from != system) {
                    _state.update { st ->
                        val mm = UnitFormatting.parseShaftDiameter(st.shaftDiameterText, from)
                        if (mm != null) {
                            st.copy(shaftDiameterText = UnitFormatting.shaftDiameterValue(mm, system))
                        } else {
                            st
                        }
                    }
                }
                prev = system
            }
        }
    }

    fun updateLabel(v: String) = _state.update { it.copy(label = v) }
    fun updateBrand(v: String) = _state.update { it.copy(brand = v) }
    fun updateModel(v: String) = _state.update { it.copy(model = v) }
    fun updateLength(v: Double) = _state.update { it.copy(length = v.coerceIn(18.0, 36.0)) }
    fun updatePointWeight(v: Int) = _state.update { it.copy(pointWeight = v.coerceIn(50, 300)) }
    fun updateFletchingType(v: FletchingType) = _state.update { it.copy(fletchingType = v) }
    fun updateFletchingLength(v: Double) = _state.update { it.copy(fletchingLength = v.coerceIn(1.0, 5.0)) }
    fun updateFletchingOffset(v: Double) = _state.update { it.copy(fletchingOffset = v.coerceIn(0.0, 10.0)) }
    fun updateNockType(v: String) = _state.update { it.copy(nockType = v) }
    fun updateTotalWeight(v: String) = _state.update { it.copy(totalWeightText = v.filter(Char::isDigit)) }
    fun updateShaftDiameter(v: String) = _state.update { it.copy(shaftDiameterText = v) }
    fun updateNotes(v: String) = _state.update { it.copy(notes = v) }

    fun dismissSavedBanner() = _state.update { it.copy(showSavedBanner = false) }

    fun save() {
        val current = _state.value
        val arrow = current.arrow ?: return
        if (!current.canSave) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val system = unitPrefs.unitSystem.first()
                val diameterMm: Double? = when (
                    val result = UnitFormatting.validateShaftDiameter(current.shaftDiameterText, system)
                ) {
                    is ShaftDiameterValidation.Empty -> null
                    is ShaftDiameterValidation.Valid -> result.mm
                    is ShaftDiameterValidation.Invalid -> {
                        _state.update { it.copy(isSaving = false, errorMessage = result.message) }
                        return@launch
                    }
                }
                val updated = arrow.copy(
                    label = current.label.trim(),
                    brand = current.brand.trim().ifEmpty { null },
                    model = current.model.trim().ifEmpty { null },
                    length = current.length,
                    pointWeight = current.pointWeight,
                    fletchingType = current.fletchingType,
                    fletchingLength = current.fletchingLength,
                    fletchingOffset = current.fletchingOffset,
                    nockType = current.nockType.trim().ifEmpty { null },
                    totalWeight = current.totalWeightText.toIntOrNull(),
                    shaftDiameter = diameterMm,
                    notes = current.notes.trim().ifEmpty { null },
                )
                repository.saveArrowConfig(updated)
                _state.update { it.copy(arrow = updated, isSaving = false, showSavedBanner = true) }
            } catch (t: Throwable) {
                _state.update { it.copy(isSaving = false, errorMessage = t.message ?: "Failed to save arrow") }
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            try {
                repository.deleteArrowConfig(arrowId)
                _state.update { it.copy(isDeleted = true) }
            } catch (t: Throwable) {
                _state.update { it.copy(errorMessage = t.message ?: "Failed to delete arrow") }
            }
        }
    }
}
