package com.andrewnguyen.bowpress.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.andrewnguyen.bowpress.core.model.SessionType
import com.andrewnguyen.bowpress.core.model.ShootingDistance
import com.andrewnguyen.bowpress.core.model.TargetFaceType
import com.andrewnguyen.bowpress.core.model.TargetLayout
import com.andrewnguyen.bowpress.core.model.ThreeDScoringSystem
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
    // Target layout — single vs 3-spot triangle/vertical. Added in schema v12;
    // the SQL DEFAULT keeps the additive AutoMigration NOT-NULL-safe and legacy
    // rows render as a single face (the prior behaviour).
    @ColumnInfo(defaultValue = "SINGLE")
    val targetLayout: TargetLayout = TargetLayout.SINGLE,
    // Optional shooting distance (e.g. 20yd / 50m / 70m). Added in schema v3 — nullable
    // by design so legacy rows stay null and the analytics filter only matches when set.
    val distance: ShootingDistance? = null,
    // Optional human-supplied session title. Added in schema v4 — nullable so legacy
    // rows stay null and the renderer falls back to "Range · {distance}"-style defaults.
    val title: String? = null,
    // Practice discipline — range vs 3D course. Added in schema v15; the SQL
    // DEFAULT keeps the additive AutoMigration NOT-NULL-safe and legacy rows
    // decode as RANGE (the prior, only, behaviour).
    @ColumnInfo(defaultValue = "RANGE")
    val sessionType: SessionType = SessionType.RANGE,
    // 3D scoring system — set only for 3D-course sessions. Added in schema v15;
    // nullable so range rows stay null.
    val scoringSystem: ThreeDScoringSystem? = null,
    val pendingSync: Boolean = false,
)
