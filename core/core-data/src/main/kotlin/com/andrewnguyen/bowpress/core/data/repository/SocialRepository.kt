package com.andrewnguyen.bowpress.core.data.repository

import com.andrewnguyen.bowpress.core.data.converters.toDto
import com.andrewnguyen.bowpress.core.data.converters.toEntity
import com.andrewnguyen.bowpress.core.data.seed.DevMockData
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
import com.andrewnguyen.bowpress.core.model.ActivityItem
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
import com.andrewnguyen.bowpress.core.model.JoinClubBody
import com.andrewnguyen.bowpress.core.model.JoinLeagueBody
import com.andrewnguyen.bowpress.core.model.League
import com.andrewnguyen.bowpress.core.model.LeaderboardRow
import com.andrewnguyen.bowpress.core.model.LeagueAttachment
import com.andrewnguyen.bowpress.core.model.LeagueStandingRow
import com.andrewnguyen.bowpress.core.model.LeagueSubmission
import com.andrewnguyen.bowpress.core.model.InvitationStatus
import com.andrewnguyen.bowpress.core.model.SendFriendRequestBody
import com.andrewnguyen.bowpress.core.model.SendInvitationBody
import com.andrewnguyen.bowpress.core.model.ShareSessionBody
import com.andrewnguyen.bowpress.core.model.ShareSessionResult
import com.andrewnguyen.bowpress.core.model.SharedSession
import com.andrewnguyen.bowpress.core.model.SharedSessionDetail
import com.andrewnguyen.bowpress.core.model.SocialBlock
import com.andrewnguyen.bowpress.core.model.SocialInvitation
import com.andrewnguyen.bowpress.core.model.SocialPendingCount
import com.andrewnguyen.bowpress.core.model.SocialProfile
import com.andrewnguyen.bowpress.core.model.SocialVisibility
import com.andrewnguyen.bowpress.core.model.SubmitScoreBody
import com.andrewnguyen.bowpress.core.model.UpdateAnnouncementBody
import com.andrewnguyen.bowpress.core.model.UpdateClubBody
import com.andrewnguyen.bowpress.core.model.UpdateLeagueBody
import com.andrewnguyen.bowpress.core.model.UpdateSocialProfileRequest
import com.andrewnguyen.bowpress.core.model.Division
import com.andrewnguyen.bowpress.core.network.BowPressApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
) {

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

    suspend fun refreshFeed() {
        val remote = api.getActivityFeed()
        feedDao.clear()
        feedDao.upsertAll(remote.map { it.toEntity() })
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
                SocialPendingCount(
                    friendRequests = friendRequests,
                    invitations = invitations,
                    total = friendRequests + invitations,
                )
            }
    }

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
                userId = feedRow.actorHandle,
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
            .getOrElse { DevMockData.clubAnnouncements[clubId].orEmpty() }
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
            .getOrElse { DevMockData.leagueAttachments[leagueId].orEmpty() }
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
}
