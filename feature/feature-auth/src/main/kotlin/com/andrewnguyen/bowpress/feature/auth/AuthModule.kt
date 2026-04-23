package com.andrewnguyen.bowpress.feature.auth

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt wiring for feature-auth. Today that's only [GoogleSignInClient] — the rest
 * is picked up via `@Inject constructor` on the ViewModel + repository.
 *
 * Tests can replace this module with `@TestInstallIn` to inject a fake
 * [GoogleSignInClient] and avoid touching Credential Manager.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindGoogleSignInClient(
        impl: CredentialManagerGoogleSignInClient,
    ): GoogleSignInClient
}
