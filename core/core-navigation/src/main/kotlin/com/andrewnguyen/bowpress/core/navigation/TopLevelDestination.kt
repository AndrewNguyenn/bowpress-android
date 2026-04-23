package com.andrewnguyen.bowpress.core.navigation

/**
 * The top-level destinations shown in the app's bottom navigation.
 * Order here is the order they appear in the bottom bar.
 */
enum class TopLevelDestination(val route: String) {
    Analytics(route = "analytics"),
    Log(route = "log"),
    Session(route = "session"),
    Equipment(route = "equipment"),
    Settings(route = "settings"),
}
