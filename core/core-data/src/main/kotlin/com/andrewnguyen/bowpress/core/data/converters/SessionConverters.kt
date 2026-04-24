package com.andrewnguyen.bowpress.core.data.converters

import com.andrewnguyen.bowpress.core.database.entities.SessionEndEntity
import com.andrewnguyen.bowpress.core.database.entities.SessionEntity
import com.andrewnguyen.bowpress.core.model.SessionConditions
import com.andrewnguyen.bowpress.core.model.SessionEnd
import com.andrewnguyen.bowpress.core.model.ShootingSession

fun SessionEntity.toDto(): ShootingSession = ShootingSession(
    id = id,
    bowId = bowId,
    bowConfigId = bowConfigId,
    arrowConfigId = arrowConfigId,
    startedAt = startedAt,
    endedAt = endedAt,
    notes = notes,
    feelTags = feelTags,
    conditions = if (windSpeed == null && tempF == null && lighting == null) null else SessionConditions(windSpeed, tempF, lighting),
    arrowCount = arrowCount,
    targetFaceType = targetFaceType,
    distance = distance,
)

fun ShootingSession.toEntity(pendingSync: Boolean = false): SessionEntity = SessionEntity(
    id = id,
    bowId = bowId,
    bowConfigId = bowConfigId,
    arrowConfigId = arrowConfigId,
    startedAt = startedAt,
    endedAt = endedAt,
    notes = notes,
    feelTags = feelTags,
    arrowCount = arrowCount,
    windSpeed = conditions?.windSpeed,
    tempF = conditions?.tempF,
    lighting = conditions?.lighting,
    targetFaceType = targetFaceType,
    distance = distance,
    pendingSync = pendingSync,
)

fun SessionEndEntity.toDto(): SessionEnd = SessionEnd(
    id = id,
    sessionId = sessionId,
    endNumber = endNumber,
    notes = notes,
    completedAt = completedAt,
)

fun SessionEnd.toEntity(pendingSync: Boolean = false): SessionEndEntity = SessionEndEntity(
    id = id,
    sessionId = sessionId,
    endNumber = endNumber,
    notes = notes,
    completedAt = completedAt,
    pendingSync = pendingSync,
)
