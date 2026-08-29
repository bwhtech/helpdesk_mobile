package com.example.helpdeskanalytics.ui.screens.reports

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FilterAltOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.helpdeskanalytics.data.report.ExportMime
import com.example.helpdeskanalytics.data.report.ReportExporter
import com.example.helpdeskanalytics.domain.model.report.AggregateFunction
import com.example.helpdeskanalytics.domain.model.report.ChartType
import com.example.helpdeskanalytics.domain.model.report.ColumnType
import com.example.helpdeskanalytics.domain.model.report.DETAIL_ROW_CAP
import com.example.helpdeskanalytics.domain.model.report.DateRangePreset
import com.example.helpdeskanalytics.domain.model.report.FilterOperator
import com.example.helpdeskanalytics.domain.model.report.FilterOption
import com.example.helpdeskanalytics.domain.model.report.offersOptions
import com.example.helpdeskanalytics.domain.model.report.ReportAggregate
import com.example.helpdeskanalytics.domain.model.report.ReportColumn
import com.example.helpdeskanalytics.domain.model.report.ReportConfig
import com.example.helpdeskanalytics.domain.model.report.ReportFilter
import com.example.helpdeskanalytics.domain.model.report.ReportMode
import com.example.helpdeskanalytics.domain.model.report.ReportResult
import com.example.helpdeskanalytics.domain.model.report.SortDirection
import com.example.helpdeskanalytics.ui.components.DateRangeSheet
import com.example.helpdeskanalytics.ui.components.ReportBarChart
import com.example.helpdeskanalytics.ui.components.ReportDonutChart
import com.example.helpdeskanalytics.ui.theme.FrappeRadius
import com.example.helpdeskanalytics.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel

private const val NONE_KEY = "__none__"
private const val PREVIEW_ROW_CAP = 60
private const val VALUE_MENU_CAP = 50
private const val VALUE_SEARCH_THRESHOLD = 8

private data class MenuItem(val key: String, val label: String)

