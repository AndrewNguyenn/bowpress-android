package com.andrewnguyen.bowpress.core.network

import com.andrewnguyen.bowpress.core.model.AcceptInvitationBody
import com.andrewnguyen.bowpress.core.model.ActivityItem
import com.andrewnguyen.bowpress.core.model.AdminMatrix
import com.andrewnguyen.bowpress.core.model.AnalyticsOverview
import com.andrewnguyen.bowpress.core.model.AnalyticsSuggestion
import com.andrewnguyen.bowpress.core.model.ArrowConfiguration
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.Bow
import com.andrewnguyen.bowpress.core.model.BowConfiguration
import com.andrewnguyen.bowpress.core.model.Club
import com.andrewnguyen.bowpress.core.model.ClubFeedItem
import com.andrewnguyen.bowpress.core.model.ClubMember
import com.andrewnguyen.bowpress.core.model.CompareView
import com.andrewnguyen.bowpress.core.model.ConfigurationChange
import com.andrewnguyen.bowpress.core.model.CreateClubBody
import com.andrewnguyen.bowpress.core.model.CreateLeagueBody
import com.andrewnguyen.bowpress.core.model.DeviceToken
import com.andrewnguyen.bowpress.core.model.DriftResponse
import com.andrewnguyen.bowpress.core.model.Entitlement
import com.andrewnguyen.bowpress.core.model.FriendProfile
import com.andrewnguyen.bowpress.core.model.Friendship
import com.andrewnguyen.bowpress.core.model.JoinClubBody
import com.andrewnguyen.bowpress.core.model.JoinLeagueBody
import com.andrewnguyen.bowpress.core.model.League
import com.andrewnguyen.bowpress.core.model.LeaderboardRow
import com.andrewnguyen.bowpress.core.model.LeagueStandingRow
import com.andrewnguyen.bowpress.core.model.LeagueSubmission
import com.andrewnguyen.bowpress.core.model.PeriodComparison
import com.andrewnguyen.bowpress.core.model.SendFriendRequestBody
import com.andrewnguyen.bowpress.core.model.SendInvitationBody
import com.andrewnguyen.bowpress.core.model.SessionEnd
import com.andrewnguyen.bowpress.core.model.ShootingSession
import com.andrewnguyen.bowpress.core.model.SocialInvitation
import com.andrewnguyen.bowpress.core.model.SocialPendingCount
import com.andrewnguyen.bowpress.core.model.SocialProfile
import com.andrewnguyen.bowpress.core.model.SubmitScoreBody
import com.andrewnguyen.bowpress.core.model.TagCorrelation
import com.andrewnguyen.bowpress.core.model.TimelineResponse
import com.andrewnguyen.bowpress.core.model.TrendsResponse
import com.andrewnguyen.bowpress.core.model.UpdateClubBody
import com.andrewnguyen.bowpress.core.model.UpdateLeagueBody
import com.andrewnguyen.bowpress.core.model.UpdateSocialProfileRequest
import com.andrewnguyen.bowpress.core.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit surface of the BowPress Cloudflare Worker. Routes mirror
 * `bowpress-api/src/app.ts`:
 *   /auth  /me  /bows  /bow-configurations  /arrow-configs  /sessions
 *   /sessions/:sid/plots  /sessions/:sid/ends  /config-changes
 *   /bows/:bid/suggestions  /bows/:bid/tag-correlations
 *   /suggestions/:id/read  /suggestions/:id/dismiss
 *   /analytics/overview  /analytics/comparison
 *   /subscription  /device-tokens
 *
 * Every method is `suspend`. The Hilt-provided OkHttp stack attaches the Bearer token
 * (see `AuthInterceptor`) and converts non-2xx into `ApiException` (see `ErrorInterceptor`).
 */
interface BowPressApi {

    // ---- Auth -----------------------------------------------------------------

    @POST("auth/signup")
    suspend fun signUp(@Body body: SignUpRequest): Response<SignUpResponse>

    @POST("auth/signin")
    suspend fun signIn(@Body body: SignInRequest): AuthResponse

    @POST("auth/signin-apple")
    suspend fun signInWithApple(@Body body: AppleSignInRequest): AuthResponse

    @POST("auth/signin-google")
    suspend fun signInWithGoogle(@Body body: GoogleSignInRequest): AuthResponse

    @POST("auth/verify-email")
    suspend fun verifyEmail(@Body body: VerifyEmailRequest): AuthResponse

    @POST("auth/resend-verification")
    suspend fun resendVerification(@Body body: ResendVerificationRequest)

