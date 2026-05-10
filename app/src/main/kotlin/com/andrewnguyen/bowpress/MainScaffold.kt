package com.andrewnguyen.bowpress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
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
 * iOS `MainTabView` order (Dashboard → Analytics → Session → Equipment →
 * Settings) with Kenrokuen chrome: AppPaper ground, pondDk selected tint,
 * Inter-tracked labels. Each tab is a nested sub-graph so its back-stack
 * survives tab switches.
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
        containerColor = AppPaper,
        bottomBar = {
            // 1dp AppLine top border drawn above the bar — matches the
            // analytics-japanese.html tab frame. We do it via a Column so
            // the NavigationBar stays untouched from an accessibility
            // perspective (hit targets unchanged).
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(AppLine),
                )
                NavigationBar(
                    containerColor = AppPaper,
                    tonalElevation = 0.dp,
                    modifier = Modifier.testTag(TestTags.MainTabBar),
                ) {
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
                            label = {
                                Text(
                                    text = tab.label.uppercase(),
                                    style = interUI(9.sp, FontWeight.SemiBold)
                                        .copy(letterSpacing = 0.2.em),
                                )
                            },
                            // Material's default picks a secondary-container
                            // indicator pill; we want the bar itself to carry
                            // the paper ground and only the icon/label tint
                            // to change, so the indicator gets a transparent
                            // fill. Selected → pondDk, idle → ink3.
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent,
                                selectedIconColor = AppPondDk,
                                unselectedIconColor = AppInk3,
                                selectedTextColor = AppPondDk,
                                unselectedTextColor = AppInk3,
                            ),
                        )
                    }
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
