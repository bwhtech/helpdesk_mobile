package io.github.kaulith.helpdeskanalytics.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.kaulith.helpdeskanalytics.domain.model.report.ChartPoint
import io.github.kaulith.helpdeskanalytics.domain.model.report.formatNumber
import io.github.kaulith.helpdeskanalytics.ui.theme.Spacing

/** Charts stay readable up to this many slices; the rest fold into "Other". */
private const val MAX_SLICES = 8

@Composable
private fun chartColors(): List<Color> {
    val cs = MaterialTheme.colorScheme
    return listOf(
        cs.primary,
        cs.tertiary,
        cs.secondary,
        cs.primaryContainer,
        cs.tertiaryContainer,
        cs.secondaryContainer,
        cs.surfaceTint,
        cs.outline
    )
}

@Composable
fun ReportBarChart(points: List<ChartPoint>, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val colors = chartColors()
    val shown = points.take(MAX_SLICES)
    val max = shown.maxOfOrNull { it.value }?.takeIf { it > 0.0 } ?: return

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        shown.forEachIndexed { index, point ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = point.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = formatNumber(point.value),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurfaceVariant
                    )
                }
                Canvas(modifier = Modifier.fillMaxWidth().height(10.dp)) {
                    val radius = size.height / 2
                    drawRoundRect(
                        color = cs.surfaceContainerHighest,
                        cornerRadius = CornerRadius(radius)
                    )
                    val width = (point.value / max * size.width).toFloat()
                    if (width > 0f) {
                        drawRoundRect(
                            color = colors[index % colors.size],
                            size = Size(width.coerceAtLeast(radius * 2), size.height),
                            cornerRadius = CornerRadius(radius)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReportDonutChart(points: List<ChartPoint>, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val colors = chartColors()
    val slices = foldTail(points)
    val total = slices.sumOf { it.value }.takeIf { it > 0.0 } ?: return

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.base)
    ) {
        // No centre total: the measure may be an average, and averages do not add up.
        Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
            Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                val thickness = size.minDimension * 0.22f
                val inset = thickness / 2
                var startAngle = -90f
                slices.forEachIndexed { index, slice ->
                    val sweep = (slice.value / total * 360.0).toFloat()
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - thickness, size.height - thickness),
                        style = Stroke(width = thickness)
                    )
                    startAngle += sweep
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            slices.forEachIndexed { index, slice ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colors[index % colors.size])
                    )
                    Text(
                        text = slice.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = Spacing.xs)
                    )
                    Text(
                        text = formatNumber(slice.value),
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun foldTail(points: List<ChartPoint>): List<ChartPoint> {
    if (points.size <= MAX_SLICES) return points
    val head = points.take(MAX_SLICES - 1)
    val rest = points.drop(MAX_SLICES - 1).sumOf { it.value }
    return head + ChartPoint("Other", rest)
}
