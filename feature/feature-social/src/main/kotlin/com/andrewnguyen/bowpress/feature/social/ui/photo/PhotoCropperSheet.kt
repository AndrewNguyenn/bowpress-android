package com.andrewnguyen.bowpress.feature.social.ui.photo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.yalantis.ucrop.UCrop
import java.util.UUID

/**
 * Crop aspect mode — mirrors iOS [PhotoCropMode].
 *
 * `.Square` locks the crop box to 1:1 and hides the aspect picker — used for
 * profile avatars, which render in a circular frame and must look right at
 * any scale.
 *
 * `.Free` lets the archer drag the crop box to any aspect — used for session
 * photos, where a wide target paper, a tall arrow-on-foam close-up, and a
 * square stage shot are all valid.
 */
enum class PhotoCropMode {
    Square,
    Free,
}

/**
 * Identifiable wrapper around a source `Uri` so a caller can drive a FIFO
 * cropper queue without colliding Uri equality with the cropper's re-entry
 * (mirrors iOS `PendingCropImage`). Each pending image is a one-shot — picking
 * a new photo creates a new wrapper.
 */
data class PendingCropImage(
    val id: String = UUID.randomUUID().toString(),
    val source: Uri,
)

/**
 * Compose-friendly bridge to uCrop's Activity-based crop API — the Android
 * Mantis-equivalent that mirrors iOS [PhotoCropperSheet] in
 * `Sources/BowPress/Components/PhotoCropperSheet.swift`. Used as a building
 * block from three call sites: avatar (1:1 locked), session-end FinishSheet
 * (free-aspect), and the shared-session edit gallery (free-aspect, FIFO).
 *
 * uCrop is launch-an-Activity-with-an-Intent shaped, so this composable owns:
 *  - the [UCropContract] that boxes the Intent build + result parse,
 *  - a one-shot `LaunchedEffect` that fires the launcher whenever
 *    [PhotoCropperLaunch.image] flips non-null,
 *  - the callback semantics — `onResult(Uri?)`: non-null = cropped, null =
 *    cancelled / failed (same convention as iOS).
 *
 * **Side-effect-free until launched.** A composable that just declares this
 * doesn't trigger anything; the host pushes a [PendingCropImage] into
 * [PhotoCropperLaunch.image] when ready, this fires the activity, then the
 * host clears [PhotoCropperLaunch.image] in the result callback to disarm.
 *
 * @sample
 * ```
 * var crop by remember { mutableStateOf<PendingCropImage?>(null) }
 * PhotoCropperHost(
 *     launch = PhotoCropperLaunch(image = crop, mode = PhotoCropMode.Free),
 *     onResult = { uri ->
 *         crop = null
 *         if (uri != null) handleCropped(uri)
 *     },
 * )
 * ```
 */
data class PhotoCropperLaunch(
    val image: PendingCropImage?,
    val mode: PhotoCropMode,
)

@Composable
fun PhotoCropperHost(
    launch: PhotoCropperLaunch,
    onResult: (croppedUri: Uri?) -> Unit,
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(UCropContract(context)) { croppedUri ->
        onResult(croppedUri)
    }
    // Track the last-launched id so a re-composition with the same wrapper
    // doesn't fire uCrop a second time. A new pick produces a new id, which
    // flips this back to "fire". Cancelled/empty image → reset to null.
    var lastLaunchedId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(launch.image?.id, launch.mode) {
        val pending = launch.image
        if (pending == null) {
            lastLaunchedId = null
            return@LaunchedEffect
        }
        if (lastLaunchedId == pending.id) return@LaunchedEffect
        lastLaunchedId = pending.id
        val params = CropParams(source = pending.source, mode = launch.mode)
        launcher.launch(params)
    }
}

/**
 * Inputs for an in-flight crop — the source Uri and the aspect mode. Boxed
 * for [UCropContract.createIntent].
 */
internal data class CropParams(val source: Uri, val mode: PhotoCropMode)

/**
 * `ActivityResultContract` wrapping uCrop's Intent build + parse. Keeps the
 * Compose call site terse and the destination-Uri creation honest (a fresh
 * cache file per launch — uCrop refuses to overwrite an existing output).
 *
 * @return the cropped image Uri on success (the Intent's `OUTPUT_URI` extra),
 *         or `null` on cancel / no-result. Errors are surfaced through
 *         uCrop's own RESULT_ERROR — we treat them as null for the caller,
 *         since the host wants a binary "did the archer get a cropped image
 *         or not" answer; logging is best-effort.
 */
internal class UCropContract(private val context: Context) : ActivityResultContract<CropParams, Uri?>() {

    override fun createIntent(context: Context, input: CropParams): Intent {
        // Destination must be writable + unique per launch — a stale file in
        // the cache would skip the crop and feed back the previous image.
        val dest = Uri.fromFile(
            java.io.File.createTempFile("ucrop_", ".jpg", context.cacheDir),
        )
        val options = UCrop.Options().apply {
            setCompressionFormat(android.graphics.Bitmap.CompressFormat.JPEG)
            // Quality matches iOS Mantis output — high but not lossless,
            // because we re-encode the result through
            // TargetPhotoStore.downscaledForUpload for upload anyway. 92
            // gives a faithful preview without inflating the cache file.
            setCompressionQuality(92)
            setHideBottomControls(false)
            setFreeStyleCropEnabled(input.mode == PhotoCropMode.Free)
            // Kenrokuen palette so the cropper doesn't look like a foreign
            // surface dropped on top of the Compose app. uCrop themes via
            // resource ints — we sample the design tokens here.
            setToolbarColor(AppPaper.toArgb())
            setStatusBarColor(AppInk.toArgb())
            setActiveControlsWidgetColor(AppPondDk.toArgb())
            setToolbarWidgetColor(AppInk.toArgb())
        }
        val builder = UCrop.of(input.source, dest).withOptions(options)
        val configured = when (input.mode) {
            PhotoCropMode.Square -> builder.withAspectRatio(1f, 1f)
            PhotoCropMode.Free -> builder
        }
        return configured.getIntent(context)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if (resultCode == Activity.RESULT_OK && intent != null) {
            return UCrop.getOutput(intent)
        }
        // RESULT_CANCELED, RESULT_ERROR, or missing intent — treat as cancel.
        return null
    }
}

/**
 * Helper to bridge a Compose [androidx.compose.ui.graphics.Color] to the
 * `@ColorInt` Int that uCrop expects on its setter API. Compose's `toArgb()`
 * already returns ARGB; mirror name for site-call clarity.
 */
private fun androidx.compose.ui.graphics.Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt(),
)
