package com.andrewnguyen.bowpress.feature.analytics.sessiondetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.BowPressColors
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import androidx.compose.ui.unit.em

/**
 * Session detail scaffold. iOS `SessionDetailSheet` is a 500+ line view with a
 * shot-distribution heatmap, end selector, group-stat chips, X·10 ring detail,
 * and replot. This iter stages the route + top bar + "Shot distribution"
 * header + the iOS-matching "All N arrows · M ends" caption so future iters
 * can land the heatmap on top of real data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: String,
    onBack: () -> Unit,
    viewModel: SessionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { /* future: edit metadata */ }) {
                        Text("Edit")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppPaper),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Shot distribution",
                    style = frauncesDisplay(22.sp, italic = true).copy(color = AppInk),
                )
                if (state.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator(color = BowPressColors.Accent) }
                } else {
                    // Mirrors iOS "All <n> arrows · <m> ends" caption.
                    val arrowLabel = if (state.arrowCount == 1) "1 arrow" else "${state.arrowCount} arrows"
                    val endLabel = if (state.endCount == 1) "1 end" else "${state.endCount} ends"
                    Text(
                        text = "All $arrowLabel · $endLabel",
                        style = jetbrainsMono(11.sp).copy(letterSpacing = 0.04.em, color = AppInk3),
                    )
                }
            }
            Box(
                modifier = Modifier.align(Alignment.Center),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Shot distribution heatmap coming soon",
                    style = MaterialTheme.typography.bodyMedium.copy(color = AppInk3),
                )
            }
        }
    }
}
