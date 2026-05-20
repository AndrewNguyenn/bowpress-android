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

/**
 * Trophy-case section (§15) — an eyebrow header + the achievement rows, or an
 * empty state. Drop into a scrolling Column on the Friend profile / You
 * screen. [ownerLabel] tailors the empty-state copy ("You haven't" vs
 * "They haven't").
 */
@Composable
fun TrophyCaseSection(
    achievements: List<Achievement>,
    ownerLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
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

        if (achievements.isEmpty()) {
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
        } else {
            achievements.forEachIndexed { index, achievement ->
                if (index > 0) Spacer(Modifier.height(6.dp))
                AchievementRow(achievement = achievement, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
