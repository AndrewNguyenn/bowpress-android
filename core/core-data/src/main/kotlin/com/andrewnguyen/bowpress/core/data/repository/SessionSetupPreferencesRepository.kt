package com.andrewnguyen.bowpress.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.andrewnguyen.bowpress.core.model.TargetFaceType
import com.andrewnguyen.bowpress.core.model.TargetLayout
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionSetupDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "session_setup_preferences",
)

private val LAST_TARGET_LAYOUT_KEY = stringPreferencesKey("session.lastTargetLayout")

/**
 * Sticky last manually-picked target face — restored at setup before
 * falling back to the bow-type default. Mirrors iOS `@AppStorage("session.
 * lastSelectedTargetFaceType")` (parity gap C2). The key is left null when
 * the archer has never overridden the face, so the existing bow-default
 * smart-pick still kicks in on a fresh install.
 */
private val LAST_TARGET_FACE_TYPE_KEY = stringPreferencesKey("session.lastSelectedTargetFaceType")

/**
 * Persists sticky session-setup choices via DataStore. Currently just the
 * last multi-spot Vegas layout the archer picked — mirrors iOS
 * `@AppStorage("session.lastTargetLayout")`.
 *
 * When the setup screen re-enters the 20yd + 6-ring combo it restores this
 * pick so the archer doesn't have to re-choose Triangle vs Vertical every
 * time; only [TargetLayout.isMultiSpot] values are stored.
 */
@Singleton
class SessionSetupPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * The sticky last multi-spot layout, or [TargetLayout.SINGLE] when none
     * has been stored (the canonical "no sticky pick" sentinel).
     */
    val lastTargetLayout: Flow<TargetLayout> =
        context.sessionSetupDataStore.data.map { prefs ->
            prefs[LAST_TARGET_LAYOUT_KEY]
                ?.let { raw -> runCatching { TargetLayout.valueOf(raw) }.getOrNull() }
                ?: TargetLayout.SINGLE
        }

    /**
     * Persist a sticky multi-spot layout. A [TargetLayout.SINGLE] is ignored —
     * single is the implicit default everywhere, so storing it would just
     * churn the value when the layout field collapses off-combo.
     */
    suspend fun setLastTargetLayout(layout: TargetLayout) {
        if (!layout.isMultiSpot) return
        context.sessionSetupDataStore.edit { prefs ->
            prefs[LAST_TARGET_LAYOUT_KEY] = layout.name
        }
    }

    // ── Last target face (parity gap C2) ─────────────────────────────────────

    /**
     * The sticky last manually-picked target face, or null when none has been
     * stored. The setup screen prefers this over the bow-type default so a
     * compound archer who manually switched to 6-ring doesn't get bumped
     * back to 10-ring just because their bow default points there. Mirrors
     * iOS `@AppStorage("session.lastSelectedTargetFaceType")`.
     */
    val lastTargetFaceType: Flow<TargetFaceType?> =
        context.sessionSetupDataStore.data.map { prefs ->
            prefs[LAST_TARGET_FACE_TYPE_KEY]
                ?.let { raw -> runCatching { TargetFaceType.valueOf(raw) }.getOrNull() }
        }

    /** Persist the archer's last manual target-face pick. */
    suspend fun setLastTargetFaceType(face: TargetFaceType) {
        context.sessionSetupDataStore.edit { prefs ->
            prefs[LAST_TARGET_FACE_TYPE_KEY] = face.name
        }
    }
}
