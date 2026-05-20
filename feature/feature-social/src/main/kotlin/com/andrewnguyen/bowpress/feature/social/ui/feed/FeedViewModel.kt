package com.andrewnguyen.bowpress.feature.social.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.model.ActivityItem
import com.andrewnguyen.bowpress.core.model.Club
import com.andrewnguyen.bowpress.core.model.Friendship
import com.andrewnguyen.bowpress.core.model.League
import com.andrewnguyen.bowpress.core.model.SocialProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedUiState(
    val feed: List<ActivityItem> = emptyList(),
    val friends: List<Friendship> = emptyList(),
    val pendingRequests: List<Friendship> = emptyList(),
    val clubs: List<Club> = emptyList(),
    val leagues: List<League> = emptyList(),
    val myProfile: SocialProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _myProfile = MutableStateFlow<SocialProfile?>(null)

    val uiState: StateFlow<FeedUiState> = combine(
        socialRepository.observeFeed(),
        socialRepository.observeFriends(),
        socialRepository.observePendingRequests(),
        socialRepository.observeClubs(),
        socialRepository.observeLeagues(),
    ) { feed, friends, pending, clubs, leagues ->
        FeedUiState(
            feed = feed,
            friends = friends,
            pendingRequests = pending,
            clubs = clubs,
            leagues = leagues,
            myProfile = _myProfile.value,
            isLoading = _isLoading.value,
            error = _error.value,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FeedUiState(isLoading = true),
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            runCatching {
                socialRepository.refreshFeed()
                socialRepository.refreshFriends()
                socialRepository.refreshClubs()
                socialRepository.refreshLeagues()
                _myProfile.value = socialRepository.getMyProfile()
            }.onFailure { e ->
                _error.value = e.message
            }
            _isLoading.value = false
        }
    }

    fun dismissError() {
        _error.value = null
    }
}
