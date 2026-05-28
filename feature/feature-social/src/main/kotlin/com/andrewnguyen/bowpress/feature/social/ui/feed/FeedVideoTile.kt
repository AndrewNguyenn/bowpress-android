package com.andrewnguyen.bowpress.feature.social.ui.feed

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.andrewnguyen.bowpress.core.data.social.LocalVideoStore
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppLine2
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPine
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.model.ActivityVideo
import com.andrewnguyen.bowpress.core.model.VideoStatus

// ── FeedVideoTimeRef ─────────────────────────────────────────────────────
//
// Mirrors iOS `FeedVideoTimeRef` — a reference-typed cursor that carries
// the inline tile's *latest observed* playback position across tap →
// fullscreen → dismiss, without going through Compose state (which would
// re-compose every observable read). The inline tile writes to
// `observedTimeMs` while playing; the tap closure reads it to seed
// fullscreen; the fullscreen dismiss writes `pendingResumeMs` back so
// the inline tile can resume from where fullscreen ended.

/**
 * Bidirectional time cursor for a single feed video card. One instance
 * per `sharedSessionId` so resuming fullscreen always lands on the
 * inline tile's last frame; lives in [FeedVideoTimeStore], owned by the
 * feed screen.
 *
 * Holds mutable state but is intentionally **not** Compose-observable —
 * the inline tile writes to it at 4Hz, and observable writes would
 * re-render the whole feed lazyrow per tick.
 */
class FeedVideoTimeRef {
    /** Most recent cursor (ms) the inline tile observed; -1 = unknown. */
    var observedTimeMs: Long = C.TIME_UNSET
    /**
     * Pending seek (ms) the inline tile should apply on its next
     * lifecycle pass. Set by the fullscreen dismiss; cleared by the tile
     * after seeking so a redundant lifecycle update doesn't re-seek.
     */
    var pendingResumeMs: Long = C.TIME_UNSET
}

/**
 * Lazy bag of [FeedVideoTimeRef]s keyed by `sharedSessionId`. Owned by
 * the feed screen as `remember { FeedVideoTimeStore() }` so dict mutation
 * doesn't trigger a feed re-render — the refs themselves are the state
 * the inline tile + the fullscreen modal consult.
 */
class FeedVideoTimeStore {
    private val refs = mutableMapOf<String, FeedVideoTimeRef>()
    fun ref(key: String): FeedVideoTimeRef = refs.getOrPut(key) { FeedVideoTimeRef() }
}

// ── FeedVideoTile ────────────────────────────────────────────────────────
//
// Muted-autoplay video tile that sits inline on a Social Feed card.
// Plays the moment the row scrolls into view, pauses the moment it
// scrolls out OR the host opens the fullscreen player on top of it.
// Forwards a tap → fullscreen via the host-owned `onTap` callback.
//
// Local-first URL resolution mirrors iOS: the post's owner gets the
// cached file from [LocalVideoStore]; everyone else hits the HLS
// manifest. A pending video with no local cache shows the processing
// badge until the Stream webhook flips it ready.

/**
 * Inline video tile for a feed card.
 *
 * @param sessionId The owning shared session's local id — drives the
 *  [LocalVideoStore] lookup for the post's own author. Pass null for
 *  friend-side surfaces where the local cache is never populated for
 *  someone else's video.
 * @param video The wire video record. Status drives whether the tile
 *  attempts playback (`ready` only) or shows the processing badge.
 * @param timeRef Bidirectional time cursor for tap → fullscreen → resume.
 *  Required for the tap-from-playing case to seed fullscreen with the
 *  live playback position.
 * @param isFullscreenOpen Set by the host while the fullscreen cover for
 *  THIS tile is showing. Suppresses inline playback so the same video
 *  isn't decoded in two players at once.
 * @param onTap Fires when the user taps the tile. The host reads
 *  `timeRef.observedTimeMs` and seeds fullscreen with it.
 */
