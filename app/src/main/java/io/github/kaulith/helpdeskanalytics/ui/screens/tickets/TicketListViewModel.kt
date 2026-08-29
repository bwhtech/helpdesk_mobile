package io.github.kaulith.helpdeskanalytics.ui.screens.tickets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kaulith.helpdeskanalytics.domain.model.Agent
import io.github.kaulith.helpdeskanalytics.domain.model.Priority
import io.github.kaulith.helpdeskanalytics.domain.model.Status
import io.github.kaulith.helpdeskanalytics.domain.model.Ticket
import io.github.kaulith.helpdeskanalytics.domain.model.filter.FilterCondition
import io.github.kaulith.helpdeskanalytics.domain.model.filter.FilterOperator
import io.github.kaulith.helpdeskanalytics.domain.model.filter.FilterableField
import io.github.kaulith.helpdeskanalytics.domain.model.filter.TicketFilterFields
import io.github.kaulith.helpdeskanalytics.domain.model.filter.matches
import io.github.kaulith.helpdeskanalytics.domain.repository.AgentRepository
import io.github.kaulith.helpdeskanalytics.domain.repository.TicketRepository
import io.github.kaulith.helpdeskanalytics.util.Constants
import io.github.kaulith.helpdeskanalytics.util.Result
import kotlinx.coroutines.FlowPreview
import kotlinx.datetime.Clock
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SortOption(val label: String) {
    NEWEST("Newest First"),
    OLDEST("Oldest First"),
    PRIORITY_HIGH("Priority: High to Low"),
    PRIORITY_LOW("Priority: Low to High"),
    SLA_URGENT("SLA Urgency")
}

data class TicketListUiState(
    val tickets: List<Ticket> = emptyList(),
    val filteredTickets: List<Ticket> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val conditions: List<FilterCondition<Ticket>> = emptyList(),
    val sortOption: SortOption = SortOption.NEWEST,
    val showPendingOnly: Boolean = false,
    val activeAgent: Agent? = null,
    val canWrite: Boolean = false,
    val selectedTicketIds: Set<String> = emptySet(),
)

sealed interface TicketListEvent {
    data class StatusChanged(val ticketId: String, val previous: Status) : TicketListEvent
    data class PriorityChanged(val ticketId: String, val previous: Priority) : TicketListEvent
    data class BulkStatusChanged(val count: Int) : TicketListEvent
}

