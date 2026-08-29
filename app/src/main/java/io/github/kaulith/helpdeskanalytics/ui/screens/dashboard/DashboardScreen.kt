package io.github.kaulith.helpdeskanalytics.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.layout.asPaddingValues
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import io.github.kaulith.helpdeskanalytics.R
import io.github.kaulith.helpdeskanalytics.domain.model.PeriodMetrics
import io.github.kaulith.helpdeskanalytics.domain.model.TicketMetrics
import androidx.compose.material.icons.outlined.ErrorOutline
import io.github.kaulith.helpdeskanalytics.ui.components.EmptyBlock
import io.github.kaulith.helpdeskanalytics.ui.components.OnResume
import io.github.kaulith.helpdeskanalytics.ui.components.SkeletonBox
import io.github.kaulith.helpdeskanalytics.ui.components.SkeletonCard
import io.github.kaulith.helpdeskanalytics.ui.components.animatedCount
import io.github.kaulith.helpdeskanalytics.ui.theme.FrappeRadius
import io.github.kaulith.helpdeskanalytics.ui.theme.Spacing
import io.github.kaulith.helpdeskanalytics.util.formatResolutionTime
import io.github.kaulith.helpdeskanalytics.util.formatResponseTime
import io.github.kaulith.helpdeskanalytics.util.formatTrendPercentage
import org.koin.androidx.compose.koinViewModel
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    OnResume { viewModel.refresh() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        when {
            uiState.isLoading && uiState.metrics == null -> DashboardSkeletonLoading()
            uiState.error != null && uiState.metrics == null -> EmptyBlock(
                title = stringResource(R.string.dashboard_error_title),
                description = uiState.error ?: "",
                icon = Icons.Outlined.ErrorOutline,
                actionLabel = stringResource(R.string.action_retry),
                onAction = { viewModel.refresh(force = true) },
            )
            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh(force = true) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    uiState.metrics?.let { metrics ->
                        DashboardContent(
                            metrics = metrics,
                            userName = uiState.activeAgent?.name ?: uiState.userName
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardSkeletonLoading() {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Hero placeholder, matching the HeroHeader curve and height.
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(cs.surfaceContainerHigh)
                .padding(Spacing.lg),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                SkeletonBox(width = 96.dp, height = 14.dp)
                Spacer(Modifier.height(Spacing.xs))
                SkeletonBox(width = 200.dp, height = 28.dp)
                Spacer(Modifier.height(Spacing.xs))
                SkeletonBox(width = 160.dp, height = 14.dp)
            }
        }
        // Quick stats row: 3 columns.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.base),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            repeat(3) { SkeletonCard(modifier = Modifier.weight(1f)) }
        }
        // Overview metrics: 2 rows of 2 tiles.
        Column(
            modifier = Modifier.padding(horizontal = Spacing.base),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            repeat(2) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    SkeletonCard(modifier = Modifier.weight(1f))
                    SkeletonCard(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Content
// ---------------------------------------------------------------------------

private sealed interface DashboardItem {
    data object Hero : DashboardItem
    data object QuickStats : DashboardItem
    data object OverviewTitle : DashboardItem
    data class Metric(val key: String) : DashboardItem
    data object ActivityTitle : DashboardItem
    data class Period(val key: String) : DashboardItem
}

@Composable
private fun DashboardContent(metrics: TicketMetrics, userName: String?) {
    val statusBarInset = WindowInsets.statusBars.asPaddingValues()
        .calculateTopPadding()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Spacing.xl2),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        item(key = "hero") { HeroHeader(userName = userName, topInset = statusBarInset) }
        item(key = "quick") { QuickStatsRow(metrics = metrics) }
        item(key = "overview-title") {
            SectionTitle(stringResource(R.string.dashboard_section_overview))
        }
        item(key = "metrics") { MetricGrid(metrics) }
        item(key = "activity-title") {
            SectionTitle(stringResource(R.string.dashboard_section_activity))
        }
        item(key = "week") {
            PeriodSummaryCard(title = "This week", period = metrics.thisWeek, index = 0)
        }
        item(key = "month") {
            PeriodSummaryCard(title = "This month", period = metrics.thisMonth, index = 1)
        }
    }
}

@Composable
private fun SectionTitle(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = Spacing.base)
    )
}

// ---------------------------------------------------------------------------
// Hero: tonal surface, no gradient. M3 surfaceContainerHigh + onSurface text.
// ---------------------------------------------------------------------------

