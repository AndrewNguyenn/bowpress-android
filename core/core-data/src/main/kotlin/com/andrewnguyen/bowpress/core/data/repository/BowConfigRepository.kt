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
     * Pin [configId] as the reference for [bowId]. Clears the flags on every other
     * config for the same bow so the "only one pinned per bow" invariant holds
     * locally before/after the server round-trip (matches iOS `updateReferencePin`).
     */
    suspend fun setReference(bowId: String, configId: String, manuallyPinned: Boolean) {
        val siblings = dao.findByBow(bowId)
        for (entity in siblings) {
            val shouldBeRef = entity.id == configId
            val nextIsRef = shouldBeRef
            val nextPinned = shouldBeRef && manuallyPinned
            if (entity.isReference != nextIsRef || entity.referenceManuallyPinned != nextPinned) {
                dao.upsert(
                    entity.copy(
                        isReference = nextIsRef,
                        referenceManuallyPinned = nextPinned,
                        pendingSync = entity.id == configId || entity.pendingSync,
                    ),
                )
            }
        }
        runCatching {
            val updated = api.updateBowConfiguration(
                configId,
                UpdateBowConfigRequest(
                    isReference = true,
                    referenceManuallyPinned = manuallyPinned,
                ),
            )
            dao.upsert(updated.toEntity(pendingSync = false))
        }.onFailure { syncService.enqueueSync() }
    }

    /** Unpin [configId] as the reference for [bowId]. */
    suspend fun clearReference(bowId: String, configId: String) {
        val entity = dao.findById(configId) ?: return
        dao.upsert(
            entity.copy(
                isReference = false,
                referenceManuallyPinned = false,
                pendingSync = true,
            ),
        )
        runCatching {
            val updated = api.updateBowConfiguration(
                configId,
                UpdateBowConfigRequest(
                    isReference = false,
                    referenceManuallyPinned = false,
                ),
            )
            dao.upsert(updated.toEntity(pendingSync = false))
        }.onFailure { syncService.enqueueSync() }
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
