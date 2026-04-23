package com.andrewnguyen.bowpress.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

/**
 * Placeholder NavHost. The real navigation graph — including auth gating and
 * feature-level sub-graphs — is wired up in task #8 (app integration).
 */
@Composable
fun BowPressNavHost(
    navController: NavHostController,
    startDestination: String = TopLevelDestination.Analytics.route,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        TopLevelDestination.entries.forEach { dest ->
            composable(dest.route) {
                // Feature graphs attached here by app integration task.
            }
        }
    }
}
