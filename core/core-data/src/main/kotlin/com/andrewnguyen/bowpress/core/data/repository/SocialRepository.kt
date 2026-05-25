package com.andrewnguyen.bowpress.core.data.repository

import com.andrewnguyen.bowpress.core.data.converters.toDto
import com.andrewnguyen.bowpress.core.data.converters.toEntity
import com.andrewnguyen.bowpress.core.data.seed.DevMockData
import com.andrewnguyen.bowpress.core.data.social.SessionPhotoCache
import com.andrewnguyen.bowpress.core.database.dao.AchievementDao
import com.andrewnguyen.bowpress.core.database.dao.ActivityFeedDao
import com.andrewnguyen.bowpress.core.database.dao.ArrowPlotDao
import com.andrewnguyen.bowpress.core.database.dao.BlockDao
import com.andrewnguyen.bowpress.core.database.dao.ClubDao
import com.andrewnguyen.bowpress.core.database.dao.FriendshipDao
import com.andrewnguyen.bowpress.core.database.dao.InvitationDao
import com.andrewnguyen.bowpress.core.database.dao.LeagueDao
import com.andrewnguyen.bowpress.core.database.dao.SessionDao
import com.andrewnguyen.bowpress.core.database.dao.SessionEndDao
import com.andrewnguyen.bowpress.core.database.dao.SocialProfileDao
import com.andrewnguyen.bowpress.core.model.AcceptInvitationBody
import com.andrewnguyen.bowpress.core.model.Achievement
import com.andrewnguyen.bowpress.core.model.ActivityActor
import com.andrewnguyen.bowpress.core.model.ActivityComment
import com.andrewnguyen.bowpress.core.model.ActivityItem
import com.andrewnguyen.bowpress.core.model.FeedPage
import com.andrewnguyen.bowpress.core.model.CommentSort
import com.andrewnguyen.bowpress.core.model.AdminMatrix
import com.andrewnguyen.bowpress.core.model.BlockKind
import com.andrewnguyen.bowpress.core.model.BlockMode
import com.andrewnguyen.bowpress.core.model.AttachmentKind
import com.andrewnguyen.bowpress.core.model.Club
import com.andrewnguyen.bowpress.core.model.ClubAnnouncement
import com.andrewnguyen.bowpress.core.model.ClubFeedItem
import com.andrewnguyen.bowpress.core.model.ClubMember
import com.andrewnguyen.bowpress.core.model.CompareView
import com.andrewnguyen.bowpress.core.model.CreateAnnouncementBody
import com.andrewnguyen.bowpress.core.model.CreateAttachmentBody
import com.andrewnguyen.bowpress.core.model.CreateBlockBody
import com.andrewnguyen.bowpress.core.model.CreateClubBody
import com.andrewnguyen.bowpress.core.model.CreateLeagueBody
import com.andrewnguyen.bowpress.core.model.FriendProfile
import com.andrewnguyen.bowpress.core.model.Friendship
import com.andrewnguyen.bowpress.core.model.FriendshipSource
import com.andrewnguyen.bowpress.core.model.HandleSuggestion
import com.andrewnguyen.bowpress.core.model.JoinClubBody
import com.andrewnguyen.bowpress.core.model.JoinLeagueBody
import com.andrewnguyen.bowpress.core.model.League
import com.andrewnguyen.bowpress.core.model.LeaderboardRow
import com.andrewnguyen.bowpress.core.model.LeagueAttachment
import com.andrewnguyen.bowpress.core.model.LeagueStandingRow
import com.andrewnguyen.bowpress.core.model.LeagueSubmission
import com.andrewnguyen.bowpress.core.model.InvitationStatus
import com.andrewnguyen.bowpress.core.model.NotificationList
import com.andrewnguyen.bowpress.core.model.SendFriendRequestBody
import com.andrewnguyen.bowpress.core.model.SendInvitationBody
import com.andrewnguyen.bowpress.core.model.SessionLocation
import com.andrewnguyen.bowpress.core.model.ShareSessionBody
import com.andrewnguyen.bowpress.core.model.ShareSessionResult
import com.andrewnguyen.bowpress.core.model.SharedSession
import com.andrewnguyen.bowpress.core.model.SharedSessionDetail
import com.andrewnguyen.bowpress.core.model.SharedSessionPhoto
import com.andrewnguyen.bowpress.core.model.SocialBlock
import com.andrewnguyen.bowpress.core.model.SocialInvitation
import com.andrewnguyen.bowpress.core.model.SocialPendingCount
import com.andrewnguyen.bowpress.core.model.SocialProfile
import com.andrewnguyen.bowpress.core.model.SocialVisibility
import com.andrewnguyen.bowpress.core.model.PostCommentBody
import com.andrewnguyen.bowpress.core.model.SubmitScoreBody
import com.andrewnguyen.bowpress.core.model.ToggleLikeResponse
import com.andrewnguyen.bowpress.core.model.UpdateAnnouncementBody
import com.andrewnguyen.bowpress.core.model.TrophyDef
import com.andrewnguyen.bowpress.core.model.UpdateClubBody
import com.andrewnguyen.bowpress.core.model.UpdateLeagueBody
import com.andrewnguyen.bowpress.core.model.UpdateSocialProfileRequest
import com.andrewnguyen.bowpress.core.model.Division
import com.andrewnguyen.bowpress.core.network.BowPressApi
import android.content.Context
import android.content.pm.ApplicationInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Social repository — **online-first for v1**.
 *
 * Reads are served from Room as Flows so the UI stays reactive and survives
 * process death, but Room is a *read cache*, not the source of truth: every
 * write goes to the API first and only updates Room on success. There is no
 * offline write queue. The `pendingSync` columns on the social entities are
 * reserved for a future offline-write iteration and are unused today.
 *
 * `refresh*` methods pull from the API and upsert into Room; `observe*`
 * methods expose the cached rows.
 */
