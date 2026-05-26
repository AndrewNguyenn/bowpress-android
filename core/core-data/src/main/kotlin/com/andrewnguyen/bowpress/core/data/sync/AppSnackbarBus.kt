package com.andrewnguyen.bowpress.core.data.sync

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide bus for non-blocking snackbar hints — primarily the C1
 * partial-share failure ("Posted, but your description didn't attach.").
 *
 * Mirrors iOS `SessionViewModel.lastSharePartialFailure` reaching the
 * `MainTabView` alert surface. On Android the SessionViewModel's lifecycle
 * is shorter than the screen the user navigates back to, so the partial-
 * failure hint travels through this app-scoped singleton; [AppStateViewModel]
 * collects from [events] and exposes the latest message via UiState; the
 * Compose tree at [MainScaffold] renders a `Snackbar` driven off that
 * field and clears it via [consume].
 *
 * Multiple consecutive emits coalesce: only the most recent message is
 * surfaced (so a retry can't queue a backlog of stale hints).
 */
@Singleton
class AppSnackbarBus @Inject constructor() {

    private val _events = MutableSharedFlow<String>(
        replay = 1,
        extraBufferCapacity = 1,
    )

    /** Collected by [AppStateViewModel] at construction. */
    val events: SharedFlow<String> = _events.asSharedFlow()

    /** Surface a non-blocking hint. Safe to call from any thread. */
    fun emit(message: String) {
        _events.tryEmit(message)
    }
}
