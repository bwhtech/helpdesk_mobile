package io.github.kaulith.helpdeskanalytics.util

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.abs

fun Instant.toRelativeTime(): String {
    val now = Clock.System.now()
    val diff = now - this
    val seconds = abs(diff.inWholeSeconds)
    return when {
        seconds < 60 -> "Just now"
        seconds < 3600 -> "${seconds / 60}m ago"
        seconds < 86400 -> "${seconds / 3600}h ago"
        seconds < 604800 -> "${seconds / 86400}d ago"
        seconds < 2592000 -> "${seconds / 604800}w ago"
        else -> "${seconds / 2592000}mo ago"
    }
}

fun Float.formatResponseTime(): String {
    return when {
        this < 60f -> "${this.toInt()} min"
        this < 1440f -> "%.1f hrs".format(this / 60f)
        else -> "%.1f days".format(this / 1440f)
    }
}

fun Float.formatResolutionTime(): String {
    return when {
        this < 1f -> "${(this * 60).toInt()} min"
        this < 24f -> "%.1f hrs".format(this)
        else -> "%.1f days".format(this / 24f)
    }
}

fun Float.formatTrendPercentage(): String {
    val prefix = if (this > 0) "+" else ""
    return "$prefix%.0f%%".format(this)
}

fun Modifier.shimmerEffect(): Modifier = composed {
    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val highlightColor = MaterialTheme.colorScheme.surface
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200)
        ),
        label = "shimmer"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                baseColor.copy(alpha = 0.6f),
                highlightColor.copy(alpha = 0.3f),
                baseColor.copy(alpha = 0.6f),
            ),
            start = Offset(translateAnim - 200f, translateAnim - 200f),
            end = Offset(translateAnim, translateAnim)
        ),
        shape = RoundedCornerShape(8.dp)
    )
}
