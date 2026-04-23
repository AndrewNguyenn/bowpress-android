package com.andrewnguyen.bowpress.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Mirrors iOS `PersistentSession`. `feelTags` is persisted as a JSON string via the
 * `List<String>` converter.
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val bowId: String,
    val bowConfigId: String,
    val arrowConfigId: String,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val notes: String = "",
    val feelTags: List<String> = emptyList(),
    val arrowCount: Int = 0,
    // Session conditions (iOS stores them inline on the DTO; here we flatten to three nullable columns)
    val windSpeed: Double? = null,
    val tempF: Double? = null,
    val lighting: String? = null,
    val pendingSync: Boolean = false,
)
