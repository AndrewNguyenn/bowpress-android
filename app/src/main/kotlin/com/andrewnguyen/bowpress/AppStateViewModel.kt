package com.andrewnguyen.bowpress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.SuggestionRepository
import com.andrewnguyen.bowpress.core.data.repository.UnitPreferencesRepository
import com.andrewnguyen.bowpress.core.data.repository.UserRepository
import com.andrewnguyen.bowpress.core.data.sync.AnalyticsRefreshBus
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
    private val pushInitializer: PushInitializer,
    billingManager: PlayBillingManager,
    analyticsRefreshBus: AnalyticsRefreshBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AppUiState(
            isAuthenticated = userRepository.isSignedIn,
            currentUser = null,
            isHydrating = userRepository.isSignedIn,
            unreadSuggestionCount = 0,
        ),
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    /** Active unit system, collected once here and published via CompositionLocal. */
    val unitSystem: StateFlow<UnitSystem> = unitPreferencesRepository.unitSystem.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UnitSystem.DEFAULT,
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

    fun setUnitSystem(system: UnitSystem) {
        viewModelScope.launch { unitPreferencesRepository.setUnitSystem(system) }
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

        if (userRepository.isSignedIn) hydrate()
    }

    fun onSignedIn() {
        _uiState.value = _uiState.value.copy(isAuthenticated = true, isHydrating = true)
        hydrate()
    }

    fun onSignedOut() {
        userRepository.signOut()
        _uiState.value = AppUiState(
            isAuthenticated = false,
            currentUser = null,
            isHydrating = false,
            unreadSuggestionCount = 0,
        )
    }

    private fun hydrate() {
        viewModelScope.launch {
            runCatching { userRepository.refreshProfile() }
            pushInitializer.start()
            _uiState.value = _uiState.value.copy(isHydrating = false)
        }
    }
}

data class AppUiState(
    val isAuthenticated: Boolean,
    val currentUser: User?,
    val isHydrating: Boolean,
    val unreadSuggestionCount: Int,
)