class TicketListViewModel(
    private val repository: TicketRepository,
    private val agentRepository: AgentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TicketListUiState())
    val uiState: StateFlow<TicketListUiState> = _uiState.asStateFlow()

    private val _events = Channel<TicketListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _searchQuery = MutableStateFlow("")
    private var ticketsJob: Job? = null
    private var loggedInEmail: String? = null

    init {
        loadLoggedInUser()
        observeSearch()
        observeActiveAgent()
    }

    private fun loadLoggedInUser() {
        viewModelScope.launch {
            repository.getCurrentUser().collect { result ->
                if (result is Result.Success) {
                    loggedInEmail = result.data.email
                    // If no active agent, reload with logged-in email
                    if (_uiState.value.activeAgent == null) {
                        loadTickets(result.data.email)
                    }
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        _searchQuery.value = query
    }

    /** Status quick-filter chip: a single equals condition, added or cleared in place. */
    fun onStatusFilterChange(status: Status?) =
        setQuickEquals(TicketFilterFields.STATUS, status?.displayName)

    private fun setQuickEquals(field: FilterableField<Ticket>, value: String?) {
        _uiState.update { state ->
            val others = state.conditions.filterNot {
                it.field.fieldname == field.fieldname && it.operator == FilterOperator.EQUALS
            }
            val next = if (value == null) others
            else others + FilterCondition(field, FilterOperator.EQUALS, value)
            state.copy(conditions = next)
        }
        applyFilters()
    }

    /** Advanced builder commits the full condition list. */
    fun onConditionsChange(conditions: List<FilterCondition<Ticket>>) {
        _uiState.update { it.copy(conditions = conditions) }
        applyFilters()
    }

    fun clearFilters() {
        _uiState.update { it.copy(conditions = emptyList()) }
        applyFilters()
    }

    fun onSortOptionChange(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
        applyFilters()
    }

    fun togglePendingOnly() {
        _uiState.update { it.copy(showPendingOnly = !it.showPendingOnly) }
        applyFilters()
    }

    /** Cycle status forward: OPEN → REPLIED → RESOLVED → OPEN. Skips terminal CLOSED. */
    fun cycleStatus(ticket: Ticket) {
        if (!_uiState.value.canWrite) return
        val next = when (ticket.status) {
            Status.OPEN -> Status.REPLIED
            Status.REPLIED -> Status.RESOLVED
            Status.AWAITING_APPROVAL -> Status.RESOLVED
            Status.RESOLVED -> Status.OPEN
            Status.CLOSED -> Status.OPEN
        }
        viewModelScope.launch {
            val result = repository.updateTicketStatus(ticket.id, next)
            if (result is Result.Success) {
                _events.send(TicketListEvent.StatusChanged(ticket.id, ticket.status))
            }
        }
    }

    fun undoStatus(ticketId: String, previous: Status) {
        viewModelScope.launch { repository.updateTicketStatus(ticketId, previous) }
    }

    fun toggleSelected(ticketId: String) {
        if (!_uiState.value.canWrite) return
        _uiState.update {
            val next = it.selectedTicketIds.toMutableSet().apply {
                if (!add(ticketId)) remove(ticketId)
            }
            it.copy(selectedTicketIds = next)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedTicketIds = emptySet()) }
    }

    fun bulkSetStatus(status: Status) {
        val ids = _uiState.value.selectedTicketIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { id -> repository.updateTicketStatus(id, status) }
            _events.send(TicketListEvent.BulkStatusChanged(ids.size))
            clearSelection()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            repository.refresh()
            val email = _uiState.value.activeAgent?.email ?: loggedInEmail
            loadTickets(email)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeSearch() {
        viewModelScope.launch {
            _searchQuery
                .debounce(Constants.SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { applyFilters() }
        }
    }

    private fun observeActiveAgent() {
        viewModelScope.launch {
            agentRepository.getActiveAgent().collect { agent ->
                _uiState.update { it.copy(activeAgent = agent, canWrite = repository.canWrite()) }
                val email = agent?.email ?: loggedInEmail
                loadTickets(email)
            }
        }
    }

    private fun loadTickets(assignedTo: String?) {
        ticketsJob?.cancel()
        ticketsJob = viewModelScope.launch {
            repository.getTickets(assignedTo = assignedTo).collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.update {
                        it.copy(isLoading = it.tickets.isEmpty())
                    }
                    is Result.Success -> {
                        _uiState.update { it.copy(tickets = result.data, isLoading = false, error = null) }
                        applyFilters()
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(isLoading = false, error = result.exception.message ?: "Failed to load tickets")
                    }
                }
            }
        }
    }

    private fun applyFilters() {
        val state = _uiState.value
        var filtered = state.tickets

        // Pending only filter (Open or Replied, not yet resolved/closed)
        if (state.showPendingOnly) {
            filtered = filtered.filter {
                it.status == Status.OPEN || it.status == Status.REPLIED
            }
        }

        // Field conditions (quick chips + advanced builder), AND-combined
        if (state.conditions.isNotEmpty()) {
            filtered = filtered.filter { state.conditions.matches(it) }
        }

        // Search
        if (state.searchQuery.isNotBlank()) {
            filtered = filtered.filter { ticket ->
                ticket.subject.contains(state.searchQuery, ignoreCase = true) ||
                        ticket.id.contains(state.searchQuery, ignoreCase = true)
            }
        }

        // Sort
        filtered = when (state.sortOption) {
            SortOption.NEWEST -> filtered.sortedByDescending { it.createdAt }
            SortOption.OLDEST -> filtered.sortedBy { it.createdAt }
            SortOption.PRIORITY_HIGH -> filtered.sortedByDescending { it.priority.weight }
            SortOption.PRIORITY_LOW -> filtered.sortedBy { it.priority.weight }
            SortOption.SLA_URGENT -> {
                val now = Clock.System.now()
                filtered.sortedWith(
                    compareBy<Ticket> { !it.isOverdue(now) }
                        .thenBy { ticket ->
                            ticket.responseBy?.toEpochMilliseconds() ?: Long.MAX_VALUE
                        }
                )
            }
        }

        _uiState.update { it.copy(filteredTickets = filtered) }
    }
}
