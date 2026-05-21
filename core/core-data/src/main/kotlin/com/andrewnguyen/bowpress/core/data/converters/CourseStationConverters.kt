package com.andrewnguyen.bowpress.core.data.converters

import com.andrewnguyen.bowpress.core.database.entities.CourseStationEntity
import com.andrewnguyen.bowpress.core.model.CourseStation

fun CourseStationEntity.toDto(): CourseStation = CourseStation(
    id = id,
    sessionId = sessionId,
    stationNumber = stationNumber,
    estimatedDistance = estimatedDistance,
    distanceUnit = distanceUnit,
    angleDegrees = angleDegrees,
    bearingDegrees = bearingDegrees,
    latitude = latitude,
    longitude = longitude,
    ring = ring,
    plotX = plotX,
    plotY = plotY,
    hasScenePhoto = hasScenePhoto,
    hasArrowPhoto = hasArrowPhoto,
    shotAt = shotAt,
    notes = notes,
)

fun CourseStation.toEntity(pendingSync: Boolean = false): CourseStationEntity = CourseStationEntity(
    id = id,
    sessionId = sessionId,
    stationNumber = stationNumber,
    estimatedDistance = estimatedDistance,
    distanceUnit = distanceUnit,
    angleDegrees = angleDegrees,
    bearingDegrees = bearingDegrees,
    latitude = latitude,
    longitude = longitude,
    ring = ring,
    plotX = plotX,
    plotY = plotY,
    hasScenePhoto = hasScenePhoto,
    hasArrowPhoto = hasArrowPhoto,
    shotAt = shotAt,
    notes = notes,
    pendingSync = pendingSync,
)
