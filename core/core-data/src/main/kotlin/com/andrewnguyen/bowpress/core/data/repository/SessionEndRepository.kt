package com.andrewnguyen.bowpress.core.data.repository

import com.andrewnguyen.bowpress.core.data.converters.toDto
import com.andrewnguyen.bowpress.core.data.converters.toEntity
import com.andrewnguyen.bowpress.core.data.sync.BackgroundSyncService
import com.andrewnguyen.bowpress.core.database.dao.SessionEndDao
import com.andrewnguyen.bowpress.core.model.SessionEnd
import com.andrewnguyen.bowpress.core.network.BowPressApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionEndRepository @Inject constructor(
    private val dao: SessionEndDao,
    private val api: BowPressApi,
    private val syncService: BackgroundSyncService,
) {
    fun observeBySession(sessionId: String): Flow<List<SessionEnd>> =
        dao.observeBySession(sessionId).map { rows -> rows.map { it.toDto() } }

    suspend fun getBySession(sessionId: String): List<SessionEnd> =
        dao.findBySession(sessionId).map { it.toDto() }

    suspend fun saveEnd(end: SessionEnd) {
        dao.upsert(end.toEntity(pendingSync = true))
        syncService.enqueueSync()
    }

    suspend fun refreshForSession(sessionId: String) {
        val remote = api.fetchEnds(sessionId)
        // Preserve local pending edits — iOS Fix #4 parity.
        val pendingIds = dao.findPendingSync().mapTo(HashSet()) { it.id }
        val safe = remote.filter { it.id !in pendingIds }
        dao.upsertAll(safe.map { it.toEntity(pendingSync = false) })
    }

    suspend fun flushPendingSync() {
        val pending = dao.findPendingSync()
        for (entity in pending) {
            val dto = entity.toDto()
            runCatching {
                api.completeEnd(entity.sessionId, dto)
                dao.markSynced(entity.id)
            }
        }
    }
}
