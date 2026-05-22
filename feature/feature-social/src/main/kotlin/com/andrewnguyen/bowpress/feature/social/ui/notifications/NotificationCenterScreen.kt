package com.andrewnguyen.bowpress.feature.social.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppLine2
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.NotificationCategory
import com.andrewnguyen.bowpress.core.model.SocialNotification
import com.andrewnguyen.bowpress.feature.social.ui.SocialAvatar
import com.andrewnguyen.bowpress.feature.social.ui.avatarInitials
import com.andrewnguyen.bowpress.feature.social.ui.socialRelativeTime
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * The notification center — reached from the bell in the Social top-nav.
 * Rows group by recency (Today · This week · Earlier), carry an ink rule +
 * paper-2 tint when unread, filter by category, swipe to dismiss, and clear
 * in bulk. Design: explorations/Social Notifications.html.
 */
@Composable
fun NotificationCenterScreen(
    onBack: () -> Unit,
    onItemClick: (SocialNotification) -> Unit,
    viewModel: NotificationCenterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper),
    ) {
        // ── Top nav ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(top = 6.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "‹  Social",
                    style = interUI(10.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.32.em),
                    color = AppPondDk,
                    modifier = Modifier.clickable(onClick = onBack),
                )
                Text("Notifications", style = frauncesDisplay(28.sp), color = AppInk)
                Text(
                    if (state.unread > 0) "${state.unread} new" else "all caught up",
                    style = jetbrainsMono(10.sp),
                    color = AppInk3,
                )
            }
            if (state.items.any { !it.read }) {
                Text(
                    "MARK ALL READ ›",
                    style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                    color = AppPondDk,
                    modifier = Modifier.clickable { viewModel.markAllRead() },
                )
            }
        }
        HorizontalDivider(color = AppLine, thickness = 1.dp)

        // ── Filter pills ─────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (cat in FILTER_PILLS) {
                val n = if (cat == NotificationCategory.All) state.items.size
                else state.items.count { it.category == cat }
                FilterPill(cat, n, on = state.filter == cat) { viewModel.setFilter(cat) }
            }
        }
        HorizontalDivider(color = AppLine, thickness = 1.dp)

        // ── Content ──────────────────────────────────────────────
        val now = Instant.now()
        val visible = state.visible
        when {
            state.isLoading -> CenteredNotice("Loading…")
            state.error != null -> Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CenteredNotice(state.error ?: "Couldn't load notifications.")
                Text(
                    "RETRY ›",
                    style = interUI(10.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                    color = AppPondDk,
                    modifier = Modifier.clickable { viewModel.load() },
                )
            }
            visible.isEmpty() -> EmptyState(state.filter)
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
            ) {
                for (group in NotifGroup.entries) {
                    val rows = visible.filter { group.matches(it.createdAt, now) }
                    notifSection(group.label, rows, now, onItemClick, viewModel)
                }
                item {
                    Text(
                        "CLEAR ALL ›",
                        style = interUI(9.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                        color = AppInk3,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.clearAll() }
                            .padding(vertical = 18.dp),
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

// ── Recency grouping ─────────────────────────────────────────────

private enum class NotifGroup(val label: String) {
    Today("TODAY"), ThisWeek("THIS WEEK"), Earlier("EARLIER");

    fun matches(at: Instant, now: Instant): Boolean {
        val zone = ZoneId.systemDefault()
        val days = ChronoUnit.DAYS.between(
            at.atZone(zone).toLocalDate(), now.atZone(zone).toLocalDate(),
        )
        return when (this) {
            // `<= 0` also absorbs any future-dated row (clock skew) into Today,
            // so the three buckets stay exhaustive and nothing is dropped.
            Today -> days <= 0L
            ThisWeek -> days in 1L..6L
            Earlier -> days >= 7L
        }
    }
}

private val FILTER_PILLS = listOf(
    NotificationCategory.All,
    NotificationCategory.Kudos,
    NotificationCategory.Comments,
    NotificationCategory.Mentions,
    NotificationCategory.League,
)

// ── Section + rows ───────────────────────────────────────────────

private fun LazyListScope.notifSection(
    label: String,
    rows: List<SocialNotification>,
    now: Instant,
    onItemClick: (SocialNotification) -> Unit,
    viewModel: NotificationCenterViewModel,
) {
    if (rows.isEmpty()) return
    item(key = "hdr_$label") {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                label,
                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = AppPondDk,
            )
            Text("${rows.size}", style = jetbrainsMono(9.sp), color = AppInk3)
        }
        HorizontalDivider(color = AppLine, thickness = 1.dp)
    }
    items(rows, key = { it.id }) { item ->
        // `items(key=)` already scopes the row's `remember`, so the
        // SwipeToDismissBox state is correctly tied to this notification id.
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) {
                    // confirmValueChange may fire more than once per gesture;
                    // ViewModel.dismiss() guards re-entry so the DELETE is sent once.
                    viewModel.dismiss(item.id)
                    true
                } else {
                    false
                }
            },
        )
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = true,
            backgroundContent = {
                // Paint the maple dismiss tray only while a swipe is in
                // progress — otherwise it bleeds behind every row at rest.
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AppMaple)
                            .padding(end = 18.dp),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Text(
                            "DISMISS",
                            style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                            color = AppPaper,
                        )
                    }
                }
            },
        ) {
            NotificationRow(item, now) {
                viewModel.markRead(item.id)
                onItemClick(item)
            }
        }
    }
}

