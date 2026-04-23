package com.andrewnguyen.bowpress.feature.subscription

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the [SubscriptionVerifier] implementation. Kept separate from
 * `core-data`'s repository module so the backend-pending stub stays local to
 * `feature-subscription` and is easy to swap once the backend lands.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SubscriptionModule {
    @Binds
    @Singleton
    abstract fun bindSubscriptionVerifier(
        impl: HttpSubscriptionVerifier,
    ): SubscriptionVerifier
}
