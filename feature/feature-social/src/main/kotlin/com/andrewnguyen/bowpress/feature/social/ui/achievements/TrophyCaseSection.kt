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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppStone
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
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
)

private fun TrophyCategory.displayName(): String = when (this) {
    TrophyCategory.skill -> "Skill"
    TrophyCategory.milestone -> "Milestone"
    TrophyCategory.streak -> "Streak"
    TrophyCategory.exploration -> "Exploration"
}

/**
 * Collectible trophy case (§18) — renders **all 12 trophies from [catalog]**,
 * each either earned (full-colour, showing tier/value + sublabel) or locked
 * (dimmed, showing the trophy description as a how-to-earn hint). Trophies are
 * grouped by [TrophyCategory] in the order: skill → milestone → streak →
 * exploration.
 *
 * When [catalog] is empty (catalogue still loading or unavailable) the section
 * falls back to showing only the earned [achievements] in the old flat list so
 * the screen never blanks. When both are empty the original empty-state card is
 * shown.
 *
 * The "N of 12 collected" counter in the header counts *distinct* earned kinds
 * so earning the same kind multiple times only counts once.
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
    // Index earned achievements by their kind (wire string) so lookups are O(1).
    val earnedByKind: Map<String, Achievement> = achievements
        .groupBy { it.kind.name }
        .mapValues { (_, list) -> list.maxByOrNull { it.createdAt }!! }

    val earnedKindCount = earnedByKind.size
    val catalogSize = catalog.size

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
            if (catalogSize > 0) {
                Text(
                    "$earnedKindCount of $catalogSize collected",
                    style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                    color = AppInk3,
                )
            } else if (achievements.isNotEmpty()) {
                // Fallback: catalogue not yet loaded — just show raw count.
                Text(
                    "${achievements.size} earned",
                    style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                    color = AppInk3,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = AppLine, thickness = 1.dp)
        Spacer(Modifier.height(8.dp))

        when {
            // ── Full collectible case — catalogue available ───────────────
            catalog.isNotEmpty() -> {
                val byCategory = catalog.groupBy { it.category }
                CATEGORY_ORDER.forEach { category ->
                    val defs = byCategory[category] ?: return@forEach
                    // Category group header
                    Text(
                        text = category.displayName().uppercase(),
                        style = interUI(8.sp, FontWeight.SemiBold).copy(letterSpacing = 0.28.em),
                        color = AppInk3,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    defs.forEachIndexed { index, def ->
                        if (index > 0) Spacer(Modifier.height(4.dp))
                        val earned = earnedByKind[def.kind]
                        TrophySlotRow(
                            def = def,
                            earned = earned,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }

            // ── Fallback: catalogue not loaded — show earned list only ────
            achievements.isNotEmpty() -> {
                achievements.forEachIndexed { index, achievement ->
                    if (index > 0) Spacer(Modifier.height(6.dp))
                    AchievementRow(achievement = achievement, modifier = Modifier.fillMaxWidth())
                }
            }

            // ── Empty state ───────────────────────────────────────────────
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AppLine)
                        .background(AppPaper2)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("No achievements yet.", style = frauncesDisplay(15.sp), color = AppInk)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "$ownerLabel a PR, milestone, or streak to fill the case.",
                        style = frauncesDisplay(12.sp, italic = true),
                        color = AppInk3,
                    )
                }
            }
        }
    }
}

/**
 * A single collectible slot — full-colour with earned value when [earned] is
 * non-null, dimmed with the description hint when locked.
 */
@Composable
private fun TrophySlotRow(
    def: TrophyDef,
    earned: Achievement?,
    modifier: Modifier = Modifier,
) {
    val isEarned = earned != null
    // Try to resolve glyph + stamp from the enum; fall back gracefully for
    // unknown future kinds so the slot still renders.
    val kindEnum = runCatching { AchievementKind.valueOf(def.kind) }.getOrNull()

    Row(
        modifier = modifier
            .then(
                if (isEarned) {
                    Modifier
                        .border(1.dp, AppMaple)
                        .background(AppPaper)
                } else {
                    Modifier
                        .border(1.dp, AppLine)
                        .background(AppPaper2)
                        .alpha(0.65f)
                }
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Glyph
        val glyph = kindEnum?.glyph() ?: "○"
        Text(
            text = glyph,
            style = frauncesDisplay(20.sp),
            color = if (isEarned) AppMaple else AppStone,
        )
        Spacer(Modifier.width(12.dp))

        // Name + sublabel or hint
        Column(Modifier.weight(1f)) {
            Text(
                text = if (isEarned) (earned!!.label) else def.name,
                style = frauncesDisplay(14.sp),
                color = if (isEarned) AppMaple else AppInk3,
            )
            val sub = if (isEarned) earned!!.sublabel else def.description
            sub?.let {
                Text(
                    text = it,
                    style = jetbrainsMono(9.sp),
                    color = AppInk3,
                )
            }
        }

        // Stamp (earned) or lock indicator (locked)
        if (isEarned) {
            val stamp = kindEnum?.stamp() ?: "EARNED"
            Text(
                text = stamp,
                style = interUI(8.sp, FontWeight.SemiBold).copy(letterSpacing = 0.2.em),
                color = AppMaple,
            )
        } else {
            Text(
                text = "LOCKED",
                style = interUI(8.sp, FontWeight.SemiBold).copy(letterSpacing = 0.2.em),
                color = AppStone,
            )
        }
    }
}
