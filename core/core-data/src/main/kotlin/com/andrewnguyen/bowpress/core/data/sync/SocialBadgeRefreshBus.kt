package com.andrewnguyen.bowpress.core.data.sync

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide bus for "refresh the Social tab badge" pings.
 *
 * The Social-tab badge count (`SocialPendingCount`) is owned by
 * [AppStateViewModel], but the events that invalidate it originate
 * elsewhere: a feature ViewModel after an accept/decline, or the FCM
 * service on receipt of a `friend_request` / `club_invite` /
 * `league_invite` push. Those components call [bump]; `AppStateViewModel`
 * collects [events] and re-fetches `/social/pending-count`.
 *
 * Mirrors [AnalyticsRefreshBus] — a Hilt singleton because Android
 * Services and feature VMs can't reach `AppStateViewModel` directly.
 */
@Singleton
class SocialBadgeRefreshBus @Inject constructor() {

    private val _events = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
    )

    /** Collected by [AppStateViewModel]. */
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    /** Fire a badge-refresh ping. Safe to call from any thread; non-suspending. */
    fun bump() {
        _events.tryEmit(Unit)
    }
}
