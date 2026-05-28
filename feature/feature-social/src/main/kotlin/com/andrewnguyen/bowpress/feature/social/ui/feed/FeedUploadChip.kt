package com.andrewnguyen.bowpress.feature.social.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPine
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.model.ExportJob
import com.andrewnguyen.bowpress.core.model.ExportJobState
import kotlin.math.roundToInt

/**
 * Optimistic upload-status chip overlaid on an own-session feed card while its
 * [ExportJob] is in flight. The Android counterpart to iOS `FeedUploadChip` —
 * advances live as `ExportJobWorker` pushes bytes (the feed observes the job
 * Room flow) and clears itself when the job lands.
 *
 * Returns nothing for a terminal-failed job (the partial-failure snackbar
 * already covers permanent gaps); every in-flight / just-landed state renders.
 */
@Composable
fun FeedUploadChip(job: ExportJob, modifier: Modifier = Modifier) {
    val label = labelFor(job) ?: return
    Row(
        modifier = modifier
            .background(AppPaper)
            .border(1.dp, AppLine)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (job.state) {
            ExportJobState.Ready -> Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = AppPine,
                modifier = Modifier.size(10.dp),
            )
            ExportJobState.Failed -> Unit
            else -> CircularProgressIndicator(
                modifier = Modifier.size(10.dp),
                strokeWidth = 1.5.dp,
                color = AppPondDk,
            )
        }
        Text(
            text = label,
            color = AppInk2,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.6.sp,
        )
    }
}

private fun labelFor(job: ExportJob): String? = when (job.state) {
    ExportJobState.Pending, ExportJobState.Uploading -> {
        val pct = (job.progress * 100).roundToInt()
        if (pct > 0) "UPLOADING $pct%" else "UPLOADING"
    }
    ExportJobState.Transcoding -> "TRANSCODING"
    ExportJobState.Ready -> "POSTED"
    ExportJobState.Failed -> null
}
