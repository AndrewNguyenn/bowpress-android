package com.andrewnguyen.bowpress.core.model

/**
 * User-controlled appearance preference. Mirrors iOS `ThemePreference`:
 * `SYSTEM` follows the OS, `LIGHT` / `DARK` pin. Cycle order on the
 * Settings row: System → Light → Dark → System.
 *
 * Lives in core-model (not core-designsystem) so DataStore-backed
 * repositories can persist it without taking a Compose dependency.
 * BowPressTheme re-exports the value into a CompositionLocal.
 */
enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK;

    /** Title-Case label rendered as the value on the Settings row. */
    val displayLabel: String
        get() = when (this) {
            SYSTEM -> "System"
            LIGHT -> "Light"
            DARK -> "Dark"
        }

    /** Next preference in the cycle, used by the tap-to-cycle row. */
    val next: ThemePreference
        get() = when (this) {
            SYSTEM -> LIGHT
            LIGHT -> DARK
            DARK -> SYSTEM
        }

    companion object {
        const val STORAGE_KEY = "theme_preference"

        fun fromStorage(raw: String?): ThemePreference = when (raw) {
            "light" -> LIGHT
            "dark" -> DARK
            else -> SYSTEM
        }

        fun toStorage(p: ThemePreference): String = when (p) {
            SYSTEM -> "system"
            LIGHT -> "light"
            DARK -> "dark"
        }
    }
}
