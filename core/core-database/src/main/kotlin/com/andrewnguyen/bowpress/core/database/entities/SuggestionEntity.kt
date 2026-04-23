package com.andrewnguyen.bowpress.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.andrewnguyen.bowpress.core.model.DeliveryType
import java.time.Instant

/**
 * Mirrors iOS `PersistentSuggestion`. Evidence + applied state are persisted as JSON
 * strings (`evidenceJson`) so the nested `SuggestionEvidence` + metrics don't require
 * extra entities.
 */
@Entity(
    tableName = "suggestions",
    indices = [Index("bowId")],
)
data class SuggestionEntity(
    @PrimaryKey val id: String,
    val bowId: String,
    val createdAt: Instant,
    val parameter: String,
    val suggestedValue: String,
    val currentValue: String,
    val reasoning: String,
    val confidence: Double,
    val qualifier: String? = null,
    val wasRead: Boolean,
    val wasDismissed: Boolean = false,
    val deliveryType: DeliveryType,
    val evidenceJson: String? = null,
    val wasApplied: Boolean = false,
    val appliedAt: Instant? = null,
    val appliedConfigId: String? = null,
)
