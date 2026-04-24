package com.andrewnguyen.bowpress.core.data.preferences

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

private val Context.notificationPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "notification_preferences",
)

private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")

/**
 * Persists the user's notifications preference via DataStore. Mirrors iOS
 * `@AppStorage("notificationsEnabled")`. Default is true so existing installs
 * continue receiving pushes without an explicit opt-in.
 */
@Singleton
class NotificationPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val notificationsEnabled: Flow<Boolean> = context.notificationPreferencesDataStore.data.map { prefs ->
        prefs[NOTIFICATIONS_ENABLED_KEY] ?: true
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.notificationPreferencesDataStore.edit { prefs ->
            prefs[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }
}
