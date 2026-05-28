package com.andrewnguyen.bowpress.feature.social.ui.feed

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.interUI

/**
 * The fullscreen target a feed-screen-level state holds while a video
 * card is open. Mirrors iOS `FullscreenVideoTarget`: the URL to play,
 * a stable key (the shared session id) so the inline tile knows its
 * own fullscreen is open, and the start position the inline tile was
 * paused at.
 */
data class FullscreenVideoTarget(
    val uri: Uri,
    val sessionKey: String,
    val initialPositionMs: Long,
)

/**
 * Fullscreen video player launched from a feed [FeedVideoTile] tap.
 * Plays with audio + controls, seeded at [target]'s
 * `initialPositionMs`. On dismiss, writes the current cursor back into
 * [timeRef.pendingResumeMs] so the inline tile resumes from there.
 *
 * Owned by the feed screen (a single screen-scope instance), NOT by
 * the card — a viewer kept in the LazyColumn item would be torn down
 * the moment the card scrolls off, dismissing mid-look.
 */
@Composable
fun FullscreenVideoPlayer(
    target: FullscreenVideoTarget,
    timeRef: FeedVideoTimeRef,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val player = remember(target.uri) {
        ExoPlayer.Builder(context).build().apply {
            // Explicit HLS MediaSource for `.m3u8` URIs so R8 release
            // builds keep `HlsMediaSource$Factory` reachable. file://
            // paths still use the default progressive source.
            val isHls = target.uri.toString().contains(".m3u8")
            if (isHls) {
                val dataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                val mediaSource = androidx.media3.exoplayer.hls.HlsMediaSource
                    .Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(target.uri))
                setMediaSource(mediaSource)
            } else {
                setMediaItem(MediaItem.fromUri(target.uri))
            }
            prepare()
            if (target.initialPositionMs != C.TIME_UNSET) {
                seekTo(target.initialPositionMs)
            }
            playWhenReady = true
            // Audio on in fullscreen — opposite of the muted inline tile.
            volume = 1f
        }
    }
    DisposableEffect(target.uri) {
        onDispose { player.release() }
    }
    // Write the resume position into the timeRef synchronously on
    // dismiss — NOT in onDispose. The Dialog's dismissal flips the
    // hosting `videoFullscreen` state to null BEFORE Compose disposes
    // this composable, so the inline tile's `LaunchedEffect
    // (isFullscreenOpen)` re-runs at a moment when `pendingResumeMs`
    // hasn't been written yet, and the tile resumes at 0. Wrapping
    // dismiss ensures the write happens before the state flip.
    val dismiss: () -> Unit = {
        timeRef.pendingResumeMs = player.currentPosition
        onDismiss()
    }
    Dialog(
        onDismissRequest = dismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = true
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout
                            .RESIZE_MODE_FIT
                        this.player = player
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { it.player = player },
            )
            // Top-right CLOSE chip (matches the photo viewer's affordance).
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clickable(onClick = dismiss)
                    .padding(20.dp),
            ) {
                Text(
                    text = "CLOSE",
                    style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                    color = AppPaper,
                )
            }
        }
    }
}
