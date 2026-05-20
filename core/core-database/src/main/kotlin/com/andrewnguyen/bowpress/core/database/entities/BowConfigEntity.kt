package com.andrewnguyen.bowpress.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.andrewnguyen.bowpress.core.model.RearStabSide
import java.time.Instant

/** Mirrors iOS `PersistentBowConfig`. Single flat table — no joins. */
@Entity(tableName = "bow_configurations")
data class BowConfigEntity(
    @PrimaryKey val id: String,
    val bowId: String,
    val createdAt: Instant,
    val label: String? = null,

    val drawLength: Double,
    val letOffPct: Double? = null,

    val peepHeight: Double? = null,
    val dLoopLength: Double? = null,
    val topCableTwists: Int? = null,
    val bottomCableTwists: Int? = null,
    val mainStringTopTwists: Int? = null,
    val mainStringBottomTwists: Int? = null,
    val topLimbTurns: Double? = null,
    val bottomLimbTurns: Double? = null,

    val restVertical: Int,
    val restHorizontal: Int,
    val restDepth: Double,

    val sightPosition: Int? = null,
    /** Measured riser-to-pin distance, stored as an inches value. */
    val sightPinDistance: Double? = null,
    val gripAngle: Double,
    val nockingHeight: Int,

    val specificGrip: String? = null,
    val specificLimbs: String? = null,

    val frontStabWeight: Double? = null,
    val frontStabAngle: Double? = null,

    val rearStabSide: RearStabSide? = null,
    val rearStabWeight: Double? = null,
    val rearStabVertAngle: Double? = null,
    val rearStabHorizAngle: Double? = null,

    val braceHeight: Double? = null,
    val tillerTop: Double? = null,
    val tillerBottom: Double? = null,
    val plungerTension: Int? = null,
    val clickerPosition: Double? = null,
    val rearStabLeftWeight: Double? = null,
    val rearStabRightWeight: Double? = null,

    // Server-computed analytics fields
    val isReference: Boolean = false,
    val referenceManuallyPinned: Boolean = false,
    val avgArrowScore: Double? = null,
    val scoreable: Boolean = false,

    val pendingSync: Boolean = false,
)
