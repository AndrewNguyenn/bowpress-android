package com.andrewnguyen.bowpress.core.data.export

import com.andrewnguyen.bowpress.core.database.dao.ExportJobDao
import com.andrewnguyen.bowpress.core.database.entities.ExportJobEntity
import com.andrewnguyen.bowpress.core.model.ExportJob
import com.andrewnguyen.bowpress.core.model.ExportJobState
import com.andrewnguyen.bowpress.core.model.SessionLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SwiftData-parity store for finish-time export jobs (iOS `ExportJobStore` +
 * its `LocalStore` CRUD). Owns the entity↔domain mapping, the reactive
 * [observeAll] stream the feed chip reads, and [enqueue] which stages blobs
 * and persists a fresh `Pending` job. The actual fan-out is driven by
 * [ExportJobWorker]; scheduling is [ExportJobScheduler].
 */
@Singleton
class ExportJobRepository @Inject constructor(
    private val dao: ExportJobDao,
    private val blobStore: ExportJobBlobStore,
) {
    /** Reactive stream of every persisted job, newest first. */
    fun observeAll(): Flow<List<ExportJob>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun getAll(): List<ExportJob> = dao.getAll().map { it.toDomain() }

    suspend fun getById(id: String): ExportJob? = dao.findById(id)?.toDomain()

    /** Persist a full job snapshot, stamping `updatedAt`. */
    suspend fun save(job: ExportJob) {
        dao.upsert(job.copy(updatedAt = Instant.now()).toEntity())
    }

    /** Drop a job's row + its staged blobs. */
    suspend fun remove(id: String) {
        dao.deleteById(id)
        blobStore.purge(id)
    }

    /**
     * Stage the upload payload on disk and persist a fresh `Pending` job.
     * Returns the new job id (the caller schedules the worker with it). The
     * single enqueue entry point for both the range and 3D-course finish
     * paths — score / X / arrowCount are pre-computed by the caller.
     */
    suspend fun enqueue(
        sessionId: String,
        shouldShare: Boolean,
        description: String,
        title: String?,
        location: SessionLocation?,
        photoData: List<ByteArray>,
        score: Int,
        xCount: Int,
        arrowCount: Int,
        distance: String?,
        face: String?,
        shotAt: Instant,
    ): String {
        val jobId = UUID.randomUUID().toString()
        val photoPaths = blobStore.stagePhotos(jobId, photoData)
        val now = Instant.now()
        val job = ExportJob(
            id = jobId,
            sessionId = sessionId,
            sharedSessionId = null,
            shouldShare = shouldShare,
            description = description,
            title = title,
            location = location,
            photoBlobPaths = photoPaths,
            videoBlobPath = null,
            state = ExportJobState.Pending,
            progress = 0.0,
            lastError = null,
            attemptCount = 0,
            photosUploaded = 0,
            score = score,
            xCount = xCount,
            arrowCount = arrowCount,
            distance = distance,
            face = face,
            shotAt = shotAt,
            createdAt = now,
            updatedAt = now,
        )
        dao.upsert(job.toEntity())
        return jobId
    }
}

// ── Mapping ──────────────────────────────────────────────────────────────────

private const val PATH_SEP = "\n"

private fun ExportJobEntity.toDomain(): ExportJob = ExportJob(
    id = id,
    sessionId = sessionId,
    sharedSessionId = sharedSessionId,
    shouldShare = shouldShare,
    description = description,
    title = title,
    location = run {
        // Capture into locals — Room's generated entity properties live in
        // another module, so the compiler can't smart-cast the nullables
        // after the null checks.
        val name = locationName
        val lat = locationLat
        val lng = locationLng
        if (name != null && lat != null && lng != null) {
            SessionLocation(name = name, latitude = lat, longitude = lng)
        } else {
            null
        }
    },
    photoBlobPaths = if (photoBlobPathsRaw.isEmpty()) emptyList() else photoBlobPathsRaw.split(PATH_SEP),
    videoBlobPath = videoBlobPath,
    state = runCatching { ExportJobState.valueOf(state) }.getOrDefault(ExportJobState.Pending),
    progress = progress,
    lastError = lastError,
    attemptCount = attemptCount,
    photosUploaded = photosUploaded,
    score = score,
    xCount = xCount,
    arrowCount = arrowCount,
    distance = distance,
    face = face,
    shotAt = shotAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun ExportJob.toEntity(): ExportJobEntity = ExportJobEntity(
    id = id,
    sessionId = sessionId,
    sharedSessionId = sharedSessionId,
    shouldShare = shouldShare,
    description = description,
    title = title,
    locationName = location?.name,
    locationLat = location?.latitude,
    locationLng = location?.longitude,
    photoBlobPathsRaw = photoBlobPaths.joinToString(PATH_SEP),
    videoBlobPath = videoBlobPath,
    state = state.name,
    progress = progress,
    lastError = lastError,
    attemptCount = attemptCount,
    photosUploaded = photosUploaded,
    score = score,
    xCount = xCount,
    arrowCount = arrowCount,
    distance = distance,
    face = face,
    shotAt = shotAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
