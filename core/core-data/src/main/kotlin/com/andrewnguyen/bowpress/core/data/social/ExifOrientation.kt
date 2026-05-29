package com.andrewnguyen.bowpress.core.data.social

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface

/**
 * Shared EXIF-orientation helpers built on androidx [ExifInterface].
 *
 * androidx [ExifInterface] reads orientation reliably across `content://` URIs
 * and HEIF on every supported API level — unlike the platform
 * `android.media.ExifInterface` (limited stream/HEIF support) and unlike
 * uCrop's hand-rolled JPEG-segment parser (which misses the tag on some
 * sources and ships sideways frames). Both photo-upload consumers route their
 * orientation math through here so there is one correct, fully-cased
 * implementation rather than two subtly-divergent copies:
 *
 *  - [PhotoDownscaler] applies orientation when re-encoding a picked photo.
 *  - `ImageOrientationNormalizer` (feature-social) normalises a source upright
 *    before uCrop.
 */
object ExifOrientation {

    /** Read the EXIF orientation tag from [uri], or NORMAL when unreadable. */
    fun read(context: Context, uri: Uri): Int =
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    /**
     * Transform matrix for an [orientation] that needs correcting, or null when
     * the image is already upright (NORMAL / UNDEFINED / unrecognised) so the
     * caller can skip allocating a rotated copy.
     */
    fun matrixFor(orientation: Int): Matrix? {
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                m.postRotate(90f); m.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                m.postRotate(270f); m.postScale(-1f, 1f)
            }
            else -> return null
        }
        return m
    }

    /**
     * Return [bitmap] rotated/flipped to match [uri]'s EXIF orientation, or the
     * same instance unchanged when no transform is needed. A new bitmap is only
     * allocated when a transform applies, so callers can compare by reference
     * (`result !== bitmap`) to decide recycling.
     */
    fun applied(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val matrix = matrixFor(read(context, uri)) ?: return bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
