package com.andrewnguyen.bowpress.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt wiring for the HTTP stack. Feature modules inject [BowPressApi] directly.
 *
 * `NetworkConfig` is resolved from `BuildConfig.DEBUG` so tests can override. The
 * OkHttp client stacks:
 *
 *   AuthInterceptor    — attaches Bearer token when the store has one
 *   ErrorInterceptor   — converts non-2xx into typed [ApiException]
 *   HttpLoggingInterceptor (debug only) — logs request/response bodies at BODY level
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideNetworkConfig(): NetworkConfig {
        // BuildConfig is generated per-module. To avoid a hard dependency on an `app`
        // BuildConfig, we resolve DEBUG by reflection off the root BuildConfig if
        // present; otherwise fall back to the platform debuggable hint by defaulting
        // to the release URL. In this codebase every module ships its own BuildConfig,
        // so a simple compile-time split via BuildConfig.DEBUG would also work. The
        // NetworkModuleDebug/Release split below picks up BuildConfig.DEBUG for you.
        val debug = com.andrewnguyen.bowpress.core.network.BuildConfig.DEBUG
        return NetworkConfig(
            baseUrl = if (debug) NetworkConfig.DEBUG_BASE_URL else NetworkConfig.RELEASE_BASE_URL,
            debug = debug,
        )
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(config: NetworkConfig): HttpLoggingInterceptor {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = if (config.debug) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
        return interceptor
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        errorInterceptor: ErrorInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(errorInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Coil ImageLoader uses this client instead of the Retrofit one so avatar
     * GETs ride through `AuthInterceptor` (Bearer attached) but skip
     * `ErrorInterceptor` (which would otherwise throw typed `ApiException`
     * + JSON-parse the body on every 404 / 401 image response — wasted work
     * and confusing debug-log noise). HTTP body logging is also dropped
     * because raw image bytes in logcat are useless.
     */
    @Provides
    @Singleton
    @Named("avatarImage")
    fun provideAvatarImageOkHttpClient(
        authInterceptor: AuthInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        client: OkHttpClient,
        json: Json,
        config: NetworkConfig,
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideBowPressApi(retrofit: Retrofit): BowPressApi =
        retrofit.create(BowPressApi::class.java)
}

/** Binds the [EncryptedPrefsTokenStore] implementation to the [TokenStore] interface. */
@Module
@InstallIn(SingletonComponent::class)
abstract class TokenStoreModule {

    @Binds
    @Singleton
    abstract fun bindTokenStore(impl: EncryptedPrefsTokenStore): TokenStore
}
