package com.andrewnguyen.bowpress.feature.social.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.data.sync.SocialBadgeRefreshBus
import com.andrewnguyen.bowpress.core.model.NotificationCategory
import com.andrewnguyen.bowpress.core.model.SocialNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the notification center — the list behind the Social bell. Mutations
 * are optimistic; on a write failure the list reloads so local state can't
 * drift from the server. Every successful mutation bumps the tab/bell badge.
 */
@HiltViewModel
class NotificationCenterViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val socialBadgeRefreshBus: SocialBadgeRefreshBus,
) : ViewModel() {

    data class UiState(
        val items: List<SocialNotification> = emptyList(),
        val unread: Int = 0,
        val filter: NotificationCategory = NotificationCategory.All,
        val isLoading: Boolean = true,
        val error: String? = null,
    ) {
        /** Items after the active filter pill — `All` shows everything. */
        val visible: List<SocialNotification>
            get() = if (filter == NotificationCategory.All) items
            else items.filter { it.category == filter }
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { socialRepository.getNotifications() }
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(items = list.items, unread = list.unread, isLoading = false)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Couldn't load notifications")
                    }
                }
        }
    }

    fun setFilter(category: NotificationCategory) {
        _uiState.update { it.copy(filter = category) }
    }

    fun markRead(id: String) {
        _uiState.update { s ->
            val items = s.items.map { if (it.id == id) it.copy(read = true) else it }
            s.copy(items = items, unread = items.count { !it.read })
        }
        commit { socialRepository.markNotificationRead(id) }
    }

    fun markAllRead() {
        _uiState.update { s ->
            s.copy(items = s.items.map { it.copy(read = true) }, unread = 0)
        }
        commit { socialRepository.markAllNotificationsRead() }
    }

    fun dismiss(id: String) {
        // `confirmValueChange` can fire more than once per swipe gesture —
        // ignore a repeat so the DELETE request is sent exactly once.
        if (_uiState.value.items.none { it.id == id }) return
        _uiState.update { s ->
            val items = s.items.filterNot { it.id == id }
            s.copy(items = items, unread = items.count { !it.read })
        }
        commit { socialRepository.dismissNotification(id) }
    }

    fun clearAll() {
        _uiState.update { it.copy(items = emptyList(), unread = 0) }
        commit { socialRepository.dismissAllNotifications() }
    }

    /**
     * The optimistic mutation has already been applied to [uiState]; run the
     * server write. On success bump the badge; on failure reload from the
     * server so the optimistic change can't silently drift.
     */
    private fun commit(write: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { write() }
                .onSuccess { socialBadgeRefreshBus.bump() }
                .onFailure { load() }
        }
    }
}
