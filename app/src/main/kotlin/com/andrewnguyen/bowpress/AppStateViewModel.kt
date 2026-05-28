package com.andrewnguyen.bowpress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.export.ExportJobScheduler
import com.andrewnguyen.bowpress.core.data.repository.SessionRepository
import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.data.repository.SuggestionRepository
import com.andrewnguyen.bowpress.core.data.repository.ThemePreferencesRepository
import com.andrewnguyen.bowpress.core.data.repository.UnitPreferencesRepository
import com.andrewnguyen.bowpress.core.model.ThemePreference
import com.andrewnguyen.bowpress.core.data.repository.UserRepository
import com.andrewnguyen.bowpress.core.data.seed.DevMockCourses
import com.andrewnguyen.bowpress.core.data.seed.DevMockDataSeeder
import com.andrewnguyen.bowpress.core.designsystem.coursemap.ElevationGridCache
import com.andrewnguyen.bowpress.core.designsystem.coursemap.MockTerrain
import com.andrewnguyen.bowpress.core.data.sync.AnalyticsRefreshBus
import com.andrewnguyen.bowpress.core.data.sync.AppSnackbarBus
import com.andrewnguyen.bowpress.core.data.sync.LocalHydration
import com.andrewnguyen.bowpress.core.data.sync.SocialBadgeRefreshBus
import com.andrewnguyen.bowpress.core.model.Entitlement
import com.andrewnguyen.bowpress.core.model.UnitSystem
import com.andrewnguyen.bowpress.core.model.User
import com.andrewnguyen.bowpress.feature.subscription.PlayBillingManager
import com.andrewnguyen.bowpress.push.PushInitializer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Top-level app state. Mirrors iOS `AppState` — but narrow: it only owns
 * signals the host activity needs (auth gate, hydration splash, unread badge).
 * Feature VMs own their own data.
 */
