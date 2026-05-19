package com.andrewnguyen.bowpress.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Persistent counterpart of [com.andrewnguyen.bowpress.core.model.SightMark].
 * Single flat table, indexed on `bowId` for the per-bow list query path.
 * `distanceUnit` is stored as the enum name string for forward compat.
 */
@Entity(tableName = "sight_marks")
data class SightMarkEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val bowId: String,
    val distance: Double,
    val distanceUnit: String, // "YARDS" or "METERS"
    val mark: Double,
    val note: String? = null,
    val isSuggestion: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
    val pendingSync: Boolean = false,
)
