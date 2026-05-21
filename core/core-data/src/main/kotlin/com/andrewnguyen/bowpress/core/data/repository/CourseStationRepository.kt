package com.andrewnguyen.bowpress.core.data.repository

import com.andrewnguyen.bowpress.core.data.converters.toDto
import com.andrewnguyen.bowpress.core.data.converters.toEntity
import com.andrewnguyen.bowpress.core.data.sync.BackgroundSyncService
import com.andrewnguyen.bowpress.core.database.dao.CourseStationDao
import com.andrewnguyen.bowpress.core.model.CourseStation
import com.andrewnguyen.bowpress.core.network.BowPressApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The 3D counterpart of [PlotRepository] — owns the `course_stations` table,
 * pushes pending writes to the server, and pulls remote state on hydration.
 */
@Singleton
class CourseStationRepository @Inject constructor(
    private val dao: CourseStationDao,
    private val api: BowPressApi,
    private val syncService: BackgroundSyncService,
) {
    fun observeBySession(sessionId: String): Flow<List<CourseStation>> =
        dao.observeBySession(sessionId).map { rows -> rows.map { it.toDto() } }

    fun observeAll(): Flow<List<CourseStation>> =
        dao.observeAll().map { rows -> rows.map { it.toDto() } }

    suspend fun getBySession(sessionId: String): List<CourseStation> =
        dao.findBySession(sessionId).map { it.toDto() }

    suspend fun saveStation(station: CourseStation) {
        dao.upsert(station.toEntity(pendingSync = true))
        syncService.enqueueSync()
    }

    suspend fun deleteStation(station: CourseStation) {
        dao.deleteById(station.id)
        runCatching { api.deleteStation(station.sessionId, station.id) }
    }

    /** Pull remote stations, preserving local pending edits (iOS Fix #4 parity). */
    suspend fun refreshForSession(sessionId: String) {
        val remote = api.fetchStations(sessionId)
        val pendingIds = dao.findPendingSync().mapTo(HashSet()) { it.id }
        val safe = remote.filter { it.id !in pendingIds }
        dao.upsertAll(safe.map { it.toEntity(pendingSync = false) })
    }

    suspend fun flushPendingSync() {
        val pending = dao.findPendingSync()
        for (entity in pending) {
            runCatching {
                api.createStation(entity.sessionId, entity.toDto())
                dao.markSynced(entity.id)
            }
        }
    }
}
