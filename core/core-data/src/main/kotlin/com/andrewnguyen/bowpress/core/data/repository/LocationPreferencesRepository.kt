package com.andrewnguyen.bowpress.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.locationPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "location_preferences",
)

private val HAS_SEEN_SOFT_PROMPT = booleanPreferencesKey("location.softPrompt.shownV1")

/**
 * Parity E7 — persists the "have we shown the value-prop location prompt
 * once already" flag (iOS commit 32b5a31, `LocationSoftPrompt.shownKey`).
 *
 * Once flipped to true the [LocationSoftPromptSheet] never re-appears on
 * this device. Versioned ("V1") so a future copy/visual revision can opt
 * archers back into seeing it again.
 */
@Singleton
class LocationPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Has the archer already seen the soft prompt on this install? */
    val hasSeenSoftPrompt: Flow<Boolean> =
        context.locationPrefsDataStore.data.map { prefs ->
            prefs[HAS_SEEN_SOFT_PROMPT] ?: false
        }

    /** Mark the prompt as shown — idempotent. */
    suspend fun markSoftPromptShown() {
        context.locationPrefsDataStore.edit { prefs ->
            prefs[HAS_SEEN_SOFT_PROMPT] = true
        }
    }
}
