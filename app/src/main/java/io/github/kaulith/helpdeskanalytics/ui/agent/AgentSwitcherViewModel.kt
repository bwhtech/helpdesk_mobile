package io.github.kaulith.helpdeskanalytics.ui.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kaulith.helpdeskanalytics.domain.model.Agent
import io.github.kaulith.helpdeskanalytics.domain.repository.AgentRepository
import io.github.kaulith.helpdeskanalytics.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AgentSwitcherUiState(
    val activeAgent: Agent? = null,
    val agents: List<Agent> = emptyList(),
    val isLoadingAgents: Boolean = false,
    val isVisible: Boolean = false,
    val isSwitchingAgent: Boolean = false,
    val agentSwitchError: String? = null,
    val writeKeyPromptAgent: Agent? = null
)

class AgentSwitcherViewModel(
    private val agentRepository: AgentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgentSwitcherUiState())
    val uiState: StateFlow<AgentSwitcherUiState> = _uiState.asStateFlow()

    init {
        observeActiveAgent()
        loadAgents()
    }

    private fun observeActiveAgent() {
        viewModelScope.launch {
            agentRepository.getActiveAgent().collect { agent ->
                _uiState.update { it.copy(activeAgent = agent) }
            }
        }
    }

    private fun loadAgents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAgents = true) }
            agentRepository.getAgents().collect { result ->
                when (result) {
                    is Result.Success -> _uiState.update {
                        it.copy(agents = result.data, isLoadingAgents = false)
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(isLoadingAgents = false)
                    }
                    is Result.Loading -> {}
                }
            }
        }
    }

    fun setActiveAgent(agent: Agent?) {
        viewModelScope.launch {
            if (agent != null && agentRepository.needsWriteKey(agent)) {
                _uiState.update { it.copy(writeKeyPromptAgent = agent) }
                return@launch
            }
            activate(agent, provisionWriteKey = false)
        }
    }

    /** Selects the agent the write-key prompt is asking about, minting their key. */
    fun confirmWriteKey() {
        val agent = _uiState.value.writeKeyPromptAgent ?: return
        _uiState.update { it.copy(writeKeyPromptAgent = null) }
        viewModelScope.launch { activate(agent, provisionWriteKey = true) }
    }

    /** Selects that agent for reading only, leaving their API secret alone. */
    fun skipWriteKey() {
        val agent = _uiState.value.writeKeyPromptAgent ?: return
        _uiState.update { it.copy(writeKeyPromptAgent = null) }
        viewModelScope.launch { activate(agent, provisionWriteKey = false) }
    }

    fun dismissWriteKeyPrompt() {
        _uiState.update { it.copy(writeKeyPromptAgent = null) }
    }

    private suspend fun activate(agent: Agent?, provisionWriteKey: Boolean) {
        _uiState.update { it.copy(isSwitchingAgent = true, agentSwitchError = null) }
        when (val result = agentRepository.setActiveAgent(agent, provisionWriteKey)) {
            is Result.Success -> _uiState.update { it.copy(isSwitchingAgent = false) }
            is Result.Error -> _uiState.update {
                it.copy(
                    isSwitchingAgent = false,
                    agentSwitchError = result.exception.message ?: "Couldn't switch agent"
                )
            }
            is Result.Loading -> {}
        }
    }

    fun dismissAgentSwitchError() {
        _uiState.update { it.copy(agentSwitchError = null) }
    }

    fun show() {
        _uiState.update { it.copy(isVisible = true) }
    }

    fun dismiss() {
        _uiState.update { it.copy(isVisible = false) }
    }
}
