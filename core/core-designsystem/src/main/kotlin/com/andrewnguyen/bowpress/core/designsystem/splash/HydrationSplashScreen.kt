package com.andrewnguyen.bowpress.core.designsystem.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPond
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.AppPondLt
import com.andrewnguyen.bowpress.core.designsystem.bp.BPWordmark
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// HydrationSplashScreen
// ---------------------------------------------------------------------------
//
// Native Compose port of the splash spec at
// bowpress-design-system/project/explorations/splash/index.html. Mirrors iOS
// HydrationSplashView: 2.4s plot-in — pond-gradient rings scale in, hairlines
// fade over them, 12 arrows plot one-by-one (last is a maple-outlined flier),
// a pond-dk crosshair settles on the centroid, wordmark fades up with a
// hairline rule, "ANALYZING YOUR DATA" + pulsing maple dot, and a bottom
// telemetry ribbon ticks in.
//
// Callers sit above a definitive-guard: `if (!splashDismissed) { ... }`. The
// splash fires `onMinimumElapsed` after 2.6s (motion gate) and `onSafety`
// after 4.5s (hard ceiling — even if hydration hangs, the splash stops
// blocking). The caller flips `splashDismissed = true` inside a 450ms fade.

private val RingEasing = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)

// Spec coord system is 200 × 200. We render into `TargetSizeDp` and scale.
private const val TargetSizeDp: Float = 220f
private const val CoordUnits: Float = 200f
private const val CoordScale: Float = TargetSizeDp / CoordUnits   // 1.1

private data class RingSpec(val radiusDp: Float, val fill: Color, val delayMs: Int)
private val rings: List<RingSpec> = listOf(
    RingSpec(96f, Color(0xFFD9E1D8), 0),
    RingSpec(76f, Color(0xFFB8CDD0), 80),
    RingSpec(56f, AppPondLt,          160),
    RingSpec(36f, AppPond,            240),
    RingSpec(16f, AppPondDk,          320),
)

private val hairlineRadii: List<Float> = listOf(96f, 86f, 76f, 66f, 56f, 46f, 36f, 26f, 16f, 8f)

private data class ArrowSpec(val x: Float, val y: Float, val flier: Boolean)
private val arrows: List<ArrowSpec> = listOf(
    ArrowSpec( 98f,  96f, false),
    ArrowSpec(104f, 102f, false),
    ArrowSpec(100f, 108f, false),
    ArrowSpec( 92f, 103f, false),
    ArrowSpec(106f,  93f, false),
    ArrowSpec( 95f,  88f, false),
    ArrowSpec(112f, 109f, false),
    ArrowSpec( 88f, 112f, false),
    ArrowSpec(102f,  84f, false),
    ArrowSpec(117f,  96f, false),
    ArrowSpec( 85f,  94f, false),
    ArrowSpec(148f,  72f, true),
)

/**
 * Full-screen splash. Parent is responsible for:
 *
 *  1. guarding with `if (!splashDismissed)` so the view is unmounted after
 *     dismissal (no partial-alpha ghost behind sparse tabs),
 *  2. flipping `splashDismissed` inside a 450ms fade once BOTH the motion
 *     gate (`onMinimumElapsed`) has fired AND hydration has settled —
 *     `onSafety` is the 4.5s hard ceiling that runs regardless.
 *
 * `onMinimumElapsed` fires at 2.6s — buys us through arrow 12 (~2.12s plot
 * end) and the centroid pulse-in at 2.3s with a grace pad.
 * `onSafety` fires at 4.5s — last-resort dismissal when hydration hangs
 * (no backend, airplane mode, etc.).
 */
@Composable
fun HydrationSplashScreen(
    onMinimumElapsed: () -> Unit = {},
    onSafety: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Single trigger — flipping `started` once fans out into each element's
    // `tween(... delayMillis = X)` so the 2.4s keyframe sequence kicks off
    // in lock-step.
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }

    // Motion gate — 2.6s minimum display. Mirrors iOS
    // HydrationSplashView.onMinimumElapsed.
    LaunchedEffect(Unit) {
        delay(2600)
        onMinimumElapsed()
    }
    // Hard safety timeout — 4.5s ceiling so a wedged hydrate doesn't
    // strand the splash indefinitely. Mirrors ContentView splashSafetyTimeout.
    LaunchedEffect(Unit) {
        delay(4500)
        onSafety()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppPaper)
            .testTag(TestTags.HydrationSplash),
    ) {
        // Top header — absolute, paired with the telemetry row at the bottom.
        HeaderStrip(
            started = started,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(start = 32.dp, end = 32.dp, top = 44.dp),
        )

        // Centered stack: target + wordmark.
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            TargetCluster(
                started = started,
                modifier = Modifier.size(TargetSizeDp.dp),
            )
            Spacer(Modifier.height(36.dp))
            WordmarkBlock(started = started)
        }

        // Bottom telemetry.
        TelemetryRow(
            started = started,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 32.dp, end = 32.dp, bottom = 40.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Header
// ---------------------------------------------------------------------------

@Composable
private fun HeaderStrip(started: Boolean, modifier: Modifier = Modifier) {
    val alpha by remember(started) {
        mutableStateOf(Animatable(if (started) 0f else 0f))
    }
    LaunchedEffect(started) {
        if (started) {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 450, delayMillis = 100, easing = EaseOut),
            )
        }
    }

    Row(
        modifier = modifier.alpha(alpha.value),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = "BOWPRESS",
            style = interUI(10.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.32.em),
            color = AppPondDk,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "v1.1.0 · SYNC",
            style = jetbrainsMono(9.sp).copy(letterSpacing = 0.06.em),
            color = AppInk3,
        )
    }
}

