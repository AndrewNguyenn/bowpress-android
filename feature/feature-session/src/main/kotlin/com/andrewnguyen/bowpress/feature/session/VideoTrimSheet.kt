package com.andrewnguyen.bowpress.feature.session

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.IntOffset
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.min

/**
 * Hard cap on a trimmed clip. Matches the API's
 * `MAX_VIDEO_DURATION_SECONDS` (21s = 20s trim + 1s rounding buffer);
 * the UI surfaces 20s as the visible cap.
 */
private const val MAX_TRIM_SECONDS = 20.0

/**
 * Compose video-trim sheet. ExoPlayer drives the preview, a custom
 * filmstrip with two handles drives the trim window, and
 * media3.Transformer exports the trimmed range to a file URI the
 * caller can hand straight to the upload pipeline.
 *
 * Mirrors iOS's YPVideoFiltersVC behavior — minus the in-picker
 * Cover tab. The trim window is capped at 20s; sources longer than
 * that open at the first 20s slice and the archer can drag the
 * window to pick a different slice within the same cap.
 *
 * iOS counterpart: vendored YPImagePicker's video trim, now using
 * AVAssetExportPresetPassthrough (frame-accurate cut, no re-encode).
 * Transformer's `setForceAudioTrack` / passthrough config delivers
 * the same: cut at the trim points without re-encoding video.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoTrimSheet(
    sourceUri: Uri,
    onCancel: () -> Unit,
    onTrimmed: (Uri) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // ExoPlayer for the live preview.
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(sourceUri))
            playWhenReady = false
            prepare()
        }
    }
    var durationMs by remember { mutableStateOf(0L) }
    var startMs by remember { mutableStateOf(0L) }
    var endMs by remember { mutableStateOf(0L) }
    var exporting by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }

    // Pull the duration once the player resolves it.
    LaunchedEffect(player) {
        while (durationMs <= 0L) {
            val d = player.duration
            if (d > 0) {
                durationMs = d
                endMs = min(d, (MAX_TRIM_SECONDS * 1000).toLong())
                break
            }
            delay(50)
        }
    }

    // Keep playback head inside the trim window — seek to start
    // whenever it crosses end.
    LaunchedEffect(player, startMs, endMs) {
        while (true) {
            if (player.isPlaying && player.currentPosition >= endMs) {
                player.seekTo(startMs)
            }
            delay(80)
        }
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Top bar.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterStart)) {
                Text("Cancel", color = Color.White, fontSize = 16.sp)
            }
            Text(
                text = "Trim",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center),
            )
            TextButton(
                onClick = {
                    if (exporting || durationMs <= 0L) return@TextButton
                    exporting = true
                    exportError = null
                    scope.launch {
                        try {
                            val out = exportTrim(
                                context = context,
                                source = sourceUri,
                                startMs = startMs,
                                endMs = endMs,
                            )
                            onTrimmed(out)
                        } catch (t: Throwable) {
                            exportError = t.message ?: "Trim failed"
                        } finally {
                            exporting = false
                        }
                    }
                },
                enabled = !exporting && durationMs > 0,
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Text(
                    text = if (exporting) "Saving…" else "Done",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // ExoPlayer preview.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.dp)
                .padding(horizontal = 16.dp),
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Trim filmstrip with two handles.
        TrimRangeBar(
            durationMs = durationMs,
            startMs = startMs,
            endMs = endMs,
            onChange = { newStart, newEnd ->
                // Enforce the 20s cap + ordering. The handle that
                // moved gets clamped against the cap (i.e. if the
                // user drags the END past start+20s, the END is
                // pinned to start+20s).
                val capMs = (MAX_TRIM_SECONDS * 1000).toLong()
                val s = max(0L, min(newStart, durationMs - 500))
                val e = min(durationMs, max(newEnd, s + 500))
                val eCapped = min(e, s + capMs)
                startMs = s
                endMs = eCapped
                // Seek the preview to the start of whatever the user
                // is currently adjusting so they see the boundary
                // frame instantly.
                player.seekTo(s)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(60.dp),
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = "${(startMs / 1000.0)}s – ${(endMs / 1000.0)}s · ${((endMs - startMs) / 1000.0)}s",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
            )
        }

        if (exportError != null) {
            Text(
                text = exportError!!,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(16.dp),
            )
        }

        if (exporting) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

@Composable
private fun TrimRangeBar(
    durationMs: Long,
    startMs: Long,
    endMs: Long,
    onChange: (newStart: Long, newEnd: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var barSize by remember { mutableStateOf(IntSize.Zero) }
    val handleHalfPx = with(LocalDensity.current) { 14.dp.toPx() }

    Box(
        modifier = modifier
            .background(Color.DarkGray)
            .onSizeChanged { barSize = it },
    ) {
        if (durationMs <= 0L || barSize.width == 0) return@Box
        val widthPx = barSize.width.toFloat()
        val startX = (startMs.toFloat() / durationMs.toFloat()) * widthPx
        val endX = (endMs.toFloat() / durationMs.toFloat()) * widthPx

        // Selected band.
        Box(
            modifier = Modifier
                .offset { IntOffset(startX.toInt(), 0) }
                .width(with(LocalDensity.current) { (endX - startX).toDp() })
                .fillMaxSize()
                .background(Color.Yellow.copy(alpha = 0.25f)),
        )

        // Left handle.
        TrimHandle(
            xPx = startX,
            onDrag = { newX ->
                val clamped = newX.coerceIn(0f, endX - 4f)
                val newStart = ((clamped / widthPx) * durationMs).toLong()
                onChange(newStart, endMs)
            },
        )

        // Right handle.
        TrimHandle(
            xPx = endX,
            onDrag = { newX ->
                val clamped = newX.coerceIn(startX + 4f, widthPx)
                val newEnd = ((clamped / widthPx) * durationMs).toLong()
                onChange(startMs, newEnd)
            },
        )
    }
}

@Composable
private fun TrimHandle(
    xPx: Float,
    onDrag: (Float) -> Unit,
) {
    val handleWidth = 14.dp
    val density = LocalDensity.current
    val halfPx = with(density) { handleWidth.toPx() / 2 }
    var dragAccum by remember { mutableStateOf(xPx) }
    LaunchedEffect(xPx) { dragAccum = xPx }
    Box(
        modifier = Modifier
            .offset { IntOffset((dragAccum - halfPx).toInt(), 0) }
            .width(handleWidth)
            .fillMaxSize()
            .background(Color.Yellow)
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    dragAccum += drag.x
                    onDrag(dragAccum)
                }
            },
    )
}

/**
 * Run media3.Transformer to export the trimmed range to a cache
 * file. Suspending — returns the output Uri once the export
 * finishes. Throws on cancel / error.
 *
 * Trim is via `EditedMediaItem.setRemoveAudio(false)` +
 * `setClippingConfiguration`, equivalent to iOS's
 * AVAssetExportPresetPassthrough — no re-encode, cut at the
 * boundary samples.
 */
@OptIn(UnstableApi::class)
private suspend fun exportTrim(
    context: Context,
    source: Uri,
    startMs: Long,
    endMs: Long,
): Uri = suspendCancellableCoroutine { cont ->
    val outFile = File(
        context.cacheDir,
        "bp_trim_${System.currentTimeMillis()}.mp4",
    )
    val clipped = MediaItem.Builder()
        .setUri(source)
        .setClippingConfiguration(
            MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startMs)
                .setEndPositionMs(endMs)
                .build(),
        )
        .build()
    val edited = EditedMediaItem.Builder(clipped).build()

    val transformer = Transformer.Builder(context)
        .addListener(object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                if (cont.isActive) cont.resume(Uri.fromFile(outFile))
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException,
            ) {
                Log.e("VideoTrimSheet", "transformer error", exportException)
                if (cont.isActive) cont.resumeWithException(exportException)
            }
        })
        .build()

    cont.invokeOnCancellation {
        transformer.cancel()
        outFile.delete()
    }

    transformer.start(edited, outFile.absolutePath)
}
