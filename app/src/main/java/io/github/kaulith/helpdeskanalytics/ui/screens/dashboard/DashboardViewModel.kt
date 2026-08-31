package io.github.kaulith.helpdeskanalytics.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kaulith.helpdeskanalytics.domain.model.Agent
import io.github.kaulith.helpdeskanalytics.domain.model.TicketMetrics
import io.github.kaulith.helpdeskanalytics.domain.repository.AgentRepository
import io.github.kaulith.helpdeskanalytics.domain.repository.TicketRepository
import io.github.kaulith.helpdeskanalytics.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val metrics: TicketMetrics? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val userName: String? = null,
    val activeAgent: Agent? = null
)

class DashboardViewModel(
    private val repository: TicketRepository,
    private val agentRepository: AgentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var metricsJob: Job? = null

    init {
        loadUser()
        observeActiveAgent()
    }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            if (force) {
                _uiState.update { it.copy(isRefreshing = true) }
                repository.refresh()
            }
            loadMetrics(_uiState.value.activeAgent?.email)
        }
    }

    private fun observeActiveAgent() {
        viewModelScope.launch {
            agentRepository.getActiveAgent().collect { agent ->
                _uiState.update { it.copy(activeAgent = agent) }
                loadMetrics(agent?.email)
            }
        }
    }

    private fun loadUser() {
        viewModelScope.launch {
            repository.getCurrentUser().collect { result ->
                if (result is Result.Success) {
                    _uiState.update { it.copy(userName = result.data.fullName) }
                }
            }
        }
    }

    private fun loadMetrics(assignedTo: String?) {
        metricsJob?.cancel()
        metricsJob = viewModelScope.launch {
            repository.getTickets(assignedTo = assignedTo).collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.update {
                        it.copy(isLoading = it.metrics == null, error = null)
                    }
                    is Result.Success -> {
                        val metrics = withContext(Dispatchers.Default) {
                            io.github.kaulith.helpdeskanalytics.data.metrics.MetricsCalculator
                                .computeMetrics(result.data)
                        }
                        _uiState.update {
                            it.copy(
                                metrics = metrics,
                                isLoading = false,
                                isRefreshing = false,
                                error = null
                            )
                        }
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = result.exception.message ?: "Failed to load metrics"
                        )
                    }
                }
            }
        }
    }
}
