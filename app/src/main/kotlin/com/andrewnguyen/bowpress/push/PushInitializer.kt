package com.andrewnguyen.bowpress.push

import com.andrewnguyen.bowpress.BuildConfig
import com.andrewnguyen.bowpress.core.data.push.DeviceTokenRegistrar
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot push registration bootstrap. Called by `MainActivity` (or the app
 * layer's auth gate) once the user is signed in. Requests the current FCM
 * token, configures the environment label, and registers with the backend.
 *
 * Mirrors iOS `PushRegistrar.requestAndRegister()` — note that on Android,
 * notification **permission** is requested by the UI layer (Android 13+ runtime
 * permission) so this class only covers the token half.
 */
@Singleton
class PushInitializer @Inject constructor(
    private val registrar: DeviceTokenRegistrar,
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun start() {
        registrar.environmentOverride =
            if (BuildConfig.DEBUG) "development" else "production"
        scope.launch {
            runCatching { FirebaseMessaging.getInstance().token.await() }
                .onSuccess { token -> registrar.register(token) }
        }
    }
}
