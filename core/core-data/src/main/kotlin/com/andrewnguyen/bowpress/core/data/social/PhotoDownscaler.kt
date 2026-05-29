package com.andrewnguyen.bowpress.core.data.social

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
     *
     * [maxLongEdge] bounds the long edge of the output bitmap; session photos
     * use [MAX_LONG_EDGE] (the default), avatars pass [AVATAR_LONG_EDGE] —
     * a profile picture only ever renders at ≤ 96 dp, so 512 px is plenty and
     * keeps the upload well under the avatar endpoint's 5 MB cap.
     */
    suspend fun downscaleToJpeg(
        uri: Uri,
        maxLongEdge: Int = MAX_LONG_EDGE,
    ): ByteArray? = withContext(Dispatchers.IO) {
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
                inSampleSize = sampleSizeFor(srcLongEdge, maxLongEdge)
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            } ?: return@runCatching null

            val oriented = ExifOrientation.applied(context, uri, decoded)
            val scaled = scaleLongEdge(oriented, maxLongEdge)

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

    companion object {
        /** Upload long-edge cap in px — keeps a typical photo well under 12 MB. */
        const val MAX_LONG_EDGE = 2048

        /**
         * Long-edge cap for profile pictures. Matches iOS `AvatarStore`'s
         * 512 px max dimension — the avatar tile never renders larger than
         * 96 dp, so a 512 px source is already 2× the needed pixel density on
         * a 3× screen, and the resulting JPEG is a few dozen KB.
         */
        const val AVATAR_LONG_EDGE = 512

        /** JPEG re-encode quality. */
        const val JPEG_QUALITY = 85
    }
}
