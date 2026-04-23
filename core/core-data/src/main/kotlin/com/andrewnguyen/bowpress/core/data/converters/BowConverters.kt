package com.andrewnguyen.bowpress.core.data.converters

import com.andrewnguyen.bowpress.core.database.entities.BowEntity
import com.andrewnguyen.bowpress.core.model.Bow

/** DTO ↔ entity converters for `Bow`. Mirrors iOS `PersistentBow.from` / `toDTO`. */
fun BowEntity.toDto(): Bow = Bow(
    id = id,
    userId = userId,
    name = name,
    bowType = bowType,
    brand = brand,
    model = model,
    createdAt = createdAt,
)

fun Bow.toEntity(pendingSync: Boolean = false): BowEntity = BowEntity(
    id = id,
    userId = userId,
    name = name,
    bowType = bowType,
    brand = brand,
    model = model,
    createdAt = createdAt,
    pendingSync = pendingSync,
)