@Composable
fun FeedVideoTile(
    sessionId: String?,
    video: ActivityVideo,
    timeRef: FeedVideoTimeRef,
    isFullscreenOpen: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // Local-first URL: own-side instant playback via LocalVideoStore.
    // Falls through to the HLS manifest for friends (or own once Stream
    // is done). Null when neither is available — the processing badge
    // overlay then handles the empty visual state.
    val playbackUri: Uri? = remember(sessionId, video.streamId, video.playbackUrl) {
        val local = sessionId?.let { LocalVideoStore.uri(context, it, video.streamId) }
        local ?: video.playbackUrl?.let(Uri::parse)
    }

    if (playbackUri == null) {
        ProcessingBadgeTile(modifier = modifier)
        return
    }

    // The ExoPlayer instance — one per (tile, URL). `remember(playbackUri)`
    // rebuilds when the URL flips (own-side pending file:// → HLS once
    // Stream is ready). The DisposableEffect below ALSO keys on the
    // player so the previous instance is released before the new one
    // takes over — otherwise we'd leak a codec + surface per URL flip.
    val player = remember(playbackUri) {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f
            repeatMode = Player.REPEAT_MODE_ONE
            // Explicit HLS MediaSource for `.m3u8` URIs so R8 release
            // builds don't drop `HlsMediaSource$Factory` from the
            // reflectively-loaded set. `file://` paths still use the
            // default progressive source.
            val isHls = playbackUri.toString().contains(".m3u8")
            if (isHls) {
                val dataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                val mediaSource = androidx.media3.exoplayer.hls.HlsMediaSource
                    .Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(playbackUri))
                setMediaSource(mediaSource)
            } else {
                setMediaItem(MediaItem.fromUri(playbackUri))
            }
            prepare()
            playWhenReady = false
        }
    }

    // Track whether the tile is on-screen. `onGloballyPositioned` fires
    // every layout pass; we treat "at least 50% of the tile overlaps the
    // window bounds" as visible — matches iOS's `.onScrollVisibilityChange
    // (threshold: 0.5)` so a half-prefetched LazyColumn row off the
    // viewport doesn't autoplay alongside the on-screen one.
    var isVisible by remember { mutableStateOf(false) }

    // Resume any pending fullscreen-dismiss seek and (re)play / pause
    // based on visibility + the fullscreen-open flag.
    LaunchedEffect(isVisible, isFullscreenOpen, player) {
        if (timeRef.pendingResumeMs != C.TIME_UNSET) {
            player.seekTo(timeRef.pendingResumeMs)
            timeRef.pendingResumeMs = C.TIME_UNSET
        }
        player.playWhenReady = isVisible && !isFullscreenOpen
    }

    // Sample the live cursor at 4Hz into the timeRef — drives the
    // tap-from-playing fullscreen handoff. Compose cancels this
    // coroutine cleanly when the effect re-keys (player swap) or the
    // composition exits.
    LaunchedEffect(player) {
        while (true) {
            timeRef.observedTimeMs = player.currentPosition
            kotlinx.coroutines.delay(250)
        }
    }

    // Pause when the host activity backgrounds AND release the ExoPlayer
    // when the tile composition exits OR the URL flips (new player below).
    // Keyed on (player, lifecycleOwner) so the old player is released
    // before the new one runs — without that, a `playbackUri` change
    // leaves a zombie codec holding a surface.
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(player, lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) { player.playWhenReady = false }
            override fun onResume(owner: LifecycleOwner) {
                // Resume only if we WERE the visible tile; the visibility
                // tracker re-runs after lifecycle.onResume so this is a
                // best-effort hint, not the authoritative gate.
                player.playWhenReady = isVisible && !isFullscreenOpen
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { coords ->
                // Match iOS `onScrollVisibilityChange(threshold: 0.5)` —
                // require ≥50% of the tile inside the parent's window
                // bounds to call it visible. Stops LazyColumn's
                // prefetched-but-off-viewport row from autoplaying
                // alongside the on-screen one.
                val parent = coords.parentLayoutCoordinates
                val tileSize = coords.size
                if (parent == null || tileSize.height == 0) {
                    isVisible = false
                    return@onGloballyPositioned
                }
                val tileInWindow = coords.boundsInWindow()
                val parentInWindow = parent.boundsInWindow()
                val intersect = tileInWindow.intersect(parentInWindow)
                val visibleArea = intersect.width.coerceAtLeast(0f) *
                    intersect.height.coerceAtLeast(0f)
                val tileArea = tileSize.width.toFloat() * tileSize.height.toFloat()
                isVisible = tileArea > 0f && visibleArea / tileArea >= 0.5f
            },
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    // Match iOS resizeAspectFill — fill the pane and crop
                    // overflowing edges, so a portrait card never letter-
                    // boxes a landscape video horizontally.
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    this.player = player
                }
            },
            modifier = Modifier.fillMaxSize().clickableNoRipple(onTap),
            update = { it.player = player },
        )

        // Processing badge sits over a paused player for non-ready videos
        // — the local-first fallback already filtered them out above when
        // there was a local cache.
        if (video.status != VideoStatus.ready && playbackUri.scheme != "file") {
            ProcessingBadge(modifier = Modifier.align(Alignment.TopStart).padding(8.dp))
        }

        // Duration label, bottom-left, mono. Hidden when the video has
        // no reported duration.
        video.durationSeconds?.let { secs ->
            DurationLabel(
                seconds = secs,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
            )
        }
    }
}

/** A tap target without Material's circular ripple (overlays a Player). */
@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
    )
}

@Composable
private fun ProcessingBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(AppInk.copy(alpha = 0.78f))
            .border(1.dp, AppPaper.copy(alpha = 0.22f))
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(10.dp),
            strokeWidth = 1.5.dp,
            color = AppMaple,
            trackColor = AppPaper.copy(alpha = 0.3f),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "PROCESSING",
            style = interUI(8.sp, FontWeight.SemiBold).copy(letterSpacing = 0.20.em),
            color = AppPaper,
        )
    }
}

@Composable
private fun DurationLabel(seconds: Double, modifier: Modifier = Modifier) {
    val m = (seconds / 60.0).toInt()
    val s = (seconds - m * 60).coerceAtLeast(0.0)
    // Locale.US — a German locale would otherwise print `0:04,5` (comma
    // decimal) and diverge from iOS's formatter.
    val text = "%d:%04.1f".format(java.util.Locale.US, m, s)
    Text(
        text = text,
        style = com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono(9.sp),
        color = AppPaper,
        modifier = modifier
            .background(AppInk.copy(alpha = 0.78f))
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

/** Empty-state tile for a video with no playable URL — paper-toned. */
@Composable
private fun ProcessingBadgeTile(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppInk),
        contentAlignment = Alignment.Center,
    ) {
        ProcessingBadge()
    }
}
