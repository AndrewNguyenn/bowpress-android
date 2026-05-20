package com.andrewnguyen.bowpress.feature.social.ui.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.model.Achievement
import com.andrewnguyen.bowpress.core.model.AchievementKind
import com.andrewnguyen.bowpress.core.model.TrophyCategory
import com.andrewnguyen.bowpress.core.model.TrophyDef

// ── Category display order ────────────────────────────────────────────────────
private val CATEGORY_ORDER = listOf(
    TrophyCategory.skill,
    TrophyCategory.milestone,
    TrophyCategory.streak,
    TrophyCategory.exploration,
    TrophyCategory.course,
    TrophyCategory.competition,
    TrophyCategory.community,
)

private fun TrophyCategory.displayName(): String = when (this) {
    TrophyCategory.skill -> "Skill"
    TrophyCategory.milestone -> "Milestone"
    TrophyCategory.streak -> "Streak"
    TrophyCategory.exploration -> "Exploration"
    TrophyCategory.course -> "3D Course"
    TrophyCategory.competition -> "Competition"
    TrophyCategory.community -> "Community"
    TrophyCategory.unknown -> "Other"
}

/**
 * Profile trophy case (§18) — a showcase of the trophies the archer has
 * **earned**, grouped by [TrophyCategory] in display order. A category with
 * nothing earned is omitted. The header counts distinct earned kinds —
 * "N collected" — with no fixed denominator, since the catalogue grows over
 * time.
 *
 * The [catalog] (`GET /social/trophies`) is still needed: it maps an earned
 * achievement's kind to its category. Until it loads the section falls back
 * to a flat earned list so the screen never blanks; when there are no
 * achievements at all the empty-state card is shown.
 *
 * [ownerLabel] tailors the empty-state copy ("Shoot" vs "They need").
 */
@Composable
fun TrophyCaseSection(
    achievements: List<Achievement>,
    catalog: List<TrophyDef>,
    ownerLabel: String,
    modifier: Modifier = Modifier,
) {
    // Newest earned achievement per kind (wire string), so a kind earned more
    // than once shows once, carrying its most recent achievement.
    val earnedByKind: Map<String, Achievement> = achievements
        .groupBy { it.kind.name }
        .mapValues { (_, list) -> list.maxByOrNull { it.createdAt }!! }

    Column(modifier = modifier.fillMaxWidth()) {
        // ── Header ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                "TROPHY CASE",
                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                color = AppInk3,
            )
            if (achievements.isNotEmpty()) {
                // Exclude `unknown` — a kind from a newer API that doesn't
                // render in the grouped body — so the count matches what shows.
                val collected = earnedByKind.keys.count { it != AchievementKind.unknown.name }
                Text(
                    "$collected collected",
                    style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                    color = AppInk3,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = AppLine, thickness = 1.dp)
        Spacer(Modifier.height(8.dp))

        when {
            // ── Empty state ───────────────────────────────────────────────
            achievements.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AppLine)
                        .background(AppPaper2)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("No trophies yet.", style = frauncesDisplay(15.sp), color = AppInk)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "$ownerLabel a PR, milestone, or streak to fill the case.",
                        style = frauncesDisplay(12.sp, italic = true),
                        color = AppInk3,
                    )
                }
            }

            // ── Earned trophies, grouped by category ──────────────────────
            catalog.isNotEmpty() -> {
                CATEGORY_ORDER.forEach { category ->
                    // Earned trophies in this category, in catalogue order.
                    val earned = catalog
                        .filter { it.category == category }
                        .mapNotNull { def -> earnedByKind[def.kind] }
                    if (earned.isEmpty()) return@forEach

                    Text(
                        text = category.displayName().uppercase(),
                        style = interUI(8.sp, FontWeight.SemiBold).copy(letterSpacing = 0.28.em),
                        color = AppInk3,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    earned.forEachIndexed { index, achievement ->
                        if (index > 0) Spacer(Modifier.height(4.dp))
                        AchievementRow(achievement = achievement, modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }

            // ── Fallback: catalogue not loaded — flat earned list ─────────
            else -> {
                achievements
                    .sortedByDescending { it.createdAt }
                    .forEachIndexed { index, achievement ->
                        if (index > 0) Spacer(Modifier.height(6.dp))
                        AchievementRow(achievement = achievement, modifier = Modifier.fillMaxWidth())
                    }
            }
        }
    }
}
