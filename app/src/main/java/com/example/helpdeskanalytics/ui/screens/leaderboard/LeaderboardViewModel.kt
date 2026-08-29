package com.example.helpdeskanalytics.ui.screens.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helpdeskanalytics.domain.model.Agent
import com.example.helpdeskanalytics.domain.model.AgentPerformance
import com.example.helpdeskanalytics.domain.model.LeaderboardPeriod
import com.example.helpdeskanalytics.domain.model.Team
import com.example.helpdeskanalytics.domain.model.filter.FilterCondition
import com.example.helpdeskanalytics.domain.model.filter.matches
import com.example.helpdeskanalytics.domain.repository.AgentRepository
import com.example.helpdeskanalytics.domain.repository.TeamRepository
import com.example.helpdeskanalytics.domain.repository.TicketRepository
import com.example.helpdeskanalytics.util.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LeaderboardUiState(
    val agents: List<AgentPerformance> = emptyList(),
    val filteredAgents: List<AgentPerformance> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val hasPermission: Boolean = true,
    val activeAgent: Agent? = null,
    val teams: List<Team> = emptyList(),
    val conditions: List<FilterCondition<AgentPerformance>> = emptyList(),
    val period: LeaderboardPeriod = LeaderboardPeriod.AllTime
)

class LeaderboardViewModel(
    private val repository: TicketRepository,
    private val agentRepository: AgentRepository,
    private val teamRepository: TeamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    private var leaderboardJob: Job? = null

    init {
        observeActiveAgent()
        loadTeams()
    }

    fun refresh(force: Boolean = false) {
        if (force) _uiState.update { it.copy(isRefreshing = true) }
        checkPermissionAndLoad(force)
    }

    fun setPeriod(period: LeaderboardPeriod) {
        if (period == _uiState.value.period) return
        _uiState.update { it.copy(period = period, isLoading = true) }
        checkPermissionAndLoad()
    }

    fun onConditionsChange(conditions: List<FilterCondition<AgentPerformance>>) {
        _uiState.update { it.copy(conditions = conditions) }
        applyFilters()
    }

    private fun observeActiveAgent() {
        viewModelScope.launch {
            agentRepository.getActiveAgent().collect { agent ->
                _uiState.update { it.copy(activeAgent = agent) }
                checkPermissionAndLoad()
            }
        }
    }

    private fun loadTeams() {
        viewModelScope.launch {
            teamRepository.getTeams().collect { result ->
                if (result is Result.Success) {
                    _uiState.update { it.copy(teams = result.data) }
                }
            }
        }
    }

    private fun checkPermissionAndLoad(force: Boolean = false) {
        leaderboardJob?.cancel()
        leaderboardJob = viewModelScope.launch {
            repository.getCurrentUser().collect { result ->
                when (result) {
                    is Result.Success -> {
                        val hasPermission = result.data.canViewLeaderboard()
                        _uiState.update { it.copy(hasPermission = hasPermission) }
                        if (hasPermission) loadAgents(force)
                        else _uiState.update { it.copy(isLoading = false) }
                    }
                    is Result.Loading -> {}
                    is Result.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.exception.message) }
                    }
                }
            }
        }
    }

    private fun loadAgents(force: Boolean) {
        viewModelScope.launch {
            repository.getAgentPerformances(_uiState.value.period, force).collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> {
                        _uiState.update { it.copy(agents = result.data, isLoading = false, isRefreshing = false, error = null) }
                        applyFilters()
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(isLoading = false, isRefreshing = false, error = result.exception.message)
                    }
                }
            }
        }
    }

    private fun applyFilters() {
        val state = _uiState.value
        val filtered = if (state.conditions.isEmpty()) {
            state.agents
        } else {
            state.agents.filter { state.conditions.matches(it) }
                .mapIndexed { index, perf -> perf.copy(rank = index + 1) }
        }
        _uiState.update { it.copy(filteredAgents = filtered) }
    }
}
