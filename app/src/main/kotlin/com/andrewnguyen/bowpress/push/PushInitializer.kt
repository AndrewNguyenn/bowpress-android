package com.andrewnguyen.bowpress.push

import android.util.Log
import com.andrewnguyen.bowpress.BuildConfig
import com.andrewnguyen.bowpress.core.data.push.DeviceTokenRegistrar
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    // SupervisorJob so a token-fetch failure doesn't cancel the scope; the
    // CoroutineExceptionHandler is a backstop so an uncaught 401 (e.g. the
    // backend rejecting the bearer-less /push/register call before sign-in)
    // never propagates to the default thread-group handler and kills the
    // process. iOS PushRegistrar runs after the user is signed in; the
    // Android entry point fires earlier, so it must tolerate auth-less
    // failures.
    private val scope = CoroutineScope(
        Dispatchers.IO +
            SupervisorJob() +
            CoroutineExceptionHandler { _, t ->
                Log.w(TAG, "Push registration failed; will retry on next launch", t)
            },
    )

    fun start() {
        registrar.environmentOverride =
            if (BuildConfig.DEBUG) "development" else "production"
        scope.launch {
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                registrar.register(token)
            }.onFailure {
                if (it is CancellationException) throw it
                Log.w(TAG, "Push registration failed", it)
            }
        }
    }

    private companion object {
        const val TAG = "PushInitializer"
    }
}
