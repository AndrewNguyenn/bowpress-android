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
 * `replay = 0` is deliberate. The bus is `@Singleton`, scoped to the
 * process — it outlives `AppStateViewModel` across sign-out → sign-in. A
 * `replay = 1` configuration would re-deliver the last stale hint to the
 * next `AppStateViewModel`'s collector on attach, firing the snackbar a
 * second time for a share that completed cleanly. The subscriber holds the
 * latest message in its own `MutableStateFlow` while the snackbar is on
 * screen, so live subscribers never miss an emit. `extraBufferCapacity = 1`
 * lets a `tryEmit` succeed even if the collector hasn't drained the previous
 * one yet, so consecutive emits coalesce instead of dropping the new hint.
 */
@Singleton
class AppSnackbarBus @Inject constructor() {

    private val _events = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1,
    )

    /** Collected by [AppStateViewModel] at construction. */
    val events: SharedFlow<String> = _events.asSharedFlow()

    /** Surface a non-blocking hint. Safe to call from any thread. */
    fun emit(message: String) {
        _events.tryEmit(message)
    }
}