// ---------------------------------------------------------------------------
// Target cluster: pond rings + hairlines + plotted arrows + centroid
// ---------------------------------------------------------------------------

@Composable
private fun TargetCluster(started: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        RingStack(started = started)
        HairlineOverlay(started = started, modifier = Modifier.fillMaxSize())
        ArrowPlot(started = started, modifier = Modifier.fillMaxSize())
        Crosshair(
            started = started,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun RingStack(started: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        rings.forEach { ring ->
            val scale = remember { Animatable(0.1f) }
            val alpha = remember { Animatable(0f) }
            LaunchedEffect(started) {
                if (started) {
                    scale.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 1400,
                            delayMillis = ring.delayMs,
                            easing = RingEasing,
                        ),
                    )
                }
            }
            LaunchedEffect(started) {
                if (started) {
                    alpha.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 1400,
                            delayMillis = ring.delayMs,
                            easing = RingEasing,
                        ),
                    )
                }
            }
            Canvas(
                modifier = Modifier
                    .size((ring.radiusDp * 2 * CoordScale).dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        this.alpha = alpha.value
                    },
            ) {
                drawCircle(color = ring.fill, radius = size.minDimension / 2f)
            }
        }
    }
}

@Composable
private fun HairlineOverlay(started: Boolean, modifier: Modifier = Modifier) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(started) {
        if (started) {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 500,
                    delayMillis = 500,
                    easing = EaseOut,
                ),
            )
        }
    }
    val density = LocalDensity.current
    val hairlineColor = AppInk
    Canvas(modifier = modifier.alpha(alpha.value)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val strokePx = with(density) { 0.4.dp.toPx() }
        val stroke = Stroke(width = strokePx)
        hairlineRadii.forEach { rDp ->
            val rPx = with(density) { (rDp * CoordScale).dp.toPx() }
            drawCircle(
                color = hairlineColor,
                radius = rPx,
                center = Offset(cx, cy),
                style = stroke,
            )
        }
    }
}

@Composable
private fun ArrowPlot(started: Boolean, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val flierColor = AppMaple
    val dotColor = AppInk
    Canvas(modifier = modifier) {
        // Nothing draws here — the real drawing happens per-arrow below.
        // Kept so the parent Box has a consistent canvas descendant.
    }
    // Per-arrow animatable trio (scale + alpha). Each arrow gets its own
    // 500ms bouncy plot starting at 800ms + 120ms * index. Bouncy is
    // approximated by a cubic bezier that overshoots mildly.
    arrows.forEachIndexed { index, arrow ->
        val scale = remember { Animatable(2.6f) }
        val alpha = remember { Animatable(0f) }
        LaunchedEffect(started) {
            if (started) {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 500,
                        delayMillis = 800 + 120 * index,
                        easing = CubicBezierEasing(0.2f, 0.7f, 0.25f, 1.2f),
                    ),
                )
            }
        }
        LaunchedEffect(started) {
            if (started) {
                alpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 500,
                        delayMillis = 800 + 120 * index,
                        easing = EaseOut,
                    ),
                )
            }
        }

        // Render dot via Canvas — positions are derived from the
        // 200-unit spec coords.
        Canvas(modifier = modifier) {
            val cx = with(density) { (arrow.x * CoordScale).dp.toPx() }
            val cy = with(density) { (arrow.y * CoordScale).dp.toPx() }
            val rPx = with(density) {
                val baseR = if (arrow.flier) 2.6f else 2.4f
                (baseR * CoordScale).dp.toPx()
            }
            if (arrow.flier) {
                val strokePx = with(density) { 1.2.dp.toPx() }
                drawCircle(
                    color = flierColor.copy(alpha = alpha.value),
                    radius = rPx * scale.value,
                    center = Offset(cx, cy),
                    style = Stroke(width = strokePx),
                )
            } else {
                drawCircle(
                    color = dotColor.copy(alpha = alpha.value),
                    radius = rPx * scale.value,
                    center = Offset(cx, cy),
                )
            }
        }
    }
}

