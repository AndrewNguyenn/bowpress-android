package com.andrewnguyen.bowpress.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.andrewnguyen.bowpress.core.model.Zone
import java.time.Instant

/** Mirrors iOS `PersistentArrowPlot`. Index on `sessionId` for fast session lookups. */
@Entity(
    tableName = "arrow_plots",
    indices = [Index("sessionId"), Index("shotAt")],
)
data class ArrowPlotEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val bowConfigId: String,
    val arrowConfigId: String,
    val ring: Int,
    val zone: Zone,
    val plotX: Double? = null,
    val plotY: Double? = null,
    val endId: String? = null,
    val shotAt: Instant,
    val excluded: Boolean = false,
    val notes: String? = null,
    val pendingSync: Boolean = false,
)
