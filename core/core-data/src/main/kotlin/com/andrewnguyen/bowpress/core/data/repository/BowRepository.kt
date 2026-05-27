package com.andrewnguyen.bowpress.core.data.repository

import com.andrewnguyen.bowpress.core.data.converters.toDto
import com.andrewnguyen.bowpress.core.data.converters.toEntity
import com.andrewnguyen.bowpress.core.data.sync.BackgroundSyncService
import com.andrewnguyen.bowpress.core.database.dao.BowDao
import com.andrewnguyen.bowpress.core.model.Bow
import com.andrewnguyen.bowpress.core.network.BowPressApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bows are persisted locally with `pendingSync=true` on every write; the background
 * sync worker flushes them to the API later. Reads expose a [Flow] off Room — Room is
 * the single source of truth. Mirrors iOS `LocalStore.fetchBows` / `save(bow:)`.
 */
@Singleton
class BowRepository @Inject constructor(
    private val dao: BowDao,
    private val api: BowPressApi,
    private val syncService: BackgroundSyncService,
) {

    fun observeBows(): Flow<List<Bow>> =
        dao.observeAll().map { rows -> rows.map { it.toDto() } }

    suspend fun getBows(): List<Bow> = dao.getAll().map { it.toDto() }

    suspend fun getBow(id: String): Bow? = dao.findById(id)?.toDto()

    /** Insert / replace a bow locally, mark it pending-sync, nudge the sync worker. */
    suspend fun saveBow(bow: Bow) {
        dao.upsert(bow.toEntity(pendingSync = true))
        syncService.enqueueSync()
    }

    /**
     * Rename an existing bow. Unlike [saveBow] (which routes via the
     * pending-sync queue → POST /bows for new bows), an existing bow already
     * exists server-side, so we PUT /bows/{id} directly and upsert the
     * authoritative response into the local cache.
     */
    suspend fun updateBowName(id: String, newName: String): Bow {
        val existing = dao.findById(id)?.toDto() ?: error("Bow $id not found locally")
        val updated = api.updateBow(id, existing.copy(name = newName))
        dao.upsert(updated.toEntity(pendingSync = false))
        return updated
    }

    /**
     * Pull the latest bows from the server and upsert them. Caller is responsible for
     * handling [com.andrewnguyen.bowpress.core.network.ApiException] (e.g. surfacing an
     * error toast). Remote data overwrites local because the server is authoritative
     * for non-pending records.
     */
    suspend fun refreshFromRemote() {
        val remote = api.fetchBows()
        if (remote.isEmpty()) return
        // Skip remote rows that have local pending edits — iOS Fix #4 parity:
        // a stale server snapshot must never overwrite a local write that
        // hasn't drained through the sync worker yet.
        val pendingIds = dao.findPendingSync().mapTo(HashSet()) { it.id }
        val safe = remote.filter { it.id !in pendingIds }
        dao.upsertAll(safe.map { it.toEntity(pendingSync = false) })
    }

    suspend fun deleteBow(id: String) {
        dao.deleteById(id)
        // Delete is surfaced to the backend via the dedicated endpoint rather than pendingSync.
        runCatching { api.deleteBow(id) }
    }

    /**
     * Drain the pending-sync queue. Called by the background worker; public so manual
     * retries can reuse the same path.
     */
    suspend fun flushPendingSync() {
        val pending = dao.findPendingSync()
        for (bow in pending) {
            val dto = bow.toDto()
            runCatching {
                api.createBow(dto)
                dao.markSynced(bow.id)
            }
        }
    }
}