    @POST("auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest)

    // ---- Me / account ---------------------------------------------------------

    @GET("me")
    suspend fun getProfile(): User

    @PATCH("me")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): User

    @DELETE("me")
    suspend fun deleteAccount(): Unit

    /** Delete-account variant carrying a password (email-provider accounts). */
    @HTTP(method = "DELETE", path = "me", hasBody = true)
    suspend fun deleteAccountWithPassword(@Body body: DeleteAccountRequest): Unit

    // ---- Bows -----------------------------------------------------------------

    @GET("bows")
    suspend fun fetchBows(): List<Bow>

    @GET("bows/{id}")
    suspend fun fetchBow(@Path("id") id: String): Bow

    @POST("bows")
    suspend fun createBow(@Body body: Bow): Bow

    @PUT("bows/{id}")
    suspend fun updateBow(@Path("id") id: String, @Body body: Bow): Bow

    @DELETE("bows/{id}")
    suspend fun deleteBow(@Path("id") id: String)

    // ---- Bow configurations ---------------------------------------------------

    @GET("bow-configurations")
    suspend fun fetchBowConfigurations(@Query("bowId") bowId: String): List<BowConfiguration>

    @GET("bow-configurations/{id}")
    suspend fun fetchBowConfiguration(@Path("id") id: String): BowConfiguration

    @POST("bow-configurations")
    suspend fun createBowConfiguration(@Body body: BowConfiguration): BowConfiguration

    @PUT("bow-configurations/{id}")
    suspend fun updateBowConfiguration(
        @Path("id") id: String,
        @Body body: UpdateBowConfigRequest,
    ): BowConfiguration

    @DELETE("bow-configurations/{id}")
    suspend fun deleteBowConfiguration(@Path("id") id: String)

    // ---- Arrow configurations -------------------------------------------------

    @GET("arrow-configs")
    suspend fun fetchArrowConfigs(): List<ArrowConfiguration>

    @GET("arrow-configs/{id}")
    suspend fun fetchArrowConfig(@Path("id") id: String): ArrowConfiguration

    @POST("arrow-configs")
    suspend fun createArrowConfig(@Body body: ArrowConfiguration): ArrowConfiguration

    @PUT("arrow-configs/{id}")
    suspend fun updateArrowConfig(
        @Path("id") id: String,
        @Body body: ArrowConfiguration,
    ): ArrowConfiguration

    @DELETE("arrow-configs/{id}")
    suspend fun deleteArrowConfig(@Path("id") id: String)

    // ---- Sessions -------------------------------------------------------------

    @GET("sessions")
    suspend fun fetchSessions(): List<ShootingSession>

    @GET("sessions/{id}")
    suspend fun fetchSession(@Path("id") id: String): ShootingSession

    @POST("sessions")
    suspend fun createSession(@Body body: ShootingSession): ShootingSession

    @PUT("sessions/{id}")
    suspend fun endSession(
        @Path("id") id: String,
        @Body body: EndSessionRequest,
    ): ShootingSession

    @PUT("sessions/{id}")
    suspend fun updateSession(
        @Path("id") id: String,
        @Body body: UpdateSessionRequest,
    ): ShootingSession

    @DELETE("sessions/{id}")
    suspend fun deleteSession(@Path("id") id: String)

    // ---- Plots / ends ---------------------------------------------------------

    @GET("sessions/{sessionId}/plots")
    suspend fun fetchPlots(@Path("sessionId") sessionId: String): List<ArrowPlot>

    @POST("sessions/{sessionId}/plots")
    suspend fun plotArrow(
        @Path("sessionId") sessionId: String,
        @Body body: ArrowPlot,
    ): ArrowPlot

    @PUT("sessions/{sessionId}/plots/{id}")
    suspend fun updatePlot(
        @Path("sessionId") sessionId: String,
        @Path("id") id: String,
        @Body body: ArrowPlot,
    ): ArrowPlot

    @DELETE("sessions/{sessionId}/plots/{id}")
    suspend fun deletePlot(
        @Path("sessionId") sessionId: String,
        @Path("id") id: String,
    )

    @GET("sessions/{sessionId}/ends")
    suspend fun fetchEnds(@Path("sessionId") sessionId: String): List<SessionEnd>

    @POST("sessions/{sessionId}/ends")
    suspend fun completeEnd(
        @Path("sessionId") sessionId: String,
        @Body body: SessionEnd,
    ): SessionEnd

