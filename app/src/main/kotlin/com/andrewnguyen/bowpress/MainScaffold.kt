package com.andrewnguyen.bowpress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Tune
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.feature.analytics.history.HistoricalSessionsScreen
import com.andrewnguyen.bowpress.feature.analytics.sessiondetail.SessionDetailScreen
import com.andrewnguyen.bowpress.feature.analytics.navigation.AnalyticsRoutes
import com.andrewnguyen.bowpress.feature.analytics.navigation.analyticsNavGraph
import com.andrewnguyen.bowpress.feature.equipment.nav.EquipmentRoutes
import com.andrewnguyen.bowpress.feature.equipment.nav.equipmentNavGraph
import com.andrewnguyen.bowpress.feature.session.SessionRoutes
import com.andrewnguyen.bowpress.feature.session.sessionNavGraph
import com.andrewnguyen.bowpress.feature.settings.SettingsRoutes
import com.andrewnguyen.bowpress.feature.settings.settingsNavGraph
import com.andrewnguyen.bowpress.feature.social.nav.SocialRoutes
import com.andrewnguyen.bowpress.feature.social.nav.socialNavGraph
import com.andrewnguyen.bowpress.feature.subscription.subscriptionNavGraph

/**
 * Root scaffold shown once the user is authenticated. Bottom bar mirrors the
 * iOS `MainTabView` order (Analytics → Log → Session → Equipment → Settings)
 * with Kenrokuen chrome: AppPaper ground, pondDk selected tint, Inter-tracked
 * labels. Each tab is a nested sub-graph so its back-stack survives tab
 * switches.
 *
 * SuggestionsDashboardScreen is no longer a top-level tab — its content is
 * rendered inline in the Analytics tab's `SuggestionsLedgerSection`, matching
 * iOS `AnalyticsView`'s layout.
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
                        val badge = if (tab == TopTab.Analytics && uiState.unreadSuggestionCount > 0) {
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
            startDestination = TopTab.Analytics.graphRoute,
            modifier = Modifier.padding(padding),
        ) {
            navigation(
                route = TopTab.Analytics.graphRoute,
                startDestination = AnalyticsRoutes.Graph,
            ) {
                analyticsNavGraph(navController)
            }

            navigation(
                route = TopTab.Log.graphRoute,
                startDestination = "tab/log/home",
            ) {
                composable("tab/log/home") {
                    HistoricalSessionsScreen(
                        // Log is a top-level tab — there is no nav back from the root.
                        onBack = { /* no-op at tab root */ },
                        onOpenSession = { sessionId ->
                            navController.navigate("tab/log/session/$sessionId")
                        },
                    )
                }
                composable(
                    route = "tab/log/session/{sessionId}",
                    arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
                ) { entry ->
                    val sessionId = entry.arguments?.getString("sessionId").orEmpty()
                    SessionDetailScreen(
                        sessionId = sessionId,
                        onBack = { navController.popBackStack() },
                    )
                }
            }

            navigation(
                route = TopTab.Session.graphRoute,
                startDestination = SessionRoutes.HOME,
            ) {
                sessionNavGraph(navController)
            }

            navigation(
                route = TopTab.Social.graphRoute,
                startDestination = SocialRoutes.FEED,
            ) {
                socialNavGraph(
                    navController = navController,
                    onSignedOut = onSignedOut,
                    onAccountClick = {
                        navController.navigate(SettingsRoutes.ACCOUNT)
                    },
                    onSubscriptionClick = {
                        navController.navigate(SettingsRoutes.PAYWALL)
                    },
                    onEquipmentClick = {
                        navController.navigate(TopTab.Equipment.graphRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNotificationsClick = { /* no-op: handled in Settings */ },
                )
                // Settings screens reachable from YouScreen (behind the avatar)
                settingsNavGraph(navController, onSignedOut = onSignedOut)
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
    // Bottom bar: Analytics, Log, Session, Social, Equipment.
    // Settings is behind the avatar in the Social feed's You screen.
    Analytics("tab/analytics", "Analytics", Icons.Filled.BarChart),
    Log("tab/log", "Log", Icons.Filled.Assignment),
    Session("tab/session", "Session", Icons.Filled.TrackChanges),
    Social("tab/social", "Social", Icons.Filled.Group),
    Equipment("tab/equipment", "Equipment", Icons.Filled.Tune),
}
