package com.andrewnguyen.bowpress.core.data.converters

import com.andrewnguyen.bowpress.core.database.entities.SightMarkEntity
import com.andrewnguyen.bowpress.core.model.DistanceUnit
import com.andrewnguyen.bowpress.core.model.SightMark

fun SightMark.toEntity(pendingSync: Boolean = false): SightMarkEntity = SightMarkEntity(
    id = id,
    userId = userId,
    bowId = bowId,
    distance = distance,
    distanceUnit = distanceUnit.name,
    mark = mark,
    note = note,
    isSuggestion = isSuggestion,
    createdAt = createdAt,
    updatedAt = updatedAt,
    pendingSync = pendingSync,
)

fun SightMarkEntity.toDto(): SightMark = SightMark(
    id = id,
    userId = userId,
    bowId = bowId,
    distance = distance,
    distanceUnit = runCatching { DistanceUnit.valueOf(distanceUnit) }
        .getOrDefault(DistanceUnit.YARDS),
    mark = mark,
    note = note,
    isSuggestion = isSuggestion,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
