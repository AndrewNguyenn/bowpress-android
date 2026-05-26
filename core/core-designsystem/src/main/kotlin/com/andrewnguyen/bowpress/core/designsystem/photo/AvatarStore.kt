package com.andrewnguyen.bowpress.core.designsystem.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

/**
 * Mirrors iOS [AvatarStore]. On-disk cache for profile pictures keyed by
 * `(userId, avatarVersion)` — a bumped version on the server's avatarVersion
 * field flips the filename, so a stale picture can never be served once
 * replaced. Older versions for the same archer are pruned whenever a newer
 * one is saved.
 */
object AvatarStore {

    /** Max long-edge of an upload-ready avatar JPEG. */
    private const val MAX_DIMENSION = 512

    /** JPEG quality — matches iOS `compressionQuality: 0.85`. */
    private const val JPEG_QUALITY = 85

    private fun directory(context: Context): File =
        File(context.filesDir, "avatars").apply { mkdirs() }

    private fun safeKey(userId: String) = userId.replace('/', '_')

    private fun file(context: Context, userId: String, version: Int): File =
        File(directory(context), "avatar-${safeKey(userId)}-$version.jpg")

    /** The cached image bytes for an exact `(userId, version)`, if present. */
    fun cachedImage(context: Context, userId: String, version: Int): ByteArray? =
        file(context, userId, version)
            .takeIf { it.exists() }
            ?.runCatching { readBytes() }
            ?.getOrNull()

    /**
     * Writes [data] for `(userId, version)` and drops any older cached
     * versions for the same archer. Returns false on filesystem failure.
     */
    fun save(context: Context, data: ByteArray, userId: String, version: Int): Boolean {
        val dir = directory(context)
        val prefix = "avatar-${safeKey(userId)}-"
        dir.listFiles()?.forEach { existing ->
            if (existing.name.startsWith(prefix)) existing.delete()
        }
        return runCatching { file(context, userId, version).writeBytes(data); true }
            .getOrDefault(false)
    }

    /**
     * Downscales a picked photo (raw bytes) to a square-ish thumbnail and
     * re-encodes it as JPEG, keeping the upload small. Mirrors iOS
     * [AvatarStore.prepareForUpload]. Returns null if the data isn't an image.
     */
    fun prepareForUpload(data: ByteArray, maxDimension: Int = MAX_DIMENSION): ByteArray? {
        val bmp = runCatching { BitmapFactory.decodeByteArray(data, 0, data.size) }
            .getOrNull() ?: return null
        return prepareForUpload(bmp, maxDimension)
    }

    /**
     * Bitmap overload — used by the in-app crop flow so an already-decoded
     * image doesn't get JPEG-encoded just to be decoded again here. Mirrors
     * iOS `prepareForUpload(_ image: UIImage)`.
     */
    fun prepareForUpload(bitmap: Bitmap, maxDimension: Int = MAX_DIMENSION): ByteArray {
        val longest = max(bitmap.width, bitmap.height)
        val scaled = if (longest > maxDimension) {
            val scale = maxDimension.toFloat() / longest
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
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
    }

    /** Convenience: decode + downscale a `content://` Uri to JPEG bytes. */
    fun prepareForUpload(context: Context, uri: Uri, maxDimension: Int = MAX_DIMENSION): ByteArray? {
        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull() ?: return null
        return prepareForUpload(bytes, maxDimension)
    }
}
