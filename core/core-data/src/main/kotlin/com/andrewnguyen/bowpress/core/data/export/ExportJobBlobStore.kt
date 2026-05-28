package com.andrewnguyen.bowpress.core.data.export

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-disk staging for an [com.andrewnguyen.bowpress.core.model.ExportJob]'s
 * upload payload — the JPEG bytes the share fan-out needs to POST.
 *
 * Mirrors iOS `ExportJobBlobStore`. Distinct from `TargetPhotoStore` (which
 * caches media for *display* and must outlive the share): these blobs are the
 * *transmission source*, deleted the moment the job reaches a terminal state.
 * They live under `cacheDir` so the OS can reclaim them under pressure — if a
 * blob is evicted before upload, the job degrades to a text-only post rather
 * than wedging (the share inputs live in the durable Room row regardless).
 *
 * Layout: `cacheDir/export-job-blobs/{jobId}/photo-{n}.jpg`.
 */
@Singleton
class ExportJobBlobStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val root: File
        get() = File(context.cacheDir, "export-job-blobs")

    fun directory(jobId: String): File = File(root, jobId)

    /**
     * Write [photos] into the job's directory (in pick order) and return their
     * absolute paths. A failed individual write drops that blob silently — the
     * job simply uploads fewer items, matching the best-effort contract.
     */
    fun stagePhotos(jobId: String, photos: List<ByteArray>): List<String> {
        val dir = directory(jobId)
        dir.mkdirs()
        val paths = mutableListOf<String>()
        photos.forEachIndexed { index, bytes ->
            val dest = File(dir, "photo-$index.jpg")
            val ok = runCatching { dest.writeBytes(bytes) }
                .onFailure { Log.w(TAG, "stage photo $index failed for $jobId", it) }
                .isSuccess
            if (ok) paths.add(dest.absolutePath)
        }
        return paths
    }

    /** Read a staged blob back for upload; null when the file is gone (cache eviction). */
    fun readPhoto(path: String): ByteArray? =
        runCatching { File(path).readBytes() }.getOrNull()

    /** Remove a job's entire blob directory. Safe on a missing directory. */
    fun purge(jobId: String) {
        runCatching { directory(jobId).deleteRecursively() }
    }

    private companion object {
        const val TAG = "ExportJobBlobStore"
    }
}