@Composable
private fun Crosshair(started: Boolean, modifier: Modifier = Modifier) {
    val scale = remember { Animatable(0.2f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(started) {
        if (started) {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 1000,
                    delayMillis = 2300,
                    easing = EaseOut,
                ),
            )
        }
    }
    LaunchedEffect(started) {
        if (started) {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 1000,
                    delayMillis = 2300,
                    easing = EaseOut,
                ),
            )
        }
    }
    val density = LocalDensity.current
    val baseTint = AppPondDk
    Canvas(modifier = modifier) {
        val cx = with(density) { (100f * CoordScale).dp.toPx() }
        val cy = with(density) { (100f * CoordScale).dp.toPx() }
        val r = with(density) { (6f * CoordScale).dp.toPx() } * scale.value
        val arm = with(density) { (7f * CoordScale).dp.toPx() } * scale.value
        val strokePx = with(density) { 0.9.dp.toPx() }
        val stroke = Stroke(width = strokePx)
        val tint = baseTint.copy(alpha = alpha.value)

        drawCircle(color = tint, radius = r, center = Offset(cx, cy), style = stroke)
        drawLine(
            color = tint,
            start = Offset(cx, cy - arm),
            end = Offset(cx, cy + arm),
            strokeWidth = strokePx,
        )
        drawLine(
            color = tint,
            start = Offset(cx - arm, cy),
            end = Offset(cx + arm, cy),
            strokeWidth = strokePx,
        )
    }
}

// ---------------------------------------------------------------------------
// Wordmark block: "bowpress" + rule + "ANALYZING YOUR DATA"
// ---------------------------------------------------------------------------

@Composable
private fun WordmarkBlock(started: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FadeUp(started = started, delayMs = 400, durationMs = 700) {
            BPWordmark(size = 36.sp)
        }
        Spacer(Modifier.height(12.dp))

        // 48 × 1dp rule — scaleX 0 → 1 at 1000ms.
        val ruleScale = remember { Animatable(0f) }
        LaunchedEffect(started) {
            if (started) {
                ruleScale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 600,
                        delayMillis = 1000,
                        easing = EaseOut,
                    ),
                )
            }
        }
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(1.dp)
                .graphicsLayer { scaleX = ruleScale.value }
                .background(AppPondDk),
        )

        Spacer(Modifier.height(10.dp))

        FadeUp(started = started, delayMs = 1100, durationMs = 600) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PulsingMapleDot()
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "ANALYZING YOUR DATA",
                    style = interUI(10.sp, FontWeight.SemiBold).copy(letterSpacing = 0.26.em),
                    color = AppInk3,
                )
            }
        }
    }
}

/**
 * 5dp × 5dp maple square with opacity pulsing 0.25 ⇄ 1.0 every 1.1s. Driven
 * by `rememberInfiniteTransition` + a linear 550ms reverse tween so the
 * full cycle is 1.1s.
 */
@Composable
private fun PulsingMapleDot() {
    val transition = rememberInfiniteTransition(label = "maple-pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "maple-alpha",
    )
    Box(
        modifier = Modifier
            .size(5.dp)
            .alpha(alpha)
            .background(AppMaple),
    )
}

// ---------------------------------------------------------------------------
// Telemetry row: "LAST SESSION" / "LOADING"
// ---------------------------------------------------------------------------

@Composable
private fun TelemetryRow(started: Boolean, modifier: Modifier = Modifier) {
    FadeUp(started = started, delayMs = 1800, durationMs = 600, modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            TelemetryColumn(
                eyebrow = "LAST SESSION",
                primary = "10.4 avg",
                suffix = "· 72% X",
                align = TextAlign.Start,
            )
            TelemetryColumn(
                eyebrow = "LOADING",
                primary = "342 arrows",
                suffix = "· 14 sess",
                align = TextAlign.End,
            )
        }
    }
}

@Composable
private fun TelemetryColumn(
    eyebrow: String,
    primary: String,
    suffix: String,
    align: TextAlign,
) {
    Column(
        horizontalAlignment = if (align == TextAlign.End) Alignment.End else Alignment.Start,
    ) {
        Text(
            text = eyebrow,
            style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
            color = AppPondDk,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = primary,
                style = frauncesDisplay(13.sp, italic = true, weight = FontWeight.Medium),
                color = AppInk,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = suffix,
                style = jetbrainsMono(9.sp).copy(letterSpacing = 0.06.em),
                color = AppInk3,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Fade-up helper — opacity 0 → 1 + translateY(6dp → 0), EaseOut, delayed.
// ---------------------------------------------------------------------------

@Composable
private fun FadeUp(
    started: Boolean,
    delayMs: Int,
    durationMs: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(6f) }
    LaunchedEffect(started) {
        if (started) {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = durationMs,
                    delayMillis = delayMs,
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
                    durationMillis = durationMs,
                    delayMillis = delayMs,
                    easing = RingEasing,
                ),
            )
        }
    }
    val density = LocalDensity.current
    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha.value
            translationY = with(density) { offsetY.value.dp.toPx() }
        },
    ) {
        content()
    }
}
