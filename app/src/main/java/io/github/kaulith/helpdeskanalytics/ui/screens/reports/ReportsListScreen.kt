package io.github.kaulith.helpdeskanalytics.ui.screens.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import io.github.kaulith.helpdeskanalytics.R
import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportConfig
import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportMode
import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportTemplate
import io.github.kaulith.helpdeskanalytics.ui.components.EmptyBlock
import io.github.kaulith.helpdeskanalytics.ui.theme.FrappeRadius
import io.github.kaulith.helpdeskanalytics.ui.theme.Spacing
import io.github.kaulith.helpdeskanalytics.util.toRelativeTime
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsListScreen(
    onBack: () -> Unit,
    onOpenTemplate: (Long?) -> Unit,
    viewModel: ReportTemplatesViewModel = koinViewModel()
) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme
    var pendingDelete by remember { mutableStateOf<ReportTemplate?>(null) }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete report") },
            text = {
                Text("Remove \"${target.name}\"? This only deletes the saved template, not any data.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTemplate(target.id)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = cs.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
            shape = FrappeRadius.xl2
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Reports",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (templates.isNotEmpty()) {
                            Text(
                                text = countLabel(templates.size, "saved report"),
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cs.surface,
                    titleContentColor = cs.onSurface,
                    navigationIconContentColor = cs.onSurface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onOpenTemplate(null) },
                containerColor = cs.primaryContainer,
                contentColor = cs.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
                shape = FrappeRadius.full
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.size(Spacing.sm))
                Text("New report", fontWeight = FontWeight.SemiBold)
            }
        },
        containerColor = cs.surface
    ) { innerPadding ->
        if (templates.isEmpty()) {
            Box(modifier = Modifier.padding(innerPadding)) {
                EmptyBlock(
                    title = stringResource(R.string.reports_empty_title),
                    description = stringResource(R.string.reports_empty_description),
                    icon = Icons.Outlined.Assessment,
                    actionLabel = stringResource(R.string.action_create_report),
                    onAction = { onOpenTemplate(null) }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(
                    start = Spacing.base,
                    end = Spacing.base,
                    top = Spacing.sm,
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(templates, key = { it.id }) { template ->
                    TemplateRow(
                        template = template,
                        onClick = { onOpenTemplate(template.id) },
                        onDuplicate = { viewModel.duplicateTemplate(template) },
                        onDelete = { pendingDelete = template }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateRow(
    template: ReportTemplate,
    onClick: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }
    val config = template.config

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = FrappeRadius.lg,
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.base),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (config != null) ModeBadge(config.mode)
                }
                Spacer(Modifier.size(Spacing.xs))
                Text(
                    text = config?.let { summaryLine(it) } ?: "Can't be opened, delete and rebuild",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Updated ${template.updatedAt.toRelativeTime()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
            }

            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = "Report options",
                        tint = cs.onSurfaceVariant
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (config != null) {
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onDuplicate()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete", color = cs.error) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Delete, contentDescription = null, tint = cs.error)
                        },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeBadge(mode: ReportMode) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = FrappeRadius.full,
        color = cs.secondaryContainer,
        contentColor = cs.onSecondaryContainer
    ) {
        Text(
            text = mode.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp)
        )
    }
}

private fun summaryLine(config: ReportConfig): String {
    val shape = when (config.mode) {
        ReportMode.SUMMARY -> countLabel(config.aggregates.size, "measure")
        ReportMode.DETAIL -> countLabel(config.columns.size, "column")
    }
    val filters = config.filters.size
        .takeIf { it > 0 }
        ?.let { countLabel(it, "filter") }
        ?: "no filters"
    return "$shape, $filters, ${config.dateRange.label.lowercase()}"
}

private fun countLabel(count: Int, noun: String): String =
    if (count == 1) "$count $noun" else "$count ${noun}s"
