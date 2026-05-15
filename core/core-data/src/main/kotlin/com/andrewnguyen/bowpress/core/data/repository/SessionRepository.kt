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

    /**
     * Pull remote and upsert, but **skip rows that have local pending edits**.
     * iOS Fix #4: a cold-launch hydration after force-quit was blowing away
     * the local `endedAt`/notes/feelTags on a just-finished session because
     * the server hadn't received the write yet. Same pattern existed here:
     * `upsertAll(remote)` overwrote the local pendingSync row with the stale
     * server snapshot, so the user landed back into the active-session screen.
     *
     * Fix mirrors iOS LocalStore.refreshSessionsPreservingPending: collect
     * ids with `pendingSync = true`, then only upsert remote rows whose id
     * is NOT in that set. Pending rows are left untouched; the sync worker
     * pushes them on the next drain.
     */
    suspend fun refreshFromRemote() {
        val remote = api.fetchSessions()
        val pendingIds = dao.findPendingSync().mapTo(HashSet()) { it.id }
        val safe = remote.filter { it.id !in pendingIds }
        dao.upsertAll(safe.map { it.toEntity(pendingSync = false) })
    }

    suspend fun deleteSession(id: String) {
        arrowPlotDao.deleteBySession(id)
        sessionEndDao.deleteBySession(id)
        dao.deleteById(id)
        runCatching { api.deleteSession(id) }
    }

    /**
     * Delete a single end (and its arrows) from an in-progress session.
     * Local + remote cleanup; mirrors iOS [APIClient.deleteEnd] + [LocalStore.deleteEnd].
     */
    suspend fun deleteEnd(sessionId: String, endId: String) {
        sessionEndDao.deleteById(endId)
        arrowPlotDao.deleteByEndId(endId)
        runCatching { api.deleteEnd(sessionId, endId) }
        syncService.enqueueSync()
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
