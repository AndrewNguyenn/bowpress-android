package com.andrewnguyen.bowpress.core.data.repository

import com.andrewnguyen.bowpress.core.data.converters.toDto
import com.andrewnguyen.bowpress.core.data.converters.toEntity
import com.andrewnguyen.bowpress.core.data.sync.BackgroundSyncService
import com.andrewnguyen.bowpress.core.database.dao.ArrowPlotDao
import com.andrewnguyen.bowpress.core.database.dao.SessionDao
import com.andrewnguyen.bowpress.core.database.dao.SessionEndDao
import com.andrewnguyen.bowpress.core.model.ShootingSession
import com.andrewnguyen.bowpress.core.network.BowPressApi
import com.andrewnguyen.bowpress.core.network.EndSessionRequest
import com.andrewnguyen.bowpress.core.network.UpdateSessionRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val dao: SessionDao,
    private val arrowPlotDao: ArrowPlotDao,
    private val sessionEndDao: SessionEndDao,
    private val api: BowPressApi,
    private val syncService: BackgroundSyncService,
) {
    /**
     * Only completed sessions — iOS filters `endedAt != nil` in `LocalStore.fetchSessions`.
     * `arrowCount` is recomputed from the live plot table so it reflects reality even
     * when `SessionEntity.arrowCount` was never updated after plotting (the source of
     * truth is the plot table, same as iOS).
     */
    fun observeCompleted(): Flow<List<ShootingSession>> =
        combine(
            dao.observeCompleted(),
            arrowPlotDao.observeAll(),
        ) { sessions, plots ->
            val countBySession = plots.groupingBy { it.sessionId }.eachCount()
            sessions.map { entity ->
                entity.toDto().copy(arrowCount = countBySession[entity.id] ?: 0)
            }
        }

    fun observeActiveSession(): Flow<ShootingSession?> =
        dao.observeActiveSession().map { it?.toDto() }

    suspend fun getAll(): List<ShootingSession> = dao.getAll().map { it.toDto() }

    suspend fun getById(id: String): ShootingSession? = dao.findById(id)?.toDto()

    suspend fun findSince(since: Instant): List<ShootingSession> =
        dao.findSince(since).map { it.toDto() }

    suspend fun saveSession(session: ShootingSession) {
        dao.upsert(session.toEntity(pendingSync = true))
        syncService.enqueueSync()
    }

    suspend fun endSession(sessionId: String, endedAt: Instant, notes: String) {
        val existing = dao.findById(sessionId) ?: return
        dao.upsert(existing.copy(endedAt = endedAt, notes = notes, pendingSync = true))
        runCatching { api.endSession(sessionId, EndSessionRequest(endedAt, notes)) }
        syncService.enqueueSync()
    }

    /**
     * Edit notes + feel tags on an already-completed session. Writes
     * locally first, then syncs; pendingSync stays true until the server
     * confirms so [flushPendingSync] can retry on the next connectivity
     * event.
     */
    suspend fun updateSession(sessionId: String, notes: String, feelTags: List<String>) {
        val existing = dao.findById(sessionId) ?: return
        dao.upsert(existing.copy(notes = notes, feelTags = feelTags, pendingSync = true))
        runCatching {
            api.updateSession(sessionId, UpdateSessionRequest(notes, feelTags))
            dao.markSynced(sessionId)
        }
        syncService.enqueueSync()
    }

    suspend fun refreshFromRemote() {
        val remote = api.fetchSessions()
        dao.upsertAll(remote.map { it.toEntity(pendingSync = false) })
    }

    suspend fun deleteSession(id: String) {
        arrowPlotDao.deleteBySession(id)
        sessionEndDao.deleteBySession(id)
        dao.deleteById(id)
        runCatching { api.deleteSession(id) }
    }

    suspend fun flushPendingSync() {
        val pending = dao.findPendingSync()
        for (entity in pending) {
            val dto = entity.toDto()
            runCatching {
                api.createSession(dto)
                dao.markSynced(entity.id)
            }
        }
    }
}
