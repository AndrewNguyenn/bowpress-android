package com.andrewnguyen.bowpress.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.andrewnguyen.bowpress.core.model.ThemePreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themePreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "theme_preferences",
)

private val THEME_PREFERENCE_KEY = stringPreferencesKey(ThemePreference.STORAGE_KEY)

/**
 * Persists the user's appearance preference via DataStore. Mirrors iOS
 * `@AppStorage("themePreference")`. Consumers observe [themePreference] as
 * a [Flow] — the app root collects it once and threads it into BowPressTheme.
 */
@Singleton
class ThemePreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val themePreference: Flow<ThemePreference> =
        context.themePreferencesDataStore.data.map { prefs ->
            ThemePreference.fromStorage(prefs[THEME_PREFERENCE_KEY])
        }

    suspend fun setThemePreference(preference: ThemePreference) {
        context.themePreferencesDataStore.edit { prefs ->
            prefs[THEME_PREFERENCE_KEY] = ThemePreference.toStorage(preference)
        }
    }
}
