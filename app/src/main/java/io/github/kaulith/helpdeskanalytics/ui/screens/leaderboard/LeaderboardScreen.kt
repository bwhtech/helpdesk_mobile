package io.github.kaulith.helpdeskanalytics.ui.screens.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.kaulith.helpdeskanalytics.R
import io.github.kaulith.helpdeskanalytics.domain.model.AgentPerformance
import io.github.kaulith.helpdeskanalytics.domain.model.LeaderboardPeriod
import io.github.kaulith.helpdeskanalytics.domain.model.Team
import io.github.kaulith.helpdeskanalytics.domain.model.filter.FilterCondition
import io.github.kaulith.helpdeskanalytics.domain.model.filter.agentFilterFields
import io.github.kaulith.helpdeskanalytics.ui.components.DateRangeSheet
import io.github.kaulith.helpdeskanalytics.ui.components.EmptyBlock
import io.github.kaulith.helpdeskanalytics.ui.components.FilterSheet
import io.github.kaulith.helpdeskanalytics.ui.components.InitialsAvatar
import io.github.kaulith.helpdeskanalytics.ui.components.OnResume
import io.github.kaulith.helpdeskanalytics.ui.components.SkeletonCard
import io.github.kaulith.helpdeskanalytics.ui.theme.FrappeRadius
import io.github.kaulith.helpdeskanalytics.ui.theme.Spacing
import io.github.kaulith.helpdeskanalytics.util.formatResolutionTime
import io.github.kaulith.helpdeskanalytics.util.formatResponseTime
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(viewModel: LeaderboardViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    OnResume { viewModel.refresh() }

    when {
        uiState.isLoading && uiState.agents.isEmpty() -> LeaderboardSkeletonLoading()
        !uiState.hasPermission -> EmptyBlock(
            title = stringResource(R.string.leaderboard_access_title),
            description = stringResource(R.string.leaderboard_access_description),
            icon = Icons.Outlined.Lock
        )
        uiState.error != null -> EmptyBlock(
            title = stringResource(R.string.leaderboard_error_title),
            description = uiState.error ?: "",
            actionLabel = stringResource(R.string.action_retry),
            onAction = { viewModel.refresh(force = true) }
        )
        uiState.filteredAgents.isEmpty() && uiState.agents.isEmpty() -> EmptyBlock(
            title = stringResource(R.string.leaderboard_empty_title),
            description = stringResource(R.string.leaderboard_empty_description),
            icon = Icons.Outlined.Groups
        )
        else -> PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh(force = true) },
            modifier = Modifier.fillMaxSize()
        ) {
            LeaderboardContent(
                agents = uiState.filteredAgents,
                teams = uiState.teams,
                conditions = uiState.conditions,
                onConditionsChange = viewModel::onConditionsChange,
                period = uiState.period,
                onPeriodChange = viewModel::setPeriod,
                isLoading = uiState.isLoading
            )
        }
    }
}

@Composable
private fun PeriodChip(
    selected: LeaderboardPeriod,
    onSelect: (LeaderboardPeriod) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }
    var showRangePicker by remember { mutableStateOf(false) }

    if (showRangePicker) {
        val custom = selected as? LeaderboardPeriod.Custom
        DateRangeSheet(
            initialStart = custom?.start,
            initialEnd = custom?.end,
            onDismiss = { showRangePicker = false },
            onConfirm = { start, end ->
                showRangePicker = false
                onSelect(LeaderboardPeriod.Custom(start, end))
            }
        )
    }

    Box {
        AssistChip(
            onClick = { menuOpen = true },
            label = { Text(selected.chipLabel(), maxLines = 1) },
            leadingIcon = { Icon(Icons.Outlined.DateRange, null, Modifier.size(16.dp)) },
            shape = FrappeRadius.full,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = if (selected == LeaderboardPeriod.AllTime) cs.surfaceContainerLow
                else cs.secondaryContainer
            )
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            LeaderboardPeriod.presets.forEach { preset ->
                DropdownMenuItem(
                    text = { Text(preset.label) },
                    onClick = {
                        menuOpen = false
                        onSelect(preset)
                    },
                    trailingIcon = if (selected == preset) {
                        { Icon(Icons.Outlined.Check, null, Modifier.size(18.dp)) }
                    } else null
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Custom range...") },
                onClick = {
                    menuOpen = false
                    showRangePicker = true
                },
                trailingIcon = if (selected is LeaderboardPeriod.Custom) {
                    { Icon(Icons.Outlined.Check, null, Modifier.size(18.dp)) }
                } else null
            )
        }
    }
}

