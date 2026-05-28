package com.andrewnguyen.bowpress.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Mirrors iOS `PersistentExportJob`. The durable record of a session's
 * finish-time share fan-out — on-disk blob paths plus the pre-computed share
 * inputs — so the share survives an app kill and `ExportJobWorker` can resume
 * it. `sharedSessionId` is the idempotency hinge; `photosUploaded` is the
 * resume watermark.
 *
 * `photoBlobPaths` is stored as a newline-joined string (`photoBlobPathsRaw`)
 * rather than going through a TypeConverter — the paths never contain
 * newlines (they're cache-dir file paths) and keeping the join local avoids
 * widening the shared `Converters` surface for a single table.
 */
@Entity(
    tableName = "export_jobs",
    indices = [Index("sessionId")],
)
data class ExportJobEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val sharedSessionId: String? = null,
    val shouldShare: Boolean,
    val description: String,
    val title: String? = null,
    val locationName: String? = null,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    /** Newline-joined absolute cache-dir blob paths, in pick order. */
    val photoBlobPathsRaw: String = "",
    /** Reserved for the deferred Android video pipeline (task #23). */
    val videoBlobPath: String? = null,
    /** `ExportJobState.name`. */
    val state: String,
    val progress: Double = 0.0,
    val lastError: String? = null,
    val attemptCount: Int = 0,
    val photosUploaded: Int = 0,
    val score: Int,
    val xCount: Int,
    val arrowCount: Int,
    val distance: String? = null,
    val face: String? = null,
    val shotAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
)
