package com.andrewnguyen.bowpress.core.network

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Minimal token store contract — isolates JWT read/write behind an interface so auth
 * interceptors don't pull in `androidx.security.crypto` directly (makes unit tests
 * trivial).
 */
interface TokenStore {
    fun getToken(): String?
    fun setToken(token: String)
    fun clear()
}

/**
 * Default implementation, backed by `EncryptedSharedPreferences`. The store uses the
 * Android Keystore-bound AES-256 master key — tokens are at-rest encrypted and
 * automatically invalidated if the app data is wiped.
 */
@Singleton
class EncryptedPrefsTokenStore @Inject constructor(
    @ApplicationContext context: Context,
) : TokenStore {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun getToken(): String? =
        prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotEmpty() }

    override fun setToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    override fun clear() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    private companion object {
        const val PREFS_FILE = "bowpress_auth"
        const val KEY_TOKEN = "auth_token"
    }
}
