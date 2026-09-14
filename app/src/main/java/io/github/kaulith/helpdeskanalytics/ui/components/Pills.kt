package io.github.kaulith.helpdeskanalytics.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.kaulith.helpdeskanalytics.domain.model.Priority
import io.github.kaulith.helpdeskanalytics.domain.model.Status
import io.github.kaulith.helpdeskanalytics.ui.theme.FrappeRadius

@Composable
fun TonalPill(
    label: String,
    container: Color,
    onContainer: Color,
    leadingIcon: ImageVector? = null,
) {
    Surface(
        shape = FrappeRadius.full,
        color = container,
        contentColor = onContainer
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(
                start = if (leadingIcon != null) 6.dp else 8.dp,
                end = 8.dp,
                top = 2.dp,
                bottom = 2.dp,
            )
        ) {
            if (leadingIcon != null) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun StatusPill(status: Status) {
    TonalPill(
        label = status.value,
        container = statusContainerColor(status),
        onContainer = statusOnContainerColor(status),
        leadingIcon = statusIcon(status),
    )
}

@Composable
fun PriorityPill(priority: Priority) {
    TonalPill(
        label = priority.value,
        container = priorityContainerColor(priority),
        onContainer = priorityOnContainerColor(priority),
        leadingIcon = priorityIcon(priority),
    )
}

fun statusIcon(status: Status): ImageVector = when (status) {
    Status.OPEN -> Icons.Outlined.RadioButtonUnchecked
    Status.REPLIED -> Icons.AutoMirrored.Outlined.Reply
    Status.AWAITING_APPROVAL -> Icons.Outlined.HourglassEmpty
    Status.RESOLVED -> Icons.Outlined.CheckCircle
    Status.CLOSED -> Icons.Outlined.Lock
}

fun priorityIcon(priority: Priority): ImageVector = when (priority) {
    Priority.URGENT -> Icons.Outlined.Bolt
    Priority.HIGH -> Icons.Outlined.PriorityHigh
    Priority.MEDIUM -> Icons.Outlined.Remove
    Priority.LOW -> Icons.Outlined.KeyboardArrowDown
}

@Composable
fun statusContainerColor(status: Status): Color {
    val cs = MaterialTheme.colorScheme
    return when (status) {
        Status.OPEN -> cs.errorContainer
        Status.REPLIED -> cs.tertiaryContainer
        Status.AWAITING_APPROVAL -> cs.secondaryContainer
        Status.RESOLVED -> cs.primaryContainer
        Status.CLOSED -> cs.surfaceContainerHighest
    }
}

@Composable
fun statusOnContainerColor(status: Status): Color {
    val cs = MaterialTheme.colorScheme
    return when (status) {
        Status.OPEN -> cs.onErrorContainer
        Status.REPLIED -> cs.onTertiaryContainer
        Status.AWAITING_APPROVAL -> cs.onSecondaryContainer
        Status.RESOLVED -> cs.onPrimaryContainer
        Status.CLOSED -> cs.onSurfaceVariant
    }
}

@Composable
fun priorityContainerColor(priority: Priority): Color {
    val cs = MaterialTheme.colorScheme
    return when (priority) {
        Priority.URGENT -> cs.errorContainer
        Priority.HIGH -> cs.tertiaryContainer
        Priority.MEDIUM -> cs.secondaryContainer
        Priority.LOW -> cs.surfaceContainerHighest
    }
}

@Composable
fun priorityOnContainerColor(priority: Priority): Color {
    val cs = MaterialTheme.colorScheme
    return when (priority) {
        Priority.URGENT -> cs.onErrorContainer
        Priority.HIGH -> cs.onTertiaryContainer
        Priority.MEDIUM -> cs.onSecondaryContainer
        Priority.LOW -> cs.onSurfaceVariant
    }
}
