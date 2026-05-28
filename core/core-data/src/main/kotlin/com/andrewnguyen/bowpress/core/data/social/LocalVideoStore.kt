package com.andrewnguyen.bowpress.core.data.social

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.InputStream

/**
 * Mirrors iOS `LocalVideoStore` — on-disk cache of the bytes the user
 * just picked / trimmed for a shared-session video, keyed by
 * `(sessionId, streamId)`. The FinishSheet upload path drops the local
 * copy here at finalize time; the feed video tile reads from it for
 * owner-side instant playback before Cloudflare Stream finishes
 * transcoding (`status` flips to `ready` + `playbackUrl` is non-null).
 *
 * Storage lives under `cacheDir/local_videos/<sessionId>/<streamId>.mp4`
 * — `cacheDir` so Android can evict on space pressure (the post is
 * already safely uploaded; we just lose the instant-play optimisation).
 *
 * Friend-side rows never touch this cache; an own row whose video is
 * still pending falls back to this when no `playbackUrl` is available,
 * and a friend's pending video just shows the processing badge until
 * the webhook flips it ready.
 */
object LocalVideoStore {

    private const val ROOT_DIR = "local_videos"

    private fun directory(context: Context, sessionId: String): File =
        File(File(context.cacheDir, ROOT_DIR), sessionId).apply { mkdirs() }

    /** The local file backing a cached video; may or may not exist. */
    private fun file(context: Context, sessionId: String, streamId: String): File =
        File(directory(context, sessionId), "$streamId.mp4")

    /** True when a local copy is on disk for the (sessionId, streamId) pair. */
    fun hasVideo(context: Context, sessionId: String, streamId: String): Boolean =
        file(context, sessionId, streamId).exists()

    /**
     * The `file://` URI of the cached video, for `ExoPlayer.MediaItem`. Returns
     * null when no local copy exists — the caller then falls back to the HLS
     * playback URL (friend-side, or own-side once Stream has transcoded).
     */
    fun uri(context: Context, sessionId: String, streamId: String): Uri? {
        val f = file(context, sessionId, streamId)
        return if (f.exists()) Uri.fromFile(f) else null
    }

    /**
     * The cached Stream UIDs for a session — used by the finish-sheet flow
     * to reconcile the local cache against the server's reported set
     * (delete any local files whose stream rows the server no longer
     * acknowledges).
     */
    fun cachedStreamIds(context: Context, sessionId: String): List<String> =
        directory(context, sessionId).listFiles()
            ?.mapNotNull { f -> f.name.removeSuffix(".mp4").takeIf { it != f.name } }
            ?: emptyList()

    /**
     * Persist [bytes] as the local video for `(sessionId, streamId)`. Returns
     * true on success. Mirrors iOS `save(_:sessionId:streamId:)`. Used by the
     * test seed; the FinishSheet picker hands in a [Uri] and calls
     * [copyFromUri] to avoid loading the whole file into memory.
     */
    fun save(context: Context, bytes: ByteArray, sessionId: String, streamId: String): Boolean =
        runCatching {
            file(context, sessionId, streamId).writeBytes(bytes)
            true
        }.getOrDefault(false)

    /**
     * Stream the picked-video [source] into the cache without loading it
     * into memory — Android's content://uris are not seekable file paths,
     * so the FinishSheet upload step uses this to materialise a local copy
     * for the inline tile before kicking off the Stream upload.
     */
    fun copyFromUri(
        context: Context,
        source: Uri,
        sessionId: String,
        streamId: String,
    ): Boolean = runCatching {
        val resolver = context.contentResolver
        val target = file(context, sessionId, streamId)
        resolver.openInputStream(source).use { input ->
            requireNotNull(input) { "openInputStream returned null for $source" }
            target.outputStream().use { output -> (input as InputStream).copyTo(output) }
        }
        true
    }.getOrDefault(false)

    /** Removes one cached video. No-op when the file isn't present. */
    fun delete(context: Context, sessionId: String, streamId: String) {
        file(context, sessionId, streamId).delete()
    }

    /** Wipes every cached video for a session — used on session delete. */
    fun deleteAll(context: Context, sessionId: String) {
        directory(context, sessionId).deleteRecursively()
    }
}