@Composable
private fun HeroHeader(userName: String?, topInset: androidx.compose.ui.unit.Dp) {
    val firstName = remember(userName) {
        userName?.split(" ")?.firstOrNull() ?: "there"
    }
    val timeGreeting = remember {
        val h = LocalTime.now().hour
        when {
            h < 12 -> "Good morning"
            h < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .padding(top = topInset + Spacing.lg, bottom = Spacing.xl)
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(
                text = timeGreeting,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Hi, $firstName",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "Here's your day at a glance.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Quick stats: three hero cards on surfaceContainer / primaryContainer.
// ---------------------------------------------------------------------------

@Composable
private fun QuickStatsRow(metrics: TicketMetrics) {
    val items = listOf(
        QuickStat(
            value = metrics.openTicketsCount,
            label = "Open",
            icon = Icons.Outlined.Inbox,
            container = MaterialTheme.colorScheme.primaryContainer,
            onContainer = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        QuickStat(
            value = metrics.today.ticketsResolved,
            label = "Resolved today",
            icon = Icons.Outlined.Timer,
            container = MaterialTheme.colorScheme.tertiaryContainer,
            onContainer = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        QuickStat(
            value = metrics.overdueCount,
            label = "Overdue",
            icon = Icons.Outlined.WarningAmber,
            container = if (metrics.overdueCount > 0)
                MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceContainerHighest,
            onContainer = if (metrics.overdueCount > 0)
                MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onSurface
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.base),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        items.forEach { stat ->
            Box(modifier = Modifier.weight(1f)) { QuickStatCard(stat = stat) }
        }
    }
}

private data class QuickStat(
    val value: Int,
    val label: String,
    val icon: ImageVector,
    val container: Color,
    val onContainer: Color
)

@Composable
private fun QuickStatCard(stat: QuickStat) {
    val displayValue = animatedCount(stat.value)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "${stat.label}: ${stat.value}"
            },
        color = stat.container,
        contentColor = stat.onContainer,
        shape = FrappeRadius.xl
    ) {
        Column(
            modifier = Modifier.padding(Spacing.base),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Icon(
                imageVector = stat.icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = stat.onContainer
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = displayValue.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = stat.onContainer
            )
            Text(
                text = stat.label,
                style = MaterialTheme.typography.labelMedium,
                color = stat.onContainer.copy(alpha = 0.8f),
                maxLines = 1
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Overview metrics: outlined / filled cards on surface, no shadow.
// ---------------------------------------------------------------------------

@Composable
private fun MetricGrid(metrics: TicketMetrics) {
    Column(
        modifier = Modifier.padding(horizontal = Spacing.base),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Box(modifier = Modifier.weight(1f)) {
                MetricTile(
                    title = "Avg response",
                    value = metrics.averageResponseTime.formatResponseTime()
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                MetricTile(
                    title = "Avg resolution",
                    value = metrics.averageResolutionTime.formatResolutionTime()
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Box(modifier = Modifier.weight(1f)) {
                MetricTile(
                    title = "Urgent open",
                    value = metrics.urgentOpenCount.toString(),
                    accent = metrics.urgentOpenCount > 0
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                MetricTile(
                    title = "Created this week",
                    value = metrics.thisWeek.ticketsCreated.toString(),
                    trendPct = metrics.thisWeek.trendPercentage
                )
            }
        }
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    trendPct: Float? = null,
    accent: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FrappeRadius.xl,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.base),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.xs))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (accent)
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
                trendPct?.let {
                    Spacer(Modifier.size(Spacing.sm))
                    TrendChip(pct = it)
                }
            }
        }
    }
}

@Composable
private fun TrendChip(pct: Float) {
    val (icon, container, onContainer) = when {
        pct > 0 -> Triple(
            Icons.AutoMirrored.Outlined.TrendingUp,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        pct < 0 -> Triple(
            Icons.AutoMirrored.Outlined.TrendingDown,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        else -> Triple(
            Icons.AutoMirrored.Outlined.TrendingFlat,
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurface
        )
    }
    Surface(
        color = container,
        contentColor = onContainer,
        shape = FrappeRadius.full
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp))
            Text(
                text = pct.formatTrendPercentage(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Activity: elevated cards with tonal surface elevation.
// ---------------------------------------------------------------------------

@Composable
private fun PeriodSummaryCard(title: String, period: PeriodMetrics, index: Int) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.base),
        shape = FrappeRadius.xl2,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TrendChip(pct = period.trendPercentage)
                }
                Spacer(Modifier.height(Spacing.lg))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PeriodStat(
                        value = period.ticketsCreated.toString(),
                        label = "Created",
                        accent = MaterialTheme.colorScheme.primary
                    )
                    PeriodStat(
                        value = period.ticketsResolved.toString(),
                        label = "Resolved",
                        accent = MaterialTheme.colorScheme.tertiary
                    )
                    PeriodStat(
                        value = period.openTickets.toString(),
                        label = "Open",
                        accent = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
}

@Composable
private fun PeriodStat(value: String, label: String, accent: Color) {
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "$label: $value"
        }
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = accent
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

