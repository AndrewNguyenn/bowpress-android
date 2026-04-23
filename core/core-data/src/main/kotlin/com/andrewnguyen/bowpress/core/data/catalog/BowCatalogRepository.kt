package com.andrewnguyen.bowpress.core.data.catalog

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads `BowCatalog.json` from assets once and caches the parsed catalog in memory.
 * Mirrors iOS `BowCatalogLoader` — the JSON blob is a verbatim copy from the iOS
 * bundle so both apps see identical manufacturers / models / colors.
 *
 * Consumers call [manufacturers], [modelsFor], or [manufacturer] after the first
 * load; subsequent calls are served from cache without touching the asset manager.
 */
@Singleton
class BowCatalogRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val mutex = Mutex()
    @Volatile private var cached: BowCatalog? = null

    /** Lazily load + cache. Safe to call from any thread. */
    suspend fun load(): BowCatalog {
        cached?.let { return it }
        return mutex.withLock {
            cached ?: readCatalog().also { cached = it }
        }
    }

    suspend fun manufacturers(): List<CatalogManufacturer> = load().manufacturers

    suspend fun manufacturer(id: String): CatalogManufacturer? =
        load().manufacturers.firstOrNull { it.id == id }

    suspend fun modelsFor(manufacturerId: String): List<CatalogModel> =
        manufacturer(manufacturerId)?.models.orEmpty()

    suspend fun model(id: String): CatalogModel? =
        load().manufacturers.flatMap { it.models }.firstOrNull { it.id == id }

    private suspend fun readCatalog(): BowCatalog = withContext(Dispatchers.IO) {
        runCatching {
            context.assets.open(ASSET_PATH).use { stream ->
                val text = stream.bufferedReader().use { it.readText() }
                json.decodeFromString(BowCatalog.serializer(), text)
            }
        }.getOrElse { BowCatalog(manufacturers = emptyList()) }
    }

    companion object {
        private const val ASSET_PATH = "BowCatalog.json"
    }
}