@Composable
private fun NotificationRow(
    item: SocialNotification,
    now: Instant,
    onClick: () -> Unit,
) {
    val accent = when {
        item.read -> AppPaper
        item.isAlert -> AppMaple
        else -> AppPondDk
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (item.read) AppPaper else AppPaper2)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = if (item.read) 0.dp else 10.dp),
    ) {
        Box(Modifier.width(2.dp).fillMaxHeight().background(accent))
        Spacer(Modifier.width(10.dp))

        if (item.actorHandle != null) {
            SocialAvatar(
                avatarInitials(item.actorDisplayName ?: item.actorHandle ?: "?"),
                size = 30,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .border(1.dp, if (item.isAlert) AppMaple else AppInk2),
                contentAlignment = Alignment.Center,
            ) {
                Text("○", style = interUI(13.sp), color = if (item.isAlert) AppMaple else AppInk2)
            }
        }
        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                style = frauncesDisplay(13.5.sp),
                color = if (item.read) AppInk2 else AppInk,
            )
            val body = item.body
            if (!body.isNullOrEmpty()) {
                Row(modifier = Modifier.padding(top = 5.dp)) {
                    Box(Modifier.width(1.dp).fillMaxHeight().background(AppLine))
                    Spacer(Modifier.width(9.dp))
                    Text(body, style = frauncesDisplay(12.5.sp), color = AppInk2, maxLines = 2)
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            socialRelativeTime(item.createdAt, now),
            style = jetbrainsMono(9.sp),
            color = AppInk3,
        )
    }
    HorizontalDivider(color = AppLine2, thickness = 1.dp)
}

@Composable
private fun FilterPill(
    category: NotificationCategory,
    count: Int,
    on: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .border(1.dp, if (on) AppPondDk else AppInk3)
            .background(if (on) AppPondDk else AppPaper)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            category.name.uppercase(),
            style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.20.em),
            color = if (on) AppPaper else AppInk3,
        )
        Text("$count", style = jetbrainsMono(8.5.sp), color = if (on) AppPaper else AppInk3)
    }
}

@Composable
private fun EmptyState(filter: NotificationCategory) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(54.dp).border(1.dp, AppInk3),
            contentAlignment = Alignment.Center,
        ) {
            Text("○", style = frauncesDisplay(22.sp), color = AppInk3)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            if (filter == NotificationCategory.All) "All caught up." else "Nothing here.",
            style = frauncesDisplay(24.sp),
            color = AppInk,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (filter == NotificationCategory.All)
                "Nothing new since you last looked. Go shoot something."
            else "No notifications in this filter.",
            style = frauncesDisplay(14.sp),
            color = AppInk2,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CenteredNotice(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = jetbrainsMono(10.sp), color = AppInk3)
    }
}
