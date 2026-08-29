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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.kaulith.helpdeskanalytics.data.update.AppUpdate
import io.github.kaulith.helpdeskanalytics.ui.theme.FrappeMotion
import io.github.kaulith.helpdeskanalytics.ui.theme.Spacing

@Composable
fun UpdateBanner(
    update: AppUpdate?,
    onOpen: (AppUpdate) -> Unit,
    onDismiss: (AppUpdate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    // Kept so the row still has something to draw while it animates out.
    var shown by remember { mutableStateOf(update) }
    if (update != null) shown = update

    AnimatedVisibility(
        visible = update != null,
        enter = expandVertically(animationSpec = FrappeMotion.spatialSize),
        exit = shrinkVertically(animationSpec = FrappeMotion.spatialSize),
        modifier = modifier,
    ) {
        val banner = shown ?: return@AnimatedVisibility
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cs.secondaryContainer)
                .padding(start = Spacing.base, end = Spacing.sm, top = Spacing.xs, bottom = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.SystemUpdate,
                contentDescription = null,
                tint = cs.onSecondaryContainer,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "Version ${banner.versionName} is available",
                style = MaterialTheme.typography.labelLarge,
                color = cs.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { onOpen(banner) }) {
                Text("Get it")
            }
            IconButton(onClick = { onDismiss(banner) }) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Dismiss update notice",
                    tint = cs.onSecondaryContainer,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
