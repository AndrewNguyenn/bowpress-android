package com.andrewnguyen.bowpress.core.network

import com.andrewnguyen.bowpress.core.model.AnalyticsOverview
import com.andrewnguyen.bowpress.core.model.AnalyticsSuggestion
import com.andrewnguyen.bowpress.core.model.ArrowConfiguration
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.Bow
import com.andrewnguyen.bowpress.core.model.BowConfiguration
import com.andrewnguyen.bowpress.core.model.ConfigurationChange
import com.andrewnguyen.bowpress.core.model.DeviceToken
import com.andrewnguyen.bowpress.core.model.DriftResponse
import com.andrewnguyen.bowpress.core.model.Entitlement
import com.andrewnguyen.bowpress.core.model.PeriodComparison
import com.andrewnguyen.bowpress.core.model.SessionEnd
import com.andrewnguyen.bowpress.core.model.ShootingSession
import com.andrewnguyen.bowpress.core.model.TagCorrelation
import com.andrewnguyen.bowpress.core.model.TimelineResponse
import com.andrewnguyen.bowpress.core.model.TrendsResponse
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
}
