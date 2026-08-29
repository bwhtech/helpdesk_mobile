package io.github.kaulith.helpdeskanalytics.ui.screens.tickets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import io.github.kaulith.helpdeskanalytics.R
import io.github.kaulith.helpdeskanalytics.domain.model.Priority
import io.github.kaulith.helpdeskanalytics.domain.model.Status
import io.github.kaulith.helpdeskanalytics.domain.model.Ticket
import io.github.kaulith.helpdeskanalytics.domain.model.filter.FilterCondition
import io.github.kaulith.helpdeskanalytics.domain.model.filter.FilterOperator
import io.github.kaulith.helpdeskanalytics.domain.model.filter.TicketFilterFields
import io.github.kaulith.helpdeskanalytics.ui.components.EmptyBlock
import io.github.kaulith.helpdeskanalytics.ui.components.FilterSheet
import io.github.kaulith.helpdeskanalytics.ui.components.SkeletonCard
import io.github.kaulith.helpdeskanalytics.ui.theme.FrappeMotion
import io.github.kaulith.helpdeskanalytics.ui.theme.FrappeRadius
import io.github.kaulith.helpdeskanalytics.ui.theme.Spacing
import io.github.kaulith.helpdeskanalytics.util.toRelativeTime
import kotlinx.datetime.Instant
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketsScreen(
    onTicketClick: (String) -> Unit = {},
    viewModel: TicketListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme
    val snackbarHostState = remember { SnackbarHostState() }
    val statusChangedText = stringResource(R.string.feedback_status_changed)
    val undoText = stringResource(R.string.feedback_undo)
    val bulkText = stringResource(R.string.bulk_selected_count)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TicketListEvent.StatusChanged -> {
                    val res = snackbarHostState.showSnackbar(
                        message = statusChangedText,
                        actionLabel = undoText,
                        duration = SnackbarDuration.Short,
                    )
                    if (res == SnackbarResult.ActionPerformed) {
                        viewModel.undoStatus(event.ticketId, event.previous)
                    }
                }
                is TicketListEvent.BulkStatusChanged -> snackbarHostState.showSnackbar(
                    message = bulkText.format(event.count),
                    duration = SnackbarDuration.Short,
                )
                else -> Unit
            }
        }
    }

    Scaffold(
        containerColor = cs.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> TicketsSkeletonLoading()
                uiState.error != null && uiState.tickets.isEmpty() -> {
                    EmptyBlock(
                        title = stringResource(R.string.tickets_error_title),
                        description = uiState.error ?: "",
                        icon = Icons.Outlined.ErrorOutline,
                        actionLabel = stringResource(R.string.action_retry),
                        onAction = viewModel::refresh
                    )
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = viewModel::refresh,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(cs.surface)
                    ) {
                        TicketsContent(
                            uiState = uiState,
                            onSearchQueryChange = viewModel::onSearchQueryChange,
                            onStatusFilterChange = viewModel::onStatusFilterChange,
                            onConditionsChange = viewModel::onConditionsChange,
                            onSortOptionChange = viewModel::onSortOptionChange,
                            onTogglePendingOnly = viewModel::togglePendingOnly,
                            onTicketClick = onTicketClick,
                            onStatusCycle = viewModel::cycleStatus,
                            onSelectionToggle = viewModel::toggleSelected,
                            onClearSelection = viewModel::clearSelection,
                            onBulkResolve = { viewModel.bulkSetStatus(Status.RESOLVED) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketsSkeletonLoading() {
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
private fun TicketsContent(
    uiState: TicketListUiState,
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterChange: (Status?) -> Unit,
    onConditionsChange: (List<FilterCondition<Ticket>>) -> Unit,
    onSortOptionChange: (SortOption) -> Unit,
    onTogglePendingOnly: () -> Unit,
    onTicketClick: (String) -> Unit,
    onStatusCycle: (Ticket) -> Unit,
    onSelectionToggle: (String) -> Unit,
    onClearSelection: () -> Unit,
    onBulkResolve: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val selectedCount = uiState.selectedTicketIds.size
    var showFilterSheet by remember { mutableStateOf(false) }

    val statusFilter = uiState.conditions
        .find { it.field.fieldname == "status" && it.operator == FilterOperator.EQUALS }?.value

    if (showFilterSheet) {
        FilterSheet(
            fields = TicketFilterFields.ALL,
            conditions = uiState.conditions,
            onApply = onConditionsChange,
            onDismiss = { showFilterSheet = false },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Contextual action bar, shown while multi-select is active.
        AnimatedVisibility(visible = selectedCount > 0) {
            Surface(color = cs.primaryContainer, contentColor = cs.onPrimaryContainer) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClearSelection) {
                        Icon(Icons.Outlined.Close, contentDescription = "Clear selection")
                    }
                    Text(
                        text = stringResource(R.string.bulk_selected_count).format(selectedCount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f).padding(start = Spacing.sm),
                    )
                    TextButton(
                        onClick = onBulkResolve,
                        colors = ButtonDefaults.textButtonColors(contentColor = cs.onPrimaryContainer),
                    ) { Text("Resolve all") }
                }
            }
        }
        // Filter / search header
        Column(
            modifier = Modifier
                .background(cs.surface)
                .padding(start = Spacing.base, end = Spacing.base, bottom = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search tickets") },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null,
                        tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
                },
                shape = FrappeRadius.full,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = cs.outline,
                    unfocusedBorderColor = cs.outlineVariant,
                    focusedContainerColor = cs.surfaceContainerLow,
                    unfocusedContainerColor = cs.surfaceContainerLow
                )
            )

            // Toolbar: pending toggle + sort menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { showFilterSheet = true },
                    label = {
                        Text(if (uiState.conditions.isEmpty()) "Filter"
                        else "Filter (${uiState.conditions.size})")
                    },
                    leadingIcon = { Icon(Icons.Outlined.FilterAlt, null, Modifier.size(16.dp)) },
                    shape = FrappeRadius.full,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (uiState.conditions.isEmpty()) cs.surfaceContainerLow
                        else cs.secondaryContainer
                    )
                )
                FilterChip(
                    selected = uiState.showPendingOnly,
                    onClick = onTogglePendingOnly,
                    label = { Text("Pending only") },
                    leadingIcon = if (uiState.showPendingOnly) {
                        { Icon(Icons.Outlined.FilterAlt, null, Modifier.size(16.dp)) }
                    } else null,
                    shape = FrappeRadius.full
                )
                Spacer(Modifier.weight(1f))
                SortMenu(currentSort = uiState.sortOption, onSortChange = onSortOptionChange)
            }

            // Status chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { onStatusFilterChange(null) },
                    label = { Text("All") },
                    shape = FrappeRadius.full
                )
                Status.entries.forEach { s ->
                    FilterChip(
                        selected = statusFilter == s.displayName,
                        onClick = {
                            onStatusFilterChange(if (statusFilter == s.displayName) null else s)
                        },
                        label = { Text(s.displayName) },
                        shape = FrappeRadius.full,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = statusContainerColor(s),
                            selectedLabelColor = statusOnContainerColor(s)
                        )
                    )
                }
            }

        }

        if (uiState.filteredTickets.isEmpty()) {
            EmptyBlock(
                title = stringResource(R.string.tickets_empty_title),
                description = stringResource(R.string.tickets_empty_description),
                icon = Icons.Outlined.Inbox
            )
        } else {
            // Tick once per minute so SLA flags recompute without per-frame Clock reads.
            val nowMinute by produceState(initialValue = System.currentTimeMillis() / 60_000L) {
                while (true) {
                    value = System.currentTimeMillis() / 60_000L
                    kotlinx.coroutines.delay(60_000L)
                }
            }
            LazyColumn(
                modifier = Modifier.background(cs.surface),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = Spacing.base,
                    vertical = Spacing.sm
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(uiState.filteredTickets, key = { it.id }) { ticket ->
                    val selectionActive = uiState.selectedTicketIds.isNotEmpty()
                    TicketCard(
                        ticket = ticket,
                        nowEpochMinute = nowMinute,
                        writeEnabled = uiState.canWrite,
                        selected = ticket.id in uiState.selectedTicketIds,
                        selectionActive = selectionActive,
                        onClick = {
                            if (selectionActive) onSelectionToggle(ticket.id)
                            else onTicketClick(ticket.id)
                        },
                        onLongPress = { onSelectionToggle(ticket.id) },
                        onStatusCycle = { onStatusCycle(ticket) },
                        onSwipeResolve = { onStatusCycle(ticket) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortMenu(currentSort: SortOption, onSortChange: (SortOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(currentSort.label) },
            leadingIcon = {
                Icon(Icons.Outlined.SwapVert, null, Modifier.size(16.dp))
            },
            shape = FrappeRadius.full,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSortChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

private data class SlaState(val overdue: Boolean, val approaching: Boolean)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun TicketCard(
    ticket: Ticket,
    nowEpochMinute: Long,
    writeEnabled: Boolean,
    selected: Boolean,
    selectionActive: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onStatusCycle: () -> Unit,
    onSwipeResolve: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme

    // SLA flags computed once per (ticket, minute tick), so no per-frame Clock reads.
    val slaState = remember(ticket, nowEpochMinute) {
        val now = Instant.fromEpochMilliseconds(nowEpochMinute * 60_000L)
        val overdue = ticket.isOverdue(now)
        val approaching = !overdue && ticket.responseBy?.let { sla ->
            val timeLeft = sla - now
            timeLeft.inWholeHours in 0..2 &&
                    (ticket.status == Status.OPEN || ticket.status == Status.REPLIED)
        } == true
        SlaState(overdue, approaching)
    }
    val isOverdue = slaState.overdue
    val isApproachingSLA = slaState.approaching

    val a11yLabel = remember(ticket, isOverdue, isApproachingSLA) {
        buildString {
            append(ticket.id); append(", ")
            append(ticket.subject); append(", ")
            append("status "); append(ticket.status.displayName); append(", ")
            append("priority "); append(ticket.priority.displayName); append(", ")
            append("updated "); append(ticket.modifiedAt.toRelativeTime())
            if (isOverdue) append(", overdue")
            else if (isApproachingSLA) append(", SLA approaching")
        }
    }

    // Swipe-to-resolve. Right-swipe past 50% resolves the ticket then snaps back.
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                onSwipeResolve()
                false // never actually dismiss the card
            } else false
        }
    )

    val cardContainer = if (selected) cs.primaryContainer else cs.surfaceContainerLow

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> cs.tertiaryContainer
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(FrappeRadius.lg)
                    .background(color)
                    .padding(start = Spacing.lg),
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = "Resolve",
                    tint = cs.onTertiaryContainer,
                )
            }
        },
        enableDismissFromStartToEnd = writeEnabled && !selectionActive,
        enableDismissFromEndToStart = false,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(FrappeRadius.lg)
                .combinedClickable(
                    role = Role.Button,
                    onClickLabel = if (selectionActive) "Toggle selection" else "Open ticket",
                    onLongClickLabel = "Multi-select",
                    onLongClick = if (writeEnabled) onLongPress else null,
                    onClick = onClick,
                )
                .semantics(mergeDescendants = true) { contentDescription = a11yLabel },
            shape = FrappeRadius.lg,
            colors = CardDefaults.cardColors(containerColor = cardContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.base),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                if (selectionActive) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { onClick() },
                        modifier = Modifier.padding(top = 2.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isOverdue -> cs.error
                                    isApproachingSLA -> cs.tertiary
                                    else -> Color.Transparent
                                }
                            )
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ticket.id,
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        if (isOverdue) {
                            TonalPill("Overdue", cs.errorContainer, cs.onErrorContainer)
                        } else if (isApproachingSLA) {
                            TonalPill("SLA soon", cs.tertiaryContainer, cs.onTertiaryContainer)
                        }
                    }
                    Text(
                        text = ticket.subject,
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tap status to cycle it forward.
                        Box(
                            modifier = Modifier
                                .clip(FrappeRadius.full)
                                .clickable(
                                    enabled = writeEnabled && !selectionActive,
                                    onClickLabel = "Cycle status",
                                    onClick = onStatusCycle,
                                )
                        ) { StatusPill(ticket.status) }
                        PriorityPill(ticket.priority)
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = ticket.modifiedAt.toRelativeTime(),
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Pills: tonal mini-badges that follow colorScheme container roles.
// ---------------------------------------------------------------------------

@Composable
internal fun TonalPill(
    label: String,
    container: Color,
    onContainer: Color,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
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
internal fun StatusPill(status: Status) {
    TonalPill(
        label = status.displayName,
        container = statusContainerColor(status),
        onContainer = statusOnContainerColor(status),
        leadingIcon = statusIcon(status),
    )
}

@Composable
internal fun PriorityPill(priority: Priority) {
    TonalPill(
        label = priority.displayName,
        container = priorityContainerColor(priority),
        onContainer = priorityOnContainerColor(priority),
        leadingIcon = priorityIcon(priority),
    )
}

internal fun statusIcon(status: Status): androidx.compose.ui.graphics.vector.ImageVector = when (status) {
    Status.OPEN -> Icons.Outlined.RadioButtonUnchecked
    Status.REPLIED -> Icons.AutoMirrored.Outlined.Reply
    Status.AWAITING_APPROVAL -> Icons.Outlined.HourglassEmpty
    Status.RESOLVED -> Icons.Outlined.CheckCircle
    Status.CLOSED -> Icons.Outlined.Lock
}

internal fun priorityIcon(priority: Priority): androidx.compose.ui.graphics.vector.ImageVector = when (priority) {
    Priority.URGENT -> Icons.Outlined.Bolt
    Priority.HIGH -> Icons.Outlined.PriorityHigh
    Priority.MEDIUM -> Icons.Outlined.Remove
    Priority.LOW -> Icons.Outlined.KeyboardArrowDown
}

@Composable
internal fun statusContainerColor(status: Status): Color {
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
internal fun statusOnContainerColor(status: Status): Color {
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
internal fun priorityContainerColor(priority: Priority): Color {
    val cs = MaterialTheme.colorScheme
    return when (priority) {
        Priority.URGENT -> cs.errorContainer
        Priority.HIGH -> cs.tertiaryContainer
        Priority.MEDIUM -> cs.secondaryContainer
        Priority.LOW -> cs.surfaceContainerHighest
    }
}

@Composable
internal fun priorityOnContainerColor(priority: Priority): Color {
    val cs = MaterialTheme.colorScheme
    return when (priority) {
        Priority.URGENT -> cs.onErrorContainer
        Priority.HIGH -> cs.onTertiaryContainer
        Priority.MEDIUM -> cs.onSecondaryContainer
        Priority.LOW -> cs.onSurfaceVariant
    }
}

