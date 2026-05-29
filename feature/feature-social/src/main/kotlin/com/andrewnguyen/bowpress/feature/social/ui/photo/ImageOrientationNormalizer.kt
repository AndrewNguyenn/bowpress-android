package com.andrewnguyen.bowpress.feature.social.ui.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.andrewnguyen.bowpress.core.data.social.ExifOrientation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * Normalises a picked image's EXIF orientation **before** it reaches uCrop.
 *
 * uCrop (2.2.8) reads source orientation with a hand-rolled JPEG-segment
 * parser ([com.yalantis.ucrop.util.ImageHeaderParser]) that misses the tag for
 * some sources — HEIC/HEIF frames and re-wrapped `content://` streams from the
 * modern photo picker. When it misses, uCrop bakes **no** rotation and writes
 * a sideways frame (its output `Orientation` tag is "0"/undefined), which
 * neither downstream encoder corrects: `FinishSheet`'s
 * `BitmapFactory.decodeByteArray` ignores EXIF, and `PhotoDownscaler` only
 * re-reads that already-zeroed uCrop-output tag. The net result is a photo
 * that renders rotated 90° on the feed (the sideways-archer bug).
 *
 * [ExifOrientation] (androidx ExifInterface) reads orientation reliably across
 * `content://` URIs and HEIF on every supported API level (minSdk 26). We apply
 * it here, write an upright, orientation-stripped temp JPEG, and feed **that**
 * to uCrop — so uCrop always sees an already-upright image and bakes nothing.
 * There is no double-rotation risk: the temp file carries no orientation tag,
 * AND uCrop's own output is orientation-normal too (it re-encodes via
 * `Bitmap.compress`, which writes no orientation tag), so the avatar /
 * edit-gallery paths that re-run `ExifOrientation.applied` on the cropped
 * output stay a no-op. (A future cropper swap must preserve that to keep the
 * chain safe.)
 *
 * This is the single chokepoint for all three crop call sites (FinishSheet,
 * the shared-session edit gallery, and avatar), since they all launch through
 * [PhotoCropperHost].
 */
internal object ImageOrientationNormalizer {

    /** Prefix for the upright temp files we drop in `cacheDir`. */
    private const val TEMP_PREFIX = "oriented_"

    /**
     * True ceiling on the long edge of the upright temp. The eventual upload is
     * bounded to 2048 px anyway (PhotoDownscaler / TargetPhotoStore), so there
     * is no quality argument for decoding the full sensor frame just to rotate
     * it — and the rotate allocates a *second* bitmap, so an unbounded source
     * is a real OOM on big phones. Matching the upload cap keeps peak memory in
     * check with no visible loss (uCrop and the downscaler both re-encode here
     * or below).
     */
    private const val MAX_LONG_EDGE = 2048

    /** JPEG quality for the upright temp — high; uCrop re-encodes after crop. */
    private const val JPEG_QUALITY = 95

    /**
     * Return a URI whose pixels are already upright. The common case — an image
     * that needs no rotation (orientation normal/undefined, or unreadable EXIF)
     * — returns [source] unchanged so it pays nothing and uCrop keeps full
     * resolution. On **any** failure this also falls back to [source]: a
     * normalise hiccup must never block the crop, only forgo the correction.
     *
     * Self-cleaning: each call first sweeps any earlier temp before writing a
     * new one. Crops are modal and sequential (uCrop is a full-screen Activity
     * drained one-at-a-time), so at most one temp is ever live — this bounds
     * `cacheDir` residue to a single file even if a crop is dismissed or
     * cancelled mid-flight, with no Compose-state lifetime to get wrong.
     */
    suspend fun normalizeForCrop(context: Context, source: Uri): Uri =
        withContext(Dispatchers.IO) {
            sweepStaleTemps(context)
            runCatching {
                val matrix = ExifOrientation.matrixFor(ExifOrientation.read(context, source))
                    ?: return@runCatching source
                val decoded = decodeBounded(context, source) ?: return@runCatching source
                val scaled = scaleLongEdge(decoded, MAX_LONG_EDGE)
                val upright = Bitmap.createBitmap(
                    scaled, 0, 0, scaled.width, scaled.height, matrix, true,
                )
                val out = File.createTempFile(TEMP_PREFIX, ".jpg", context.cacheDir)
                FileOutputStream(out).use {
                    upright.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it)
                }
                if (upright !== scaled) upright.recycle()
                if (scaled !== decoded) scaled.recycle()
                decoded.recycle()
                Uri.fromFile(out)
            }.getOrDefault(source)
        }

    /** Delete any upright temp left by an earlier (possibly abandoned) crop. */
    private fun sweepStaleTemps(context: Context) {
        runCatching {
            context.cacheDir.listFiles { f -> f.name.startsWith(TEMP_PREFIX) }
                ?.forEach { it.delete() }
        }
    }

    /** Decode [uri] subsampled so its long edge stays within [MAX_LONG_EDGE]. */
    private fun decodeBounded(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        val srcLongEdge = max(bounds.outWidth, bounds.outHeight)
        if (srcLongEdge <= 0) return null
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(srcLongEdge, MAX_LONG_EDGE)
        }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    }

    /** Largest power-of-two subsample that keeps the long edge >= [target]. */
    private fun sampleSizeFor(srcLongEdge: Int, target: Int): Int {
        var sample = 1
        while (srcLongEdge / (sample * 2) >= target) sample *= 2
        return sample
    }

    /**
     * Exact-scale the long edge down to [maxLongEdge]; no-op when already
     * within. `inSampleSize` only lands the decode *at or above* the cap (it's
     * a floor), so this turns it into a true ceiling before the rotate
     * allocates its second bitmap — keeping peak memory bounded.
     */
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
}