    @DELETE("sessions/{sessionId}/ends/{id}")
    suspend fun deleteEnd(
        @Path("sessionId") sessionId: String,
        @Path("id") id: String,
    )

    // ---- Config changes -------------------------------------------------------

    @GET("config-changes")
    suspend fun fetchConfigChanges(@Query("bowId") bowId: String): List<ConfigurationChange>

    @GET("config-changes/{id}")
    suspend fun fetchConfigChange(@Path("id") id: String): ConfigurationChange

    // ---- Analytics ------------------------------------------------------------

    @GET("analytics/overview")
    suspend fun fetchAnalyticsOverview(
        @Query("period") period: String,
        @Query("bowType") bowType: String? = null,
        @Query("distance") distance: String? = null,
    ): AnalyticsOverview

    @GET("analytics/comparison")
    suspend fun fetchAnalyticsComparison(
        @Query("period") period: String,
        @Query("bowType") bowType: String? = null,
        @Query("distance") distance: String? = null,
    ): PeriodComparison

    @GET("analytics/timeline")
    suspend fun fetchAnalyticsTimeline(
        @Query("period") period: String,
        @Query("bowType") bowType: String? = null,
        @Query("distance") distance: String? = null,
    ): TimelineResponse

    @GET("analytics/drift")
    suspend fun fetchAnalyticsDrift(
        @Query("bowId") bowId: String,
        @Query("period") period: String,
    ): DriftResponse

    @GET("analytics/trends")
    suspend fun fetchAnalyticsTrends(
        @Query("period") period: String,
        @Query("bowType") bowType: String? = null,
        @Query("distance") distance: String? = null,
    ): TrendsResponse

    @GET("bows/{bowId}/suggestions")
    suspend fun fetchSuggestions(
        @Path("bowId") bowId: String,
        @Query("includeRead") includeRead: Boolean? = null,
    ): List<AnalyticsSuggestion>

    @GET("bows/{bowId}/suggestions/{id}")
    suspend fun fetchSuggestion(
        @Path("bowId") bowId: String,
        @Path("id") id: String,
    ): AnalyticsSuggestion

    @POST("bows/{bowId}/suggestions/{id}/apply")
    suspend fun applySuggestion(
        @Path("bowId") bowId: String,
        @Path("id") id: String,
    ): ApplyResult

    @PATCH("suggestions/{id}/read")
    suspend fun markSuggestionRead(@Path("id") id: String)

    @PATCH("suggestions/{id}/dismiss")
    suspend fun dismissSuggestion(@Path("id") id: String)

    @GET("bows/{bowId}/tag-correlations")
    suspend fun fetchTagCorrelations(@Path("bowId") bowId: String): List<TagCorrelation>

    // ---- Subscription ---------------------------------------------------------

    @GET("subscription")
    suspend fun fetchEntitlement(): Entitlement

    @POST("subscription/verify")
    suspend fun verifySubscription(@Body body: VerifySubscriptionRequest): Entitlement

    @POST("subscription/verify-google")
    suspend fun verifyGoogleSubscription(@Body body: VerifyGoogleSubscriptionRequest): Entitlement

    // ---- Device tokens --------------------------------------------------------

    @POST("device-tokens")
    suspend fun registerDeviceToken(@Body body: RegisterDeviceTokenRequest): DeviceToken

    @DELETE("device-tokens/{token}")
    suspend fun deleteDeviceToken(@Path("token") token: String)

    // ---- Social — Profile -------------------------------------------------------

    @GET("social/me")
    suspend fun getSocialProfile(): SocialProfile

    @PATCH("social/me")
    suspend fun updateSocialProfile(@Body body: UpdateSocialProfileRequest): SocialProfile

    @GET("social/archers/{handle}")
    suspend fun getArcherByHandle(@Path("handle") handle: String): SocialProfile

    // ---- Social — Friendships --------------------------------------------------

    @GET("social/friends")
    suspend fun getFriends(): List<Friendship>

    @GET("social/friend-requests")
    suspend fun getFriendRequests(): List<Friendship>

    @POST("social/friend-requests")
    suspend fun sendFriendRequest(@Body body: SendFriendRequestBody): Friendship

    @POST("social/friend-requests/{id}/accept")
    suspend fun acceptFriendRequest(@Path("id") id: String): Friendship

    @DELETE("social/friend-requests/{id}")
    suspend fun deleteFriendRequest(@Path("id") id: String)

    @DELETE("social/friends/{otherUserId}")
    suspend fun unfriend(@Path("otherUserId") otherUserId: String)