/** [index] is null while adding; set while editing the filter at that position. */
private data class FilterEditor(val index: Int?, val filter: ReportFilter?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportBuilderScreen(
    templateId: Long?,
    onBack: () -> Unit,
    viewModel: ReportBuilderViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filterOptions by viewModel.filterOptions.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(templateId) {
        if (templateId != null) viewModel.loadTemplate(templateId)
    }

    var filterEditor by remember { mutableStateOf<FilterEditor?>(null) }
    var showAggregateDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var showDateSheet by remember { mutableStateOf(false) }

    val config = uiState.config
    val result = uiState.result
    val reportName = uiState.templateName.ifBlank { "Tickets report" }
    val isSummary = config.mode == ReportMode.SUMMARY

    fun saveToDocument(uri: Uri?, asPdf: Boolean) {
        val res = result ?: return
        if (uri == null) return
        scope.launch {
            withContext(Dispatchers.IO) {
                ReportExporter.writeToDocument(context, uri, res, reportName, asPdf)
            }
            snackbar.showSnackbar("Report saved")
        }
    }

    val saveCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ExportMime.CSV)
    ) { uri -> saveToDocument(uri, asPdf = false) }

    val savePdf = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ExportMime.PDF)
    ) { uri -> saveToDocument(uri, asPdf = true) }

    filterEditor?.let { editor ->
        FilterDialog(
            // A summary is computed by the server, which can only filter its own columns.
            columns = if (isSummary) ReportColumn.serverFilterable else ReportColumn.entries,
            initial = editor.filter,
            optionsState = filterOptions,
            onColumnChange = viewModel::loadFilterOptions,
            onDismiss = { filterEditor = null },
            onSubmit = { filter ->
                if (editor.index == null) {
                    viewModel.addFilter(filter)
                } else {
                    viewModel.updateFilter(editor.index, filter)
                }
                filterEditor = null
            }
        )
    }

    if (showAggregateDialog) {
        AggregateDialog(
            onDismiss = { showAggregateDialog = false },
            onAdd = { aggregate ->
                viewModel.addAggregate(aggregate)
                showAggregateDialog = false
            }
        )
    }

    if (showSaveDialog) {
        SaveDialog(
            initialName = uiState.templateName,
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                viewModel.saveTemplate(name) {
                    scope.launch { snackbar.showSnackbar("Report saved") }
                }
                showSaveDialog = false
            }
        )
    }

    if (showDateSheet) {
        DateRangeSheet(
            initialStart = config.customStart,
            initialEnd = config.customEnd,
            onDismiss = { showDateSheet = false },
            onConfirm = { start, end ->
                viewModel.setCustomRange(start, end)
                showDateSheet = false
            }
        )
    }

    fun export(asPdf: Boolean) {
        val res = result ?: return
        scope.launch {
            val file = withContext(Dispatchers.IO) {
                if (asPdf) ReportExporter.exportPdf(context, res, reportName)
                else ReportExporter.exportCsv(context, res, reportName)
            }
            ReportExporter.share(context, file, if (asPdf) ExportMime.PDF else ExportMime.CSV)
        }
    }

    if (showPreview && result != null) {
        ReportPreviewOverlay(
            result = result,
            reportName = reportName,
            onClose = { showPreview = false },
            onExport = ::export,
            onSave = { asPdf ->
                if (asPdf) savePdf.launch("$reportName.pdf") else saveCsv.launch("$reportName.csv")
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = uiState.templateName.ifBlank { "New report" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = headerSubtitle(uiState),
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant
                            )
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
                if (uiState.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cs.surface)
                    .padding(Spacing.base),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                OutlinedButton(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = FrappeRadius.full
                ) {
                    Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(Spacing.sm))
                    Text("Save")
                }
                Button(
                    onClick = { showPreview = true },
                    enabled = result != null && result.totalRows > 0,
                    modifier = Modifier.weight(1.4f),
                    shape = FrappeRadius.full,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cs.primary,
                        contentColor = cs.onPrimary
                    )
                ) {
                    Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(Spacing.sm))
                    Text("Preview & export", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = cs.surface
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(Spacing.base),
            verticalArrangement = Arrangement.spacedBy(Spacing.base)
        ) {
            uiState.error?.let { err ->
                item { NoticeCard(err, cs.errorContainer, cs.onErrorContainer) }
            }

            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ReportMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = config.mode == mode,
                            onClick = { viewModel.setMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ReportMode.entries.size)
                        ) { Text(mode.label) }
                    }
                }
            }

            item {
                BuilderSection("Date range") {
                    DropdownField(
                        value = dateRangeLabel(config),
                        items = DateRangePreset.entries.map { MenuItem(it.name, it.label) },
                        onSelect = { key ->
                            val preset = DateRangePreset.valueOf(key)
                            if (preset == DateRangePreset.CUSTOM) showDateSheet = true
                            else viewModel.setDateRange(preset)
                        }
                    )
                }
            }

            if (isSummary) {
                item {
                    BuilderSection("Measures") {
                        config.aggregates.forEachIndexed { index, aggregate ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = aggregate.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = cs.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (config.aggregates.size > 1) {
                                    IconButton(onClick = { viewModel.removeAggregate(index) }) {
                                        Icon(
                                            Icons.Outlined.Close,
                                            contentDescription = "Remove measure",
                                            tint = cs.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            if (index < config.aggregates.lastIndex) {
                                HorizontalDivider(color = cs.outlineVariant)
                            }
                        }
                        Spacer(Modifier.height(Spacing.sm))
                        OutlinedButton(
                            onClick = { showAggregateDialog = true },
                            shape = FrappeRadius.full
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(Spacing.sm))
                            Text("Add measure")
                        }
                    }
                }
            } else {
                item {
                    BuilderSection(
                        title = "Columns",
                        trailing = {
                            TextButton(
                                onClick = {
                                    viewModel.setColumns(
                                        if (config.columns.size == ReportColumn.entries.size) {
                                            ReportConfig.DEFAULT_COLUMNS
                                        } else {
                                            ReportColumn.entries
                                        }
                                    )
                                }
                            ) {
                                Text(
                                    if (config.columns.size == ReportColumn.entries.size) "Reset"
                                    else "Select all"
                                )
                            }
                        }
                    ) {
                        Text(
                            text = "${config.columns.size} of ${ReportColumn.entries.size} shown",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        ReportColumn.entries.forEach { column ->
                            val checked = column in config.columns
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .toggleable(
                                        value = checked,
                                        role = Role.Checkbox,
                                        onValueChange = { viewModel.toggleColumn(column) }
                                    )
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    column.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = cs.onSurface
                                )
                                Checkbox(checked = checked, onCheckedChange = null)
                            }
                        }
                    }
                }
            }

            item {
                BuilderSection("Filters") {
                    if (config.filters.isEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            Icon(
                                Icons.Outlined.FilterAltOff,
                                contentDescription = null,
                                tint = cs.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Every ticket in the date range is included.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurfaceVariant
                            )
                        }
                    } else {
                        config.filters.forEachIndexed { index, filter ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clickable(role = Role.Button) {
                                        filterEditor = FilterEditor(index, filter)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = filter.column.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = cs.onSurface
                                    )
                                    Text(
                                        text = "${filter.operator.label} \"${filter.value}\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = cs.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = null,
                                    tint = cs.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                IconButton(onClick = { viewModel.removeFilter(index) }) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = "Remove filter",
                                        tint = cs.onSurfaceVariant
                                    )
                                }
                            }
                            if (index < config.filters.lastIndex) {
                                HorizontalDivider(color = cs.outlineVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    OutlinedButton(
                        onClick = { filterEditor = FilterEditor(null, null) },
                        shape = FrappeRadius.full
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(Spacing.sm))
                        Text("Add filter")
                    }
                }
            }

            item {
                BuilderSection("Group & sort") {
                    FieldLabel("Group by")
                    val groupOptions = if (isSummary) ReportColumn.groupable else {
                        ReportColumn.entries.filter {
                            it.type == ColumnType.TEXT || it.type == ColumnType.BOOL
                        }
                    }
                    DropdownField(
                        value = config.groupBy?.label ?: "None",
                        items = listOf(MenuItem(NONE_KEY, "None")) +
                            groupOptions.map { MenuItem(it.key, it.label) },
                        onSelect = { key ->
                            viewModel.setGroupBy(
                                if (key == NONE_KEY) null else ReportColumn.fromKey(key)
                            )
                        }
                    )
                    Spacer(Modifier.height(Spacing.md))
                    FieldLabel(if (isSummary) "Order" else "Sort by")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        if (!isSummary) {
                            DropdownField(
                                value = config.sortBy?.label ?: "None",
                                items = listOf(MenuItem(NONE_KEY, "None")) +
                                    ReportColumn.entries.map { MenuItem(it.key, it.label) },
                                onSelect = { key ->
                                    viewModel.setSortBy(
                                        if (key == NONE_KEY) null else ReportColumn.fromKey(key)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedButton(
                            onClick = viewModel::toggleSortDirection,
                            enabled = isSummary || config.sortBy != null,
                            shape = FrappeRadius.full
                        ) {
                            Icon(
                                Icons.Outlined.SwapVert,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.size(Spacing.xs))
                            Text(sortLabel(isSummary, config.sortDirection))
                        }
                    }
                }
            }

            if (isSummary) {
                item {
                    BuilderSection("Chart") {
                        DropdownField(
                            value = config.chartType.label,
                            items = ChartType.entries.map { MenuItem(it.name, it.label) },
                            onSelect = { key -> viewModel.setChartType(ChartType.valueOf(key)) }
                        )
                    }
                }
            }

            result?.ignoredFilters?.takeIf { it.isNotEmpty() }?.let { ignored ->
                item {
                    NoticeCard(
                        "Summaries run on the server, which can't filter on " +
                            "${ignored.joinToString { it.label }}. That filter was not applied.",
                        cs.tertiaryContainer,
                        cs.onTertiaryContainer
                    )
                }
            }

            if (result != null && result.truncated) {
                item {
                    NoticeCard(
                        "This report pulls at most ${count(DETAIL_ROW_CAP)} tickets, and " +
                            "${count(result.serverTotal ?: 0)} match. Narrow the date range, add a " +
                            "filter, or switch to Summary to cover them all.",
                        cs.tertiaryContainer,
                        cs.onTertiaryContainer
                    )
                }
            }

            if (result != null && result.chart.any { it.value > 0.0 }) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = FrappeRadius.lg,
                        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow)
                    ) {
                        Box(modifier = Modifier.padding(Spacing.base)) {
                            when (config.chartType) {
                                ChartType.BAR -> ReportBarChart(result.chart)
                                ChartType.DONUT -> ReportDonutChart(result.chart)
                                ChartType.NONE -> Unit
                            }
                        }
                    }
                }
            }

            item {
                BuilderSection(
                    title = "Preview",
                    trailing = {
                        if (result != null && result.totalRows > 0) {
                            TextButton(onClick = { showPreview = true }) { Text("Open full") }
                        }
                    },
                    padded = false
                ) {
                    when {
                        uiState.isLoading -> Box(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }

                        result == null || result.totalRows == 0 -> Column(
                            modifier = Modifier.fillMaxWidth().padding(Spacing.base),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            Text(
                                text = "Nothing to show yet",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = cs.onSurface
                            )
                            Text(
                                text = "No ticket matches this date range and these filters.",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant
                            )
                        }

                        else -> PreviewTable(result)
                    }
                }
            }
        }
    }
}

