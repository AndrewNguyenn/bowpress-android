package com.andrewnguyen.bowpress.feature.social.ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags

/**
 * Instagram-style location tag (§18) — a very small "in {place}" line shown at
 * the top of a shared-session feed post. Tapping it opens the location map
 * popup. Mirrors iOS SocialComponents.LocationTag.
 */
@Composable
fun LocationTag(
    name: String,
    onTap: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .then(if (onTap != null) Modifier.clickable(onClick = onTap) else Modifier)
            .testTag(TestTags.FeedRowLocationTag)
            .semantics { contentDescription = "Shot at $name. Opens a map." },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = null,
            tint = AppPondDk,
            modifier = Modifier.size(11.dp),
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = "in $name",
            style = interUI(9.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.04.em),
            color = AppPondDk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
