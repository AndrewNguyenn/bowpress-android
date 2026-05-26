package com.andrewnguyen.bowpress.feature.social.ui.feed

import com.andrewnguyen.bowpress.core.model.FeedSummary
import com.andrewnguyen.bowpress.core.model.FeedSummaryBestSession
import com.andrewnguyen.bowpress.core.model.FeedSummaryDay
import com.andrewnguyen.bowpress.core.model.FeedSummaryInsight
import com.andrewnguyen.bowpress.core.model.FeedSummaryOpeningCard
import com.andrewnguyen.bowpress.core.model.FeedSummarySnapshot
import com.andrewnguyen.bowpress.core.model.FeedSummaryThisWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// Mirrors iOS `FeedSummary` → `FeedSummaryUi` mapping. Lives in the
// feature module so it can depend on the UI types in `FeedCarousel.kt`.

/** Wire DTO → carousel UI model. */
fun FeedSummary.toUi(now: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): FeedSummaryUi =
    FeedSummaryUi(
        thisWeek = thisWeek?.toUi(now = now, zone = zone),
        snapshot = snapshot?.toUi(),
        bestSession = bestSession?.toUi(),
        insight = insight?.toUi(),
        openingCard = openingCard.toUi(),
    )

private fun FeedSummaryThisWeek.toUi(now: LocalDate, zone: ZoneId): FeedSummaryUi.ThisWeek =
    FeedSummaryUi.ThisWeek(
        weekStreak = weekStreak,
        totalArrows = totalArrows,
        sessionCount = sessionCount,
        days = days.map { it.toUi(now = now, zone = zone) },
    )

private fun FeedSummaryDay.toUi(now: LocalDate, zone: ZoneId): FeedSummaryUi.Day {
    // iOS shifts the UTC-midnight `dayKey` into the archer's local
    // timezone for the M/T/W/T/F/S/S letter + "today" highlight, so a
    // shot logged at 7pm Sunday EST (Monday UTC) shows on the Sunday
    // bar. Mirror that.
    val localDate = runCatching {
        LocalDateTime.of(LocalDate.parse(dayKey), LocalTime.MIDNIGHT)
            .atOffset(ZoneOffset.UTC)
            .atZoneSameInstant(zone)
            .toLocalDate()
    }.getOrElse { LocalDate.MIN }
    return FeedSummaryUi.Day(
        label = MON_FIRST_LETTERS[(localDate.dayOfWeek.value - 1).coerceIn(0, 6)],
        arrows = arrows,
        isToday = localDate == now,
    )
}

private fun FeedSummarySnapshot.toUi(): FeedSummaryUi.Snapshot =
    FeedSummaryUi.Snapshot(
        sessionsThis = sessionsThis,
        sessionsLast = sessionsLast,
        arrowsThis = arrowsThis,
        arrowsLast = arrowsLast,
        avgRingThis = avgRingThis,
        avgRingLast = avgRingLast,
        rangeLabel = "${formatShortDate(rangeStart)} → ${formatShortDate(rangeEnd)}",
    )

private fun FeedSummaryBestSession.toUi(): FeedSummaryUi.BestSession =
    FeedSummaryUi.BestSession(
        sessionName = sessionName,
        avgRing = avgRing,
        xCount = xCount,
        totalArrows = totalArrows,
        bowName = bowName,
        arrows = arrows.map { FeedSummaryUi.ArrowPoint(x = it.x, y = it.y) },
        prDeltaAvgRing = prDeltaAvgRing,
        sharedSessionId = sharedSessionId,
    )

private fun FeedSummaryInsight.toUi(): FeedSummaryUi.Insight =
    FeedSummaryUi.Insight(
        headline = headline,
        sampleSize = sampleSize,
        metrics = metrics.map {
            FeedSummaryUi.InsightMetric(label = it.label, value = it.value, maple = it.maple)
        },
    )

private fun FeedSummaryOpeningCard.toUi(): FeedSummaryUi.OpeningCard = when (this) {
    FeedSummaryOpeningCard.ThisWeek -> FeedSummaryUi.OpeningCard.ThisWeek
    FeedSummaryOpeningCard.Snapshot -> FeedSummaryUi.OpeningCard.Snapshot
    FeedSummaryOpeningCard.BestSession -> FeedSummaryUi.OpeningCard.BestSession
    FeedSummaryOpeningCard.Insight -> FeedSummaryUi.OpeningCard.Insight
}

private val MON_FIRST_LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")

private val SHORT_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE MMM d", Locale.ENGLISH)

private fun formatShortDate(iso: String): String =
    runCatching { LocalDate.parse(iso).format(SHORT_DATE_FORMATTER).lowercase(Locale.ENGLISH) }
        .getOrDefault(iso)
