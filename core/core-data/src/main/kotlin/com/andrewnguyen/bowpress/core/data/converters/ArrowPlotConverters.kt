package com.andrewnguyen.bowpress.core.data.converters

import com.andrewnguyen.bowpress.core.database.entities.ArrowPlotEntity
import com.andrewnguyen.bowpress.core.model.ArrowPlot

fun ArrowPlotEntity.toDto(): ArrowPlot = ArrowPlot(
    id = id,
    sessionId = sessionId,
    bowConfigId = bowConfigId,
    arrowConfigId = arrowConfigId,
    ring = ring,
    zone = zone,
    plotX = plotX,
    plotY = plotY,
    endId = endId,
    shotAt = shotAt,
    excluded = excluded,
    notes = notes,
)

fun ArrowPlot.toEntity(pendingSync: Boolean = false): ArrowPlotEntity = ArrowPlotEntity(
    id = id,
    sessionId = sessionId,
    bowConfigId = bowConfigId,
    arrowConfigId = arrowConfigId,
    ring = ring,
    zone = zone,
    plotX = plotX,
    plotY = plotY,
    endId = endId,
    shotAt = shotAt,
    excluded = excluded,
    notes = notes,
    pendingSync = pendingSync,
)
