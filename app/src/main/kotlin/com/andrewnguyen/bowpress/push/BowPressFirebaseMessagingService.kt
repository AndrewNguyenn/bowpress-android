package com.andrewnguyen.bowpress.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.andrewnguyen.bowpress.R
import com.andrewnguyen.bowpress.core.data.push.DeviceTokenRegistrar
import com.andrewnguyen.bowpress.core.data.sync.AnalyticsRefreshBus
import com.andrewnguyen.bowpress.core.data.sync.SocialBadgeRefreshBus
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * FCM service — handles new-token delivery and foreground/background message
 * arrival.
 *
 * Mirrors iOS `PushRegistrar` (token registration) and `NotificationRouter`
 * (tap → deep link into Analytics, plus foreground haptic + analytics
 * refresh via `handleForegroundArrival(userInfo:)`). Payload contract (from
 * the backend worker):
 * ```
 * { type: "suggestion", id: "<suggestionId>", bowId: "<bowId>" }
 * ```
 * Tapping a suggestion notification deep-links into
 * `bowpress://suggestion/<id>?bowId=<bowId>`, which `MainActivity` handles.
 *
 * @see DeviceTokenRegistrar
 */
@AndroidEntryPoint
class BowPressFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var deviceTokenRegistrar: DeviceTokenRegistrar
    @Inject lateinit var analyticsRefreshBus: AnalyticsRefreshBus
    @Inject lateinit var socialBadgeRefreshBus: SocialBadgeRefreshBus

    // Backstop CoroutineExceptionHandler so a 401 from the bearer-less
    // /push/register call (FCM fires onNewToken before the user signs in)
    // doesn't crash the process. iOS PushRegistrar runs after sign-in;
    // Android FCM token delivery is async, so tolerate auth-less failures
    // and retry on the next launch via PushInitializer.
    private val scope = CoroutineScope(
        SupervisorJob() +
            Dispatchers.IO +
            CoroutineExceptionHandler { _, t ->
                Log.w("BowPressFCM", "Token registration failed", t)
            },
    )

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch {
            runCatching { deviceTokenRegistrar.register(token) }
                .onFailure {
                    if (it is CancellationException) throw it
                    Log.w("BowPressFCM", "Token registration failed", it)
                }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val intent = NotificationIntentBuilder.buildIntent(this, message.data)
        val notification = buildNotification(message, intent)
        val manager = ContextCompat.getSystemService(this, NotificationManager::class.java) ?: return
        ensureChannel(manager)
        manager.notify(message.messageId?.hashCode() ?: 0, notification)

        // A friend-request / club-invite / league-invite push changes the
        // Social-tab badge count — invalidate it so AppStateViewModel re-fetches.
        if (message.data["type"] in NotificationIntentBuilder.BADGE_AFFECTING_PUSH_TYPES) {
            socialBadgeRefreshBus.bump()
        }

        if (isAppInForeground()) {
            triggerForegroundHaptic()
            analyticsRefreshBus.bump()
        }
    }

    /**
     * Mirrors iOS `NotificationRouter.handleForegroundArrival(userInfo:)` —
     * true when any `Activity` is at least `STARTED`, i.e. the app is visible.
     */
    private fun isAppInForeground(): Boolean =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    private fun triggerForegroundHaptic() {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = ContextCompat.getSystemService(this, VibratorManager::class.java)
            mgr?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ContextCompat.getSystemService(this, Vibrator::class.java)
        }
        vibrator?.takeIf { it.hasVibrator() }?.vibrate(
            VibrationEffect.createOneShot(50L, VibrationEffect.DEFAULT_AMPLITUDE),
        )
    }

    private fun buildNotification(message: RemoteMessage, contentIntent: PendingIntent?): android.app.Notification {
        val title = message.notification?.title ?: message.data["title"] ?: "BowPress"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val type = message.data["type"] ?: ""
        val channelId = if (type in NotificationIntentBuilder.SOCIAL_PUSH_TYPES) {
            CHANNEL_SOCIAL
        } else {
            CHANNEL_ID
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "BowPress suggestions",
                        NotificationManager.IMPORTANCE_DEFAULT,
                    ).apply {
                        description = "Coaching prompts and tuning suggestions"
                    },
                )
            }
            if (manager.getNotificationChannel(CHANNEL_SOCIAL) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_SOCIAL,
                        "Social",
                        NotificationManager.IMPORTANCE_DEFAULT,
                    ).apply {
                        description = "Friend requests, league deadlines, and club activity"
                    },
                )
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "bowpress_suggestions"
        const val CHANNEL_SOCIAL = "bowpress_social"
    }
}

