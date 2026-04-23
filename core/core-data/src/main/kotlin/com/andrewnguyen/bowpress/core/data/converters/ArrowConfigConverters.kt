package com.andrewnguyen.bowpress.core.data.converters

import com.andrewnguyen.bowpress.core.database.entities.ArrowConfigEntity
import com.andrewnguyen.bowpress.core.model.ArrowConfiguration

fun ArrowConfigEntity.toDto(): ArrowConfiguration = ArrowConfiguration(
    id = id,
    userId = userId,
    label = label,
    brand = brand,
    model = model,
    length = length,
    pointWeight = pointWeight,
    fletchingType = fletchingType,
    fletchingLength = fletchingLength,
    fletchingOffset = fletchingOffset,
    nockType = nockType,
    totalWeight = totalWeight,
    shaftDiameter = shaftDiameter,
    notes = notes,
)

fun ArrowConfiguration.toEntity(pendingSync: Boolean = false): ArrowConfigEntity = ArrowConfigEntity(
    id = id,
    userId = userId,
    label = label,
    brand = brand,
    model = model,
    length = length,
    pointWeight = pointWeight,
    fletchingType = fletchingType,
    fletchingLength = fletchingLength,
    fletchingOffset = fletchingOffset,
    nockType = nockType,
    totalWeight = totalWeight,
    shaftDiameter = shaftDiameter,
    notes = notes,
    pendingSync = pendingSync,
)
