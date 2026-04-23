package com.andrewnguyen.bowpress.core.data.catalog

import kotlinx.serialization.Serializable

/**
 * Kotlin port of iOS `BowCatalog` / `CatalogModels.swift`. Wire format matches the
 * JSON under `core-designsystem/src/main/assets/BowCatalog.json` — which is a verbatim
 * copy of the iOS resource. Keep the shape aligned; both platforms share this file.
 */
@Serializable
data class BowCatalog(
    val manufacturers: List<CatalogManufacturer> = emptyList(),
)

@Serializable
data class CatalogManufacturer(
    val id: String,
    val name: String,
    val models: List<CatalogModel> = emptyList(),
)

@Serializable
data class CatalogModel(
    val id: String,
    val name: String,
    val ata: Double,
    val braceHeight: Double,
    val weight: Double,
    val iboSpeed: Int,
    val drawLengthMin: Double,
    val drawLengthMax: Double,
    val letOffOptions: List<Int> = emptyList(),
    val drawWeightOptions: List<Int> = emptyList(),
    val colors: List<CatalogColor> = emptyList(),
)

@Serializable
data class CatalogColor(
    val id: String,
    val name: String,
    val hex: String,
    val imageUrl: String? = null,
)
