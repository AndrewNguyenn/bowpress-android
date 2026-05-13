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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay

/**
 * Session detail scaffold. iOS `SessionDetailSheet` is a 500+ line view with a
 * shot-distribution heatmap, end selector, group-stat chips, X·10 ring detail,
 * and replot. This iter only stages the route + top bar + "Shot distribution"
 * header so future iters can land sub-features (heatmap first, then stats,
 * then replot) without simultaneously wiring nav.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: String,
    onBack: () -> Unit,
) {
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Shot distribution",
                    style = frauncesDisplay(22.sp, italic = true).copy(color = AppInk),
                )
                // Placeholder while the heatmap port lands. Surfaces the
                // session id so the route argument is observably wired.
                Text(
                    text = "Session: $sessionId",
                    style = MaterialTheme.typography.bodySmall.copy(color = AppInk3),
                )
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
