package com.andrewnguyen.bowpress.core.designsystem.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

/**
 * Mirrors iOS [TargetPhotoStore]. On-disk store for the optional "physical
 * target paper" photo an archer can attach when finishing a session, plus
 * the shared [downscaledForUpload] helper used by every photo-upload path.
 *
 * The photo is deliberately **local-only**: it lives as a JPEG in
 * `filesDir/target_photos/<sessionId>.jpg` and is never part of the
 * `ShootingSession` sync DTO. There is no server image-hosting endpoint for
 * the log-thumbnail use, so uploading it is a separate follow-up — keeping
 * it off the DTO means a round-tripped session can't clobber a local photo.
 *
 * File existence is the single source of truth: [hasPhoto] / [load] just
 * probe the filesystem, so no Room schema change is required.
 */
object TargetPhotoStore {

    /** Longest edge a stored photo is downscaled to. */
    private const val MAX_DIMENSION = 2048

    /** JPEG quality — matches iOS `compressionQuality: 0.8`. */
    private const val JPEG_QUALITY = 80

    private fun directory(context: Context): File =
        File(context.filesDir, "target_photos").apply { mkdirs() }

    private fun file(context: Context, sessionId: String): File =
        File(directory(context), "$sessionId.jpg")

    fun hasPhoto(context: Context, sessionId: String): Boolean =
        file(context, sessionId).exists()

    /** Raw stored JPEG bytes. nil when no photo is attached. */
    fun data(context: Context, sessionId: String): ByteArray? =
        file(context, sessionId).takeIf { it.exists() }
            ?.runCatching { readBytes() }?.getOrNull()

    fun load(context: Context, sessionId: String): Bitmap? {
        val f = file(context, sessionId)
        if (!f.exists()) return null
        return runCatching { BitmapFactory.decodeFile(f.absolutePath) }.getOrNull()
    }

    /**
     * Persist raw image data (as picked from the photo library) for a
     * session, downscaling + re-encoding to a bounded JPEG first. Returns
     * false if the write fails. Mirrors iOS `save(_:for:)`.
     */
    fun save(context: Context, data: ByteArray, sessionId: String): Boolean {
        val encoded = downscaledJPEG(data) ?: data
        return runCatching { file(context, sessionId).writeBytes(encoded); true }
            .getOrDefault(false)
    }

    /** Removes a session's photo. Safe to call when none exists. */
    fun delete(context: Context, sessionId: String) {
        file(context, sessionId).delete()
    }

    // ── Shared downscale helper ──────────────────────────────────────────────
    //
    // Used by every photo-upload path (FinishSheet, MySessionEditSheet) so the
    // ~2048px-edge q0.8 treatment is consistent. Mirrors iOS
    // `downscaledForUpload(_ image:)`. Callers that start from raw bytes
    // (rare — the crop flow always has a decoded Bitmap) should decode then
    // pass through this helper to avoid the bytes→bitmap→bytes round-trip.

    /**
     * Bitmap overload — used by the in-app crop flow so an already-decoded
     * image doesn't get JPEG-encoded just to be decoded again here. Always
     * returns a JPEG; on encoding failure returns empty bytes (the upload
     * then fails server-side and the caller surfaces its existing error path).
     */
    fun downscaledForUpload(bitmap: Bitmap): ByteArray = downscaledJPEG(bitmap)

    /** Resize if larger than [MAX_DIMENSION] + encode once as JPEG. */
    private fun downscaledJPEG(data: ByteArray): ByteArray? {
        val bmp = runCatching { BitmapFactory.decodeByteArray(data, 0, data.size) }
            .getOrNull() ?: return null
        return downscaledJPEG(bmp)
    }

    /** Resize if larger than [MAX_DIMENSION] + encode once as JPEG. */
    private fun downscaledJPEG(bitmap: Bitmap): ByteArray {
        val longest = max(bitmap.width, bitmap.height)
        val target = if (longest > MAX_DIMENSION) {
            val scale = MAX_DIMENSION.toFloat() / longest
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                /* filter = */ true,
            )
        } else {
            bitmap
        }
        return ByteArrayOutputStream().use { out ->
            target.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
    }
}