@HiltViewModel
class AppStateViewModel @Inject constructor(
    private val userRepository: UserRepository,
    suggestionRepository: SuggestionRepository,
    private val unitPreferencesRepository: UnitPreferencesRepository,
    private val themePreferencesRepository: ThemePreferencesRepository,
    private val pushInitializer: PushInitializer,
    billingManager: PlayBillingManager,
    private val analyticsRefreshBus: AnalyticsRefreshBus,
    private val devMockDataSeeder: DevMockDataSeeder,
    private val socialRepository: SocialRepository,
    private val socialBadgeRefreshBus: SocialBadgeRefreshBus,
    private val appSnackbarBus: AppSnackbarBus,
    private val localHydration: LocalHydration,
    private val sessionRepository: SessionRepository,
    private val exportJobScheduler: ExportJobScheduler,
) : ViewModel() {

    init {
        // Mirror iOS AppState DEBUG path: pre-flip the auth gate with a
        // dev fixture user so the emulator boots straight into MainScaffold
        // (iOS XCUITest baseline). Real signed-in sessions are detected by
        // the token-store check inside seedDevAuth and short-circuit.
        //
        // This init block sits BEFORE the property initializers below so
        // `_uiState`'s read of `isSignedIn` sees the seeded state — Kotlin
        // executes init blocks + property initializers in declaration order.
        if (BuildConfig.DEBUG) userRepository.seedDevAuth()
    }

    private val _uiState = MutableStateFlow(
        AppUiState(
            isAuthenticated = userRepository.isSignedIn,
            currentUser = null,
            isHydrating = userRepository.isSignedIn,
            unreadSuggestionCount = 0,
            socialPendingCount = 0,
        ),
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    /** Active unit system, collected once here and published via CompositionLocal. */
    val unitSystem: StateFlow<UnitSystem> = unitPreferencesRepository.unitSystem.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UnitSystem.DEFAULT,
    )

    /** Active theme preference, threaded into BowPressTheme. */
    val themePreference: StateFlow<ThemePreference> =
        themePreferencesRepository.themePreference.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ThemePreference.SYSTEM,
        )

    /** Current [Entitlement] observed from Play Billing. */
    val entitlement: StateFlow<Entitlement> = billingManager.entitlement

    /**
     * Mirrors iOS `SubscriptionManager.isSubscribed` — active purchase *or*
     * any in-trial status counts as subscribed for gating purposes.
     */
    val isSubscribed: StateFlow<Boolean> = billingManager.entitlement
        .map { it.isActive || it.inTrial }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    /**
     * Incrementing nonce that analytics VMs observe to force a re-fetch.
     * Bumped by [bumpAnalyticsRefresh] (explicit UI action) and by the
     * [AnalyticsRefreshBus] (FCM foreground arrival, completed sync, etc.).
     * Mirrors the iOS `AppState` refresh-observable pattern used by
     * `NotificationRouter.handleForegroundArrival(userInfo:)`.
     */
    private val _analyticsRefreshNonce = MutableStateFlow(0)
    val analyticsRefreshNonce: StateFlow<Int> = _analyticsRefreshNonce.asStateFlow()

    fun bumpAnalyticsRefresh() {
        _analyticsRefreshNonce.update { it + 1 }
    }

    /**
     * Re-fetch the Social-tab badge count (`/social/pending-count`). Called on
     * hydration, on Social-tab selection, and after any invitation/friend
     * accept-or-decline (via [SocialBadgeRefreshBus]). Best-effort: a fake dev
     * token / offline state leaves the prior count untouched.
     */
    fun refreshSocialPendingCount() {
        viewModelScope.launch {
            runCatching { socialRepository.getPendingCount() }
                .onSuccess { count ->
                    _uiState.value = _uiState.value.copy(socialPendingCount = count.total)
                }
        }
    }

    fun setUnitSystem(system: UnitSystem) {
        viewModelScope.launch { unitPreferencesRepository.setUnitSystem(system) }
    }

    /** Clear the app-wide Snackbar hint once the host has shown it. */
    fun consumePendingSnackbar() {
        _uiState.value = _uiState.value.copy(pendingSnackbar = null)
    }

    fun setThemePreference(preference: ThemePreference) {
        viewModelScope.launch { themePreferencesRepository.setThemePreference(preference) }
    }

    init {
        // Mirror signed-in user into ui state.
        userRepository.currentUser
            .onEach { user ->
                _uiState.value = _uiState.value.copy(
                    currentUser = user,
                    isAuthenticated = user != null || userRepository.isSignedIn,
                )
            }
            .launchIn(viewModelScope)

        // Unread badge: count suggestions that are unread AND not dismissed.
        suggestionRepository.observeAll()
            .map { list -> list.count { !it.wasRead && !it.wasDismissed } }
            .onEach { count ->
                _uiState.value = _uiState.value.copy(unreadSuggestionCount = count)
            }
            .launchIn(viewModelScope)

        // Any process-wide refresh ping (FCM foreground arrival, completed
        // sync, etc.) bumps the analytics nonce — feature VMs collect the
        // nonce to trigger re-fetch.
        analyticsRefreshBus.events
            .onEach { bumpAnalyticsRefresh() }
            .launchIn(viewModelScope)

        // Social-badge invalidation pings (accept/decline of an invitation or
        // friend request, social push arrival) re-fetch the pending count.
        socialBadgeRefreshBus.events
            .onEach { refreshSocialPendingCount() }
            .launchIn(viewModelScope)

        // C1 partial-share hints — surfaced as the MainScaffold Snackbar.
        appSnackbarBus.events
            .onEach { msg ->
                _uiState.value = _uiState.value.copy(pendingSnackbar = msg)
            }
            .launchIn(viewModelScope)

        if (userRepository.isSignedIn) hydrate()
    }

    fun onSignedIn() {
        _uiState.value = _uiState.value.copy(isAuthenticated = true, isHydrating = true)
        hydrate()
    }

    fun onSignedOut() {
        userRepository.signOut()
        // Cancel any in-flight session-finalize PUT so it can't ride
        // the next account's bearer when the next user signs in. The
        // network layer would 401 + swallow via runCatching, but
        // belt-and-suspenders.
        sessionRepository.cancelInFlight()
        _uiState.value = AppUiState(
            isAuthenticated = false,
            currentUser = null,
            isHydrating = false,
            unreadSuggestionCount = 0,
            socialPendingCount = 0,
        )
    }

    private fun hydrate() {
        // DEBUG: seed synthetic terrain grids for every mock 3D course first
        // thing — synchronous, pure-CPU — so the social feed's course maps
        // find a cached grid the moment they render and draw real contours
        // instead of blank paper. Mirrors iOS `DevSocialMockData.seedElevationCache()`.
        if (BuildConfig.DEBUG) seedElevationCache()
        viewModelScope.launch {
            // DEBUG: seed Room with mock archery data so the Analytics tab
            // doesn't show "Not enough data" on a fresh emulator. Mirrors
            // iOS DevMockData (Sources/BowPress/State/DevMockData.swift).
            // Idempotent — no-op once any bow exists.
            val seededFresh = if (BuildConfig.DEBUG) {
                runCatching { devMockDataSeeder.seedIfEmpty() }.getOrDefault(Unit).let { true }
            } else false
            runCatching { userRepository.refreshProfile() }
            // Pull bows + configs + sessions + per-session plots/ends/stations
            // so the Session log, Analytics, and detail screens have arrow data
            // on first launch / second device. Mirrors iOS LocalHydration.
            runCatching { localHydration.hydrateFromApi() }
            // Phase B — belt-and-suspenders resume of any finish-time share
            // job that was persisted to Room but whose WorkManager request
            // wasn't enqueued before a process kill. (WorkManager already
            // re-runs work it did persist; this only covers the
            // enqueue-but-not-scheduled gap.)
            runCatching { exportJobScheduler.resumePendingJobs() }
            pushInitializer.start()
            refreshSocialPendingCount()
            _uiState.value = _uiState.value.copy(isHydrating = false)
            // Feature VMs (AnalyticsDashboardViewModel et al.) may have
            // collected their initial flow emission while Room was still
            // empty — bump the refresh bus once after seeding so they
            // re-query and pick up the fixture data.
            if (seededFresh) analyticsRefreshBus.bump()
        }
    }

    /**
     * Synthesizes a terrain grid covering each mock 3D course's footprint and
     * stores it in [ElevationGridCache] — sized to the course's own stations +
     * inferred targets (a fixed 800 m box would shrink the course inside it and
     * break the feed map's edge-to-edge fill). The feed `CourseBand` then looks
     * the grid up and draws contours, mirroring what a live session caches
     * after its elevation fetch.
     */
    private fun seedElevationCache() {
        for (stations in DevMockCourses.all3DCourseStations) {
            MockTerrain.gridCoveringCourse(coveringStations = stations)
                ?.let { ElevationGridCache.store(it) }
        }
    }
}

data class AppUiState(
    val isAuthenticated: Boolean,
    val currentUser: User?,
    val isHydrating: Boolean,
    val unreadSuggestionCount: Int,
    /** Social-tab badge count — incoming friend requests + pending invitations. */
    val socialPendingCount: Int = 0,
    /**
     * Latest app-wide non-blocking hint, or null when nothing pending —
     * surfaced as a Snackbar at the MainScaffold level. Currently driven
     * by [AppSnackbarBus] for the C1 partial-share failure hint.
     */
    val pendingSnackbar: String? = null,
)
