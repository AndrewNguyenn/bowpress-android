package com.andrewnguyen.bowpress.core.data.sync

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide bus for "refresh analytics" pings. Any component that
 * observes a signal meaning "analytics data is stale" (FCM message
 * arrival while in foreground, completed sync, explicit user pull-to-
 * refresh) can call [bump]; [AppStateViewModel] collects from [events]
 * and increments its `analyticsRefreshNonce`, which feature VMs
 * (AnalyticsDashboardViewModel et al.) observe to re-fetch.
 *
 * Mirrors iOS `NotificationRouter.handleForegroundArrival` pattern —
 * the iOS side writes directly into an observable on `AppState`, we
 * go through a Hilt singleton because Android Services can't inject
 * ViewModels.
 */
@Singleton
class AnalyticsRefreshBus @Inject constructor() {

    private val _events = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
    )

    /** Collected by [AppStateViewModel] at construction. */
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    /** Fire a refresh ping. Safe to call from any thread; non-suspending. */
    fun bump() {
        _events.tryEmit(Unit)
    }
}
