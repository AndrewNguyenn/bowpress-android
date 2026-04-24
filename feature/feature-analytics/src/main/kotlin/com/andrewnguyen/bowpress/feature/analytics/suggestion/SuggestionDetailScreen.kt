package com.andrewnguyen.bowpress.feature.analytics.suggestion

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppLine2
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppMoss
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPine
import com.andrewnguyen.bowpress.core.designsystem.AppPond
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.BowPressTheme
import com.andrewnguyen.bowpress.core.designsystem.bp.BPCard
import com.andrewnguyen.bowpress.core.designsystem.bp.BPEditLink
import com.andrewnguyen.bowpress.core.designsystem.bp.BPEyebrow
import com.andrewnguyen.bowpress.core.designsystem.bp.BPHairlineButton
import com.andrewnguyen.bowpress.core.designsystem.bp.BPNavHeader
import com.andrewnguyen.bowpress.core.designsystem.bp.BPPrimaryButton
import com.andrewnguyen.bowpress.core.designsystem.bp.BPSectionTitle
import com.andrewnguyen.bowpress.core.designsystem.bp.BPStamp
import com.andrewnguyen.bowpress.core.designsystem.bp.BPStampTone
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.AnalyticsSuggestion
import com.andrewnguyen.bowpress.core.model.DeliveryType
import com.andrewnguyen.bowpress.core.model.SuggestionEvidence
import com.andrewnguyen.bowpress.core.model.SuggestionStatusStamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

// TODO(units): suggestion.currentValue / suggestedValue arrive from the server as
// free-form imperial strings ("+3/16\"", "+0.5 turns"). They do not honor the
// client unit toggle yet; revisit once the API emits structured {value, unit} payloads.
@Composable
fun SuggestionDetailScreen(
    onBack: () -> Unit,
    onAppliedConfig: (bowId: String) -> Unit,
    viewModel: SuggestionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        // Flip `wasRead` the first time the user opens the screen.
        viewModel.markRead()
        viewModel.events.collect { evt ->
            when (evt) {
                is SuggestionDetailEvent.Applied -> onAppliedConfig(evt.bowId)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper),
    ) {
        when {
            state.isLoading -> {
                Text(
                    text = "Loading…",
                    style = interUI(11.sp).copy(color = AppInk3),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(20.dp),
                )
            }
            state.suggestion == null -> {
                Text(
                    text = state.error ?: "Suggestion unavailable",
                    style = interUI(13.sp).copy(color = AppInk2),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(20.dp),
                )
            }
            else -> SuggestionDetailBody(
                suggestion = state.suggestion!!,
                isApplying = state.isApplying,
                error = state.error,
                onBack = onBack,
                onApply = viewModel::apply,
                onDismiss = viewModel::dismiss,
                onMarkRead = viewModel::markRead,
            )
        }
    }
}

@Composable
private fun SuggestionDetailBody(
    suggestion: AnalyticsSuggestion,
    isApplying: Boolean,
    error: String?,
    onBack: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    onMarkRead: () -> Unit,
) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize()) {
        BPNavHeader(
            eyebrow = "Bowpress",
            title = "Adjustment",
            meta = { BPEditLink(label = "Close", onClick = onBack) },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scroll)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            HeaderSection(suggestion = suggestion)
            ReasoningSection(suggestion = suggestion)
            suggestion.evidence?.let { EvidenceSection(evidence = it) }

            if (error != null) {
                Text(
                    text = error,
                    style = interUI(11.sp).copy(color = AppMaple),
                )
            }

            Spacer(Modifier.height(4.dp))
        }
        ActionFooter(
            suggestion = suggestion,
            isApplying = isApplying,
            onApply = onApply,
            onDismiss = onDismiss,
            onMarkRead = onMarkRead,
        )
    }
}

// ---------- Header ----------

