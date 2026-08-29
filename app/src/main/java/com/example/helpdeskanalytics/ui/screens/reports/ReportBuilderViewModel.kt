package com.example.helpdeskanalytics.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helpdeskanalytics.data.report.ReportEngine
import com.example.helpdeskanalytics.data.report.ReportQueryBuilder
import com.example.helpdeskanalytics.domain.model.report.ChartType
import com.example.helpdeskanalytics.domain.model.report.DateRangePreset
import com.example.helpdeskanalytics.domain.model.report.FilterOption
import com.example.helpdeskanalytics.domain.model.report.FilterValueSource
import com.example.helpdeskanalytics.domain.model.report.ReportAggregate
import com.example.helpdeskanalytics.domain.model.report.valueSource
import com.example.helpdeskanalytics.domain.model.report.ReportColumn
import com.example.helpdeskanalytics.domain.model.report.ReportConfig
import com.example.helpdeskanalytics.domain.model.report.ReportData
import com.example.helpdeskanalytics.domain.model.report.ReportFilter
import com.example.helpdeskanalytics.domain.model.report.ReportMode
import com.example.helpdeskanalytics.domain.model.report.ReportResult
import com.example.helpdeskanalytics.domain.model.report.SortDirection
import com.example.helpdeskanalytics.domain.repository.AgentRepository
import com.example.helpdeskanalytics.domain.repository.ReportDataRepository
import com.example.helpdeskanalytics.domain.repository.ReportRepository
import com.example.helpdeskanalytics.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class ReportBuilderUiState(
    val config: ReportConfig = ReportConfig(),
    val result: ReportResult? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val templateName: String = ""
)

/** Choices for the filter dialog's value field, for whichever column it is showing. */
data class FilterOptionsUiState(
    val column: ReportColumn? = null,
    val options: List<FilterOption> = emptyList(),
    val isLoading: Boolean = false
)

