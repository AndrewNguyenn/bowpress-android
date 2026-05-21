package com.andrewnguyen.bowpress.core.data.social

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Downscales a picked photo to an upload-ready JPEG (Social Feed V2 §4).
 *
 * The system photo picker ([androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia])
 * hands back a `content://` URI to a full-resolution image — often many MB.
 * The shared-session photo endpoint rejects bodies over 12 MB, and a full
 * camera frame is wasteful to ship. This bounds the long edge to
 * [MAX_LONG_EDGE] px and re-encodes as JPEG at [JPEG_QUALITY], which keeps a
 * typical phone photo comfortably under the limit.
 *
 * EXIF orientation is honoured — many cameras store a sideways frame plus a
 * rotation tag; decoding without applying it would upload a rotated image.
 */
@Singleton
class PhotoDownscaler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Read [uri], downscale + rotate it, and return the JPEG bytes. Returns
     * null when the URI cannot be read or decoded (a revoked permission, an
     * unsupported format) so the caller can skip that pick instead of failing
     * the whole batch.
     */
    suspend fun downscaleToJpeg(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            // Pass 1 — bounds only, so a huge image never inflates into memory.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val srcLongEdge = max(bounds.outWidth, bounds.outHeight)
            if (srcLongEdge <= 0) return@runCatching null

            // Pass 2 — decode at a power-of-two subsample close to the target.
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(srcLongEdge, MAX_LONG_EDGE)
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            } ?: return@runCatching null

            val oriented = applyExifOrientation(uri, decoded)
            val scaled = scaleLongEdge(oriented, MAX_LONG_EDGE)

            ByteArrayOutputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                if (scaled !== oriented) scaled.recycle()
                if (oriented !== decoded) oriented.recycle()
                decoded.recycle()
                out.toByteArray()
            }
        }.getOrNull()
    }

    /** Largest power-of-two subsample that keeps the long edge ≥ [target]. */
    private fun sampleSizeFor(srcLongEdge: Int, target: Int): Int {
        var sample = 1
        while (srcLongEdge / (sample * 2) >= target) sample *= 2
        return sample
    }

    /** Exact-scale the long edge down to [maxLongEdge]; no-op when already smaller. */
    private fun scaleLongEdge(bitmap: Bitmap, maxLongEdge: Int): Bitmap {
        val longEdge = max(bitmap.width, bitmap.height)
        if (longEdge <= maxLongEdge) return bitmap
        val ratio = maxLongEdge.toFloat() / longEdge
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            /* filter = */ true,
        )
    }

    /** Rotate [bitmap] to match the source image's EXIF orientation tag. */
    private fun applyExifOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    companion object {
        /** Upload long-edge cap in px — keeps a typical photo well under 12 MB. */
        const val MAX_LONG_EDGE = 2048

        /** JPEG re-encode quality. */
        const val JPEG_QUALITY = 85
    }
}
