package com.andrewnguyen.bowpress.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/**
 * Routes owned by `feature-settings`. Nested under the top-level `settings`
 * tab — `BowPressNavHost` maps the tab's route to [SettingsRoutes.HOME].
 */
object SettingsRoutes {
    const val HOME = "settings/home"
    const val EDIT_PROFILE = "settings/profile"
    const val CHANGE_PASSWORD = "settings/change-password"
    const val DELETE_ACCOUNT = "settings/delete-account"

    /** Paywall route used to navigate out of the settings graph. */
    const val PAYWALL = "subscription/paywall"
}

/**
 * Install the settings graph into the root [NavGraphBuilder]. [onSignedOut] is
 * invoked after the user signs out or deletes their account — the app layer
 * routes back to the auth screen.
 */
fun NavGraphBuilder.settingsNavGraph(
    navController: NavController,
    onSignedOut: () -> Unit,
) {
    composable(SettingsRoutes.HOME) {
        SettingsScreen(
            onEditProfile = { navController.navigate(SettingsRoutes.EDIT_PROFILE) },
            onChangePassword = { navController.navigate(SettingsRoutes.CHANGE_PASSWORD) },
            onDeleteAccount = { navController.navigate(SettingsRoutes.DELETE_ACCOUNT) },
            onManageSubscription = { navController.navigate(SettingsRoutes.PAYWALL) },
            onSignedOut = onSignedOut,
        )
    }

    composable(SettingsRoutes.EDIT_PROFILE) {
        EditProfileScreen(onBack = { navController.popBackStack() })
    }

    composable(SettingsRoutes.CHANGE_PASSWORD) {
        ChangePasswordScreen(onBack = { navController.popBackStack() })
    }

    composable(SettingsRoutes.DELETE_ACCOUNT) {
        DeleteAccountScreen(
            onBack = { navController.popBackStack() },
            onDeleted = onSignedOut,
        )
    }
}
