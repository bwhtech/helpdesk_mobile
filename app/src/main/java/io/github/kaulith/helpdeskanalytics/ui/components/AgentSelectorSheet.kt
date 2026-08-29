package io.github.kaulith.helpdeskanalytics.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.kaulith.helpdeskanalytics.domain.model.Agent
import io.github.kaulith.helpdeskanalytics.ui.theme.FrappeRadius
import io.github.kaulith.helpdeskanalytics.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentSelectorSheet(
    agents: List<Agent>,
    activeAgent: Agent?,
    onAgentSelected: (Agent?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    val cs = MaterialTheme.colorScheme

    val filteredAgents = remember(agents, searchQuery) {
        if (searchQuery.isBlank()) agents
        else agents.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.email.contains(searchQuery, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = cs.surfaceContainerLow,
        contentColor = cs.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .imePadding()
                .padding(horizontal = Spacing.base),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Select agent",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = cs.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search agents") },
                singleLine = true,
                shape = FrappeRadius.full,
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null,
                        tint = cs.onSurfaceVariant)
                },
            )

            Spacer(Modifier.height(Spacing.md))

            LazyColumn(modifier = Modifier.weight(1f)) {
                if (activeAgent != null && searchQuery.isBlank()) {
                    item { SectionLabel("Current") }
                    item { AgentRow(agent = activeAgent, isSelected = true, onClick = onDismiss) }
                    item {
                        Spacer(Modifier.height(Spacing.sm))
                        SectionLabel("All agents")
                    }
                }

                items(
                    items = filteredAgents.filter { it.email != activeAgent?.email },
                    key = { it.email },
                ) { agent ->
                    AgentRow(agent = agent, isSelected = false, onClick = {
                        onAgentSelected(agent)
                        onDismiss()
                    })
                }

                if (filteredAgents.isEmpty() && searchQuery.isNotBlank()) {
                    item {
                        Text(
                            text = "No agents found for \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = Spacing.lg),
                        )
                    }
                }

                item {
                    HorizontalDivider(color = cs.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) {
                                onAgentSelected(null)
                                onDismiss()
                            }
                            .padding(vertical = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Groups,
                            contentDescription = null,
                            tint = cs.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "View all agents",
                            style = MaterialTheme.typography.bodyLarge,
                            color = cs.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (activeAgent == null) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Selected",
                                tint = cs.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(Spacing.xl)) }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = Spacing.xs),
    )
}

@Composable
private fun AgentRow(agent: Agent, isSelected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) cs.primaryContainer else cs.surfaceContainerLow)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        InitialsAvatar(name = agent.name, size = 40.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = agent.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) cs.onPrimaryContainer else cs.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = agent.email,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) cs.onPrimaryContainer.copy(alpha = 0.7f)
                    else cs.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Selected",
                tint = cs.onPrimaryContainer,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