private fun count(value: Int): String = "%,d".format(value)

private fun sortLabel(isSummary: Boolean, direction: SortDirection): String = when {
    isSummary && direction == SortDirection.ASC -> "Lowest first"
    isSummary -> "Highest first"
    direction == SortDirection.ASC -> "Asc"
    else -> "Desc"
}

private fun dateRangeLabel(config: ReportConfig): String {
    if (config.dateRange != DateRangePreset.CUSTOM) return config.dateRange.label
    val start = config.customStart ?: return config.dateRange.label
    val end = config.customEnd ?: return config.dateRange.label
    return "$start to $end"
}

@Composable
private fun NoticeCard(text: String, container: Color, content: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FrappeRadius.md,
        color = container,
        contentColor = content
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun BuilderSection(
    title: String,
    trailing: @Composable () -> Unit = {},
    padded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.sm)
            )
            trailing()
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = FrappeRadius.lg,
            colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow)
        ) {
            Column(
                modifier = if (padded) Modifier.padding(Spacing.base) else Modifier,
                content = content
            )
        }
    }
}

private fun headerSubtitle(uiState: ReportBuilderUiState): String {
    val rows = uiState.result?.totalRows ?: 0
    val shape = if (uiState.config.mode == ReportMode.SUMMARY) "group" else "ticket"
    val counted = if (rows == 1) "1 $shape" else "${count(rows)} ${shape}s"
    return "$counted, ${dateRangeLabel(uiState.config).lowercase()}"
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = Spacing.xs)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    value: String,
    items: List<MenuItem>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            shape = FrappeRadius.md,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label) },
                    onClick = {
                        onSelect(item.key)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PreviewTable(result: ReportResult) {
    val cs = MaterialTheme.colorScheme
    val colWidth = 132.dp
    val tableWidth = colWidth * result.headers.size

    Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier
                .width(tableWidth)
                .background(cs.surfaceContainerHigh)
        ) {
            result.headers.forEach { header ->
                Text(
                    text = header,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(colWidth).padding(horizontal = 10.dp, vertical = 10.dp)
                )
            }
        }
        HorizontalDivider(modifier = Modifier.width(tableWidth), color = cs.outlineVariant)

        var shown = 0
        result.groups.forEach { group ->
            if (shown < PREVIEW_ROW_CAP) {
                if (group.label != null) {
                    Row(
                        modifier = Modifier
                            .width(tableWidth)
                            .background(cs.primaryContainer)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${group.label}  (${group.rows.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onPrimaryContainer
                        )
                    }
                }
                group.rows.forEach { row ->
                    if (shown < PREVIEW_ROW_CAP) {
                        Row(modifier = Modifier.width(tableWidth)) {
                            row.forEach { cell ->
                                Text(
                                    text = cell,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cs.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(colWidth).padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.width(tableWidth), color = cs.outlineVariant)
                        shown++
                    }
                }
            }
        }

        if (result.totalRows > PREVIEW_ROW_CAP) {
            Text(
                text = "Showing the first $PREVIEW_ROW_CAP of ${result.totalRows} rows; export to get them all.",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

@Composable
private fun FilterDialog(
    columns: List<ReportColumn>,
    initial: ReportFilter?,
    optionsState: FilterOptionsUiState,
    onColumnChange: (ReportColumn) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (ReportFilter) -> Unit
) {
    var column by remember {
        mutableStateOf(initial?.column ?: columns.firstOrNull() ?: ReportColumn.STATUS)
    }
    var operator by remember { mutableStateOf(initial?.operator ?: FilterOperator.EQUALS) }
    var value by remember { mutableStateOf(initial?.value.orEmpty()) }
    val operators = FilterOperator.forType(column.type)

    LaunchedEffect(column) { onColumnChange(column) }

    val picking = column.offersOptions(operator)
    val options = optionsState.options.takeIf { optionsState.column == column }.orEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add filter" else "Edit filter") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FieldLabel("Field")
                DropdownField(
                    value = column.label,
                    items = columns.map { MenuItem(it.key, it.label) },
                    onSelect = { key ->
                        ReportColumn.fromKey(key)?.let { picked ->
                            column = picked
                            operator = FilterOperator.forType(picked.type).first()
                            value = ""
                        }
                    }
                )
                FieldLabel("Condition")
                DropdownField(
                    value = operator.label,
                    items = operators.map { MenuItem(it.name, it.label) },
                    onSelect = { key ->
                        operator = FilterOperator.valueOf(key)
                        value = ""
                    }
                )
                FieldLabel("Value")
                when {
                    picking && optionsState.isLoading && optionsState.column == column ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text(
                                text = "Loading ${column.label.lowercase()} values...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                    picking && options.isNotEmpty() -> ValuePicker(
                        selected = value,
                        options = options,
                        onSelect = { value = it }
                    )

                    else -> OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                when (column.type) {
                                    ColumnType.BOOL -> "Yes or No"
                                    ColumnType.NUMBER -> "Enter a number"
                                    ColumnType.DATE -> "YYYY-MM-DD"
                                    ColumnType.TEXT -> "Enter a value"
                                }
                            )
                        },
                        singleLine = true,
                        shape = FrappeRadius.md
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(ReportFilter(column, operator, value.trim())) },
                enabled = isFilterValueValid(column.type, value)
            ) { Text(if (initial == null) "Add" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = FrappeRadius.xl2
    )
}

/** A dropdown with a search box, because Customer runs to thousands of values. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ValuePicker(
    selected: String,
    options: List<FilterOption>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    val matches = remember(search, options) {
        if (search.isBlank()) options
        else options.filter { it.label.contains(search, true) || it.value.contains(search, true) }
    }
    val selectedLabel = options.find { it.value == selected }?.label ?: selected

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            placeholder = { Text("Pick a value") },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = FrappeRadius.md,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 320.dp)
        ) {
            if (options.size > VALUE_SEARCH_THRESHOLD) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    placeholder = { Text("Search") },
                    singleLine = true,
                    shape = FrappeRadius.md
                )
            }
            matches.take(VALUE_MENU_CAP).forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        onSelect(option.value)
                        search = ""
                        expanded = false
                    }
                )
            }
            if (matches.isEmpty()) {
                DropdownMenuItem(text = { Text("No matches") }, onClick = {}, enabled = false)
            } else if (matches.size > VALUE_MENU_CAP) {
                DropdownMenuItem(
                    text = { Text("Keep typing to narrow ${count(matches.size)} matches...") },
                    onClick = {},
                    enabled = false
                )
            }
        }
    }
}

private fun isFilterValueValid(type: ColumnType, value: String): Boolean {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return false
    return when (type) {
        ColumnType.NUMBER -> trimmed.toDoubleOrNull() != null
        ColumnType.DATE -> runCatching { LocalDate.parse(trimmed) }.isSuccess
        else -> true
    }
}

@Composable
private fun AggregateDialog(
    onDismiss: () -> Unit,
    onAdd: (ReportAggregate) -> Unit
) {
    var function by remember { mutableStateOf(AggregateFunction.COUNT) }
    var column by remember { mutableStateOf(ReportColumn.aggregatable.first()) }
    val needsColumn = function != AggregateFunction.COUNT

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add measure") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FieldLabel("Function")
                DropdownField(
                    value = function.label,
                    items = AggregateFunction.entries.map { MenuItem(it.name, it.label) },
                    onSelect = { key -> function = AggregateFunction.valueOf(key) }
                )
                if (needsColumn) {
                    FieldLabel("Field")
                    DropdownField(
                        value = column.label,
                        items = ReportColumn.aggregatable.map { MenuItem(it.key, it.label) },
                        onSelect = { key -> ReportColumn.fromKey(key)?.let { column = it } }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(ReportAggregate(function, column.takeIf { needsColumn })) }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = FrappeRadius.xl2
    )
}

@Composable
private fun SaveDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save report") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    "Saved reports appear in the Reports list for quick reuse.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Report name") },
                    singleLine = true,
                    shape = FrappeRadius.md
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = FrappeRadius.xl2
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportPreviewOverlay(
    result: ReportResult,
    reportName: String,
    onClose: () -> Unit,
    onExport: (Boolean) -> Unit,
    onSave: (Boolean) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var showSaveMenu by remember { mutableStateOf(false) }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = reportName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${result.totalRows} rows, ${result.headers.size} columns",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSaveMenu = true }) {
                            Icon(Icons.Outlined.SaveAlt, contentDescription = "Save to device")
                        }
                        DropdownMenu(
                            expanded = showSaveMenu,
                            onDismissRequest = { showSaveMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Save CSV to device") },
                                onClick = {
                                    showSaveMenu = false
                                    onSave(false)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Save PDF to device") },
                                onClick = {
                                    showSaveMenu = false
                                    onSave(true)
                                }
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
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cs.surface)
                        .padding(Spacing.base),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    OutlinedButton(
                        onClick = { onExport(false) },
                        modifier = Modifier.weight(1f),
                        shape = FrappeRadius.full
                    ) {
                        Icon(Icons.Outlined.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(Spacing.sm))
                        Text("Share CSV")
                    }
                    Button(
                        onClick = { onExport(true) },
                        modifier = Modifier.weight(1f),
                        shape = FrappeRadius.full,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cs.primary,
                            contentColor = cs.onPrimary
                        )
                    ) {
                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(Spacing.sm))
                        Text("Share PDF", fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            containerColor = cs.surface
        ) { innerPadding ->
            FullReportTable(result, Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun FullReportTable(result: ReportResult, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val hScroll = rememberScrollState()
    val colWidth = 150.dp
    val tableWidth = colWidth * result.headers.size

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Row(modifier = Modifier.horizontalScroll(hScroll)) {
                Row(
                    modifier = Modifier
                        .width(tableWidth)
                        .background(cs.surfaceContainerHigh)
                ) {
                    result.headers.forEach { header ->
                        Text(
                            text = header,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(colWidth).padding(horizontal = 10.dp, vertical = 12.dp)
                        )
                    }
                }
            }
            HorizontalDivider(color = cs.outlineVariant)
        }

        result.groups.forEach { group ->
            if (group.label != null) {
                item {
                    Row(modifier = Modifier.horizontalScroll(hScroll)) {
                        Row(
                            modifier = Modifier
                                .width(tableWidth)
                                .background(cs.primaryContainer)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${group.label}  (${group.rows.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = cs.onPrimaryContainer
                            )
                        }
                    }
                    HorizontalDivider(color = cs.outlineVariant)
                }
            }
            items(group.rows) { row ->
                Row(modifier = Modifier.horizontalScroll(hScroll)) {
                    Row(modifier = Modifier.width(tableWidth)) {
                        row.forEach { cell ->
                            Text(
                                text = cell,
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(colWidth).padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(color = cs.outlineVariant)
            }
        }

        item { Spacer(Modifier.height(Spacing.xl)) }
    }
}
