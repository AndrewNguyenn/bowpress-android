package com.andrewnguyen.bowpress.feature.social.ui.session

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.andrewnguyen.bowpress.core.designsystem.AppCream
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.ActivityPhoto
import com.andrewnguyen.bowpress.core.model.PhotoStatus

// ── Session photo gallery ────────────────────────────────────────────────────
//
// Renders shared-session photos on the detail screen and as a feed-row
// preview. The photo endpoint is Bearer-gated, so bytes are fetched through
// the SocialRepository (via [SessionPhotoLoader]) rather than a plain URL —
// Coil cannot reach the endpoint without the auth header.

/**
 * Async loader for one shared-session photo's display bytes. A thin functional
 * abstraction so screens / previews can fetch a photo without holding a
 * repository reference directly — the ViewModel supplies the implementation.
 */
fun interface SessionPhotoLoader {
    /** Fetch the display-JPEG bytes for [photoId], or null when unavailable. */
    suspend fun load(sharedSessionId: String, photoId: String): ByteArray?
}

/** Decode state for one [RemoteSessionPhoto]. */
private sealed interface PhotoLoadState {
    data object Loading : PhotoLoadState
    data class Loaded(val bitmap: ImageBitmap) : PhotoLoadState
    data object Unavailable : PhotoLoadState
}

/**
 * One photo tile — fetches the display JPEG through [loader], decodes it, and
 * renders it. A `pending` photo (still transcoding server-side) shows a
 * spinner; a `failed`/missing one shows a muted placeholder. The decode runs
 * once per (sharedSessionId, photoId) pair.
 *
 * [contentScale] controls the fit — `Crop` (the default) for the cover-fit
 * grid tiles, `Fit` for the full-screen viewer. [background] is the letterbox
 * ground shown while loading and behind a `Fit`-scaled photo. [border] draws
 * the 1dp `AppLine` hairline frame — `true` for the standalone Part-1 tiles,
 * `false` for the photo strip, whose 1dp grid seams come from the strip's own
 * `AppLine` background showing through the gutters (a per-cell border there
 * would double the internal seams and add a 4-side outer frame).
 *
 * [onUnavailable] fires with [photo.id] once the tile resolves to the
 * placeholder state — i.e. the feed believed the photo was `ready` but its
 * bytes 404 / fail to decode (e.g. a partially-completed delete left an
 * orphaned `ready` row). The feed card uses this to drop the photo and fall
 * back to the full-width scorecard instead of showing a blank pane. Defaults
 * to null — contexts that should keep showing the placeholder (detail gallery,
 * full-screen viewer) simply don't pass it.
 */
@Composable
fun RemoteSessionPhoto(
    sharedSessionId: String,
    photo: ActivityPhoto,
    loader: SessionPhotoLoader,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    background: androidx.compose.ui.graphics.Color = AppCream,
    border: Boolean = true,
    onUnavailable: ((String) -> Unit)? = null,
) {
    var state by remember(sharedSessionId, photo.id) {
        mutableStateOf<PhotoLoadState>(PhotoLoadState.Loading)
    }

    LaunchedEffect(sharedSessionId, photo.id, photo.status) {
        if (photo.status != PhotoStatus.ready) {
            // pending → keep the spinner; failed/unknown → placeholder.
            if (photo.status == PhotoStatus.pending) {
                state = PhotoLoadState.Loading
            } else {
                state = PhotoLoadState.Unavailable
                onUnavailable?.invoke(photo.id)
            }
            return@LaunchedEffect
        }
        state = PhotoLoadState.Loading
        val bytes = loader.load(sharedSessionId, photo.id)
        val resolved = if (bytes != null) {
            val bmp = runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
                .getOrNull()
            if (bmp != null) PhotoLoadState.Loaded(bmp.asImageBitmap()) else PhotoLoadState.Unavailable
        } else {
            PhotoLoadState.Unavailable
        }
        state = resolved
        // A `ready` photo whose bytes are gone — let the host collapse it.
        if (resolved is PhotoLoadState.Unavailable) onUnavailable?.invoke(photo.id)
    }

    Box(
        modifier = modifier
            .background(background)
            .then(if (border) Modifier.border(1.dp, AppLine) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        when (val s = state) {
            is PhotoLoadState.Loading -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = AppPondDk,
            )
            is PhotoLoadState.Loaded -> Image(
                bitmap = s.bitmap,
                contentDescription = "Session photo",
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
            is PhotoLoadState.Unavailable -> Text(
                text = "—",
                style = frauncesDisplay(16.sp),
                color = AppInk3,
            )
        }
    }
}

/** Fixed feed-row photo-preview band height. */
private val FEED_GALLERY_HEIGHT = 116.dp

/**
 * The photo gallery on a feed row — a horizontally scrollable strip of the
 * session's photos. Rendered in place of the discipline preview when the
 * shared session has photos (Social Feed V2 §4).
 */
@Composable
fun FeedPhotoGallery(
    sharedSessionId: String,
    photos: List<ActivityPhoto>,
    loader: SessionPhotoLoader,
    modifier: Modifier = Modifier,
) {
    if (photos.isEmpty()) return
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .height(FEED_GALLERY_HEIGHT)
            .testTag(TestTags.FeedRowPhotoGallery),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(photos, key = { it.id }) { photo ->
            RemoteSessionPhoto(
                sharedSessionId = sharedSessionId,
                photo = photo,
                loader = loader,
                modifier = Modifier
                    .height(FEED_GALLERY_HEIGHT)
                    .aspectRatio(1f),
            )
        }
    }
}

/**
 * The photo gallery on the session detail screen — a full-width scrollable
 * strip of large tiles. When [onRemovePhoto] is non-null (owner-editable
 * mode), tapping a tile removes that photo.
 */
@Composable
fun DetailPhotoGallery(
    sharedSessionId: String,
    photos: List<ActivityPhoto>,
    loader: SessionPhotoLoader,
    modifier: Modifier = Modifier,
    onRemovePhoto: ((ActivityPhoto) -> Unit)? = null,
) {
    if (photos.isEmpty()) return
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .testTag(TestTags.SessionDetailPhotoGallery),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(photos, key = { it.id }) { photo ->
            Box(
                modifier = Modifier
                    .height(220.dp)
                    .aspectRatio(0.78f)
                    .then(
                        if (onRemovePhoto != null) {
                            Modifier.clickable { onRemovePhoto(photo) }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                RemoteSessionPhoto(
                    sharedSessionId = sharedSessionId,
                    photo = photo,
                    loader = loader,
                    modifier = Modifier.fillMaxSize(),
                )
                if (onRemovePhoto != null) {
                    // "Tap to remove" affordance — a small maple ✕ chip.
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(AppCream)
                            .border(1.dp, AppMaple)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "✕ remove",
                            style = frauncesDisplay(10.sp, italic = false),
                            color = AppMaple,
                        )
                    }
                }
            }
        }
    }
}
