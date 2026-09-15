package io.github.kaulith.helpdeskanalytics.ui.screens.tickets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kaulith.helpdeskanalytics.domain.model.Agent
import io.github.kaulith.helpdeskanalytics.domain.model.Status
import io.github.kaulith.helpdeskanalytics.domain.model.Ticket
import io.github.kaulith.helpdeskanalytics.domain.model.TicketPreset
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
import kotlin.time.Clock
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
import kotlinx.datetime.TimeZone

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
    val preset: TicketPreset? = null,
    val activeAgent: Agent? = null,
    val canWrite: Boolean = false,
    val selectedTicketIds: Set<String> = emptySet(),
)

sealed interface TicketListEvent {
    data class StatusChanged(val ticketId: String, val previous: Status) : TicketListEvent
    data class BulkStatusChanged(val updated: Int, val selected: Int) : TicketListEvent
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
    private var presetApplied = false

    init {
        observeSearch()
        observeActiveAgent()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        _searchQuery.value = query
    }

    /** Status quick-filter chip: a single equals condition, added or cleared in place. */
    fun onStatusFilterChange(status: Status?) =
        setQuickEquals(TicketFilterFields.STATUS, status?.value)

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
        _uiState.update { it.copy(conditions = emptyList(), preset = null) }
        applyFilters()
    }

    /** Dashboard quick stat handoff: show exactly the tickets that card counted. */
    fun onPresetChange(preset: TicketPreset?) {
        _uiState.update { it.copy(preset = preset) }
        applyFilters()
    }

    /**
     * The preset the screen was opened with. Applied once: a later call is the same
     * back stack entry recomposing, which must not restore a chip the user cleared.
     * Open is a plain status, so it selects the Open status chip instead of a preset.
     */
    fun openWithPreset(preset: TicketPreset?) {
        if (presetApplied) return
        presetApplied = true
        if (preset == TicketPreset.OPEN) onStatusFilterChange(Status.OPEN) else onPresetChange(preset)
    }

    fun onSortOptionChange(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
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
                replaceTicket(result.data)
                _events.send(TicketListEvent.StatusChanged(ticket.id, ticket.status))
            }
        }
    }

    fun undoStatus(ticketId: String, previous: Status) {
        viewModelScope.launch {
            val result = repository.updateTicketStatus(ticketId, previous)
            if (result is Result.Success) replaceTicket(result.data)
        }
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
            val updated = ids.count { id ->
                val result = repository.updateTicketStatus(id, status)
                if (result is Result.Success) replaceTicket(result.data)
                result is Result.Success
            }
            _events.send(TicketListEvent.BulkStatusChanged(updated, ids.size))
            clearSelection()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            repository.refresh()
            loadTickets(_uiState.value.activeAgent?.email)
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
                loadTickets(agent?.email)
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

    private fun replaceTicket(updated: Ticket) {
        _uiState.update { state ->
            state.copy(tickets = state.tickets.map { if (it.id == updated.id) updated else it })
        }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        var filtered = state.tickets

        // Dashboard preset, same predicate the quick stat counted with. One clock for the
        // whole pass, so a ticket cannot fall on both sides of its deadline mid-filter.
        state.preset?.let { preset ->
            val now = Clock.System.now()
            val zone = TimeZone.currentSystemDefault()
            filtered = filtered.filter { preset.matches(it, now, zone) }
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
