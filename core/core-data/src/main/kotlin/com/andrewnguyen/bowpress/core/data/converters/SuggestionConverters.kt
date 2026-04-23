package com.andrewnguyen.bowpress.core.data.converters

import com.andrewnguyen.bowpress.core.database.entities.SuggestionEntity
import com.andrewnguyen.bowpress.core.model.AnalyticsSuggestion
import com.andrewnguyen.bowpress.core.model.SuggestionEvidence
import kotlinx.serialization.json.Json

/**
 * Suggestion evidence is persisted as a JSON blob — the nested `metrics` + `sessionIds`
 * lists would otherwise need their own entities for not much gain. We round-trip via
 * kotlinx.serialization so the on-disk shape stays identical to the wire format.
 */
private val suggestionJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

fun SuggestionEntity.toDto(): AnalyticsSuggestion = AnalyticsSuggestion(
    id = id,
    bowId = bowId,
    createdAt = createdAt,
    parameter = parameter,
    suggestedValue = suggestedValue,
    currentValue = currentValue,
    reasoning = reasoning,
    confidence = confidence,
    qualifier = qualifier,
    wasRead = wasRead,
    wasDismissed = wasDismissed,
    deliveryType = deliveryType,
    evidence = evidenceJson?.let {
        runCatching {
            suggestionJson.decodeFromString(SuggestionEvidence.serializer(), it)
        }.getOrNull()
    },
    wasApplied = wasApplied,
    appliedAt = appliedAt,
    appliedConfigId = appliedConfigId,
)

fun AnalyticsSuggestion.toEntity(): SuggestionEntity = SuggestionEntity(
    id = id,
    bowId = bowId,
    createdAt = createdAt,
    parameter = parameter,
    suggestedValue = suggestedValue,
    currentValue = currentValue,
    reasoning = reasoning,
    confidence = confidence,
    qualifier = qualifier,
    wasRead = wasRead,
    wasDismissed = wasDismissed,
    deliveryType = deliveryType,
    evidenceJson = evidence?.let {
        suggestionJson.encodeToString(SuggestionEvidence.serializer(), it)
    },
    wasApplied = wasApplied,
    appliedAt = appliedAt,
    appliedConfigId = appliedConfigId,
)
