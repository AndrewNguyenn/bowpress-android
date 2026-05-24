package com.andrewnguyen.bowpress.feature.social.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.data.sync.SocialBadgeRefreshBus
import com.andrewnguyen.bowpress.core.model.ActivityItem
import com.andrewnguyen.bowpress.core.model.Club
import com.andrewnguyen.bowpress.core.model.Friendship
import com.andrewnguyen.bowpress.core.model.League
import com.andrewnguyen.bowpress.core.model.LeagueStatus
import com.andrewnguyen.bowpress.core.model.SocialProfile
import com.andrewnguyen.bowpress.core.model.ToggleLikeResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

data class FeedUiState(
    val feed: List<ActivityItem> = emptyList(),
    val friends: List<Friendship> = emptyList(),
    val pendingRequests: List<Friendship> = emptyList(),
    val clubs: List<Club> = emptyList(),
    val leagues: List<League> = emptyList(),
    val myProfile: SocialProfile? = null,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val nextCursor: String? = null,
    val error: String? = null,
) {
    /**
     * True when an active league's deadline is within the urgent window —
     * drives the maple tint on the Social-landing League nav card.
     */
    val leagueDeadlineNear: Boolean
        get() = leagues.any { it.status == LeagueStatus.active && it.deadlineWithin(URGENT_WINDOW) }

    /**
     * Which empty-state variant to show when the feed is empty and loading is
     * complete. Null when there are items in the feed or when loading is still
     * in progress.
     *
     *  - [FeedEmptyVariant.NewUser] — no friends AND no clubs AND no leagues:
     *    the archer is brand-new; show welcoming CTAs.
     *  - [FeedEmptyVariant.QuietWeek] — connected but the quiet period has
     *    nothing yet; show a softer "quiet week" notice.
     */
    val emptyVariant: FeedEmptyVariant?
        get() {
            if (isLoading || feed.isNotEmpty()) return null
            return if (friends.isEmpty() && clubs.isEmpty() && leagues.isEmpty()) {
                FeedEmptyVariant.NewUser
            } else {
                FeedEmptyVariant.QuietWeek
            }
        }

    private companion object {
        val URGENT_WINDOW: Duration = Duration.ofDays(3)
    }
}

/** Which flavour of empty-feed placeholder to render. */
enum class FeedEmptyVariant { NewUser, QuietWeek }

/** Whether this league's end date falls inside [window] from now (and hasn't passed). */
fun League.deadlineWithin(window: Duration): Boolean {
    val now = Instant.now()
    val endsAt = schedule.endsAt
    return endsAt.isAfter(now) && Duration.between(now, endsAt) <= window
}

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val socialBadgeRefreshBus: SocialBadgeRefreshBus,
) : ViewModel() {

    /**
     * Photo loader for feed-row photo previews — fetches the Bearer-gated
     * display JPEG through the repository. Stable across recompositions.
     */
    val photoLoader = com.andrewnguyen.bowpress.feature.social.ui.session.SessionPhotoLoader {
            sharedSessionId, photoId ->
        socialRepository.fetchSharedSessionPhotoBytes(sharedSessionId, photoId)
    }

    private val _error = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _isLoadingMore = MutableStateFlow(false)
    private val _nextCursor = MutableStateFlow<String?>(null)
    private val _myProfile = MutableStateFlow<SocialProfile?>(null)

    // The five repository flows carry the list data; the local MutableStateFlows
    // must ALSO be combine inputs — `combine` only re-emits when a source flow
    // changes, so reading `.value` inside the lambda would leave those fields
    // stuck on their initial values.
    //
    // Repo flows are pre-combined into one FeedUiState stub to stay within
    // `combine`'s 5-flow typed-arity limit. The two pagination flows
    // (_isLoadingMore, _nextCursor) are pre-combined into a Pair for the same
    // reason; the outer combine then merges four inputs.
    private val lists: Flow<FeedUiState> = combine(
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
        )
    }

    private val paginationState: Flow<Pair<Boolean, String?>> = combine(
        _isLoadingMore,
        _nextCursor,
    ) { loadingMore, cursor -> loadingMore to cursor }

    val uiState: StateFlow<FeedUiState> = combine(
        lists,
        _myProfile,
        _isLoading,
        _error,
        paginationState,
    ) { base, profile, loading, error, pagination ->
        base.copy(
            myProfile = profile,
            isLoading = loading,
            isLoadingMore = pagination.first,
            nextCursor = pagination.second,
            error = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FeedUiState(isLoading = true),
    )

    /**
     * Drives the notification-bell badge in the feed top-nav — the same
     * pending-count total the Social *tab* badge shows.
     *
     * This re-fetches independently rather than reading AppStateViewModel's
     * `socialPendingCount` on purpose: AppStateViewModel lives in the `app`
     * module, which `feature-social` cannot depend on. SocialBadgeRefreshBus
     * is the decoupling mechanism — both owners re-fetch on the same ping, so
     * the bell and the tab badge stay consistent without a shared owner.
     */
    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    init {
        refresh()
        refreshPendingCount()
        // A notification mutation (mark-read / dismiss / clear) bumps the bus;
        // re-fetch so the bell badge stays in sync after the user acts.
        socialBadgeRefreshBus.events
            .onEach { refreshPendingCount() }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        _nextCursor.value = null
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            runCatching {
                val firstPage = socialRepository.refreshFeed()
                _nextCursor.value = firstPage.nextCursor
                socialRepository.refreshFriends()
                socialRepository.refreshClubs()
                socialRepository.refreshLeagues()
                _myProfile.value = socialRepository.getMyProfile()
            }.onFailure { e ->
                _error.value = e.message
            }
            _isLoading.value = false
        }
        refreshPendingCount()
    }

    /**
     * Load the next page of the activity feed using the current [_nextCursor].
     * No-ops if there is no cursor (no more pages) or a load is already in
     * flight. New items appear automatically via the Room `observeAll()` flow.
     */
    fun loadMore() {
        val cursor = _nextCursor.value ?: return
        if (_isLoadingMore.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            runCatching {
                val page = socialRepository.loadMoreFeed(cursor)
                _nextCursor.value = page.nextCursor
            }.onFailure { /* silently ignore, user can scroll again */ }
            _isLoadingMore.value = false
        }
    }

    private fun refreshPendingCount() {
        viewModelScope.launch {
            runCatching { socialRepository.getPendingCount() }
                .onSuccess { _pendingCount.value = it.total }
        }
    }

    fun dismissError() {
        _error.value = null
    }

    /**
     * Toggle the caller's like on a feed subject (Social Feed V2 §5). Returns
     * the server-authoritative `{ likeCount, likedByMe }` so the row's
     * optimistic state can reconcile; the repository also patches the Room
     * feed cache for every row sharing the subject.
     */
    suspend fun toggleLike(subjectId: String, currentlyLiked: Boolean): ToggleLikeResponse =
        socialRepository.toggleLike(subjectId, currentlyLiked)
}
