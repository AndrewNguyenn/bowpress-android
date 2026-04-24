package com.andrewnguyen.bowpress

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.andrewnguyen.bowpress.feature.analytics.navigation.AnalyticsRoutes
import com.andrewnguyen.bowpress.feature.analytics.navigation.analyticsNavGraph
import com.andrewnguyen.bowpress.feature.analytics.suggestions.SuggestionsDashboardScreen
import com.andrewnguyen.bowpress.feature.equipment.nav.EquipmentRoutes
import com.andrewnguyen.bowpress.feature.equipment.nav.equipmentNavGraph
import com.andrewnguyen.bowpress.feature.session.SessionRoutes
import com.andrewnguyen.bowpress.feature.session.sessionNavGraph
import com.andrewnguyen.bowpress.feature.settings.SettingsRoutes
import com.andrewnguyen.bowpress.feature.settings.settingsNavGraph
import com.andrewnguyen.bowpress.feature.subscription.subscriptionNavGraph

/**
 * Root scaffold shown once the user is authenticated. Bottom bar mirrors the
 * iOS `MainTabView` order: Dashboard → Analytics → Session → Equipment → Settings.
 * Each tab is a nested sub-graph so its back-stack is preserved across tab
 * switches (iOS `NavigationStack`-per-tab parity).
 */
@Composable
fun MainScaffold(
    uiState: AppUiState,
    onSignedOut: () -> Unit,
) {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.hierarchy?.firstOrNull()?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopTab.entries.forEach { tab ->
                    val selected = currentRoute?.startsWith(tab.graphRoute) == true
                    val badge = if (tab == TopTab.Dashboard && uiState.unreadSuggestionCount > 0) {
                        uiState.unreadSuggestionCount
                    } else null

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.graphRoute) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            if (badge != null) {
                                BadgedBox(badge = { Badge { Text(badge.toString()) } }) {
                                    Icon(tab.icon, contentDescription = tab.label)
                                }
                            } else {
                                Icon(tab.icon, contentDescription = tab.label)
                            }
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TopTab.Dashboard.graphRoute,
            modifier = Modifier.padding(padding),
        ) {
            navigation(
                route = TopTab.Dashboard.graphRoute,
                startDestination = "tab/dashboard/home",
            ) {
                composable("tab/dashboard/home") {
                    SuggestionsDashboardScreen(
                        onOpenSuggestion = { bowId, suggestionId ->
                            navController.navigate(
                                AnalyticsRoutes.suggestionDetail(bowId = bowId, suggestionId = suggestionId),
                            )
                        },
                    )
                }
            }

            navigation(
                route = TopTab.Analytics.graphRoute,
                startDestination = AnalyticsRoutes.Graph,
            ) {
                analyticsNavGraph(navController)
            }

            navigation(
                route = TopTab.Session.graphRoute,
                startDestination = SessionRoutes.HOME,
            ) {
                sessionNavGraph(navController)
            }

            navigation(
                route = TopTab.Equipment.graphRoute,
                startDestination = EquipmentRoutes.HOME,
            ) {
                equipmentNavGraph(
                    navController = navController,
                    currentUserId = { uiState.currentUser?.id.orEmpty() },
                )
            }

            navigation(
                route = TopTab.Settings.graphRoute,
                startDestination = SettingsRoutes.HOME,
            ) {
                settingsNavGraph(navController, onSignedOut = onSignedOut)
            }

            // Paywall lives outside a single tab so it can be reached from
            // settings *and* from the 402 SubscriptionRequired interceptor
            // deep link.
            subscriptionNavGraph(navController)
        }
    }
}

private enum class TopTab(
    val graphRoute: String,
    val label: String,
    val icon: ImageVector,
) {
    Dashboard("tab/dashboard", "Home", Icons.Filled.Home),
    Analytics("tab/analytics", "Analytics", Icons.Filled.BarChart),
    Session("tab/session", "Session", Icons.Filled.Whatshot),
    Equipment("tab/equipment", "Equipment", Icons.Filled.Build),
    Settings("tab/settings", "Settings", Icons.Filled.Settings),
}
