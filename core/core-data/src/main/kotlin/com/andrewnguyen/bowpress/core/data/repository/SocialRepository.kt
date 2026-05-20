package com.andrewnguyen.bowpress.core.data.repository

import com.andrewnguyen.bowpress.core.data.converters.toDto
import com.andrewnguyen.bowpress.core.data.converters.toEntity
import com.andrewnguyen.bowpress.core.database.dao.ActivityFeedDao
import com.andrewnguyen.bowpress.core.database.dao.ClubDao
import com.andrewnguyen.bowpress.core.database.dao.FriendshipDao
import com.andrewnguyen.bowpress.core.database.dao.InvitationDao
import com.andrewnguyen.bowpress.core.database.dao.LeagueDao
import com.andrewnguyen.bowpress.core.database.dao.SocialProfileDao
import com.andrewnguyen.bowpress.core.model.AcceptInvitationBody
import com.andrewnguyen.bowpress.core.model.ActivityItem
import com.andrewnguyen.bowpress.core.model.AdminMatrix
import com.andrewnguyen.bowpress.core.model.Club
import com.andrewnguyen.bowpress.core.model.ClubFeedItem
import com.andrewnguyen.bowpress.core.model.ClubMember
import com.andrewnguyen.bowpress.core.model.CompareView
import com.andrewnguyen.bowpress.core.model.CreateClubBody
import com.andrewnguyen.bowpress.core.model.CreateLeagueBody
import com.andrewnguyen.bowpress.core.model.FriendProfile
import com.andrewnguyen.bowpress.core.model.Friendship
import com.andrewnguyen.bowpress.core.model.FriendshipSource
import com.andrewnguyen.bowpress.core.model.JoinClubBody
import com.andrewnguyen.bowpress.core.model.JoinLeagueBody
import com.andrewnguyen.bowpress.core.model.League
import com.andrewnguyen.bowpress.core.model.LeaderboardRow
import com.andrewnguyen.bowpress.core.model.LeagueStandingRow
import com.andrewnguyen.bowpress.core.model.LeagueSubmission
import com.andrewnguyen.bowpress.core.model.InvitationStatus
import com.andrewnguyen.bowpress.core.model.SendFriendRequestBody
import com.andrewnguyen.bowpress.core.model.SendInvitationBody
import com.andrewnguyen.bowpress.core.model.SocialInvitation
import com.andrewnguyen.bowpress.core.model.SocialPendingCount
import com.andrewnguyen.bowpress.core.model.SocialProfile
import com.andrewnguyen.bowpress.core.model.SocialVisibility
import com.andrewnguyen.bowpress.core.model.SubmitScoreBody
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
}
