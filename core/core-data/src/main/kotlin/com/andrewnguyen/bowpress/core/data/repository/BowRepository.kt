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
     * Pull the latest bows from the server and upsert them. Caller is responsible for
     * handling [com.andrewnguyen.bowpress.core.network.ApiException] (e.g. surfacing an
     * error toast). Remote data overwrites local because the server is authoritative
     * for non-pending records.
     */
    suspend fun refreshFromRemote() {
        val remote = api.fetchBows()
        if (remote.isEmpty()) return
        dao.upsertAll(remote.map { it.toEntity(pendingSync = false) })
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
