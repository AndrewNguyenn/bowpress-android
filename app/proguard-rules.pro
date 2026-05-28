# Add project-specific ProGuard rules here.
# See https://developer.android.com/build/shrink-code for reference.

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# Kotlinx serialization keeps @Serializable classes — rules are bundled with the library.

# uCrop — launched via Intent so R8 can't trace the reference and strips the class.
-keep class com.yalantis.ucrop.UCropActivity

# Media3 — DefaultMediaSourceFactory reflectively loads HlsMediaSource$Factory
# when the URI smells like HLS. R8 release builds strip the class because the
# direct import is gone (the call site goes through Class.forName). Keep the
# HLS module's public API so Cloudflare Stream playback survives minification.
# The FeedVideoTile + FullscreenVideoPlayer also wire HlsMediaSource directly
# now (belt + braces), but the keep rule defends the runtime-load path too.
-keep class androidx.media3.exoplayer.hls.** { *; }
-keep class androidx.media3.datasource.** { *; }
