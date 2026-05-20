package com.andrewnguyen.bowpress.feature.equipment.arrow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.ArrowConfigRepository
import com.andrewnguyen.bowpress.core.data.repository.UnitPreferencesRepository
import com.andrewnguyen.bowpress.core.model.ArrowConfiguration
import com.andrewnguyen.bowpress.core.model.FletchingType
import com.andrewnguyen.bowpress.core.model.ShaftDiameterValidation
import com.andrewnguyen.bowpress.core.model.UnitFormatting
import com.andrewnguyen.bowpress.core.model.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Backs [AddArrowScreen]. Arrow configs are simpler than bow configs — no
 * conditional sections — so the form is flat.
 */
@HiltViewModel
class AddArrowViewModel @Inject constructor(
    private val repository: ArrowConfigRepository,
    private val unitPrefs: UnitPreferencesRepository,
) : ViewModel() {

    data class UiState(
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
        val isSaving: Boolean = false,
        val savedArrowId: String? = null,
        val errorMessage: String? = null,
    ) {
        val canSave: Boolean get() = label.trim().isNotEmpty() && !isSaving
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        // Re-render the in-flight diameter text when the user flips unit systems,
        // so a value typed as mm reads back correctly once shown as inches.
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

    fun save(userId: String) {
        val current = _state.value
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
                val arrow = ArrowConfiguration(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
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
                repository.saveArrowConfig(arrow)
                _state.update { it.copy(isSaving = false, savedArrowId = arrow.id) }
            } catch (t: Throwable) {
                _state.update { it.copy(isSaving = false, errorMessage = t.message ?: "Failed to save arrow") }
            }
        }
    }
}
