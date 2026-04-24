package com.andrewnguyen.bowpress.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.andrewnguyen.bowpress.core.model.UnitSystem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.unitPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences",
)

private val UNIT_SYSTEM_KEY = stringPreferencesKey(UnitSystem.STORAGE_KEY)

/**
 * Persists the user's unit-system preference via DataStore. Mirrors iOS `@AppStorage("unitSystem")`.
 *
 * Consumers observe [unitSystem] as a [Flow] — the app root collects it once and
 * publishes the current value through a CompositionLocal so every Compose screen
 * can read it without prop-drilling.
 */
@Singleton
class UnitPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val unitSystem: Flow<UnitSystem> = context.unitPreferencesDataStore.data.map { prefs ->
        prefs[UNIT_SYSTEM_KEY]
            ?.let { raw -> runCatching { UnitSystem.valueOf(raw) }.getOrNull() }
            ?: UnitSystem.DEFAULT
    }

    suspend fun setUnitSystem(system: UnitSystem) {
        context.unitPreferencesDataStore.edit { prefs ->
            prefs[UNIT_SYSTEM_KEY] = system.name
        }
    }
}
