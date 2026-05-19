package com.andrewnguyen.bowpress

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.andrewnguyen.bowpress.core.designsystem.BowPressTheme
import com.andrewnguyen.bowpress.core.designsystem.LocalUnitSystem
import com.andrewnguyen.bowpress.core.designsystem.LocalUnitSystemSetter
import com.andrewnguyen.bowpress.core.designsystem.splash.HydrationSplashScreen
import com.andrewnguyen.bowpress.feature.auth.AuthRoutes
import com.andrewnguyen.bowpress.feature.auth.authNavGraph
import com.andrewnguyen.bowpress.feature.subscription.ReadOnlyGate

private const val ROUTE_MAIN = "main"

/**
 * Top-level composable. Gates on auth state, then mounts either the auth
 * graph or the main scaffold. Hydration splash overlays the whole surface
 * while the initial `/me` + suggestion fetch is in flight.
 *
 * Splash dismissal mirrors iOS `ContentView` — definitive-guard pattern:
 *  1. Render splash while `splashDismissed` is false (no partial-opacity
 *     ghost behind sparse tabs).
 *  2. Require BOTH the 2.6s motion gate (`onMinimumElapsed`) AND hydration
 *     to settle before flipping `splashDismissed`.
 *  3. Safety timeout at 4.5s fires dismissal regardless — handles the
 *     no-backend case where the profile refresh wedges.
 *  4. Wrap the flip in a 450ms fade via `AnimatedVisibility`.
 */
@Composable
fun BowPressApp(
    viewModel: AppStateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val unitSystem by viewModel.unitSystem.collectAsStateWithLifecycle()
    val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
    val isSubscribed by viewModel.isSubscribed.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    // Splash dismissal state — flipped once both gates align, or the safety
    // timeout fires. The `AnimatedVisibility` below wraps the flip in a
    // 450ms fade so the splash layer doesn't pop off.
    var splashSettled by remember { mutableStateOf(false) }
    var splashDismissed by remember { mutableStateOf(false) }

    // Natural-path dismissal: motion gate + hydration settled.
    LaunchedEffect(splashSettled, uiState.isHydrating) {
        if (splashSettled && !uiState.isHydrating) {
            splashDismissed = true
        }
    }

    BowPressTheme(
        preference = themePreference,
        onPreferenceChange = viewModel::setThemePreference,
    ) {
    CompositionLocalProvider(
        LocalUnitSystem provides unitSystem,
        LocalUnitSystemSetter provides viewModel::setUnitSystem,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = if (uiState.isAuthenticated) ROUTE_MAIN else AuthRoutes.GRAPH,
            ) {
                authNavGraph(navController) {
                    viewModel.onSignedIn()
                    navController.navigate(ROUTE_MAIN) {
                        popUpTo(AuthRoutes.GRAPH) { inclusive = true }
                        launchSingleTop = true
                    }
                }

                composable(ROUTE_MAIN) {
                    ReadOnlyGate(isReadOnly = !isSubscribed) {
                        MainScaffold(
                            uiState = uiState,
                            onSignedOut = {
                                viewModel.onSignedOut()
                                navController.navigate(AuthRoutes.GRAPH) {
                                    popUpTo(ROUTE_MAIN) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }
            }

            // Splash only paints when authenticated. The auth gate pays for
            // its own loading state; we don't want the hydration splash
            // overlaying the sign-in form.
            if (uiState.isAuthenticated) {
                AnimatedVisibility(
                    visible = !splashDismissed,
                    enter = androidx.compose.animation.fadeIn(tween(0)),
                    exit = fadeOut(tween(durationMillis = 450)),
                ) {
                    HydrationSplashScreen(
                        onMinimumElapsed = { splashSettled = true },
                        onSafety = { splashDismissed = true },
                    )
                }
            }
        }
    }
    }
}