class ReportBuilderViewModel(
    private val reportDataRepository: ReportDataRepository,
    private val reportRepository: ReportRepository,
    private val agentRepository: AgentRepository
) : ViewModel() {

    private val _config = MutableStateFlow(ReportConfig())
    private val _data = MutableStateFlow<ReportData?>(null)
    private val _loading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)
    private val _templateName = MutableStateFlow("")
    private val _filterOptions = MutableStateFlow(FilterOptionsUiState())

    val filterOptions: StateFlow<FilterOptionsUiState> = _filterOptions

    private var templateId: Long? = null
    private var templateLoaded = false
    private var filterOptionsJob: Job? = null

    val uiState: StateFlow<ReportBuilderUiState> = combine(
        _config, _data, _loading, _error, _templateName
    ) { config, data, loading, error, name ->
        ReportBuilderUiState(
            config = config,
            result = data?.let { ReportEngine.build(config, it) },
            isLoading = loading,
            error = error,
            templateName = name
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportBuilderUiState())

    // Only the query shape reaches the network; picking columns reprojects rows already fetched.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val reportData: Flow<Result<ReportData>> = _config
        .map { ReportQueryBuilder.build(it) }
        .distinctUntilChanged()
        .flatMapLatest { reportDataRepository.runReport(it) }

    init {
        viewModelScope.launch {
            reportData.collect { result ->
                when (result) {
                    is Result.Success -> {
                        _data.value = result.data
                        _loading.value = false
                        _error.value = null
                    }
                    is Result.Error -> {
                        _loading.value = false
                        _error.value = result.exception.message ?: "Failed to run this report"
                    }
                    // Detail and summary rows are not interchangeable, so drop the old shape.
                    Result.Loading -> {
                        _loading.value = true
                        _data.value = null
                    }
                }
            }
        }
    }

    /** Populates the builder from a saved template. Runs once per screen instance. */
    fun loadTemplate(id: Long) {
        if (templateLoaded) return
        templateLoaded = true
        viewModelScope.launch {
            val template = reportRepository.getTemplate(id) ?: return@launch
            val config = template.config
            if (config == null) {
                _error.value = "This saved report can't be opened"
                return@launch
            }
            _config.value = config
            templateId = template.id
            _templateName.value = template.name
        }
    }

    fun setMode(mode: ReportMode) {
        if (mode == _config.value.mode) return
        val config = _config.value
        _config.value = when (mode) {
            // A summary can only group on a column the server knows how to bucket.
            ReportMode.SUMMARY -> config.copy(
                mode = mode,
                groupBy = config.groupBy?.takeIf { it in ReportColumn.groupable }
                    ?: ReportColumn.STATUS
            )
            ReportMode.DETAIL -> config.copy(mode = mode)
        }
    }

    fun setColumns(columns: List<ReportColumn>) {
        if (columns.isEmpty()) return
        _config.value = _config.value.copy(columns = columns)
    }

    fun toggleColumn(column: ReportColumn) {
        val current = _config.value.columns
        val updated = when {
            column !in current -> current + column
            current.size > 1 -> current - column
            else -> current
        }
        _config.value = _config.value.copy(columns = updated)
    }

    /** Fills the filter dialog's value picker. Cheap and cached for all but the first call. */
    fun loadFilterOptions(column: ReportColumn) {
        if (_filterOptions.value.column == column) return
        filterOptionsJob?.cancel()
        val source = column.valueSource()
        if (source is FilterValueSource.Fixed) {
            _filterOptions.value = FilterOptionsUiState(column, source.options)
            return
        }
        if (source is FilterValueSource.Typed) {
            _filterOptions.value = FilterOptionsUiState(column)
            return
        }
        _filterOptions.value = FilterOptionsUiState(column, isLoading = true)
        filterOptionsJob = viewModelScope.launch {
            val options = when (source) {
                FilterValueSource.Agents -> agentOptions()
                is FilterValueSource.Distinct -> distinctOptions(source.frappeField)
                else -> emptyList()
            }
            _filterOptions.value = FilterOptionsUiState(column, options)
        }
    }

    private suspend fun agentOptions(): List<FilterOption> {
        val loaded = agentRepository.getAgents().first { it !is Result.Loading }
        return (loaded as? Result.Success)?.data.orEmpty().map { FilterOption(it.email, it.name) }
    }

    private suspend fun distinctOptions(frappeField: String): List<FilterOption> =
        when (val result = reportDataRepository.distinctValues(frappeField)) {
            is Result.Success -> result.data.map { FilterOption(it, it) }
            else -> emptyList()
        }

    fun addFilter(filter: ReportFilter) {
        _config.value = _config.value.copy(filters = _config.value.filters + filter)
    }

    fun updateFilter(index: Int, filter: ReportFilter) {
        _config.value = _config.value.copy(
            filters = _config.value.filters.mapIndexed { i, existing ->
                if (i == index) filter else existing
            }
        )
    }

    fun removeFilter(index: Int) {
        _config.value = _config.value.copy(
            filters = _config.value.filters.filterIndexed { i, _ -> i != index }
        )
    }

    fun addAggregate(aggregate: ReportAggregate) {
        _config.value = _config.value.copy(aggregates = _config.value.aggregates + aggregate)
    }

    fun removeAggregate(index: Int) {
        val remaining = _config.value.aggregates.filterIndexed { i, _ -> i != index }
        if (remaining.isEmpty()) return
        _config.value = _config.value.copy(aggregates = remaining)
    }

    fun setChartType(chartType: ChartType) {
        _config.value = _config.value.copy(chartType = chartType)
    }

    fun setGroupBy(column: ReportColumn?) {
        _config.value = _config.value.copy(groupBy = column)
    }

    fun setSortBy(column: ReportColumn?) {
        _config.value = _config.value.copy(sortBy = column)
    }

    fun toggleSortDirection() {
        val next = if (_config.value.sortDirection == SortDirection.ASC) {
            SortDirection.DESC
        } else {
            SortDirection.ASC
        }
        _config.value = _config.value.copy(sortDirection = next)
    }

    fun setDateRange(preset: DateRangePreset) {
        _config.value = _config.value.copy(dateRange = preset)
    }

    fun setCustomRange(start: LocalDate, end: LocalDate) {
        _config.value = _config.value.copy(
            dateRange = DateRangePreset.CUSTOM,
            customStart = start,
            customEnd = end
        )
    }

    fun saveTemplate(name: String, onSaved: () -> Unit) {
        viewModelScope.launch {
            templateId = reportRepository.saveTemplate(name, _config.value, templateId)
            _templateName.value = name
            onSaved()
        }
    }
}
