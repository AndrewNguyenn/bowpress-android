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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.andrewnguyen.bowpress.R
import com.andrewnguyen.bowpress.core.data.push.DeviceTokenRegistrar
import com.andrewnguyen.bowpress.core.data.sync.AnalyticsRefreshBus
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch { deviceTokenRegistrar.register(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val intent = NotificationIntentBuilder.buildIntent(this, message.data)
        val notification = buildNotification(message, intent)
        val manager = ContextCompat.getSystemService(this, NotificationManager::class.java) ?: return
        ensureChannel(manager)
        manager.notify(message.messageId?.hashCode() ?: 0, notification)

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
        return NotificationCompat.Builder(this, CHANNEL_ID)
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
        }
    }

    companion object {
        const val CHANNEL_ID = "bowpress_suggestions"
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
        return when (type) {
            "suggestion" -> {
                val id = data["id"] ?: return null
                val bowId = data["bowId"]
                val base = "bowpress://suggestion/$id"
                if (bowId.isNullOrEmpty()) base
                else "$base?bowId=${java.net.URLEncoder.encode(bowId, Charsets.UTF_8.name())}"
            }
            else -> null
        }
    }

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
