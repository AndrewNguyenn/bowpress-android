package com.andrewnguyen.bowpress.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.andrewnguyen.bowpress.core.model.ShootingDistance
import com.andrewnguyen.bowpress.core.model.TargetFaceType
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
    // Target face (WA 6-ring vs 10-ring). Added in schema v2; legacy rows default to SIX_RING
    // which matches the renderer behaviour prior to the migration. The SQL DEFAULT clause is
    // required so the migration and any future raw INSERTs satisfy the NOT NULL constraint;
    // it also makes the Room schema validator happy when diffing against the migration.
    @ColumnInfo(defaultValue = "SIX_RING")
    val targetFaceType: TargetFaceType = TargetFaceType.SIX_RING,
    // Optional shooting distance (e.g. 20yd / 50m / 70m). Added in schema v3 — nullable
    // by design so legacy rows stay null and the analytics filter only matches when set.
    val distance: ShootingDistance? = null,
    val pendingSync: Boolean = false,
)
