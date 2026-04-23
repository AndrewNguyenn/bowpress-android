package com.andrewnguyen.bowpress.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.andrewnguyen.bowpress.core.model.FletchingType

/** Mirrors iOS `PersistentArrowConfig`. */
@Entity(tableName = "arrow_configurations")
data class ArrowConfigEntity(
    @PrimaryKey val id: String,
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
    /** Shaft diameter is stored as the raw mm value (see [com.andrewnguyen.bowpress.core.model.ShaftDiameter.rawValue]). */
    val shaftDiameter: Double? = null,
    val notes: String? = null,
    val pendingSync: Boolean = false,
)
