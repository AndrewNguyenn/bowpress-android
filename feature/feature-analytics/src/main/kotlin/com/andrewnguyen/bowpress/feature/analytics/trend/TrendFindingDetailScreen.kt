package com.andrewnguyen.bowpress.feature.analytics.trend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPine
import com.andrewnguyen.bowpress.core.designsystem.AppPond
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.AppStone
import com.andrewnguyen.bowpress.core.designsystem.bp.BPEyebrow
import com.andrewnguyen.bowpress.core.designsystem.bp.BPStamp
import com.andrewnguyen.bowpress.core.designsystem.bp.BPStampTone
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.TrendBadge
import com.andrewnguyen.bowpress.core.model.TrendFinding
import com.andrewnguyen.bowpress.core.model.TrendTone

/**
 * Detail screen pushed when the user taps a row in the Trend analysis ledger
 * on the analytics dashboard. Mirrors iOS `TrendFindingDetailView`:
 *  - header (eyebrow finding-N + title + metric tag) and a divider hairline
 *  - body paragraph rendered with extra line-spacing
 *  - explainer keyed to the badge (Gain / Watch / Hold)
 *  - cues breakdown with bullet points and **bold** parser
 *  - footnote with finding rank
 *
 * The finding payload is small, so it's passed via the analytics VM rather
 * than a refetch — the screen is driven entirely by the [finding] arg.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendFindingDetailScreen(
    finding: TrendFinding,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = AppPaper,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppInk,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppPaper),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 18.dp,
                vertical = 18.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item { Header(finding = finding) }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(AppLine),
                )
            }
            item { BodyParagraph(text = finding.body) }
            item { BadgeExplainer(badge = finding.badge) }
            if (!finding.cues.isNullOrEmpty()) {
                item { CuesBreakdown(raw = finding.cues!!) }
            }
            item { Footnote(index = finding.index) }
        }
    }
}

@Composable
private fun Header(finding: TrendFinding) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "${roman(finding.index)} · finding",
                style = frauncesDisplay(13.sp, italic = true).copy(
                    color = AppPond,
                    letterSpacing = 0.02.em,
                ),
            )
            Text(
                text = finding.title,
                style = frauncesDisplay(22.sp, italic = true).copy(color = AppInk),
            )
            Text(
                text = finding.metric.text,
                style = jetbrainsMono(13.sp, weight = FontWeight.Medium).copy(
                    color = metricTone(finding.metric.tone),
                ),
            )
        }
        BPStamp(
            text = finding.badge.label(),
            tone = finding.badge.stampTone(),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun BodyParagraph(text: String) {
    Text(
        text = text,
        style = interUI(13.5.sp).copy(
            color = AppInk2,
            lineHeight = 20.sp,
        ),
    )
}

@Composable
private fun BadgeExplainer(badge: TrendBadge) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BPEyebrow(text = "WHAT THIS MEANS", tone = badge.eyebrowColor())
        Text(
            text = badge.explainerCopy(),
            style = interUI(12.5.sp).copy(
                color = AppInk2,
                lineHeight = 18.sp,
            ),
        )
    }
}

@Composable
private fun CuesBreakdown(raw: String) {
    val parts = raw.split(" · ")
    val header = parts.firstOrNull() ?: return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BPEyebrow(text = header.uppercase())
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            parts.drop(1).forEach { segment ->
                CueRow(raw = segment)
            }
        }
    }
}

@Composable
private fun CueRow(raw: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(4.dp)
                .clip(CircleShape)
                .background(AppPond),
        )
        Text(
            text = parseBold(raw),
            style = jetbrainsMono(11.5.sp).copy(letterSpacing = 0.02.em),
        )
    }
}

@Composable
private fun Footnote(index: Int) {
    Spacer(Modifier.height(2.dp))
    Text(
        text = "finding #$index · ranked by actionability",
        style = interUI(10.sp).copy(color = AppInk3),
    )
}

private fun parseBold(s: String) = buildAnnotatedString {
    var remaining = s
    while (true) {
        val open = remaining.indexOf("**")
        if (open < 0) {
            withStyle(SpanStyle(color = AppInk2)) { append(remaining) }
            return@buildAnnotatedString
        }
        if (open > 0) {
            withStyle(SpanStyle(color = AppInk2)) { append(remaining.substring(0, open)) }
        }
        val afterOpen = remaining.substring(open + 2)
        val close = afterOpen.indexOf("**")
        if (close < 0) {
            withStyle(SpanStyle(color = AppInk2)) { append(afterOpen) }
            return@buildAnnotatedString
        }
        withStyle(SpanStyle(color = AppInk, fontWeight = FontWeight.Medium)) {
            append(afterOpen.substring(0, close))
        }
        remaining = afterOpen.substring(close + 2)
    }
}

private fun metricTone(tone: TrendTone): Color = when (tone) {
    TrendTone.POSITIVE -> AppPine
    TrendTone.NEGATIVE -> AppMaple
    TrendTone.NEUTRAL -> AppPondDk
}

private fun TrendBadge.label(): String = when (this) {
    TrendBadge.GAIN -> "GAIN"
    TrendBadge.WATCH -> "WATCH"
    TrendBadge.HOLD -> "HOLD"
}

private fun TrendBadge.stampTone(): BPStampTone = when (this) {
    TrendBadge.GAIN -> BPStampTone.Pine
    TrendBadge.WATCH -> BPStampTone.Maple
    TrendBadge.HOLD -> BPStampTone.Stone
}

private fun TrendBadge.eyebrowColor(): Color = when (this) {
    TrendBadge.GAIN -> AppPine
    TrendBadge.WATCH -> AppMaple
    TrendBadge.HOLD -> AppInk3
}

private fun TrendBadge.explainerCopy(): String = when (this) {
    TrendBadge.GAIN ->
        "A Gain finding flags something working well. Hold the current setup and " +
            "use this to understand what's contributing to the result so you can " +
            "replicate it."
    TrendBadge.WATCH ->
        "A Watch finding flags an emerging pattern worth acting on before it sets. " +
            "The cues below are the most common levers — start with the bolded ones."
    TrendBadge.HOLD ->
        "A Hold finding doesn't have enough signal yet. Keep shooting; the trend " +
            "will either firm up or fade with more sessions."
}

private fun roman(n: Int): String {
    val table = listOf("", "i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x", "xi", "xii")
    return if (n in table.indices) table[n] else n.toString()
}
