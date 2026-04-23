package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Mirrors iOS `Bow`. `bowType`, `brand`, `model` default when absent — older cached
 * responses may not carry them. The primary server source of truth is `bowController.ts`.
 */
@Serializable
data class Bow(
    val id: String,
    val userId: String,
    val name: String,
    val bowType: BowType = BowType.COMPOUND,
    val brand: String = "",
    val model: String = "",
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
)
