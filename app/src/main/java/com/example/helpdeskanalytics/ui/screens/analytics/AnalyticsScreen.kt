package com.example.helpdeskanalytics.ui.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.example.helpdeskanalytics.R
import com.example.helpdeskanalytics.domain.model.PeriodMetrics
import com.example.helpdeskanalytics.domain.model.ResponseTimePercentiles
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.example.helpdeskanalytics.ui.components.CircularProgressCard
import com.example.helpdeskanalytics.ui.components.EmptyBlock
import com.example.helpdeskanalytics.ui.components.OnResume
import com.example.helpdeskanalytics.ui.components.LineChart
import com.example.helpdeskanalytics.ui.components.SkeletonCard
import com.example.helpdeskanalytics.ui.theme.FrappeRadius
import com.example.helpdeskanalytics.ui.theme.Spacing
import com.example.helpdeskanalytics.util.formatResponseTime
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme
    OnResume { viewModel.refresh() }

    when {
        uiState.isLoading -> AnalyticsSkeletonLoading()
        uiState.error != null -> EmptyBlock(
            title = stringResource(R.string.analytics_error_title),
            description = uiState.error ?: "",
            icon = Icons.Outlined.ErrorOutline,
            actionLabel = stringResource(R.string.action_retry),
            onAction = viewModel::refresh
        )
        else -> PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize()
        ) {
            AnalyticsContent(uiState = uiState, onTimeRangeChange = viewModel::setTimeRange)
        }
    }
}

@Composable
private fun AnalyticsSkeletonLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(Spacing.base),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Spacer(Modifier.height(Spacing.sm))
        repeat(4) { SkeletonCard() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeRangeSelector(selected: TimeRange, onSelect: (TimeRange) -> Unit) {
    val entries = TimeRange.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        entries.forEachIndexed { index, range ->
            SegmentedButton(
                selected = selected == range,
                onClick = { onSelect(range) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = entries.size)
            ) { Text(range.label) }
        }
    }
}

@Composable
private fun AnalyticsContent(uiState: AnalyticsUiState, onTimeRangeChange: (TimeRange) -> Unit) {
    val cs = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.surface),
        contentPadding = PaddingValues(
            start = Spacing.base,
            end = Spacing.base,
            top = Spacing.sm,
            bottom = Spacing.lg
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item("range") {
            TimeRangeSelector(selected = uiState.selectedTimeRange, onSelect = onTimeRangeChange)
        }
        item("summary") {
            SummaryCard(
                totalTickets = uiState.totalTickets,
                openPercentage = uiState.openPercentage,
                resolvedPercentage = uiState.resolvedPercentage
            )
        }
        item("status") {
            ChartCard(title = "Status distribution") {
                PieChart(uiState.statusDistribution)
            }
        }
        item("priority") {
            ChartCard(title = "Priority distribution") {
                BarChart(uiState.priorityDistribution)
            }
        }
        if (uiState.ticketsByDay.size >= 2) {
            item("trend") {
                ChartCard(title = "Tickets over time") {
                    LineChart(data = uiState.ticketsByDay)
                }
            }
        }
        uiState.responseTimePercentiles?.let { p ->
            item("percentiles") { PercentilesCard(p) }
        }
        item("sla") {
            CircularProgressCard(title = "SLA compliance", percentage = uiState.slaCompliance)
        }
        uiState.thisMonth?.let { m ->
            item("month") { ThisMonthCard(m) }
        }
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FrappeRadius.lg,
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(Spacing.base)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(Spacing.md))
            content()
        }
    }
}

@Composable
private fun SummaryCard(totalTickets: Int, openPercentage: Float, resolvedPercentage: Float) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FrappeRadius.lg,
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.base),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryStat(value = "$totalTickets", label = "Total", color = cs.onSurface)
            SummaryStat(value = "%.0f%%".format(openPercentage), label = "Open", color = cs.error)
            SummaryStat(
                value = "%.0f%%".format(resolvedPercentage),
                label = "Resolved",
                color = cs.primary
            )
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String, color: Color) {
    val cs = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = cs.onSurfaceVariant
        )
    }
}

@Composable
private fun PercentilesCard(p: ResponseTimePercentiles) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FrappeRadius.lg,
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(Spacing.base)) {
            Text(
                "Response time percentiles",
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(Spacing.md))
            PercentileRow("P50 (median)", p.p50)
            PercentileRow("P90", p.p90)
            PercentileRow("P95", p.p95)
        }
    }
}

@Composable
private fun PercentileRow(label: String, value: Float) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        Text(
            value.formatResponseTime(),
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ThisMonthCard(month: PeriodMetrics) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FrappeRadius.lg,
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(Spacing.base)) {
            Text(
                "This month",
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(Spacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryStat("${month.ticketsCreated}", "Created", cs.tertiary)
                SummaryStat("${month.ticketsResolved}", "Resolved", cs.primary)
                SummaryStat("${month.openTickets}", "Open", cs.error)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Charts: colors drawn from colorScheme so they shift with the active accent.
// ---------------------------------------------------------------------------

@Composable
private fun chartPalette(): List<Color> {
    val cs = MaterialTheme.colorScheme
    return listOf(
        cs.primary,
        cs.tertiary,
        cs.secondary,
        cs.error,
        cs.primaryContainer,
        cs.tertiaryContainer
    )
}

@Composable
private fun PieChart(data: List<ChartData>) {
    if (data.isEmpty()) return
    val cs = MaterialTheme.colorScheme
    val colors = chartPalette()
    val total = data.sumOf { it.value.toDouble() }.toFloat()

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(140.dp).aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                data.forEachIndexed { i, d ->
                    val sweep = (d.value / total) * 360f
                    drawArc(
                        color = colors[i % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = true,
                        size = Size(size.width, size.height)
                    )
                    startAngle += sweep
                }
                // Donut hole, surface-colored, gives breathing room
                drawCircle(
                    color = cs.surfaceContainerLow,
                    radius = size.minDimension / 3.5f
                )
            }
        }
        Spacer(Modifier.width(Spacing.md))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            data.forEachIndexed { i, d ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colors[i % colors.size])
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        "${d.label} (${d.value.toInt()})",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun BarChart(data: List<ChartData>) {
    if (data.isEmpty()) return
    val cs = MaterialTheme.colorScheme
    val colors = chartPalette()
    val maxValue = data.maxOf { it.value }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        data.forEachIndexed { index, d ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = d.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.width(70.dp)
                )
                Spacer(Modifier.width(Spacing.sm))
                val fraction = if (maxValue > 0) d.value / maxValue else 0f
                Canvas(modifier = Modifier.weight(1f).height(24.dp)) {
                    // Track
                    drawRoundRect(
                        color = cs.surfaceContainerHigh,
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                    // Bar
                    drawRoundRect(
                        color = colors[index % colors.size],
                        size = Size(size.width * fraction, size.height),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                }
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = "${d.value.toInt()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = cs.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Error / enter helpers.
// ---------------------------------------------------------------------------


