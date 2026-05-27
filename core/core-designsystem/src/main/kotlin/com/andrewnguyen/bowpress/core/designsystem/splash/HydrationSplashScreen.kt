package com.andrewnguyen.bowpress.core.designsystem.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.bp.BPWordmark
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// HydrationSplashScreen
// ---------------------------------------------------------------------------
//
// Native Compose port of the "Hairline Target" splash spec at
// bowpress-design-system/project/explorations/splash/Hairline Target.html.
// Six concentric hairline rings draw in sequence (each rotated 8° more
// than the previous so the stroke heads fan), a maple bullseye dot lands
// at the center, and the wordmark slides up from the bottom. Replaces the
// prior ANALYZING YOUR DATA target-plot splash.
//
// Caller (BowPressApp) flips `splashDismissed` inside a fade-out once
// BOTH `onMinimumElapsed` (motion gate, 3.5s) AND hydration have settled,
// or unconditionally once `onSafety` (4.5s ceiling) fires.

private val RingEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

// Design canvas — ring radii below are in this same coordinate space, so
// the Canvas at `CanvasSizeDp.dp` renders 1:1 with the design.
private const val CanvasSizeDp: Float = 320f

// Ring radii in design units. Innermost (24) is the X-ring and carries
// a slightly heavier stroke.
private val ringRadii: List<Float> = listOf(144f, 120f, 96f, 72f, 48f, 24f)

// Timeline (ms). Compressed slightly vs. the looping HTML demo so the
// splash doesn't drag past ~3.5s on a warm hydrate.
private const val RingStartMs: Int = 250
private const val RingStaggerMs: Int = 170
private const val RingDrawMs: Int = 1300
private const val DotStartMs: Int = 2200
private const val DotDurationMs: Int = 500
private const val WordmarkStartMs: Int = 2450
private const val WordmarkDurationMs: Int = 850
private const val MinimumElapsedMs: Long = 3500
private const val SafetyTimeoutMs: Long = 4500

/**
 * Full-screen splash. Caller is responsible for:
 *
 *  1. guarding with `if (!splashDismissed)` so the view is unmounted after
 *     dismissal (no partial-alpha ghost behind sparse tabs),
 *  2. flipping `splashDismissed` inside a fade-out once BOTH the motion
 *     gate (`onMinimumElapsed`) has fired AND hydration has settled —
 *     `onSafety` is the 4.5s hard ceiling that runs regardless.
 */
@Composable
fun HydrationSplashScreen(
    onMinimumElapsed: () -> Unit = {},
    onSafety: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }

    LaunchedEffect(Unit) {
        delay(MinimumElapsedMs)
        onMinimumElapsed()
    }
    LaunchedEffect(Unit) {
        delay(SafetyTimeoutMs)
        onSafety()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppPaper)
            .testTag(TestTags.HydrationSplash),
    ) {
        // Target + wordmark stacked vertically and centered, so the layout
        // adapts naturally across the screen-size matrix instead of pinning
        // either element a fixed distance from a screen edge.
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            TargetCluster(
                started = started,
                modifier = Modifier.size(CanvasSizeDp.dp),
            )
            Spacer(Modifier.height(40.dp))
            WordmarkSlideUp(started = started)
        }
    }
}

// ---------------------------------------------------------------------------
// Target cluster: 6 hairline rings + maple bullseye dot
// ---------------------------------------------------------------------------

@Composable
private fun TargetCluster(started: Boolean, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val strokePx = with(density) { 0.7.dp.toPx() }
    val heavyStrokePx = with(density) { 1.2.dp.toPx() }

    // One Animatable per ring, allocated once for the lifetime of the
    // composition. Hoisting out of `forEachIndexed` keeps the `remember`
    // slot table independent of how the list is iterated — safer than the
    // positional slot pattern of `remember`-inside-`mapIndexed`.
    val sweeps = remember { List(ringRadii.size) { Animatable(0f) } }
    sweeps.forEachIndexed { index, anim ->
        LaunchedEffect(started, index) {
            if (started) {
                anim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = RingDrawMs,
                        delayMillis = RingStartMs + index * RingStaggerMs,
                        easing = RingEasing,
                    ),
                )
            }
        }
    }

    val dotAlpha = remember { Animatable(0f) }
    LaunchedEffect(started) {
        if (started) {
            dotAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = DotDurationMs,
                    delayMillis = DotStartMs,
                    easing = EaseOut,
                ),
            )
        }
    }

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        // Canvas is sized to CanvasSizeDp.dp, so size.minDimension carries
        // the design-to-pixel scale. Ring radii are already in the design
        // canvas's coord space, so multiply through.
        val pxPerDesignUnit = size.minDimension / CanvasSizeDp

        ringRadii.forEachIndexed { index, rDesign ->
            val rPx = rDesign * pxPerDesignUnit
            val isInnermost = index == ringRadii.lastIndex
            // -90° puts the stroke head at 12 o'clock; the +8° per-ring
            // offset fans the heads as they draw — same effect as the
            // HTML demo's `transform: rotate(...)` on each <circle>.
            val startAngle = -90f + index * 8f
            val sweepAngle = 360f * sweeps[index].value
            drawArc(
                color = AppInk,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(cx - rPx, cy - rPx),
                size = Size(rPx * 2, rPx * 2),
                style = Stroke(width = if (isInnermost) heavyStrokePx else strokePx),
            )
        }
        // Maple bullseye dot — 3.2 design units radius.
        val dotRadius = 3.2f * pxPerDesignUnit
        drawCircle(
            color = AppMaple.copy(alpha = dotAlpha.value),
            radius = dotRadius,
            center = Offset(cx, cy),
        )
    }
}

// ---------------------------------------------------------------------------
// Wordmark slide-up — opacity 0→1 + translateY(22dp → 0), EaseOut.
// ---------------------------------------------------------------------------

@Composable
private fun WordmarkSlideUp(started: Boolean) {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(22f) }
    LaunchedEffect(started) {
        if (started) {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = WordmarkDurationMs,
                    delayMillis = WordmarkStartMs,
                    easing = EaseOut,
                ),
            )
        }
    }
    LaunchedEffect(started) {
        if (started) {
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = WordmarkDurationMs,
                    delayMillis = WordmarkStartMs,
                    easing = EaseOut,
                ),
            )
        }
    }
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .alpha(alpha.value)
            .graphicsLayer { translationY = with(density) { offsetY.value.dp.toPx() } },
    ) {
        BPWordmark(size = 34.sp)
    }
}
