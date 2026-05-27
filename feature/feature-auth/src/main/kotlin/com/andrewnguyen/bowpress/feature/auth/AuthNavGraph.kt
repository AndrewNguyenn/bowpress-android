package com.andrewnguyen.bowpress.feature.auth

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument

/**
 * Nav routes exposed by feature-auth. Import these from the host NavHost.
 */
object AuthRoutes {
    const val GRAPH = "auth"
    const val LANDING = "auth/landing"
    const val EMAIL = "auth/email"
    /**
     * `auth/verify/{email}` — email is path-encoded (Uri.encode) so `+` and
     * `@` survive. Don't use `URLEncoder.encode` here: it FORM-encodes (space
     * → `+`) and pairs badly with Compose Navigation's auto-Uri.decode on
     * receive, producing double-decode crashes for any literal `%` in the
     * value.
     */
    const val VERIFY = "auth/verify/{email}"
    fun verifyFor(email: String): String =
        "auth/verify/${Uri.encode(email)}"
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
            // Compose Navigation already ran Uri.decode on the path arg, so
            // the raw value IS the original email — see [AuthRoutes.VERIFY].
            val email = backStackEntry.arguments?.getString("email").orEmpty()
            VerifyEmailScreen(
                email = email,
                onCancel = { navController.popBackStack() },
                onVerified = onSignedIn,
            )
        }
    }
}
