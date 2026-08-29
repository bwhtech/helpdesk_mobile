package io.github.kaulith.helpdeskanalytics.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kaulith.helpdeskanalytics.data.local.credentials.CredentialsManager
import io.github.kaulith.helpdeskanalytics.data.local.preferences.PreferencesManager
import io.github.kaulith.helpdeskanalytics.domain.model.Agent
import io.github.kaulith.helpdeskanalytics.domain.repository.AgentRepository
import io.github.kaulith.helpdeskanalytics.domain.repository.AuthRepository
import io.github.kaulith.helpdeskanalytics.domain.repository.TicketRepository
import io.github.kaulith.helpdeskanalytics.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: String = "system",
    val dynamicColor: Boolean = false,
    val colorScheme: String = "ocean",
    val showLogoutDialog: Boolean = false,
    val showClearCacheDialog: Boolean = false,
    val serverUrl: String = "",
    val userName: String = "",
    val userRole: String = "",
    val activeAgent: Agent? = null,
    val agents: List<Agent> = emptyList(),
    val isLoadingAgents: Boolean = false,
    val showAgentSelector: Boolean = false,
    val isSwitchingAgent: Boolean = false,
    val agentSwitchError: String? = null,
    val writeKeyPromptAgent: Agent? = null
)

class SettingsViewModel(
    private val preferencesManager: PreferencesManager,
    private val repository: TicketRepository,
    private val authRepository: AuthRepository,
    private val credentialsManager: CredentialsManager,
    private val agentRepository: AgentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadPreferences()
        loadAccountInfo()
        observeActiveAgent()
        loadAgents()
    }

    private fun loadAccountInfo() {
        val siteUrl = credentialsManager.getSiteUrl() ?: ""
        val displayUrl = siteUrl.removePrefix("https://").removePrefix("http://")
        _uiState.update { it.copy(serverUrl = displayUrl) }

        viewModelScope.launch {
            repository.getCurrentUser().collect { result ->
                if (result is Result.Success) {
                    val user = result.data
                    _uiState.update {
                        it.copy(
                            userName = user.fullName ?: user.email,
                            userRole = user.roles.firstOrNull { r ->
                                r.startsWith("HD")
                            } ?: user.roles.firstOrNull() ?: ""
                        )
                    }
                }
            }
        }
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            preferencesManager.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            preferencesManager.dynamicColor.collect { enabled ->
                _uiState.update { it.copy(dynamicColor = enabled) }
            }
        }
        viewModelScope.launch {
            preferencesManager.colorScheme.collect { scheme ->
                _uiState.update { it.copy(colorScheme = scheme) }
            }
        }
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

    fun showAgentSelector() {
        _uiState.update { it.copy(showAgentSelector = true) }
    }

    fun dismissAgentSelector() {
        _uiState.update { it.copy(showAgentSelector = false) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDynamicColor(enabled)
        }
    }

    fun setColorScheme(scheme: String) {
        viewModelScope.launch {
            preferencesManager.setColorScheme(scheme)
        }
    }

    fun showLogoutDialog() {
        _uiState.update { it.copy(showLogoutDialog = true) }
    }

    fun dismissLogoutDialog() {
        _uiState.update { it.copy(showLogoutDialog = false) }
    }

    fun showClearCacheDialog() {
        _uiState.update { it.copy(showClearCacheDialog = true) }
    }

    fun dismissClearCacheDialog() {
        _uiState.update { it.copy(showClearCacheDialog = false) }
    }

    fun clearCache(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearCache()
            _uiState.update { it.copy(showClearCacheDialog = false) }
            onComplete()
        }
    }

    fun logout(onLogout: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            preferencesManager.clearAll()
            repository.clearCache()
            _uiState.update { it.copy(showLogoutDialog = false) }
            onLogout()
        }
    }
}
