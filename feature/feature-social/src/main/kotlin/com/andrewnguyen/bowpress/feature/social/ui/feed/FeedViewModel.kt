package com.andrewnguyen.bowpress.feature.social.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.data.sync.SocialBadgeRefreshBus
import com.andrewnguyen.bowpress.core.model.ActivityItem
import com.andrewnguyen.bowpress.core.model.Club
import com.andrewnguyen.bowpress.core.model.Friendship
import com.andrewnguyen.bowpress.core.model.FriendshipDirection
import com.andrewnguyen.bowpress.core.model.FriendshipStatus
import com.andrewnguyen.bowpress.core.model.InvitationKind
import com.andrewnguyen.bowpress.core.model.InvitationStatus
import com.andrewnguyen.bowpress.core.model.League
import com.andrewnguyen.bowpress.core.model.LeagueStatus
import com.andrewnguyen.bowpress.core.model.SocialInvitation
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
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

data class FeedUiState(
    val feed: List<ActivityItem> = emptyList(),
    val friends: List<Friendship> = emptyList(),
    val pendingRequests: List<Friendship> = emptyList(),
    val clubs: List<Club> = emptyList(),
    val leagues: List<League> = emptyList(),
    // iOS parity (A2) — F/C/L header pills carry `actionables only`
    // badges (pending incoming friend requests + pending club/league
    // invites). Holding the live `SocialInvitation` rows here lets us
    // derive both pending counts (badge) and total counts (subline) from
    // a single source.
    val pendingInvitations: List<SocialInvitation> = emptyList(),
    val myProfile: SocialProfile? = null,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val nextCursor: String? = null,
    val error: String? = null,
) {
    /**
     * iOS parity (A2) — number of pending INCOMING friend requests. The
     * outgoing direction never lights up the badge (an archer doesn't
     * need to be reminded that they sent a request).
     */
    val pendingIncomingFriendRequests: Int
        get() = pendingRequests.count {
            it.status == FriendshipStatus.pending && it.direction == FriendshipDirection.incoming
        }

    /** iOS parity (A2) — number of pending club invitations awaiting decision. */
    val pendingClubInvites: Int
        get() = pendingInvitations.count {
            it.kind == InvitationKind.club && it.status == InvitationStatus.pending
        }

    /** iOS parity (A2) — number of pending league invitations awaiting decision. */
    val pendingLeagueInvites: Int
        get() = pendingInvitations.count {
            it.kind == InvitationKind.league && it.status == InvitationStatus.pending
        }

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

    /**
     * Generation counter that guards [loadMore] against a concurrent [refresh].
     * Incremented synchronously at the top of every [refresh] (before any
     * suspending call); [loadMore] captures the value before its fetch and
     * discards the resulting page if the generation has advanced — that
     * prevents a stale page-2 from being appended onto a freshly-reloaded
     * page-1 (duplicate rows + stale `nextCursor`). Mirrors iOS
     * `SocialTabView.loadGeneration` (commit 0fac113).
     */
    private val loadGeneration = AtomicInteger(0)

    /**
     * The cursor whose [loadMore] is in flight (or has just completed) — used
     * to suppress a duplicate fetch when the same sentinel re-fires before the
     * cursor advances. Cleared when the generation counter discards a stale
     * page so the new generation can re-trigger pagination cleanly.
     */
    private var lastLoadedCursor: String? = null

    // The five repository flows carry the list data; the local MutableStateFlows
    // must ALSO be combine inputs — `combine` only re-emits when a source flow
    // changes, so reading `.value` inside the lambda would leave those fields
    // stuck on their initial values.
    //
    // Repo flows are pre-combined into one FeedUiState stub to stay within
    // `combine`'s 5-flow typed-arity limit. The two pagination flows
    // (_isLoadingMore, _nextCursor) are pre-combined into a Pair for the same
    // reason; the outer combine then merges four inputs.
    //
    // iOS parity (A2) — `observeInvitations()` joins the same pre-combine to
    // drive the Clubs / Leagues pill badges. We pre-pair friends + invitations
    // into one input so the 5-flow typed limit still holds.
    private val lists: Flow<FeedUiState> = combine(
        socialRepository.observeFeed(),
        combine(
            socialRepository.observeFriends(),
            socialRepository.observeInvitations(),
        ) { friends, invitations -> friends to invitations },
        socialRepository.observePendingRequests(),
        socialRepository.observeClubs(),
        socialRepository.observeLeagues(),
    ) { feed, friendsAndInvites, pending, clubs, leagues ->
        FeedUiState(
            feed = feed,
            friends = friendsAndInvites.first,
            pendingInvitations = friendsAndInvites.second,
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
        // Bump the generation BEFORE any suspending call so any [loadMore]
        // currently awaiting a fetch discards its stale page rather than
        // appending onto the refreshed page-1.
        loadGeneration.incrementAndGet()
        lastLoadedCursor = null
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
                // iOS parity (A2) — invitations drive the Clubs / Leagues
                // pill badges; refresh them on every feed load so the
                // header reflects the same window as the activity list.
                socialRepository.getInvitations()
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
     * No-ops if there is no cursor (no more pages), a load is already in
     * flight, or the same cursor was just loaded (sentinel re-fire). New items
     * appear automatically via the Room `observeAll()` flow.
     *
     * Guarded by [loadGeneration] against a concurrent [refresh]: if the
     * generation advances during the fetch, the page is dropped and
     * [lastLoadedCursor] is cleared so the new generation can re-trigger
     * pagination once the refreshed `nextCursor` lands. Mirrors iOS
     * `SocialTabView.loadMore` (commit 0fac113).
     */
    fun loadMore() {
        val cursor = _nextCursor.value ?: return
        if (_isLoadingMore.value) return
        if (cursor == lastLoadedCursor) return
        lastLoadedCursor = cursor
        val genAtStart = loadGeneration.get()
        viewModelScope.launch {
            _isLoadingMore.value = true
            runCatching {
                val page = socialRepository.loadMoreFeed(cursor)
                // [refresh] fired during the fetch — discard this stale page
                // and reset [lastLoadedCursor] so pagination can re-trigger
                // with the new generation's cursor.
                if (loadGeneration.get() != genAtStart) {
                    lastLoadedCursor = null
                    return@runCatching
                }
                _nextCursor.value = page.nextCursor
            }.onFailure {
                // Same-generation failure — clear the cursor guard so the user
                // can retry by scrolling again. (A cross-generation failure
                // also benefits from this reset.)
                lastLoadedCursor = null
            }
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
