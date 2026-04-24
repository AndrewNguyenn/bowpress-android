package com.andrewnguyen.bowpress

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.andrewnguyen.bowpress.core.designsystem.LocalUnitSystem
import com.andrewnguyen.bowpress.core.designsystem.LocalUnitSystemSetter
import com.andrewnguyen.bowpress.core.designsystem.splash.HydrationSplashScreen
import com.andrewnguyen.bowpress.feature.auth.AuthRoutes
import com.andrewnguyen.bowpress.feature.auth.authNavGraph

private const val ROUTE_MAIN = "main"

/**
 * Top-level composable. Gates on auth state, then mounts either the auth
 * graph or the main scaffold. Hydration splash overlays the whole surface
 * while the initial `/me` + suggestion fetch is in flight.
 */
@Composable
fun BowPressApp(
    viewModel: AppStateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val unitSystem by viewModel.unitSystem.collectAsStateWithLifecycle()
    val navController = rememberNavController()

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

        HydrationSplashScreen(isHydrating = uiState.isHydrating)
    }
    }
}