@Composable
private fun HeaderSection(suggestion: AnalyticsSuggestion) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BPEyebrow(text = "Parameter")
                Text(
                    text = bowParameterDisplayName(suggestion.parameter),
                    style = frauncesDisplay(28.sp, italic = true).copy(color = AppInk),
                )
            }
            val stampText = resolvedStatusStamp(suggestion)
            BPStamp(text = stampText.uppercase(), tone = stampTone(stampText))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            ValueBlock(
                label = "Current",
                value = suggestion.currentValue.ifBlank { "—" },
                modifier = Modifier.weight(1f),
                tone = AppInk,
            )
            Text(
                text = "→",
                style = frauncesDisplay(22.sp, italic = true).copy(color = AppMoss),
            )
            ValueBlock(
                label = "Suggested",
                value = suggestion.suggestedValue.ifBlank { "—" },
                modifier = Modifier.weight(1f),
                tone = AppPondDk,
            )
        }

        ConfidenceRow(confidence = suggestion.confidence)

        if (suggestion.wasApplied) {
            Row(
                modifier = Modifier
                    .background(AppPine.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "✓",
                    style = frauncesDisplay(14.sp, italic = true).copy(color = AppPine),
                )
                Spacer(Modifier.width(6.dp))
                val label = suggestion.appliedAt?.let {
                    val fmt = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
                        .withZone(ZoneId.systemDefault())
                    "Applied ${fmt.format(it)}"
                } ?: "Applied"
                Text(
                    text = label.uppercase(),
                    style = interUI(9.sp, weight = FontWeight.SemiBold).copy(
                        letterSpacing = 0.22.em,
                        color = AppPine,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ValueBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tone: Color,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BPEyebrow(text = label)
        Text(
            text = value,
            style = frauncesDisplay(22.sp, italic = true).copy(color = tone),
        )
    }
}

@Composable
private fun ConfidenceRow(confidence: Double) {
    val pct = (confidence * 100).roundToInt()
    val pctFraction = confidence.coerceIn(0.0, 1.0).toFloat()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BPEyebrow(text = "Confidence")
            Spacer(Modifier.weight(1f))
            Text(
                text = "$pct%",
                style = jetbrainsMono(11.sp, weight = FontWeight.Medium).copy(color = AppInk),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(AppLine),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pctFraction)
                    .height(3.dp)
                    .background(AppPond),
            )
        }
    }
}

// ---------- Reasoning ----------

@Composable
private fun ReasoningSection(suggestion: AnalyticsSuggestion) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BPSectionTitle(title = "Why")
        Text(
            text = suggestion.reasoning,
            style = interUI(13.sp).copy(color = AppInk2),
        )
        suggestion.qualifier?.takeIf { it.isNotBlank() }?.let { q ->
            Text(
                text = q,
                style = interUI(11.sp).copy(color = AppInk3),
            )
        }
    }
}

// ---------- Evidence ----------

@Composable
private fun EvidenceSection(evidence: SuggestionEvidence) {
    val dateFmt = remember(evidence.windowStart, evidence.windowEnd) {
        DateTimeFormatter.ofPattern("MMM d", Locale.US).withZone(ZoneId.systemDefault())
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BPSectionTitle(title = "Evidence", aside = "${evidence.sampleSize} arrows")
        val windowLabel = "${dateFmt.format(evidence.windowStart)} — ${dateFmt.format(evidence.windowEnd)}"
        val preamble = buildAnnotatedString {
            withStyle(SpanStyle(color = AppInk2)) {
                append("Based on ${evidence.sampleSize} arrows across ${evidence.sessionIds.size} sessions · ")
            }
            withStyle(SpanStyle(color = AppInk, fontWeight = FontWeight.Medium)) {
                append(windowLabel)
            }
        }
        Text(
            text = preamble,
            style = interUI(11.5.sp),
        )
        if (evidence.metrics.isNotEmpty()) {
            BPCard(inset = true) {
                evidence.metrics.forEachIndexed { idx, metric ->
                    MetricRow(metric = metric)
                    if (idx < evidence.metrics.size - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(AppLine2),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricRow(metric: SuggestionEvidence.Metric) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = metric.label,
            style = interUI(12.sp).copy(color = AppInk2),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = metric.value,
            style = frauncesDisplay(14.sp, italic = true).copy(color = AppInk),
        )
        metric.deltaFromBaseline?.takeIf { it.isNotBlank() }?.let { delta ->
            Spacer(Modifier.width(8.dp))
            val fg = deltaColor(delta)
            val bg = when {
                delta.startsWith("+") -> AppPine.copy(alpha = 0.16f)
                delta.startsWith("-") -> AppMaple.copy(alpha = 0.12f)
                else -> Color.Transparent
            }
            Text(
                text = delta,
                style = jetbrainsMono(10.sp).copy(color = fg),
                modifier = Modifier
                    .background(bg)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            )
        }
    }
}

private fun deltaColor(delta: String): Color = when {
    delta.startsWith("+") -> AppPine
    delta.startsWith("-") -> AppMaple
    else -> AppInk3
}

// ---------- Footer actions ----------

@Composable
private fun ActionFooter(
    suggestion: AnalyticsSuggestion,
    isApplying: Boolean,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    onMarkRead: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppPaper)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppLine))
        Spacer(Modifier.height(2.dp))
        val title = when {
            isApplying -> "Applying…"
            suggestion.wasApplied -> "Already applied"
            else -> "Apply"
        }
        val subtitle = when {
            isApplying -> "writing to your bow config"
            suggestion.wasApplied -> "your config is up-to-date"
            else -> "press to create a new bow config"
        }
        BPPrimaryButton(
            title = title,
            subtitle = subtitle,
            enabled = !isApplying && !suggestion.wasApplied,
            onClick = onApply,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BPHairlineButton(
                label = if (suggestion.wasDismissed) "Dismissed" else "Dismiss",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                borderTone = if (suggestion.wasDismissed) AppLine else AppMaple,
                labelTone = if (suggestion.wasDismissed) AppInk3 else AppMaple,
            )
            BPHairlineButton(
                label = if (suggestion.wasRead) "Read" else "Mark read",
                onClick = onMarkRead,
                modifier = Modifier.weight(1f),
                borderTone = if (suggestion.wasRead) AppLine else AppPondDk,
                labelTone = if (suggestion.wasRead) AppInk3 else AppPondDk,
            )
        }
    }
}

