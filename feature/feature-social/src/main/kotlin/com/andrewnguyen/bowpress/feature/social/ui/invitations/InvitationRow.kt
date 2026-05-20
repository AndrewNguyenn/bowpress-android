package com.andrewnguyen.bowpress.feature.social.ui.invitations

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPine
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.AppStone
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.SocialInvitation
import com.andrewnguyen.bowpress.feature.social.ui.SocialAvatar
import com.andrewnguyen.bowpress.feature.social.ui.avatarInitials

/**
 * One pending club/league invitation row with Accept / Decline actions.
 * Shared by the Clubs and Leagues "Invites" sections so the affordance
 * reads identically across both surfaces.
 */
@Composable
fun InvitationRow(
    invitation: SocialInvitation,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SocialAvatar(initials = avatarInitials(invitation.inviterHandle), size = 32)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(invitation.targetName, style = frauncesDisplay(14.sp), color = AppInk)
            Text(
                "invited by @${invitation.inviterHandle}",
                style = jetbrainsMono(9.5.sp),
                color = AppInk3,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .border(1.dp, AppPine)
                    .background(AppPine)
                    .clickable(onClick = onAccept)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    "ACCEPT",
                    style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                    color = AppPaper,
                )
            }
            Box(
                modifier = Modifier
                    .border(1.dp, AppStone)
                    .clickable(onClick = onDecline)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    "DECLINE",
                    style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                    color = AppStone,
                )
            }
        }
    }
}

/** Section eyebrow used above an invites list — matches the "MY CLUBS" style. */
@Composable
fun InvitesSectionHeader(count: Int) {
    Text(
        "INVITES · $count",
        style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
        color = AppPondDk,
    )
}
