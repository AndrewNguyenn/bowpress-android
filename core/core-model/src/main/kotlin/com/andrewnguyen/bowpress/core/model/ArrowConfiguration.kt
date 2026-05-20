package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.Serializable

/**
 * Mirrors iOS `ArrowConfiguration`. `shaftDiameter` is wire-transported as a Double
 * millimetre value — free-input, see [UnitFormatting.parseShaftDiameter].
 */
@Serializable
data class ArrowConfiguration(
    val id: String,
    val userId: String,
    val label: String,
    val brand: String? = null,
    val model: String? = null,
    val length: Double,
    val pointWeight: Int,
    val fletchingType: FletchingType,
    val fletchingLength: Double,
    val fletchingOffset: Double,
    val nockType: String? = null,
    val totalWeight: Int? = null,
    /** Outside shaft diameter in millimetres. Free-input — see [UnitFormatting.parseShaftDiameter]. */
    val shaftDiameter: Double? = null,
    val notes: String? = null,
) {
    /**
     * One-line arrow spec used in Session and Log surfaces.
     * e.g. `28.5" · 110 gr · vane` (imperial) or `72.4 cm · 7.1 g · vane` (metric).
     */
    fun specSummary(system: UnitSystem): String {
        val len = UnitFormatting.length(inches = length, system = system)
        val mass = UnitFormatting.arrowMass(grains = pointWeight, system = system)
        return "$len · $mass · ${fletchingType.name.lowercase()}"
    }
}
