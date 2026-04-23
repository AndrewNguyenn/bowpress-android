package com.andrewnguyen.bowpress.core.data.repository

import com.andrewnguyen.bowpress.core.data.converters.toDto
import com.andrewnguyen.bowpress.core.data.converters.toEntity
import com.andrewnguyen.bowpress.core.data.sync.BackgroundSyncService
import com.andrewnguyen.bowpress.core.database.dao.BowConfigDao
import com.andrewnguyen.bowpress.core.model.BowConfiguration
import com.andrewnguyen.bowpress.core.network.BowPressApi
import com.andrewnguyen.bowpress.core.network.UpdateBowConfigRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Mirrors iOS `LocalStore` bow-config methods. */
@Singleton
class BowConfigRepository @Inject constructor(
    private val dao: BowConfigDao,
    private val api: BowPressApi,
    private val syncService: BackgroundSyncService,
) {
    fun observeByBow(bowId: String): Flow<List<BowConfiguration>> =
        dao.observeByBow(bowId).map { rows -> rows.map { it.toDto() } }

    suspend fun getByBow(bowId: String): List<BowConfiguration> =
        dao.findByBow(bowId).map { it.toDto() }

    suspend fun getById(id: String): BowConfiguration? =
        dao.findById(id)?.toDto()

    suspend fun saveConfig(config: BowConfiguration) {
        dao.upsert(config.toEntity(pendingSync = true))
        syncService.enqueueSync()
    }

    suspend fun refreshForBow(bowId: String) {
        val remote = api.fetchBowConfigurations(bowId)
        dao.upsertAll(remote.map { it.toEntity(pendingSync = false) })
    }

    /**
     * Toggle the "reference" pin. The server recomputes once the pipeline sees a new
     * session, so we write the flag locally + push through the typed PATCH endpoint.
     */
    suspend fun setReference(id: String, isReference: Boolean): BowConfiguration {
        val updated = api.updateBowConfiguration(id, UpdateBowConfigRequest(isReference = isReference))
        dao.upsert(updated.toEntity(pendingSync = false))
        return updated
    }

    suspend fun deleteConfig(id: String) {
        dao.deleteById(id)
        runCatching { api.deleteBowConfiguration(id) }
    }

    suspend fun flushPendingSync() {
        val pending = dao.findPendingSync()
        for (entity in pending) {
            val dto = entity.toDto()
            runCatching {
                api.createBowConfiguration(dto)
                dao.markSynced(entity.id)
            }
        }
    }
}