@Singleton
class SocialRepository @Inject constructor(
    private val api: BowPressApi,
    private val profileDao: SocialProfileDao,
    private val friendshipDao: FriendshipDao,
    private val clubDao: ClubDao,
    private val feedDao: ActivityFeedDao,
    private val leagueDao: LeagueDao,
    private val invitationDao: InvitationDao,
    private val blockDao: BlockDao,
    private val achievementDao: AchievementDao,
    private val sessionDao: SessionDao,
    private val sessionEndDao: SessionEndDao,
    private val plotDao: ArrowPlotDao,
    private val photoCache: SessionPhotoCache,
    @ApplicationContext private val context: Context,
) {

    private val isDebugBuild: Boolean
        get() = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    // ── Profile ───────────────────────────────────────────────────────────────

    fun observeMyProfile(userId: String): Flow<SocialProfile?> =
        profileDao.observe(userId).map { it?.toDto() }

    /**
     * The signed-in user's social profile as a reactive stream, id-free —
     * only one profile is ever cached locally, so a seeded (DEBUG) or
     * previously-fetched profile surfaces immediately even before/without a
     * successful remote refresh.
     */
    fun observeMyProfile(): Flow<SocialProfile?> =
        profileDao.observeAny().map { it?.toDto() }

    /**
     * Best-effort fetch of the current user's profile. Tries the API and
     * caches the result; on failure (offline, or a DEBUG build whose token
     * isn't routable) falls back to the locally cached/seeded profile so
     * callers get data instead of an exception.
     */
    suspend fun getMyProfile(): SocialProfile {
        return runCatching {
            val remote = api.getSocialProfile()
            profileDao.upsert(remote.toEntity())
            remote
        }.getOrElse { err ->
            profileDao.findAny()?.toDto() ?: throw err
        }
    }

    suspend fun updateMyProfile(
        handle: String? = null,
        displayName: String? = null,
        visibility: SocialVisibility? = null,
    ): SocialProfile {
        val updated = api.updateSocialProfile(
            UpdateSocialProfileRequest(handle, displayName, visibility),
        )
        profileDao.upsert(updated.toEntity())
        return updated
    }

    suspend fun searchArcher(handle: String): SocialProfile =
        api.getArcherByHandle(handle)

    /**
     * Handle search for the `@`-mention autocomplete (mentions contract §3.1).
     * [query] is the in-progress `@token` prefix (a leading `@` is tolerated —
     * the server trims it). The query is lowercased — handles are lowercase
     * per the contract — so the method is self-consistent whatever the caller
     * passes. Returns up to 8 suggestions, friends-first; an empty/blank query
     * short-circuits to an empty list rather than hitting the API (the
     * contract requires `q` length ≥ 1).
     */
    suspend fun searchHandles(query: String): List<HandleSuggestion> {
        val q = query.removePrefix("@").trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return api.searchHandles(q)
    }

    /**
     * Resolve a bare handle to its archer profile (mentions contract §3.2) —
     * used to turn a tapped `@handle` mention span into a profile screen
     * navigation. Returns null when the handle resolves to no real archer
     * (an unresolved mention is a no-op tap).
     */
    suspend fun resolveHandle(handle: String): SocialProfile? =
        runCatching { api.getArcherByHandle(handle.removePrefix("@").trim()) }.getOrNull()

    // ── Friendships ───────────────────────────────────────────────────────────

    fun observeFriends(): Flow<List<Friendship>> =
        friendshipDao.observeFriends().map { rows -> rows.map { it.toDto() } }

    fun observePendingRequests(): Flow<List<Friendship>> =
        friendshipDao.observePendingRequests().map { rows -> rows.map { it.toDto() } }

    suspend fun refreshFriends() {
        val friends = api.getFriends()
        val requests = api.getFriendRequests()
        friendshipDao.upsertAll(friends.map { it.toEntity() })
        friendshipDao.upsertAll(requests.map { it.toEntity() })
    }

    suspend fun sendFriendRequest(handle: String, source: FriendshipSource? = null): Friendship {
        val result = api.sendFriendRequest(SendFriendRequestBody(handle, source))
        friendshipDao.upsert(result.toEntity())
        return result
    }

    suspend fun acceptFriendRequest(id: String): Friendship {
        val result = api.acceptFriendRequest(id)
        friendshipDao.upsert(result.toEntity())
        return result
    }

    suspend fun declineOrCancelRequest(id: String) {
        api.deleteFriendRequest(id)
        friendshipDao.deleteById(id)
    }

    suspend fun unfriend(otherUserId: String) {
        api.unfriend(otherUserId)
        friendshipDao.deleteByOtherUserId(otherUserId)
    }

    suspend fun getFriendProfile(otherUserId: String): FriendProfile =
        api.getFriendProfile(otherUserId)

    suspend fun getCompareView(
        otherUserId: String,
        distance: String? = null,
        face: String? = null,
    ): CompareView = api.getCompareView(otherUserId, distance, face)

    // ── Clubs ─────────────────────────────────────────────────────────────────

    fun observeClubs(): Flow<List<Club>> =
        clubDao.observeAll().map { rows -> rows.map { it.toDto() } }

    suspend fun refreshClubs() {
        val remote = api.getClubs()
        clubDao.upsertAll(remote.map { it.toEntity() })
    }

    suspend fun getClub(id: String): Club {
        val remote = api.getClub(id)
        clubDao.upsert(remote.toEntity())
        return remote
    }

    suspend fun createClub(name: String, description: String? = null): Club {
        val result = api.createClub(CreateClubBody(name, description))
        clubDao.upsert(result.toEntity())
        return result
    }

    suspend fun updateClub(id: String, name: String? = null, description: String? = null, notes: String? = null): Club {
        val result = api.updateClub(id, UpdateClubBody(name, description, notes))
        clubDao.upsert(result.toEntity())
        return result
    }

    suspend fun deleteClub(id: String) {
        api.deleteClub(id)
        clubDao.deleteById(id)
    }

    suspend fun joinClub(inviteCode: String): Club {
        val result = api.joinClub(JoinClubBody(inviteCode))
        clubDao.upsert(result.toEntity())
        return result
    }

    suspend fun leaveClub(id: String) {
        api.leaveClub(id)
        clubDao.deleteById(id)
    }

    suspend fun getClubMembers(id: String): List<ClubMember> =
        api.getClubMembers(id)

    suspend fun getClubFeed(id: String): List<ClubFeedItem> =
        api.getClubFeed(id)

    suspend fun getClubLeaderboard(id: String, scope: String = "30d"): List<LeaderboardRow> =
        api.getClubLeaderboard(id, scope)

    // ── Leagues ───────────────────────────────────────────────────────────────

    fun observeLeagues(): Flow<List<League>> =
        leagueDao.observeAll().map { rows -> rows.map { it.toDto() } }

    suspend fun refreshLeagues() {
        val remote = api.getLeagues()
        leagueDao.upsertAll(remote.map { it.toEntity() })
    }

    suspend fun getLeague(id: String): League {
        val remote = api.getLeague(id)
        leagueDao.upsert(remote.toEntity())
        return remote
    }

    suspend fun createLeague(body: CreateLeagueBody): League {
        val result = api.createLeague(body)
        leagueDao.upsert(result.toEntity())
        return result
    }

    suspend fun updateLeague(id: String, body: UpdateLeagueBody): League {
        val result = api.updateLeague(id, body)
        leagueDao.upsert(result.toEntity())
        return result
    }

    suspend fun joinLeagueByCode(inviteCode: String, division: Division): League {
        val result = api.joinLeagueByCode(JoinLeagueBody(inviteCode, division))
        leagueDao.upsert(result.toEntity())
        return result
    }

    suspend fun joinLeague(id: String, division: Division): League {
        val result = api.joinLeague(id, JoinLeagueBody(division = division))
        leagueDao.upsert(result.toEntity())
        return result
    }

    suspend fun leaveLeague(id: String) {
        api.leaveLeague(id)
        leagueDao.deleteById(id)
    }

    suspend fun deleteLeague(id: String) {
        api.deleteLeague(id)
        leagueDao.deleteById(id)
    }

    suspend fun submitScore(id: String, body: SubmitScoreBody): LeagueSubmission =
        api.submitLeagueScore(id, body)

    suspend fun getLeagueSubmissions(id: String): List<LeagueSubmission> =
        api.getLeagueSubmissions(id)

    suspend fun getLeagueStandings(id: String): List<LeagueStandingRow> =
        api.getLeagueStandings(id)

    suspend fun getLeagueAdminMatrix(id: String): AdminMatrix =
        api.getLeagueAdminMatrix(id)

    // ── Activity Feed ─────────────────────────────────────────────────────────

    fun observeFeed(): Flow<List<ActivityItem>> =
        feedDao.observeAll().map { rows -> rows.map { it.toDto() } }

    suspend fun refreshFeed(): FeedPage {
        val remote = api.getActivityFeed()
        feedDao.clear()
        feedDao.upsertAll(remote.items.map { it.toEntity() })
        return remote
    }

    /**
     * Append the next page of the activity feed to the Room cache without
     * clearing it. Returns the [FeedPage] so the caller can advance the cursor.
     */
    suspend fun loadMoreFeed(cursor: String): FeedPage {
        val remote = api.getActivityFeed(cursor = cursor)
        feedDao.upsertAll(remote.items.map { it.toEntity() })  // append, don't clear
        return remote
    }

    // ── Invitations (§11) ──────────────────────────────────────────────────────
    //
    // Online-first like the rest: the API is the source of truth; successful
    // fetches upsert into the `invitations` Room cache so the Invites surfaces
    // render reactively and the DEBUG seed can populate the badge offline.

    fun observeInvitations(): Flow<List<SocialInvitation>> =
        invitationDao.observeAll().map { rows -> rows.map { it.toDto() } }

    /**
     * Fetch pending club/league invitations. On API success the cache is
     * replaced; on failure the last cached rows are returned (DEBUG / offline).
     */
    suspend fun getInvitations(): List<SocialInvitation> {
        return runCatching { api.getInvitations() }
            .onSuccess { remote ->
                invitationDao.clear()
                invitationDao.upsertAll(remote.map { it.toEntity() })
            }
            .getOrElse {
                invitationDao.getAll().map { it.toDto() }
            }
    }

    /**
     * Accept a club/league invitation. [division] is only relevant for league
     * invites; it defaults server-side to the league's first division. The
     * row is dropped from the cache on success.
     */
    suspend fun acceptInvitation(id: String, division: Division? = null): SocialInvitation {
        val result = api.acceptInvitation(id, AcceptInvitationBody(division))
        invitationDao.deleteById(id)
        return result
    }

    /** Decline (invitee) or revoke (inviter) an invitation. */
    suspend fun declineInvitation(id: String) {
        api.declineInvitation(id)
        invitationDao.deleteById(id)
    }

    suspend fun inviteToClub(clubId: String, handle: String): SocialInvitation =
        api.inviteToClub(clubId, SendInvitationBody(handle))

    suspend fun inviteToLeague(leagueId: String, handle: String): SocialInvitation =
        api.inviteToLeague(leagueId, SendInvitationBody(handle))

    // ── Pending count (§12) ─────────────────────────────────────────────────────

    /**
     * Fetch the Social-tab badge count (incoming friend requests + pending
     * invitations). API-first; on failure falls back to a count computed from
     * the Room cache so the badge still reflects seeded DEBUG data.
     */
    suspend fun getPendingCount(): SocialPendingCount {
        return runCatching { api.getPendingCount() }
            .getOrElse {
                val friendRequests = friendshipDao.incomingPendingCount()
                val invitations = invitationDao.pendingCount()
                // Offline: fold the seeded unread notifications into the
                // badge total in a DEBUG build, so the tab badge agrees with
                // the notification screen's "N new".
                val notifications = if (isDebugBuild) DevMockData.notificationList.unread else 0
                SocialPendingCount(
                    friendRequests = friendRequests,
                    invitations = invitations,
                    notifications = notifications,
                    total = friendRequests + invitations + notifications,
                )
            }
    }

    // ── Notification center (§13) ───────────────────────────────────────────────
    //
    // Live list — no Room cache; the notification screen always fetches fresh.
    // A DEBUG build falls back to a seeded list when the API is unreachable,
    // so the screen is populated offline like the rest of the social feature.

    /**
     * The caller's notifications (newest first) + the unread header count.
     * On failure in a DEBUG build, falls back to [DevMockData.notificationList]
     * so the notification center is populated offline — like the rest of the
     * social feature.
     */
    suspend fun getNotifications(): NotificationList =
        runCatching { api.getNotifications() }
            .getOrElse { e -> if (isDebugBuild) DevMockData.notificationList else throw e }

    /** Mark every notification read — clears the bell. */
    suspend fun markAllNotificationsRead() = api.markAllNotificationsRead()

    /** Mark one notification read (on tap-through). */
    suspend fun markNotificationRead(id: String) = api.markNotificationRead(id)

    /** Dismiss one notification — swipe-to-dismiss. */
    suspend fun dismissNotification(id: String) = api.dismissNotification(id)

    /** Dismiss every notification — the "clear all" action. */
    suspend fun dismissAllNotifications() = api.dismissAllNotifications()

    // ── Mute / block (§14) ──────────────────────────────────────────────────────
    //
    // Online-first like the rest: the API is the source of truth; successful
    // fetches replace the `blocks` Room cache so the "Muted & blocked" list
    // renders reactively and the DEBUG seed works offline.

    /** Reactive stream of the signed-in user's mutes + blocks from Room. */
    fun observeBlocks(): Flow<List<SocialBlock>> =
        blockDao.observeAll().map { rows -> rows.map { it.toDto() } }

    /**
     * Fetch all mutes/blocks. On API success the cache is replaced; on failure
     * the last cached rows are returned (DEBUG / offline).
     */
    suspend fun getBlocks(): List<SocialBlock> {
        return runCatching { api.getBlocks() }
            .onSuccess { remote ->
                blockDao.clear()
                blockDao.upsertAll(remote.map { it.toEntity() })
            }
            .getOrElse {
                blockDao.getAll().map { it.toDto() }
            }
    }

    /**
     * Mute or block [targetId]. Re-posting the same target updates the [mode]
     * server-side. The returned row is cached. Blocking an `archer` severs the
     * friendship server-side; the local friendship cache row is dropped to
     * match without waiting for a friends refresh.
     */
    suspend fun createBlock(kind: BlockKind, targetId: String, mode: BlockMode): SocialBlock {
        val result = api.createBlock(CreateBlockBody(kind, targetId, mode))
        blockDao.upsert(result.toEntity())
        if (kind == BlockKind.archer && mode == BlockMode.block) {
            friendshipDao.deleteByOtherUserId(targetId)
        }
        return result
    }

    /** Unmute / unblock — removes the block row both remotely and from the cache. */
    suspend fun deleteBlock(id: String) {
        api.deleteBlock(id)
        blockDao.deleteById(id)
    }

    // ── Trophy catalogue (§18) ────────────────────────────────────────────────────

    /**
     * The full 12-entry trophy catalogue — used by [TrophyCaseSection] to render
     * every trophy slot (earned or locked). Online-first: a successful fetch is
     * returned directly; on failure (offline / DEBUG fake token) falls back to
     * [DevMockData.trophyCatalog] so the collectible case renders in DEBUG.
     */
    suspend fun getTrophyCatalog(): List<TrophyDef> =
        runCatching { api.getTrophyCatalog() }
            .getOrElse { if (isDebugBuild) DevMockData.trophyCatalog else emptyList() }

    // ── Shared sessions & achievements (§15) ─────────────────────────────────────

    /**
     * Publish a saved session to the friend feed. Server-authoritative: it
     * records the session, runs the achievement engine, and writes a feed row.
     * Idempotent per `sessionId`. Callers fire this on session save — see
     * `SocialSessionSharer` in the app module.
     */
    suspend fun shareSession(body: ShareSessionBody): ShareSessionResult =
        api.shareSession(body)

    /**
     * The signed-in user's trophy case. Online-first: a successful fetch
     * replaces the cached rows for the user; on failure (offline / DEBUG
     * fake token) the cached/seeded rows are returned. The cache is keyed by
     * the user id carried on the achievement rows themselves.
     */
    suspend fun getMyAchievements(): List<Achievement> {
        return runCatching { api.getMyAchievements() }
            .onSuccess { remote -> cacheAchievements(remote) }
            .getOrElse {
                val myUserId = runCatching { getMyProfile().userId }.getOrNull()
                if (myUserId != null) {
                    achievementDao.getForUser(myUserId).map { it.toDto() }
                } else {
                    emptyList()
                }
            }
    }

    /** A friend's trophy case (visibility-gated server-side). Same caching. */
    suspend fun getFriendAchievements(otherUserId: String): List<Achievement> {
        return runCatching { api.getFriendAchievements(otherUserId) }
            .onSuccess { remote -> cacheAchievements(remote) }
            .getOrElse { achievementDao.getForUser(otherUserId).map { it.toDto() } }
    }

    /** Replace the cached achievement rows for every user id present in [remote]. */
    private suspend fun cacheAchievements(remote: List<Achievement>) {
        remote.map { it.userId }.toSet().forEach { achievementDao.clearForUser(it) }
        achievementDao.upsertAll(remote.map { it.toEntity() })
    }

    // ── Friend session detail (§16) ──────────────────────────────────────────────

    /**
     * A friend's full shared-session detail — scorecard ends + plotted arrows
     * for the target face. API-first; on failure (offline / DEBUG fake token)
     * it falls back to assembling the detail from the cached activity feed +
     * the local session/ends/arrows tables, so the screen still renders in
     * DEBUG. `session`/`ends`/`arrows` come back empty when the owner deleted
     * the underlying session.
     */
    suspend fun getSharedSessionDetail(sharedSessionId: String): SharedSessionDetail {
        return runCatching { api.getSharedSessionDetail(sharedSessionId) }
            .getOrElse {
                buildSharedSessionDetailFromCache(sharedSessionId)
                    ?: throw it
            }
    }

    // ── Likes & Comments (Social Feed V2 contract §5) ────────────────────────────

    /**
     * Toggle the caller's like on a feed subject. [currentlyLiked] is the
     * caller's current like state as the UI knows it: a `true` un-likes
     * (`DELETE`), a `false` likes (`POST`). The server endpoints are
     * idempotent, so a stale toggle is safe.
     *
     * On success the cached feed rows for [subjectId] are patched with the
     * server-authoritative `{ likeCount, likedByMe }` — a shared session has
     * one friend row + N club rows that the feed de-dupes by subject (§5.1),
     * so every row is updated. The fresh state is returned for the caller's
     * optimistic UI to reconcile against.
     */
    suspend fun toggleLike(subjectId: String, currentlyLiked: Boolean): ToggleLikeResponse {
        val result = if (currentlyLiked) {
            api.unlikeActivity(subjectId)
        } else {
            api.likeActivity(subjectId)
        }
        feedDao.updateLikeState(subjectId, result.likeCount, result.likedByMe)
        return result
    }

    /**
     * The comment thread for a feed subject (§6.3) — the **top-level** comments
     * in the requested [sort] order, each carrying its nested `replies`
     * (oldest→newest) and `replyCount`. Each comment is hydrated with its
     * author handle + display name and the caller's like state.
     * Visibility-gated server-side.
     *
     * The server is authoritative on ordering; this only normalises the reply
     * lists to oldest→newest defensively so the UI never has to re-sort them.
     */
    suspend fun getActivityComments(
        subjectId: String,
        sort: CommentSort = CommentSort.recent,
    ): List<ActivityComment> =
        api.getActivityComments(subjectId, sort.wire)
            .map { c -> c.copy(replies = c.replies.sortedBy { it.createdAt }) }

    /**
     * Post a comment (or, when [parentCommentId] is set, a reply) to a feed
     * subject. [body] is trimmed; the server enforces the 1–1000-char bound (a
     * blank/over-long body is rejected 400). On success the cached feed rows
     * for [subjectId] get their `commentCount` atomically incremented so the
     * comment button reflects the new total without a feed refresh.
     *
     * A reply also counts toward the subject's `commentCount` — the count is
     * the total thread size, top-level comments plus replies.
     */
    suspend fun postComment(
        subjectId: String,
        body: String,
        parentCommentId: String? = null,
    ): ActivityComment {
        val created = api.postActivityComment(
            subjectId,
            PostCommentBody(body.trim(), parentCommentId = parentCommentId?.takeIf { it.isNotBlank() }),
        )
        // Atomic +1 on every cached row for the subject — concurrent posts
        // cannot lose an increment (see ActivityFeedDao.adjustCommentCount).
        feedDao.adjustCommentCount(subjectId, +1)
        return created
    }

    /**
     * The full liker list for a feed subject (§6.4) — backs the tap-to-see-all
     * kudos sheet. Visibility-gated server-side.
     */
    suspend fun getActivityLikers(subjectId: String): List<ActivityActor> =
        api.getActivityLikers(subjectId)

    /**
     * Toggle the caller's like on a **comment** (§6.2). A comment is a likeable
     * subject — liking comment `c` reuses the generic
     * `POST/DELETE /social/activity/:subjectId/like` with the comment id as the
     * subject. [currentlyLiked] is the caller's current like state as the UI
     * knows it. Unlike [toggleLike], no feed-cache row is patched (a comment
     * never has an `activity_feed` row); the fresh `{ likeCount, likedByMe }`
     * is returned for the caller's optimistic UI to reconcile against.
     */
    suspend fun toggleCommentLike(commentId: String, currentlyLiked: Boolean): ToggleLikeResponse =
        if (currentlyLiked) {
            api.unlikeActivity(commentId)
        } else {
            api.likeActivity(commentId)
        }

    /**
     * Delete a comment. The server allows the comment's author **or** the
     * subject owner (moderation) — a caller who is neither gets a 403.
     * [canDelete] is the client-side permission gate computed by
     * [canDeleteComment]; the call short-circuits with an
     * [IllegalStateException] when it is false so a forbidden delete never
     * reaches the network.
     *
     * On success the cached `commentCount` for [subjectId] is atomically
     * decremented (clamped at 0).
     */
    suspend fun deleteComment(
        subjectId: String,
        commentId: String,
        canDelete: Boolean,
    ) {
        check(canDelete) { "Only the comment's author or the post owner can delete this comment." }
        api.deleteActivityComment(subjectId, commentId)
        // Atomic -1, clamped at 0 (see ActivityFeedDao.adjustCommentCount).
        feedDao.adjustCommentCount(subjectId, -1)
    }

    // ── Social Feed V2 — edit a shared session (contract §3) ─────────────────────

    /**
     * Owner-only edit of a shared session — set/clear the title and/or the
     * location tag. The server recomputes the activity headline across every
     * feed row carrying this `sharedSessionId` and renames the owner's
     * underlying session, so on success the local feed cache is refreshed.
     *
     * Contract §3 is a **partial** update: an omitted field is left unchanged,
     * an explicit JSON `null` clears it. The PATCH JSON object is built by hand
     * so each field is included only when it actually changed from the
     * [originalTitle] / [originalLocation] loaded with the session:
     *  - title unchanged   → key omitted (no spurious session rename, §3 effect 3)
     *  - title set blank   → `"title": null` (clears the custom headline)
     *  - title set         → `"title": "…"`
     *  - location unchanged→ key omitted
     *  - location cleared  → `"location": null`
     *  - location set      → `"location": { … }`
     *
     * When nothing changed the PATCH is skipped and the loaded summary is
     * returned. [newTitle] is trimmed; a blank trimmed title counts as a clear.
     */
    suspend fun editSharedSession(
        sharedSessionId: String,
        newTitle: String?,
        newDescription: String?,
        newLocation: SessionLocation?,
        originalTitle: String?,
        originalDescription: String?,
        originalLocation: SessionLocation?,
    ): SharedSession {
        // Normalise: a blank title is a clear (null); trim a real one.
        val normalizedNewTitle = newTitle?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedOriginalTitle = originalTitle?.trim()?.takeIf { it.isNotEmpty() }
        val titleChanged = normalizedNewTitle != normalizedOriginalTitle
        // Description (migration 0039) — same blank-is-a-clear normalisation.
        val normalizedNewDescription = newDescription?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedOriginalDescription = originalDescription?.trim()?.takeIf { it.isNotEmpty() }
        val descriptionChanged = normalizedNewDescription != normalizedOriginalDescription
        val locationChanged = newLocation != originalLocation

        if (!titleChanged && !descriptionChanged && !locationChanged) {
            // Nothing to send — return the current summary unchanged.
            return api.getSharedSessionDetail(sharedSessionId).sharedSession
        }

        val patch: JsonObject = buildJsonObject {
            if (titleChanged) {
                // null → explicit JSON null (clear); else the new value.
                if (normalizedNewTitle == null) {
                    put("title", JsonNull)
                } else {
                    put("title", normalizedNewTitle)
                }
            }
            if (descriptionChanged) {
                if (normalizedNewDescription == null) {
                    put("description", JsonNull)
                } else {
                    put("description", normalizedNewDescription)
                }
            }
            if (locationChanged) {
                if (newLocation == null) {
                    put("location", JsonNull)
                } else {
                    put("location", patchJson.encodeToJsonElement(SessionLocation.serializer(), newLocation))
                }
            }
        }

        val body = patchJson.encodeToString(JsonObject.serializer(), patch)
            .toRequestBody(JSON_MEDIA_TYPE)
        val result = api.patchSharedSession(sharedSessionId, body)
        // The server rewrote every activity row for this shared session —
        // refresh so the feed shows the new headline / location immediately.
        runCatching { refreshFeed() }
        return result.sharedSession
    }

    // ── Social Feed V2 — multi-photo gallery (contract §4) ───────────────────────

    /**
     * Upload one JPEG to a shared session's gallery. [jpegBytes] must already
     * be a downscaled display-ready JPEG (the caller bounds the long edge).
     * Returns the new `pending` photo record; its display image becomes
     * fetchable from [fetchSharedSessionPhotoBytes] once the server transcode
     * finishes.
     */
    suspend fun uploadSharedSessionPhoto(
        sharedSessionId: String,
        jpegBytes: ByteArray,
    ): SharedSessionPhoto {
        val body = jpegBytes.toRequestBody(JPEG_MEDIA_TYPE)
        return api.uploadSharedSessionPhoto(sharedSessionId, body)
    }

    /** All photos on a shared session, ordered by position. Visibility-gated. */
    suspend fun listSharedSessionPhotos(sharedSessionId: String): List<SharedSessionPhoto> =
        api.listSharedSessionPhotos(sharedSessionId).sortedBy { it.position }

    /**
     * Fetch the display-JPEG bytes for one photo through the authenticated
     * Retrofit stack (the photo endpoint is Bearer-gated, so the image cannot
     * be loaded by a plain URL fetch). Returns null when the photo is still
     * transcoding (202) or has failed / been removed (404) — the caller shows
     * a placeholder rather than an error.
     *
     * Bytes for a `ready` photo are immutable, so a successful fetch is cached
     * in the process-scoped [SessionPhotoCache]: the feed recycles rows as it
     * scrolls and would otherwise re-download the same photo on every recycle.
     */
    suspend fun fetchSharedSessionPhotoBytes(
        sharedSessionId: String,
        photoId: String,
    ): ByteArray? {
        photoCache.get(photoId)?.let { return it }
        return runCatching {
            val response = api.getSharedSessionPhoto(sharedSessionId, photoId)
            if (response.isSuccessful && response.code() == 200) {
                response.body()?.bytes()?.also { bytes ->
                    photoCache.put(photoId, bytes)
                }
            } else {
                // 202 still transcoding, or 404 failed/missing — don't cache.
                response.body()?.close()
                null
            }
        }.getOrNull()
    }

    /** Owner-only — remove a photo (both R2 objects + the record). */
    suspend fun deleteSharedSessionPhoto(sharedSessionId: String, photoId: String) {
        api.deleteSharedSessionPhoto(sharedSessionId, photoId)
    }

    /**
     * Owner-only — delete the shared session post. Server cascades the
     * fanout (every subscriber's feed row, the like/comment thread,
     * notifications, the gallery's R2 bytes). The owner's underlying
     * `shooting_sessions` row is kept; if the archer also wants to lose
     * the session-log entry, they delete from the Log tab instead (which
     * runs the same cascade).
     *
     * Bumps `refreshFeed` so the row drops out of the cached feed on the
     * next render — same pattern as [editSharedSession].
     */
    suspend fun deleteSharedSession(sharedSessionId: String) {
        api.deleteSharedSession(sharedSessionId)
        runCatching { refreshFeed() }
    }

    /**
     * Offline/DEBUG fallback for [getSharedSessionDetail]: locate the tapped
     * shared session in the cached activity feed (the §15 feed rows carry an
     * `ActivitySession` payload), then join the local session/ends/arrows
     * tables by its `sessionId`.
     */
    private suspend fun buildSharedSessionDetailFromCache(
        sharedSessionId: String,
    ): SharedSessionDetail? {
        val feedRow = feedDao.getAll()
            .map { it.toDto() }
            .firstOrNull { it.session?.sharedSessionId == sharedSessionId }
            ?: return null
        val activitySession = feedRow.session ?: return null

        val session = sessionDao.findById(activitySession.sessionId)?.toDto()
        val ends = if (session != null) {
            sessionEndDao.findBySession(activitySession.sessionId).map { it.toDto() }
        } else {
            emptyList()
        }
        val arrows = if (session != null) {
            plotDao.findBySession(activitySession.sessionId).map { it.toDto() }
        } else {
            emptyList()
        }

        return SharedSessionDetail(
            sharedSession = SharedSession(
                id = sharedSessionId,
                userId = feedRow.actorUserId,
                sessionId = activitySession.sessionId,
                score = activitySession.score,
                xCount = activitySession.xCount,
                arrowCount = activitySession.arrowCount,
                distance = activitySession.distance,
                face = activitySession.face,
                title = session?.title,
                shotAt = session?.startedAt ?: feedRow.createdAt,
                createdAt = feedRow.createdAt,
            ),
            ownerHandle = feedRow.actorHandle,
            ownerDisplayName = feedRow.actorDisplayName,
            session = session,
            ends = ends,
            arrows = arrows,
            // A 3D-course feed row carries its walked stations inline on the
            // ActivitySession — forward them so the friend-session detail can
            // draw the course map instead of a "session unavailable" notice.
            stations = activitySession.stations.orEmpty(),
        )
    }

    // ── Club announcement board (§17) ─────────────────────────────────────────

    /**
     * A club's announcement board — members read, pinned-first then newest.
     * On API failure (offline / DEBUG fake token) falls back to the seeded
     * DevMockData fixture so the board renders in DEBUG.
     */
    suspend fun getClubAnnouncements(clubId: String): List<ClubAnnouncement> =
        runCatching { api.getClubAnnouncements(clubId) }
            .getOrElse { if (isDebugBuild) DevMockData.clubAnnouncements[clubId].orEmpty() else emptyList() }
            .sortedWith(compareByDescending<ClubAnnouncement> { it.pinned }.thenByDescending { it.createdAt })

    /** Host-only: post a new announcement to a club's board. */
    suspend fun postClubAnnouncement(
        clubId: String,
        body: String,
        pinned: Boolean = false,
    ): ClubAnnouncement =
        api.postClubAnnouncement(clubId, CreateAnnouncementBody(body, pinned))

    /** Host-only: pin or unpin an announcement. */
    suspend fun setClubAnnouncementPinned(
        clubId: String,
        announcementId: String,
        pinned: Boolean,
    ): ClubAnnouncement =
        api.updateClubAnnouncement(clubId, announcementId, UpdateAnnouncementBody(pinned))

    /** Host-only: delete an announcement. */
    suspend fun deleteClubAnnouncement(clubId: String, announcementId: String) {
        api.deleteClubAnnouncement(clubId, announcementId)
    }

    // ── League attachments (§17) ──────────────────────────────────────────────

    /**
     * A league's attachments — host + entrants, newest first. On API failure
     * (offline / DEBUG fake token) falls back to the seeded DevMockData
     * fixture so the section renders in DEBUG.
     */
    suspend fun getLeagueAttachments(leagueId: String): List<LeagueAttachment> =
        runCatching { api.getLeagueAttachments(leagueId) }
            .getOrElse { if (isDebugBuild) DevMockData.leagueAttachments[leagueId].orEmpty() else emptyList() }
            .sortedByDescending { it.createdAt }

    /**
     * Host-only: add an attachment. A `link`/`file` kind requires a non-blank
     * [url]; a `note` requires [note]. Throws [IllegalArgumentException] on a
     * mismatch so the caller never posts an invalid attachment.
     */
    suspend fun addLeagueAttachment(
        leagueId: String,
        kind: AttachmentKind,
        title: String,
        url: String? = null,
        note: String? = null,
    ): LeagueAttachment {
        when (kind) {
            AttachmentKind.LINK, AttachmentKind.FILE ->
                require(!url.isNullOrBlank()) { "A $kind attachment needs a URL." }
            AttachmentKind.NOTE ->
                require(!note.isNullOrBlank()) { "A note attachment needs note text." }
        }
        return api.postLeagueAttachment(
            leagueId,
            CreateAttachmentBody(kind = kind, title = title, url = url, note = note),
        )
    }

    /** Host-only: remove an attachment. */
    suspend fun deleteLeagueAttachment(leagueId: String, attachmentId: String) {
        api.deleteLeagueAttachment(leagueId, attachmentId)
    }

    private companion object {
        /** Content type for the §4 photo upload — always a JPEG. */
        val JPEG_MEDIA_TYPE = "image/jpeg".toMediaType()

        /** Content type for the §3 hand-built PATCH body. */
        val JSON_MEDIA_TYPE = "application/json".toMediaType()

        /**
         * Json for the §3 PATCH body. `explicitNulls = true` (unlike the shared
         * network Json) so a `JsonNull` written for a cleared field actually
         * serializes as `"field": null` — the contract's "clear" signal.
         */
        val patchJson = Json { explicitNulls = true }
    }
}
