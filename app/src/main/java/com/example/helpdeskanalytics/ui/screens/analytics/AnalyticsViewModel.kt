package com.example.helpdeskanalytics.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helpdeskanalytics.data.metrics.MetricsCalculator
import com.example.helpdeskanalytics.domain.model.Agent
import com.example.helpdeskanalytics.domain.model.PeriodMetrics
import com.example.helpdeskanalytics.domain.model.Priority
import com.example.helpdeskanalytics.domain.model.ResponseTimePercentiles
import com.example.helpdeskanalytics.domain.model.SlaState
import com.example.helpdeskanalytics.domain.model.Status
import com.example.helpdeskanalytics.domain.model.Ticket
import com.example.helpdeskanalytics.domain.model.agentResolutionSla
import com.example.helpdeskanalytics.domain.repository.AgentRepository
import com.example.helpdeskanalytics.domain.repository.TicketRepository
import com.example.helpdeskanalytics.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

data class ChartData(
    val label: String,
    val value: Float
)

enum class TimeRange(val label: String) {
    WEEK("Week"),
    MONTH("Month"),
    QUARTER("Quarter"),
    ALL("All")
}

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedTimeRange: TimeRange = TimeRange.ALL,
    val statusDistribution: List<ChartData> = emptyList(),
    val priorityDistribution: List<ChartData> = emptyList(),
    val ticketsByDay: List<ChartData> = emptyList(),
    val totalTickets: Int = 0,
    val openPercentage: Float = 0f,
    val resolvedPercentage: Float = 0f,
    val responseTimePercentiles: ResponseTimePercentiles? = null,
    val slaCompliance: Float = 0f,
    val thisMonth: PeriodMetrics? = null,
    val activeAgent: Agent? = null
)

class AnalyticsViewModel(
    private val repository: TicketRepository,
    private val agentRepository: AgentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private var allTickets: List<Ticket> = emptyList()
    private var analyticsJob: Job? = null

    init {
        observeActiveAgent()
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadAnalytics(_uiState.value.activeAgent)
    }

    fun setTimeRange(range: TimeRange) {
        _uiState.update { it.copy(selectedTimeRange = range) }
        viewModelScope.launch {
            recompute(filterByRange(allTickets, range), range)
        }
    }

    private fun observeActiveAgent() {
        viewModelScope.launch {
            agentRepository.getActiveAgent().collect { agent ->
                _uiState.update { it.copy(activeAgent = agent) }
                loadAnalytics(agent)
            }
        }
    }

    private fun loadAnalytics(activeAgent: Agent?) {
        analyticsJob?.cancel()
        analyticsJob = viewModelScope.launch {
            repository.getTickets(assignedTo = activeAgent?.email).collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> {
                        allTickets = result.data
                        val range = _uiState.value.selectedTimeRange
                        recompute(filterByRange(allTickets, range), range)
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(isLoading = false, isRefreshing = false, error = result.exception.message)
                    }
                }
            }
        }
    }

    private suspend fun recompute(tickets: List<Ticket>, range: TimeRange) = withContext(Dispatchers.Default) {
        val metrics = MetricsCalculator.computeMetrics(allTickets)
        val sla = calculateSlaCompliance(tickets)

        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                error = null,
                selectedTimeRange = range,
                statusDistribution = calculateStatusDistribution(tickets),
                priorityDistribution = calculatePriorityDistribution(tickets),
                ticketsByDay = calculateTicketsByDay(tickets),
                totalTickets = tickets.size,
                openPercentage = if (tickets.isNotEmpty())
                    tickets.count { t -> t.status == Status.OPEN || t.status == Status.REPLIED }
                        .toFloat() / tickets.size * 100 else 0f,
                resolvedPercentage = if (tickets.isNotEmpty())
                    tickets.count { t -> t.status == Status.RESOLVED || t.status == Status.CLOSED }
                        .toFloat() / tickets.size * 100 else 0f,
                responseTimePercentiles = metrics.responseTimePercentiles,
                slaCompliance = sla,
                thisMonth = metrics.thisMonth
            )
        }
    }

    private fun rangeCutoff(range: TimeRange): LocalDate? {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return when (range) {
            TimeRange.WEEK -> today.minus(7, DateTimeUnit.DAY)
            TimeRange.MONTH -> today.minus(1, DateTimeUnit.MONTH)
            TimeRange.QUARTER -> today.minus(3, DateTimeUnit.MONTH)
            TimeRange.ALL -> null
        }
    }

    private fun filterByRange(tickets: List<Ticket>, range: TimeRange): List<Ticket> {
        val cutoff = rangeCutoff(range) ?: return tickets
        val tz = TimeZone.currentSystemDefault()
        return tickets.filter {
            it.createdAt.toLocalDateTime(tz).date >= cutoff
        }
    }

    private fun calculateStatusDistribution(tickets: List<Ticket>): List<ChartData> {
        return Status.entries.map { status ->
            ChartData(
                label = status.displayName,
                value = tickets.count { it.status == status }.toFloat()
            )
        }.filter { it.value > 0 }
    }

    private fun calculatePriorityDistribution(tickets: List<Ticket>): List<ChartData> {
        return Priority.entries.map { priority ->
            ChartData(
                label = priority.displayName,
                value = tickets.count { it.priority == priority }.toFloat()
            )
        }.filter { it.value > 0 }
    }

    private fun calculateTicketsByDay(tickets: List<Ticket>): List<ChartData> {
        val tz = TimeZone.currentSystemDefault()
        return tickets
            .groupBy { it.createdAt.toLocalDateTime(tz).date }
            .toSortedMap()
            .map { (date, dayTickets) ->
                val label = "%d/%d".format(date.dayOfMonth, date.monthNumber)
                ChartData(label = label, value = dayTickets.size.toFloat())
            }
    }

    private fun calculateSlaCompliance(tickets: List<Ticket>): Float {
        val judged = tickets.mapNotNull {
            when (it.agentResolutionSla()) {
                SlaState.MET -> true
                SlaState.BREACHED -> false
                SlaState.DUE, SlaState.NONE -> null
            }
        }
        if (judged.isEmpty()) return 0f
        return (judged.count { it }.toFloat() / judged.size) * 100f
    }
}
