package com.andrewnguyen.bowpress.feature.social.ui.leagues

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppLine2
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPine
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.Division
import com.andrewnguyen.bowpress.core.model.HandicapConfig
import com.andrewnguyen.bowpress.core.model.HandicapEquation
import com.andrewnguyen.bowpress.core.model.LeagueEntryRule
import com.andrewnguyen.bowpress.core.model.LeagueSchedule
import com.andrewnguyen.bowpress.core.model.LeagueScheduleKind
import com.andrewnguyen.bowpress.core.model.LeagueType
import com.andrewnguyen.bowpress.feature.social.ui.label
import java.time.Instant
import java.time.temporal.ChronoUnit

@Composable
fun LeagueComposerScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    viewModel: LeagueViewModel = hiltViewModel(),
) {
    val state by viewModel.composerState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(top = 6.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "‹  Leagues",
                    style = interUI(10.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.32.em),
                    color = AppPondDk,
                    modifier = Modifier.clickable(onClick = onBack),
                )
                Text("New League", style = frauncesDisplay(28.sp), color = AppInk)
            }
        }
        HorizontalDivider(color = AppLine, thickness = 1.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(14.dp))

            // League name
            FieldHeader("LEAGUE NAME")
            BasicTextField(
                value = state.name,
                onValueChange = viewModel::updateComposerName,
                textStyle = frauncesDisplay(18.sp).copy(color = AppInk),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.SocialLeagueComposerName),
                decorationBox = { inner ->
                    if (state.name.isEmpty()) Text("League name", style = frauncesDisplay(18.sp), color = AppInk3)
                    inner()
                },
            )
            HorizontalDivider(color = AppLine, thickness = 1.dp, modifier = Modifier.padding(top = 8.dp))

            // Type: individual | team
            Spacer(Modifier.height(14.dp))
            FieldHeader("TYPE")
            ToggleRow(
                options = listOf("Individual" to LeagueType.individual, "Team" to LeagueType.team),
                selected = state.leagueType,
                onSelect = viewModel::updateComposerType,
            )

            // Divisions
            Spacer(Modifier.height(14.dp))
            FieldHeader("DIVISIONS")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Division.entries.forEach { div ->
                    val selected = div in state.divisions
                    Box(
                        modifier = Modifier
                            .border(1.dp, if (selected) AppPondDk else AppLine)
                            .background(if (selected) AppPondDk else AppPaper)
                            .clickable {
                                val new = if (selected) state.divisions - div else state.divisions + div
                                if (new.isNotEmpty()) viewModel.updateComposerDivisions(new)
                            }
                            .padding(10.dp, 6.dp),
                    ) {
                        Text(
                            div.label(),
                            style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                            color = if (selected) AppPaper else AppInk3,
                        )
                    }
                }
            }

            // Round spec
            Spacer(Modifier.height(14.dp))
            FieldHeader("ROUND")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("ENDS", style = interUI(8.sp).copy(letterSpacing = 0.2.em), color = AppInk3)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(6, 10, 12, 20).forEach { n ->
                            val sel = state.endCount == n
                            Box(
                                modifier = Modifier
                                    .border(1.dp, if (sel) AppPondDk else AppLine)
                                    .background(if (sel) AppPondDk else AppPaper)
                                    .clickable { viewModel.updateComposerEndCount(n) }
                                    .padding(8.dp, 4.dp),
                            ) {
                                Text("$n", style = jetbrainsMono(11.sp), color = if (sel) AppPaper else AppInk3)
                            }
                        }
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("ARROWS/END", style = interUI(8.sp).copy(letterSpacing = 0.2.em), color = AppInk3)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3, 6).forEach { n ->
                            val sel = state.arrowsPerEnd == n
                            Box(
                                modifier = Modifier
                                    .border(1.dp, if (sel) AppPondDk else AppLine)
                                    .background(if (sel) AppPondDk else AppPaper)
                                    .clickable { viewModel.updateComposerArrowsPerEnd(n) }
                                    .padding(8.dp, 4.dp),
                            ) {
                                Text("$n", style = jetbrainsMono(11.sp), color = if (sel) AppPaper else AppInk3)
                            }
                        }
                    }
                }
            }

            // Schedule
            Spacer(Modifier.height(14.dp))
            FieldHeader("SCHEDULE")
            ToggleRow(
                options = listOf("Single" to LeagueScheduleKind.single, "Weekly" to LeagueScheduleKind.weekly),
                selected = state.scheduleKind,
                onSelect = viewModel::updateComposerScheduleKind,
            )
            if (state.scheduleKind == LeagueScheduleKind.weekly) {
                Spacer(Modifier.height(8.dp))
                Text("WEEKS", style = interUI(8.sp).copy(letterSpacing = 0.2.em), color = AppInk3)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(4, 6, 8, 10, 12).forEach { n ->
                        val sel = state.totalWeeks == n
                        Box(
                            modifier = Modifier
                                .border(1.dp, if (sel) AppPondDk else AppLine)
                                .background(if (sel) AppPondDk else AppPaper)
                                .clickable { viewModel.updateComposerTotalWeeks(n) }
                                .padding(8.dp, 4.dp),
                        ) {
                            Text("$n", style = jetbrainsMono(11.sp), color = if (sel) AppPaper else AppInk3)
                        }
                    }
                }
            }

            // Handicap
            Spacer(Modifier.height(14.dp))
            FieldHeader("HANDICAP")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HandicapEquation.entries.forEach { eq ->
                    val sel = state.handicapConfig.equation == eq
                    Box(
                        modifier = Modifier
                            .border(1.dp, if (sel) AppPondDk else AppLine)
                            .background(if (sel) AppPondDk else AppPaper)
                            .clickable {
                                viewModel.updateComposerHandicap(HandicapConfig(eq, state.handicapConfig.allowancePct, state.handicapConfig.setupWeeks))
                            }
                            .padding(8.dp, 5.dp),
                    ) {
                        Text(
                            eq.name.uppercase(),
                            style = interUI(8.sp, FontWeight.SemiBold).copy(letterSpacing = 0.2.em),
                            color = if (sel) AppPaper else AppInk3,
                        )
                    }
                }
            }
            if (state.handicapConfig.equation == HandicapEquation.allowance) {
                Spacer(Modifier.height(8.dp))
                Text("ALLOWANCE %", style = interUI(8.sp).copy(letterSpacing = 0.2.em), color = AppInk3)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.6, 0.8, 1.0).forEach { pct ->
                        val sel = state.handicapConfig.allowancePct == pct
                        Box(
                            modifier = Modifier
                                .border(1.dp, if (sel) AppPondDk else AppLine)
                                .background(if (sel) AppPondDk else AppPaper)
                                .clickable {
                                    viewModel.updateComposerHandicap(state.handicapConfig.copy(allowancePct = pct))
                                }
                                .padding(8.dp, 4.dp),
                        ) {
                            Text("${(pct * 100).toInt()}%", style = jetbrainsMono(11.sp), color = if (sel) AppPaper else AppInk3)
                        }
                    }
                }
            }

            // Entry rule
            Spacer(Modifier.height(14.dp))
            FieldHeader("ENTRY RULE")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(LeagueEntryRule.open, LeagueEntryRule.`invite-only`).forEach { rule ->
                    val sel = state.entryRule == rule
                    Box(
                        modifier = Modifier
                            .border(1.dp, if (sel) AppPondDk else AppLine)
                            .background(if (sel) AppPondDk else AppPaper)
                            .clickable { viewModel.updateComposerEntryRule(rule) }
                            .padding(8.dp, 5.dp),
                    ) {
                        Text(
                            rule.name.uppercase().replace("-", " "),
                            style = interUI(8.sp, FontWeight.SemiBold).copy(letterSpacing = 0.18.em),
                            color = if (sel) AppPaper else AppInk3,
                        )
                    }
                }
            }

            state.error?.let { err ->
                Spacer(Modifier.height(10.dp))
                Text(err, style = jetbrainsMono(10.sp), color = AppMaple)
            }

            // Create CTA
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppPondDk)
                    .clickable {
                        if (state.name.isNotBlank() && !state.isSaving) {
                            val now = Instant.now()
                            val schedule = LeagueSchedule(
                                kind = state.scheduleKind,
                                startsAt = now,
                                endsAt = now.plus(
                                    (if (state.scheduleKind == LeagueScheduleKind.weekly) state.totalWeeks * 7L else 30L),
                                    ChronoUnit.DAYS,
                                ),
                                totalWeeks = if (state.scheduleKind == LeagueScheduleKind.weekly) state.totalWeeks else null,
                                weekDeadlineDow = if (state.scheduleKind == LeagueScheduleKind.weekly) 0 else null,
                            )
                            viewModel.createLeague(schedule) { league -> onCreated(league.id) }
                        }
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Create League", style = frauncesDisplay(18.sp).copy(color = AppPaper))
                    if (state.isSaving) {
                        Text("Saving…", style = interUI(9.sp).copy(color = AppPaper.copy(alpha = 0.7f)))
                    }
                }
                Text("›", style = frauncesDisplay(30.sp).copy(color = AppPaper))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FieldHeader(label: String) {
    Text(label, style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em), color = AppInk3)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun <T> ToggleRow(options: List<Pair<String, T>>, selected: T, onSelect: (T) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppLine)
            .background(AppPaper2),
    ) {
        options.forEach { (label, value) ->
            val sel = selected == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (sel) AppPaper else AppPaper2)
                    .clickable { onSelect(value) }
                    .padding(8.dp, 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label.uppercase(),
                    style = interUI(8.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.2.em),
                    color = if (sel) AppPondDk else AppInk3,
                )
            }
        }
    }
}
