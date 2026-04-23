package com.andrewnguyen.bowpress.core.designsystem.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.andrewnguyen.bowpress.core.designsystem.BowPressColors
import com.andrewnguyen.bowpress.core.designsystem.R
import kotlinx.coroutines.delay

/**
 * Launch splash shown while [isHydrating] is true. Mirrors iOS
 * `HydrationSplashView` — Lottie archery animation loops with an
 * "Analyzing your data…" label underneath, then fades out when hydration
 * completes.
 *
 * Usage:
 * ```
 * Box {
 *     AppContent()
 *     HydrationSplashScreen(isHydrating = appState.isHydrating)
 * }
 * ```
 *
 * The splash takes the full container size and sits on top; parent is
 * expected to be a `Box` (or any stack-style layout).
 */
@Composable
fun HydrationSplashScreen(
    isHydrating: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isHydrating,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        SplashContent()
    }
}

@Composable
private fun SplashContent() {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.archery_hero),
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = Int.MAX_VALUE,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (composition != null) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(220.dp),
                )
            } else {
                // Bounded composition load — show a spinner placeholder so the
                // screen isn't blank while the JSON parses off the main thread.
                CircularProgressIndicator(
                    color = BowPressColors.Accent,
                    modifier = Modifier.size(48.dp),
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "BowPress",
                color = BowPressColors.Accent,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            )

            Spacer(Modifier.height(8.dp))

            AnalyzingDotsLabel()
        }
    }
}

/** "Analyzing your data" with trailing dots that cycle 1→2→3→1…, ~0.4s per step. */
@Composable
private fun AnalyzingDotsLabel() {
    var phase by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(400)
            phase = (phase + 1) % 3
        }
    }
    val dots = ".".repeat(phase + 1).padEnd(3, ' ')
    Text(
        text = "Analyzing your data$dots",
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}
