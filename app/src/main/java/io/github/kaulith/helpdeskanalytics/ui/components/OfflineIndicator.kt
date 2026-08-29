package io.github.kaulith.helpdeskanalytics.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.kaulith.helpdeskanalytics.ui.theme.FrappeMotion
import io.github.kaulith.helpdeskanalytics.ui.theme.Spacing

@Composable
fun OfflineIndicator(
    isOffline: Boolean,
    lastSyncTimestamp: Long,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    AnimatedVisibility(
        visible = isOffline,
        enter = expandVertically(animationSpec = FrappeMotion.spatialSize),
        exit = shrinkVertically(animationSpec = FrappeMotion.spatialSize),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cs.errorContainer)
                .padding(horizontal = Spacing.base, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudOff,
                contentDescription = null,
                tint = cs.onErrorContainer,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = buildString {
                    append("No connection")
                    if (lastSyncTimestamp > 0) {
                        append(" • Last sync ${formatRelativeTime(lastSyncTimestamp)}")
                    }
                },
                style = MaterialTheme.typography.labelMedium,
                color = cs.onErrorContainer,
                modifier = Modifier.padding(start = Spacing.sm),
            )
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / 60_000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "${days}d ago"
    }
}