/**
 * Builds the [Intent] + [PendingIntent] pair used for both runtime tap handling
 * and unit testing. Extracted so tests can assert the URI without booting an
 * Android runtime.
 */
object NotificationIntentBuilder {

    /**
     * The deep-link URI the tap targets, as a raw string. Lives here (instead
     * of inside [buildDeepLinkUri]) so it can be unit-tested on a pure-JVM
     * classpath without pulling `android.net.Uri` in.
     */
    fun buildDeepLinkUriString(data: Map<String, String>): String? {
        val type = data["type"] ?: return null
        // Mentions contract §3.3 — the mention / reply pushes carry an explicit
        // `deepLink` (the API's `extra.deepLink`) pointing at the feed or the
        // shared session. Honour it directly when present so the routing
        // target is server-driven; the per-type fallbacks below cover a push
        // that omits it.
        data["deepLink"]?.takeIf { it.isNotBlank() }?.let { explicit ->
            if (type in MENTION_PUSH_TYPES) return explicit
        }
        return when (type) {
            // Mentions contract §3.3 — a mention in a post / comment, or a
            // reply on a thread the user is in. Without an explicit `deepLink`
            // these route at the shared session when a `subjectId` is carried,
            // else the feed.
            "mention_post", "mention_comment", "comment_reply" -> {
                val subjectId = data["subjectId"]
                if (!subjectId.isNullOrEmpty()) "bowpress://social/sessions/$subjectId"
                else "bowpress://social"
            }
            "suggestion" -> {
                val id = data["id"] ?: return null
                val bowId = data["bowId"]
                val base = "bowpress://suggestion/$id"
                if (bowId.isNullOrEmpty()) base
                else "$base?bowId=${java.net.URLEncoder.encode(bowId, Charsets.UTF_8.name())}"
            }
            // Social push types per contract §9
            "friend_request" -> "bowpress://social/friends"
            "friend_pr" -> "bowpress://social/friends"
            "league_deadline" -> {
                val leagueId = data["leagueId"]
                if (!leagueId.isNullOrEmpty()) "bowpress://social/leagues/$leagueId"
                else "bowpress://social/leagues"
            }
            "club_activity" -> {
                val clubId = data["clubId"]
                if (!clubId.isNullOrEmpty()) "bowpress://social/clubs/$clubId"
                else "bowpress://social"
            }
            // Invitation / accepted push types per contract §13
            "friend_accepted" -> "bowpress://social/friends"
            "club_invite" -> "bowpress://social/clubs"
            "league_invite" -> "bowpress://social/leagues"
            // Club announcement board post — §17. Deep-links to the club.
            "club_announcement" -> {
                val clubId = data["clubId"]
                if (!clubId.isNullOrEmpty()) "bowpress://social/clubs/$clubId"
                else "bowpress://social/clubs"
            }
            else -> null
        }
    }

    /**
     * Mention / reply push `type` values (mentions contract §3.3) — a mention
     * in a post or comment, or a reply on a thread the user was pulled into.
     */
    val MENTION_PUSH_TYPES: Set<String> = setOf(
        "mention_post", "mention_comment", "comment_reply",
    )

    /** Push `type` values routed to the Social notification channel. */
    val SOCIAL_PUSH_TYPES: Set<String> = setOf(
        "friend_request", "friend_pr", "league_deadline", "club_activity",
        "friend_accepted", "club_invite", "league_invite", "club_announcement",
    ) + MENTION_PUSH_TYPES

    /**
     * Push `type` values that change the Social-tab badge count — receipt
     * should trigger a `/social/pending-count` re-fetch (§12).
     */
    val BADGE_AFFECTING_PUSH_TYPES: Set<String> = setOf(
        "friend_request", "club_invite", "league_invite",
    )

    /** The URI the tap deep-links to, given a payload map. */
    fun buildDeepLinkUri(data: Map<String, String>): Uri? =
        buildDeepLinkUriString(data)?.let(Uri::parse)

    /**
     * Build a [PendingIntent] that launches `MainActivity` via the deep link.
     * Returns `null` if the payload doesn't carry a tappable target.
     */
    fun buildIntent(context: Context, data: Map<String, String>): PendingIntent? {
        val uri = buildDeepLinkUri(data) ?: return null
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            setPackage(context.packageName)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, uri.hashCode(), intent, flags)
    }
}
