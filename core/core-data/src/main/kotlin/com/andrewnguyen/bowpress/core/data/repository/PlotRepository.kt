package com.andrewnguyen.bowpress.core.data.repository

import com.andrewnguyen.bowpress.core.data.converters.toDto
import com.andrewnguyen.bowpress.core.data.converters.toEntity
import com.andrewnguyen.bowpress.core.data.sync.BackgroundSyncService
import com.andrewnguyen.bowpress.core.database.dao.ArrowPlotDao
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.network.BowPressApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlotRepository @Inject constructor(
    private val dao: ArrowPlotDao,
    private val api: BowPressApi,
    private val syncService: BackgroundSyncService,
) {
    fun observeBySession(sessionId: String): Flow<List<ArrowPlot>> =
        dao.observeBySession(sessionId).map { rows -> rows.map { it.toDto() } }

    fun observeAll(): Flow<List<ArrowPlot>> =
        dao.observeAll().map { rows -> rows.map { it.toDto() } }

    suspend fun getBySession(sessionId: String): List<ArrowPlot> =
        dao.findBySession(sessionId).map { it.toDto() }

    suspend fun findSince(since: Instant): List<ArrowPlot> =
        dao.findSince(since).map { it.toDto() }

    suspend fun getAll(): List<ArrowPlot> = dao.getAll().map { it.toDto() }

    suspend fun savePlot(plot: ArrowPlot) {
        dao.upsert(plot.toEntity(pendingSync = true))
        syncService.enqueueSync()
    }

    suspend fun refreshForSession(sessionId: String) {
        val remote = api.fetchPlots(sessionId)
        dao.upsertAll(remote.map { it.toEntity(pendingSync = false) })
    }

    suspend fun deletePlot(plot: ArrowPlot) {
        dao.deleteById(plot.id)
        runCatching { api.deletePlot(plot.sessionId, plot.id) }
    }

    suspend fun flushPendingSync() {
        val pending = dao.findPendingSync()
        for (entity in pending) {
            val dto = entity.toDto()
            runCatching {
                api.plotArrow(entity.sessionId, dto)
                dao.markSynced(entity.id)
            }
        }
    }
}
