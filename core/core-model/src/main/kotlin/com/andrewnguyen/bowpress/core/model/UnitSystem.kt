package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * User-selectable unit system. Mirrors iOS `UnitSystem` — wire value is the
 * lower-case enum name so it can ride through any preference persistence layer
 * (DataStore, server-side user settings) without transformation.
 *
 * Persistence is shallow: switching systems never mutates stored BowConfiguration
 * or ArrowConfiguration values — every numeric field keeps its canonical unit
 * (inches / grains / ounces / mm / degrees / percent). Only display and input
 * conversion observe the active system.
 */
@Serializable
enum class UnitSystem {
    @SerialName("imperial") IMPERIAL,
    @SerialName("metric") METRIC;

    val label: String
        get() = when (this) {
            IMPERIAL -> "Imperial"
            METRIC -> "Metric"
        }

    companion object {
        /** Key used by DataStore / any future preference layer. */
        const val STORAGE_KEY = "unitSystem"
        val DEFAULT = IMPERIAL
    }
}
