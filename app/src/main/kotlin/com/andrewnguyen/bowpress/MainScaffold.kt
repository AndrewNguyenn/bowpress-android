package com.andrewnguyen.bowpress

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
import com.andrewnguyen.bowpress.feature.session.threed.ThreeDAnalyticsScreen
import com.andrewnguyen.bowpress.feature.session.threed.ThreeDLogDetailScreen
import com.andrewnguyen.bowpress.feature.settings.SettingsRoutes
import com.andrewnguyen.bowpress.feature.settings.settingsNavGraph
import com.andrewnguyen.bowpress.feature.social.nav.SocialRoutes
import com.andrewnguyen.bowpress.feature.social.nav.socialNavGraph
import com.andrewnguyen.bowpress.feature.social.ui.location.LocationSoftPromptSheet
import com.andrewnguyen.bowpress.feature.social.ui.location.LocationSoftPromptViewModel
import com.andrewnguyen.bowpress.feature.subscription.subscriptionNavGraph
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel

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
// Page-transition duration for the main NavHost — ~80% faster than
// navigation-compose's 700ms default fade so tab switches feel instant.
private const val NAV_TRANSITION_MS = 140

@Composable
fun MainScaffold(
    uiState: AppUiState,
    onSignedOut: () -> Unit,
    onSocialTabSelected: () -> Unit = {},
    appStateViewModel: AppStateViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.hierarchy?.firstOrNull()?.route

    // Parity E7 — value-prop location prompt, shown once on first launch
    // after sign-in. ViewModel pulls the persisted "seen" flag from
    // DataStore; we mark seen on dismiss / allow either way.
    val softPromptVm: LocationSoftPromptViewModel = hiltViewModel()
    val shouldShowSoftPrompt by softPromptVm.shouldShow.collectAsState()
    if (shouldShowSoftPrompt) {
        LocationSoftPromptSheet(
            archerName = uiState.currentUser?.name.orEmpty(),
            onResolved = { softPromptVm.markSeen() },
        )
    }

    // C1 — partial-share failure hint surface. The SessionViewModel that
    // fired the share may be torn down by the time the archer lands back on
    // Log; the message rides through [AppSnackbarBus] and is dispatched as
    // a non-blocking Snackbar here.
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.pendingSnackbar) {
        val msg = uiState.pendingSnackbar ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        appStateViewModel.consumePendingSnackbar()
    }

    Scaffold(
        containerColor = AppPaper,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = AppInk3,
                    contentColor = AppPaper,
                )
            }
        },
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
                        val badge = when {
                            tab == TopTab.Analytics && uiState.unreadSuggestionCount > 0 ->
                                uiState.unreadSuggestionCount
                            tab == TopTab.Social && uiState.socialPendingCount > 0 ->
                                uiState.socialPendingCount
                            else -> null
                        }

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                // Re-poll the Social badge whenever the Social
                                // tab is (re-)selected — §12 contract.
                                if (tab == TopTab.Social) onSocialTabSelected()
                                navController.navigate(tab.graphRoute) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                // iOS parity (A4) — archery-specific monoline
                                // tab glyphs replace the generic Material icons.
                                // Painters resolve via R.drawable on the per-tab
                                // resource id, kept in the TopTab enum.
                                val painter = painterResource(tab.iconRes)
                                if (badge != null) {
                                    BadgedBox(badge = { Badge { Text(badge.toString()) } }) {
                                        Icon(painter, contentDescription = tab.label)
                                    }
                                } else {
                                    Icon(painter, contentDescription = tab.label)
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
            // iOS parity (A1) — Feed is the home tab. The route string
            // `tab/social` is preserved so existing deep links (which all
            // target `bowpress://social/...`) still land here without any
            // server- or client-side rewrite.
            startDestination = TopTab.Social.graphRoute,
            modifier = Modifier.padding(padding),
            // navigation-compose's default page transition is a 700ms fade,
            // which reads as sluggish when switching tabs. Drop it to 140ms
            // (~80% faster) for a snappy cross-fade between pages.
            enterTransition = { fadeIn(tween(NAV_TRANSITION_MS)) },
            exitTransition = { fadeOut(tween(NAV_TRANSITION_MS)) },
            popEnterTransition = { fadeIn(tween(NAV_TRANSITION_MS)) },
            popExitTransition = { fadeOut(tween(NAV_TRANSITION_MS)) },
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
                        onOpenSession = { sessionId, isThreeDCourse ->
                            val leaf = if (isThreeDCourse) "course" else "session"
                            navController.navigate("tab/log/$leaf/$sessionId")
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
                composable(
                    route = "tab/log/course/{sessionId}",
                    arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
                ) { entry ->
                    val sessionId = entry.arguments?.getString("sessionId").orEmpty()
                    ThreeDLogDetailScreen(
                        sessionId = sessionId,
                        onBack = { navController.popBackStack() },
                        onOpenAnalytics = { navController.navigate("tab/log/3d-analytics") },
                    )
                }
                composable("tab/log/3d-analytics") {
                    ThreeDAnalyticsScreen(onBack = { navController.popBackStack() })
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
    @androidx.annotation.DrawableRes val iconRes: Int,
) {
    // iOS parity (A1) — Feed is the home tab, so the Social entry is first
    // in declaration order (Compose iterates `entries` for the bottom bar).
    // Order: Feed · Log · Session · Analytics · Equipment.
    //
    // The route string `tab/social` is preserved on the Social entry so any
    // deep link / saved-state route survives the rename; only the
    // user-visible label is "Feed".
    //
    // iOS parity (A4) — archery-specific tab glyphs replace the Material
    // icons. The drawables live in :core:core-designsystem and are tinted
    // by NavigationBarItemDefaults per selection state.
    Social("tab/social", "Feed", com.andrewnguyen.bowpress.core.designsystem.R.drawable.bp_tab_feed),
    Log("tab/log", "Log", com.andrewnguyen.bowpress.core.designsystem.R.drawable.bp_tab_log),
    Session("tab/session", "Session", com.andrewnguyen.bowpress.core.designsystem.R.drawable.bp_tab_session),
    Analytics("tab/analytics", "Analytics", com.andrewnguyen.bowpress.core.designsystem.R.drawable.bp_tab_analytics),
    Equipment("tab/equipment", "Equipment", com.andrewnguyen.bowpress.core.designsystem.R.drawable.bp_tab_equipment),
}