// ---------- Helpers ----------

private fun resolvedStatusStamp(suggestion: AnalyticsSuggestion): String {
    suggestion.statusStamp?.let {
        return when (it) {
            SuggestionStatusStamp.NEW -> "New"
            SuggestionStatusStamp.PROPOSED -> "Proposed"
            SuggestionStatusStamp.GOOD -> "Good"
            SuggestionStatusStamp.REVIEW -> "Review"
        }
    }
    return when {
        suggestion.wasApplied -> "Applied"
        suggestion.confidence >= 0.85 -> "Good"
        suggestion.confidence < 0.6 -> "Review"
        else -> "Proposed"
    }
}

private fun stampTone(stamp: String): BPStampTone = when (stamp.lowercase(Locale.US)) {
    "new", "proposed", "applied" -> BPStampTone.Pond
    "good" -> BPStampTone.Pine
    "review", "dismissed" -> BPStampTone.Maple
    else -> BPStampTone.Pond
}

private fun bowParameterDisplayName(raw: String): String {
    val map = mapOf(
        "drawLength" to "Draw Length",
        "letOffPct" to "Let-Off %",
        "peepHeight" to "Peep Height",
        "dLoopLength" to "D-Loop Length",
        "topCableTwists" to "Top Cable Twists",
        "bottomCableTwists" to "Bottom Cable Twists",
        "mainStringTopTwists" to "Main String Top Twists",
        "mainStringBottomTwists" to "Main String Bottom Twists",
        "topLimbTurns" to "Top Limb Turns",
        "bottomLimbTurns" to "Bottom Limb Turns",
        "restVertical" to "Rest Vertical",
        "restHorizontal" to "Rest Horizontal",
        "restDepth" to "Rest Depth",
        "sightPosition" to "Sight Position",
        "gripAngle" to "Grip Angle",
        "nockingHeight" to "Nocking Height",
    )
    map[raw]?.let { return it }
    val sb = StringBuilder()
    raw.forEachIndexed { idx, c ->
        if (idx > 0 && c.isUpperCase()) sb.append(' ')
        sb.append(c)
    }
    return sb.toString().replaceFirstChar { it.uppercase() }
}


// ---------- Previews ----------

@Preview(showBackground = true)
@Composable
private fun SuggestionDetailPreview() {
    BowPressTheme {
        SuggestionDetailBody(
            suggestion = previewSuggestion,
            isApplying = false,
            error = null,
            onBack = {},
            onApply = {},
            onDismiss = {},
            onMarkRead = {},
        )
    }
}

private val previewSuggestion = AnalyticsSuggestion(
    id = "preview",
    bowId = "bow1",
    createdAt = Instant.now(),
    parameter = "restVertical",
    suggestedValue = "+3/16\"",
    currentValue = "+2/16\"",
    reasoning = "Vertical impact bias detected across last 3 sessions.",
    confidence = 0.82,
    qualifier = "Re-verify after 2 sessions.",
    wasRead = false,
    wasDismissed = false,
    deliveryType = DeliveryType.PUSH,
    evidence = SuggestionEvidence(
        sampleSize = 47,
        sessionIds = listOf("s1", "s2", "s3"),
        windowStart = LocalDate.now().minusDays(14).atStartOfDay(ZoneId.systemDefault()).toInstant(),
        windowEnd = Instant.now(),
        metrics = listOf(
            SuggestionEvidence.Metric("Average score", "10.5", "+0.4"),
            SuggestionEvidence.Metric("Vertical drift", "0.09 in", "+0.06 in"),
        ),
        patternType = "directional_drift",
    ),
)
