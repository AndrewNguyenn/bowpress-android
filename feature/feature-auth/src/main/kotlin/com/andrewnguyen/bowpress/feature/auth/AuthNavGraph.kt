package com.andrewnguyen.bowpress.feature.auth

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Nav routes exposed by feature-auth. Import these from the host NavHost.
 */
object AuthRoutes {
    const val GRAPH = "auth"
    const val LANDING = "auth/landing"
    const val EMAIL = "auth/email"
    /** `auth/verify/{email}` — email is URL-encoded so `+` and `@` survive. */
    const val VERIFY = "auth/verify/{email}"
    fun verifyFor(email: String): String =
        "auth/verify/${URLEncoder.encode(email, StandardCharsets.UTF_8.name())}"
}

/**
 * Install the auth flow under [AuthRoutes.GRAPH]. The caller passes a
 * [onSignedIn] callback that fires when the user is fully authenticated
 * (either via sign-in, verify-email, or Google). The host is responsible for
 * popping the auth graph and navigating to the main flow.
 *
 * Usage from the app NavHost:
 * ```
 * NavHost(navController, startDestination = AuthRoutes.GRAPH) {
 *     authNavGraph(navController) { navController.navigate("main") }
 *     // …
 * }
 * ```
 */
fun NavGraphBuilder.authNavGraph(
    navController: NavController,
    onSignedIn: () -> Unit,
) {
    navigation(startDestination = AuthRoutes.LANDING, route = AuthRoutes.GRAPH) {
        composable(AuthRoutes.LANDING) {
            AuthScreen(
                onNavigateToEmail = { navController.navigate(AuthRoutes.EMAIL) },
                onSignedIn = onSignedIn,
            )
        }
        composable(AuthRoutes.EMAIL) {
            EmailAuthScreen(
                onCancel = { navController.popBackStack() },
                onSignedIn = onSignedIn,
                onNavigateToVerify = { email ->
                    navController.navigate(AuthRoutes.verifyFor(email))
                },
            )
        }
        composable(
            route = AuthRoutes.VERIFY,
            arguments = listOf(navArgument("email") { type = NavType.StringType }),
        ) { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString("email").orEmpty()
            val email = URLDecoder.decode(encoded, StandardCharsets.UTF_8.name())
            VerifyEmailScreen(
                email = email,
                onCancel = { navController.popBackStack() },
                onVerified = onSignedIn,
            )
        }
    }
}
