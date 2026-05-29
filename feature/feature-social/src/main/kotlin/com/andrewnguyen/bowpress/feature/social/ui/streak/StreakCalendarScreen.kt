package com.andrewnguyen.bowpress.feature.social.ui.streak

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.StreakCalendarDay
import com.andrewnguyen.bowpress.core.model.StreakCalendarWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RowHeight = 42.dp
private val HeaderHeight = 18.dp
private val WEEKDAY_LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")
private val MONTH_LABEL_FORMAT = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.ENGLISH)

@Composable
fun StreakCalendarScreen(
    onBack: () -> Unit,
    viewModel: StreakCalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper)
            .verticalScroll(rememberScrollState()),
    ) {
        // Top nav — back to Feed (iOS parity: "‹ Feed"). No Share button.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(top = 6.dp, bottom = 8.dp),
        ) {
            Text(
                "‹  Feed",
                style = interUI(10.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.32.em),
                color = AppPondDk,
                modifier = Modifier.clickable(onClick = onBack),
            )
            Text("Your streak", style = frauncesDisplay(28.sp), color = AppInk)
        }

        Column(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            MonthHeader(
                label = state.displayed.format(MONTH_LABEL_FORMAT),
                canGoPrev = state.canGoPrev,
                canGoNext = state.canGoNext,
                onPrev = viewModel::prevMonth,
                onNext = viewModel::nextMonth,
            )
            StatRow(
                weekStreak = state.calendar?.weekStreak ?: 0,
                activities = state.calendar?.activitiesInMonth ?: 0,
            )
            CalendarBody(
                weeks = state.calendar?.weeks.orEmpty(),
                days = state.calendar?.days.orEmpty(),
                weekStreak = state.calendar?.weekStreak ?: 0,
                failed = state.failed && state.calendar == null,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MonthHeader(
    label: String,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = frauncesDisplay(23.sp, italic = true, weight = FontWeight.Medium),
            color = AppInk,
        )
        Spacer(Modifier.weight(1f))
        NavChevron(Icons.Filled.ChevronLeft, enabled = canGoPrev, onClick = onPrev)
        Spacer(Modifier.width(10.dp))
        NavChevron(Icons.Filled.ChevronRight, enabled = canGoNext, onClick = onNext)
    }
}

@Composable
private fun NavChevron(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (enabled) AppPondDk else AppLine,
        modifier = Modifier
            .size(30.dp)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
    )
}

@Composable
private fun StatRow(weekStreak: Int, activities: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        Stat(label = "Your Streak", value = "$weekStreak Weeks")
        Stat(label = "Streak Activities", value = "$activities")
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label.uppercase(Locale.ENGLISH),
            style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
            color = AppInk3,
        )
        Text(
            text = value,
            style = frauncesDisplay(17.sp, italic = true, weight = FontWeight.Medium),
            color = AppInk,
        )
    }
}

@Composable
private fun CalendarBody(
    weeks: List<StreakCalendarWeek>,
    days: List<StreakCalendarDay>,
    weekStreak: Int,
    failed: Boolean,
) {
    if (failed) {
        Text(
            "Couldn't load your streak.",
            style = frauncesDisplay(14.sp, italic = true, weight = FontWeight.Normal),
            color = AppInk3,
        )
        return
    }
    val daysByKey = remember(days) { days.associateBy { it.dayKey } }
    val today = LocalDate.now()
    val zone = remember { ZoneId.systemDefault() }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            WeekdayHeader()
            weeks.forEach { week ->
                WeekRow(week = week, daysByKey = daysByKey, today = today, zone = zone)
            }
        }
        FlameColumn(weeks = weeks, weekStreak = weekStreak)
    }
}

@Composable
private fun WeekdayHeader() {
    Row(modifier = Modifier.height(HeaderHeight)) {
        WEEKDAY_LETTERS.forEach { letter ->
            Text(
                text = letter,
                style = interUI(8.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.16.em),
                color = AppInk3,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun WeekRow(
    week: StreakCalendarWeek,
    daysByKey: Map<String, StreakCalendarDay>,
    today: LocalDate,
    zone: ZoneId,
) {
    val weekStart = remember(week.weekStartKey) { LocalDate.parse(week.weekStartKey) }
    Row(modifier = Modifier.height(RowHeight), verticalAlignment = Alignment.CenterVertically) {
        for (offset in 0..6) {
            val date = weekStart.plusDays(offset.toLong())
            val day = daysByKey[date.toString()]
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (day != null) {
                    // "Today" in the device's local TZ — same rule the feed
                    // card uses (FeedSummaryMapper), so the card's highlighted
                    // day and this calendar agree on which cell is today.
                    val localDate = LocalDateTime.of(date, LocalTime.MIDNIGHT)
                        .atOffset(ZoneOffset.UTC)
                        .atZoneSameInstant(zone)
                        .toLocalDate()
                    DayCircle(day = day, isToday = localDate == today)
                }
                // Spill-over day from an adjacent month → blank slot.
            }
        }
    }
}

@Composable
private fun DayCircle(day: StreakCalendarDay, isToday: Boolean) {
    val shot = day.sessionCount > 0
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(32.dp)) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (shot) AppPondDk else Color.Transparent)
                .border(
                    width = if (isToday) 1.5.dp else 1.dp,
                    color = if (isToday) AppMaple else AppLine,
                    shape = CircleShape,
                ),
        )
        if (shot) {
            TargetGlyph()
        } else {
            Text(
                text = "${day.dayOfMonth}",
                style = jetbrainsMono(11.sp, FontWeight.Medium),
                color = if (isToday) AppInk else AppInk2,
            )
        }
    }
}

/** Tiny bullseye drawn in paper on a filled "shot" circle. */
@Composable
private fun TargetGlyph() {
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .border(1.4.dp, AppPaper, CircleShape),
        )
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(AppPaper),
        )
    }
}

@Composable
private fun FlameColumn(weeks: List<StreakCalendarWeek>, weekStreak: Int) {
    Column(
        modifier = Modifier
            .width(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(AppMaple.copy(alpha = 0.10f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Spacer(Modifier.height(HeaderHeight))
        weeks.forEach { week ->
            Box(
                modifier = Modifier.height(RowHeight),
                contentAlignment = Alignment.Center,
            ) {
                CheckCell(week.maintained)
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((-5).dp),
            ) {
                repeat(3) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = null,
                        tint = AppMaple,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
            Text(
                text = "$weekStreak",
                style = interUI(12.sp, FontWeight.Bold),
                color = AppMaple,
            )
        }
    }
}

@Composable
private fun CheckCell(maintained: Boolean) {
    if (maintained) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(AppMaple),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = AppPaper,
                modifier = Modifier.size(12.dp),
            )
        }
    } else {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(1.dp, AppMaple.copy(alpha = 0.35f), CircleShape),
        )
    }
}
