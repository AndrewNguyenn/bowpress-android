package com.andrewnguyen.bowpress.feature.equipment.sightmarks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.AppCream
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine2
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.DistanceUnit
import com.andrewnguyen.bowpress.core.model.SightMark
import com.andrewnguyen.bowpress.core.model.SightMarkSuggester

/**
 * Per-bow sight-marks list. Mirrors iOS `SightMarksListView`:
 *   - Calibration-status card at the top (chart placeholder + guidance)
 *   - "Marks" section listing each mark in distance order
 *   - Plus-button in the top bar opens the edit sheet
 *
 * iOS shows a quadratic-fit chart; the chart is deferred to a follow-up
 * (complex to port + the suggester math drives the actual user value).
 * The guidance panel surfaces what's needed to unlock suggestions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SightMarksListScreen(
    onBack: () -> Unit,
    userId: String,
    viewModel: SightMarksViewModel = hiltViewModel(),
) {
    val marks by viewModel.marks.collectAsStateWithLifecycle()
    val measured by viewModel.measuredMarks.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<SightMark?>(null) }
    var adding by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AppPaper2,
        contentColor = AppInk,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppPaper2,
                    titleContentColor = AppInk,
                    navigationIconContentColor = AppInk,
                    actionIconContentColor = AppInk,
                ),
                title = { /* large title is rendered inside body */ },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Surface(
                            shape = CircleShape,
                            color = AppCream,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = AppPondDk,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { adding = true },
                        modifier = Modifier.testTag("sight_marks_add_button"),
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add sight mark",
                            tint = AppPondDk,
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = "Sight Marks",
                    style = frauncesDisplay(28.sp, italic = true, weight = FontWeight.Bold)
                        .copy(color = AppInk),
                )
            }
            item {
                CalibrationCard(measuredCount = measured.size)
            }
            item {
                Text(
                    text = "MARKS · ${marks.size}",
                    style = jetbrainsMono(11.sp).copy(letterSpacing = 0.22.em, color = AppInk3),
                )
            }
            if (marks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppCream)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No marks yet — tap + to add your first one.",
                            style = frauncesDisplay(14.sp, italic = true).copy(color = AppInk3),
                        )
                    }
                }
            } else {
                items(marks, key = { it.id }) { mark ->
                    SightMarkRow(
                        mark = mark,
                        onClick = { editing = mark },
                        onDelete = { viewModel.delete(mark.id) },
                    )
                }
            }
        }
    }

    if (adding) {
        SightMarkEditSheet(
            initial = null,
            onSave = { d, u, m, note ->
                viewModel.add(distance = d, unit = u, mark = m, note = note, userId = userId)
                adding = false
            },
            onDelete = null,
            onDismiss = { adding = false },
        )
    }
    editing?.let { target ->
        SightMarkEditSheet(
            initial = target,
            onSave = { d, u, m, note ->
                viewModel.update(
                    target.copy(distance = d, distanceUnit = u, mark = m, note = note),
                )
                editing = null
            },
            onDelete = {
                viewModel.delete(target.id)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun CalibrationCard(measuredCount: Int) {
    val (eyebrow, body) = when {
        measuredCount >= SightMarkSuggester.MIN_MARK_COUNT ->
            "CALIBRATED" to "Tap a distance below to see the suggested mark from your quadratic fit. Outliers stand out — re-shoot any mark that looks off."
        measuredCount > 0 ->
            "NEEDS ${SightMarkSuggester.MIN_MARK_COUNT - measuredCount} MORE" to "Add at least 3 measured marks spanning 20+ yards to unlock suggestions."
        else ->
            "BLANK" to "Add your first measured mark — distance + sight reading. Three marks across a 20-yard spread unlocks the quadratic suggester."
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppCream)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = eyebrow,
            style = jetbrainsMono(11.sp).copy(letterSpacing = 0.22.em, color = AppPondDk),
        )
        Text(
            text = body,
            style = frauncesDisplay(15.sp, italic = true).copy(color = AppInk2),
        )
    }
}

@Composable
private fun SightMarkRow(
    mark: SightMark,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AppCream)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("sight_mark_row_${mark.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = "${"%g".format(mark.distance)} ${mark.distanceUnit.shortLabel}",
                style = frauncesDisplay(18.sp, italic = true, weight = FontWeight.Medium)
                    .copy(color = AppInk),
            )
            if (mark.note != null) {
                Text(
                    text = mark.note!!,
                    style = frauncesDisplay(12.sp, italic = true).copy(color = AppInk3),
                )
            }
            if (mark.isSuggestion) {
                Text(
                    text = "SUGGESTED",
                    style = jetbrainsMono(9.sp).copy(letterSpacing = 0.22.em, color = AppPondDk),
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "%.2f".format(mark.mark),
                style = frauncesDisplay(22.sp, italic = true, weight = FontWeight.Medium)
                    .copy(color = AppPondDk),
            )
        }
    }
}
