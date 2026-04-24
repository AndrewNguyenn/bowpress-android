package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.Serializable

/**
 * Mirrors iOS `ArrowConfiguration`. `shaftDiameter` is wire-transported as a Double
 * millimetre value — see [ShaftDiameter.rawValue].
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
    /**
     * Wire value is a Double matching one of [ShaftDiameter.rawValue]. We keep the raw
     * number on the DTO so unfamiliar values don't blow up decoding; use
     * [shaftDiameter] to snap to the enum.
     */
    val shaftDiameter: Double? = null,
    val notes: String? = null,
) {
    /** Enum resolution of [shaftDiameter]. Returns null if the raw value doesn't match a known entry. */
    val shaftDiameterEnum: ShaftDiameter?
        get() = ShaftDiameter.fromRaw(shaftDiameter)

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
