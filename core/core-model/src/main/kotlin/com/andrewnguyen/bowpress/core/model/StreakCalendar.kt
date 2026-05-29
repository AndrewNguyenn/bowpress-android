package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.Serializable

// Mirrors the API's GET /social/streak-calendar contract (bowpress-api
// `buildStreakCalendar`) and iOS `StreakCalendar`. UTC-day bucketed; the
// client lays out the Mon→Sun grid from the real month dates, 1:1 with
// `weeks` so the trailing flame column lines up with each calendar row.

@Serializable
data class StreakCalendarDay(
    /** "YYYY-MM-DD" UTC. */
    val dayKey: String,
    val dayOfMonth: Int,
    val sessionCount: Int,
)

@Serializable
data class StreakCalendarWeek(
    /** Monday UTC "YYYY-MM-DD". */
    val weekStartKey: String,
    val maintained: Boolean,
)

@Serializable
data class StreakCalendar(
    val year: Int,
    val month: Int,
    val days: List<StreakCalendarDay> = emptyList(),
    val weeks: List<StreakCalendarWeek> = emptyList(),
    val weekStreak: Int = 0,
    val activitiesInMonth: Int = 0,
)
