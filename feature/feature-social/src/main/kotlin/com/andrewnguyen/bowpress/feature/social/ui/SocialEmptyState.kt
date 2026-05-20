package com.andrewnguyen.bowpress.feature.social.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags

/**
 * A CTA button spec for [SocialEmptyState]. Each action is rendered as a
 * dashed-border ghost-button card inside the empty state, matching the iOS
 * `.ghost-btn` design.
 *
 * @param label     Uppercase bold label displayed on the primary line.
 * @param sublabel  Italic sub-line rendered beneath the label in mono.
 * @param onClick   Invoked when the user taps the button.
 * @param testTag   Optional test tag for the button container.
 */
data class EmptyAction(
    val label: String,
    val sublabel: String,
    val onClick: () -> Unit,
    val testTag: String = "",
)

/**
 * Reusable social-layer empty-state card.
 *
 * Matches the iOS first-run/quiet design: a dashed-border outer card, a
 * 54 dp square bordered icon box in the Kenrokuen pond colour, an italic
 * Fraunces title, an italic body in AppInk2, and zero or more ghost-button
 * CTA rows beneath.
 *
 * Two standard variants are composed on [FeedScreen]:
 *  - **New user** — welcoming card with "Add a friend" + "Find a club" CTAs.
 *  - **Quiet week** — softer notice with no CTAs.
 *
 * @param icon      Single glyph / short string rendered inside the icon box
 *                  (e.g. "◎").
 * @param title     Italic Fraunces title (e.g. "Your range, connected.").
 * @param message   Italic Fraunces body message.
 * @param actions   Zero or more [EmptyAction] items; each becomes a ghost-btn
 *                  row. Pass an empty list for the quiet-week variant.
 * @param modifier  Forwarded to the outer container.
 */
@Composable
fun SocialEmptyState(
    icon: String,
    title: String,
    message: String,
    actions: List<EmptyAction> = emptyList(),
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .dashedBorder(color = AppLine, strokeWidth = 1.dp, dashLength = 6.dp, gapLength = 4.dp)
            .background(AppPaper2)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 54 dp bordered icon box — mirrors iOS `.empty .icon` (54pt, 1pt border).
        Box(
            modifier = Modifier
                .size(54.dp)
                .dashedBorder(color = AppPondDk, strokeWidth = 1.dp, dashLength = 5.dp, gapLength = 3.dp)
                .background(AppPaper2),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = icon,
                style = frauncesDisplay(22.sp),
                color = AppPondDk,
            )
        }

        Spacer(Modifier.height(18.dp))

        // Italic Fraunces title — mirrors iOS `.empty h3`.
        Text(
            text = title,
            style = frauncesDisplay(18.sp, italic = true),
            color = AppInk,
        )

        Spacer(Modifier.height(10.dp))

        // Italic Fraunces body — mirrors iOS `.empty p`.
        Text(
            text = message,
            style = frauncesDisplay(13.sp, italic = true),
            color = AppInk2,
        )

        if (actions.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                actions.forEach { action ->
                    GhostButton(action = action)
                }
            }
        }
    }
}

/**
 * A single dashed-border ghost-button CTA row. Mirrors iOS `.ghost-btn`:
 * dashed outer border, uppercase bold Inter label + italic JetBrains Mono
 * sub-line.
 */
@Composable
private fun GhostButton(action: EmptyAction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .dashedBorder(color = AppPondDk, strokeWidth = 1.dp, dashLength = 5.dp, gapLength = 3.dp)
            .background(AppPaper2)
            .clickable(onClick = action.onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .then(if (action.testTag.isNotEmpty()) Modifier.testTag(action.testTag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = action.label.uppercase(),
                style = interUI(11.sp, FontWeight.Bold).copy(
                    letterSpacing = 0.22.em,
                ),
                color = AppPondDk,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = action.sublabel,
                style = frauncesDisplay(11.sp, italic = true),
                color = AppInk3,
            )
        }
        Text(
            text = "→",
            style = jetbrainsMono(12.sp),
            color = AppPondDk,
        )
    }
}

// ---------------------------------------------------------------------------
// Dashed-border modifier helper
// ---------------------------------------------------------------------------

/**
 * Draws a rectangular dashed border around the composable using [drawBehind].
 * Compose's built-in [Modifier.border] only supports solid strokes; dashes
 * require a custom [PathEffect] on the [Stroke] drawn in a Canvas pass.
 */
private fun Modifier.dashedBorder(
    color: Color,
    strokeWidth: Dp,
    dashLength: Dp,
    gapLength: Dp,
): Modifier = this.drawBehind {
    val strokePx = strokeWidth.toPx()
    val dashPx = dashLength.toPx()
    val gapPx = gapLength.toPx()
    val half = strokePx / 2f
    drawRoundRect(
        color = color,
        topLeft = androidx.compose.ui.geometry.Offset(half, half),
        size = androidx.compose.ui.geometry.Size(size.width - strokePx, size.height - strokePx),
        cornerRadius = CornerRadius.Zero,
        style = Stroke(
            width = strokePx,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, gapPx), 0f),
        ),
    )
}
