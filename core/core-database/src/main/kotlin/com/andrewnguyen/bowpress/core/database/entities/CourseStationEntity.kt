package com.andrewnguyen.bowpress.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Mirrors iOS `PersistentCourseStation`. One foam-target shot on a 3D course
 * — the 3D counterpart of [ArrowPlotEntity]. Added in schema v15.
 */
@Entity(
    tableName = "course_stations",
    indices = [Index("sessionId")],
)
data class CourseStationEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val stationNumber: Int,
    val estimatedDistance: Double? = null,
    val distanceUnit: String? = null,
    val angleDegrees: Double = 0.0,
    val bearingDegrees: Double? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val ring: Int = 0,
    val plotX: Double? = null,
    val plotY: Double? = null,
    val hasScenePhoto: Boolean = false,
    val hasArrowPhoto: Boolean = false,
    val shotAt: Instant,
    val notes: String? = null,
    val pendingSync: Boolean = false,
)