    @GET("social/friends/{otherUserId}/profile")
    suspend fun getFriendProfile(@Path("otherUserId") otherUserId: String): FriendProfile

    @GET("social/friends/{otherUserId}/compare")
    suspend fun getCompareView(
        @Path("otherUserId") otherUserId: String,
        @Query("distance") distance: String? = null,
        @Query("face") face: String? = null,
    ): CompareView

    // ---- Social — Clubs --------------------------------------------------------

    @GET("social/clubs")
    suspend fun getClubs(): List<Club>

    @POST("social/clubs")
    suspend fun createClub(@Body body: CreateClubBody): Club

    @GET("social/clubs/{id}")
    suspend fun getClub(@Path("id") id: String): Club

    @PATCH("social/clubs/{id}")
    suspend fun updateClub(@Path("id") id: String, @Body body: UpdateClubBody): Club

    @DELETE("social/clubs/{id}")
    suspend fun deleteClub(@Path("id") id: String)

    @POST("social/clubs/join")
    suspend fun joinClub(@Body body: JoinClubBody): Club

    @DELETE("social/clubs/{id}/members/me")
    suspend fun leaveClub(@Path("id") id: String)

    @GET("social/clubs/{id}/members")
    suspend fun getClubMembers(@Path("id") id: String): List<ClubMember>

    @GET("social/clubs/{id}/feed")
    suspend fun getClubFeed(@Path("id") id: String): List<ClubFeedItem>

    @GET("social/clubs/{id}/leaderboard")
    suspend fun getClubLeaderboard(
        @Path("id") id: String,
        @Query("scope") scope: String = "30d",
    ): List<LeaderboardRow>

    // ---- Social — Leagues ------------------------------------------------------

    @GET("social/leagues")
    suspend fun getLeagues(): List<League>

    @POST("social/leagues")
    suspend fun createLeague(@Body body: CreateLeagueBody): League

    @GET("social/leagues/{id}")
    suspend fun getLeague(@Path("id") id: String): League

    @PATCH("social/leagues/{id}")
    suspend fun updateLeague(@Path("id") id: String, @Body body: UpdateLeagueBody): League

    @DELETE("social/leagues/{id}")
    suspend fun deleteLeague(@Path("id") id: String)

    @POST("social/leagues/join")
    suspend fun joinLeagueByCode(@Body body: JoinLeagueBody): League

    @POST("social/leagues/{id}/join")
    suspend fun joinLeague(@Path("id") id: String, @Body body: JoinLeagueBody): League

    @DELETE("social/leagues/{id}/entries/me")
    suspend fun leaveLeague(@Path("id") id: String)

    @POST("social/leagues/{id}/submissions")
    suspend fun submitLeagueScore(
        @Path("id") id: String,
        @Body body: SubmitScoreBody,
    ): LeagueSubmission

    @GET("social/leagues/{id}/submissions")
    suspend fun getLeagueSubmissions(@Path("id") id: String): List<LeagueSubmission>

    @GET("social/leagues/{id}/standings")
    suspend fun getLeagueStandings(@Path("id") id: String): List<LeagueStandingRow>

    @GET("social/leagues/{id}/admin")
    suspend fun getLeagueAdminMatrix(@Path("id") id: String): AdminMatrix

    // ---- Social — Activity Feed ------------------------------------------------

    @GET("social/feed")
    suspend fun getActivityFeed(): List<ActivityItem>

    // ---- Social — Invitations (§11) --------------------------------------------

    @GET("social/invitations")
    suspend fun getInvitations(): List<SocialInvitation>

    @POST("social/invitations/{id}/accept")
    suspend fun acceptInvitation(
        @Path("id") id: String,
        @Body body: AcceptInvitationBody = AcceptInvitationBody(),
    ): SocialInvitation

    @DELETE("social/invitations/{id}")
    suspend fun declineInvitation(@Path("id") id: String)

    @POST("social/clubs/{id}/invites")
    suspend fun inviteToClub(
        @Path("id") id: String,
        @Body body: SendInvitationBody,
    ): SocialInvitation

    @POST("social/leagues/{id}/invites")
    suspend fun inviteToLeague(
        @Path("id") id: String,
        @Body body: SendInvitationBody,
    ): SocialInvitation

    // ---- Social — Pending count (§12) ------------------------------------------

    @GET("social/pending-count")
    suspend fun getPendingCount(): SocialPendingCount

    // ---- Social — Dev notify (for e2e tests) -----------------------------------

    @POST("social/dev/notify")
    suspend fun sendDevNotify(@Body body: Map<String, String>)
}
