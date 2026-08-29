package com.example.helpdeskanalytics.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.helpdeskanalytics.domain.model.Agent
import com.example.helpdeskanalytics.ui.theme.Spacing

/** Shows which agent the app is acting as. Switching happens in Settings. */
@Composable
fun AgentChip(
    agent: Agent?,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(9999.dp)
    Row(
        modifier = modifier
            .padding(end = Spacing.sm)
            .clip(shape)
            .background(cs.surfaceContainerHigh)
            .padding(start = Spacing.xs, end = Spacing.sm, top = Spacing.xs, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        InitialsAvatar(name = agent?.name ?: "All", size = 24.dp)
        Text(
            text = agent?.name?.split(" ")?.first() ?: "All",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = cs.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
