package com.andrewnguyen.bowpress.core.data.repository

import com.andrewnguyen.bowpress.core.data.converters.toDto
import com.andrewnguyen.bowpress.core.data.converters.toEntity
import com.andrewnguyen.bowpress.core.data.sync.BackgroundSyncService
import com.andrewnguyen.bowpress.core.database.dao.ArrowConfigDao
import com.andrewnguyen.bowpress.core.model.ArrowConfiguration
import com.andrewnguyen.bowpress.core.network.BowPressApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArrowConfigRepository @Inject constructor(
    private val dao: ArrowConfigDao,
    private val api: BowPressApi,
    private val syncService: BackgroundSyncService,
) {
    fun observeAll(): Flow<List<ArrowConfiguration>> =
        dao.observeAll().map { rows -> rows.map { it.toDto() } }

    suspend fun getAll(): List<ArrowConfiguration> = dao.getAll().map { it.toDto() }

    suspend fun getById(id: String): ArrowConfiguration? = dao.findById(id)?.toDto()

    suspend fun saveArrowConfig(config: ArrowConfiguration) {
        dao.upsert(config.toEntity(pendingSync = true))
        syncService.enqueueSync()
    }

    suspend fun refreshFromRemote() {
        val remote = api.fetchArrowConfigs()
        dao.upsertAll(remote.map { it.toEntity(pendingSync = false) })
    }

    suspend fun deleteArrowConfig(id: String) {
        dao.deleteById(id)
        runCatching { api.deleteArrowConfig(id) }
    }

    suspend fun flushPendingSync() {
        val pending = dao.findPendingSync()
        for (entity in pending) {
            val dto = entity.toDto()
            runCatching {
                api.createArrowConfig(dto)
                dao.markSynced(entity.id)
            }
        }
    }
}
