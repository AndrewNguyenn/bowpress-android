package com.andrewnguyen.bowpress

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Named

/**
 * Implements [Configuration.Provider] so WorkManager picks up our
 * [HiltWorkerFactory] and can construct `@HiltWorker`s
 * ([com.andrewnguyen.bowpress.core.data.sync.BowPressSyncWorker] in particular)
 * with their injected dependencies.
 *
 * Implements [ImageLoaderFactory] so Coil's singleton ImageLoader routes
 * through a dedicated `@Named("avatarImage")` OkHttp client — auth attached
 * via `AuthInterceptor`, but `ErrorInterceptor` is left off the chain so a
 * non-200 image response doesn't get turned into a typed `ApiException`
 * with a JSON-parse attempt on raw image bytes. Without this, every
 * missing-avatar 404 would surface as a noisy stack trace in debug logs.
 */
@HiltAndroidApp
class BowPressApplication : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(): ImageLoader {
        val okHttp = EntryPointAccessors.fromApplication(
            this,
            OkHttpClientEntryPoint::class.java,
        ).okHttpClient()
        return ImageLoader.Builder(this)
            .okHttpClient(okHttp)
            .build()
    }
}

/**
 * Hilt entry point for grabbing the avatar-image OkHttp client outside the
 * injection graph (Application class can't be @Inject-constructed).
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface OkHttpClientEntryPoint {
    @Named("avatarImage")
    fun okHttpClient(): OkHttpClient
}
