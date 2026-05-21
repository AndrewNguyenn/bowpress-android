package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors iOS `SessionType`. What kind of practice a [ShootingSession] records.
 *
 * `RANGE` is the original fixed-distance model; `THREE_D_COURSE` is the 3D
 * field discipline — a GPS-located walk shooting foam-animal targets at
 * varying distances, with ordered [CourseStation]s instead of ends.
 *
 * Legacy rows predate the field, so the wire value defaults to `range`.
 */
@Serializable
enum class SessionType {
    @SerialName("range") RANGE,
    @SerialName("3d_course") THREE_D_COURSE;

    val label: String
        get() = when (this) {
            RANGE -> "Range"
            THREE_D_COURSE -> "3D Course"
        }

    /** One-line subtitle under the type tile on the setup screen. */
    val subtitle: String
        get() = when (this) {
            RANGE -> "Fixed distance"
            THREE_D_COURSE -> "Walked · ranged"
        }

    /** The string the API/DB store. */
    val wire: String
        get() = when (this) {
            RANGE -> "range"
            THREE_D_COURSE -> "3d_course"
        }
}