/** The ISO label of a custom range is too wide for a chip. */
private fun LeaderboardPeriod.chipLabel(): String = when (this) {
    is LeaderboardPeriod.Custom -> "${start.shortLabel()} to ${end.shortLabel()}"
    else -> label
}

private fun LocalDate.shortLabel(): String =
    "$day ${month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}"

@Composable
private fun LeaderboardSkeletonLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = Spacing.base),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Spacer(Modifier.height(Spacing.sm))
        repeat(6) { SkeletonCard() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeaderboardContent(
    agents: List<AgentPerformance>,
    teams: List<Team>,
    conditions: List<FilterCondition<AgentPerformance>>,
    onConditionsChange: (List<FilterCondition<AgentPerformance>>) -> Unit,
    period: LeaderboardPeriod,
    onPeriodChange: (LeaderboardPeriod) -> Unit,
    isLoading: Boolean
) {
    val cs = MaterialTheme.colorScheme
    var showFilterSheet by remember { mutableStateOf(false) }

    if (showFilterSheet) {
        FilterSheet(
            fields = agentFilterFields(teams),
            conditions = conditions,
            onApply = onConditionsChange,
            onDismiss = { showFilterSheet = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(cs.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.base, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = { showFilterSheet = true },
                label = {
                    Text(if (conditions.isEmpty()) "Filter" else "Filter (${conditions.size})")
                },
                leadingIcon = { Icon(Icons.Outlined.FilterAlt, null, Modifier.size(16.dp)) },
                shape = FrappeRadius.full,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (conditions.isEmpty()) cs.surfaceContainerLow
                    else cs.secondaryContainer
                )
            )
            Spacer(Modifier.weight(1f))
            PeriodChip(selected = period, onSelect = onPeriodChange)
        }
        // A period with no cached counts refetches; keep the ranking visible while it lands.
        if (isLoading && agents.isNotEmpty()) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (agents.isEmpty() && conditions.isNotEmpty()) {
            EmptyBlock(
                title = "No agents match",
                description = "No agents match the current filters."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(Spacing.base),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(agents, key = { it.agentEmail }) { agent ->
                    AgentRow(agent = agent)
                }
            }
        }
    }
}

@Composable
private fun AgentRow(agent: AgentPerformance) {
    val cs = MaterialTheme.colorScheme
    // Top 3 sit on a primary-container tint so they pop above the rest of the
    // list. Current user always gets the highlight regardless of rank.
    val container = when {
        agent.isCurrentUser -> cs.primaryContainer
        agent.rank <= 3 -> cs.tertiaryContainer
        else -> cs.surfaceContainerLow
    }
    val onContainer = when {
        agent.isCurrentUser -> cs.onPrimaryContainer
        agent.rank <= 3 -> cs.onTertiaryContainer
        else -> cs.onSurface
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FrappeRadius.lg,
        colors = CardDefaults.cardColors(containerColor = container, contentColor = onContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.base),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            RankBadge(agent.rank)
            InitialsAvatar(name = agent.agentName)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = agent.agentName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = onContainer
                )
                Text(
                    text = "${agent.ticketsResolved} resolved",
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer.copy(alpha = 0.75f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = agent.averageResponseTime.formatResponseTime(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = onContainer
                )
                Text(
                    text = agent.averageResolutionTime.formatResolutionTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = onContainer.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
private fun RankBadge(rank: Int) {
    val cs = MaterialTheme.colorScheme
    val label = when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "#$rank"
    }
    Box(
        modifier = Modifier.width(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = if (rank <= 3) MaterialTheme.typography.headlineSmall
            else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (rank <= 3) cs.onTertiaryContainer else cs.onSurfaceVariant
        )
    }
}

