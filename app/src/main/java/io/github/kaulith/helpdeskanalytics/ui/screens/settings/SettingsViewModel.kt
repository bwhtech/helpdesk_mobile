package io.github.kaulith.helpdeskanalytics.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kaulith.helpdeskanalytics.data.local.credentials.CredentialsManager
import io.github.kaulith.helpdeskanalytics.data.local.preferences.PreferencesManager
import io.github.kaulith.helpdeskanalytics.domain.model.Agent
import io.github.kaulith.helpdeskanalytics.domain.model.SiteProfile
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
    val dynamicColor: Boolean = true,
    val colorScheme: String = "ocean",
    val showLogoutDialog: Boolean = false,
    val showClearCacheDialog: Boolean = false,
    val serverUrl: String = "",
    val userEmail: String? = null,
    val userName: String = "",
    val userRole: String = "",
    val activeAgent: Agent? = null,
    val otherSites: List<SiteProfile> = emptyList()
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
    }

    private fun loadAccountInfo() {
        _uiState.update { it.copy(serverUrl = displayUrl(credentialsManager.getSiteUrl() ?: "")) }

        viewModelScope.launch {
            val otherSites = authRepository.otherSites().map { it.copy(siteUrl = displayUrl(it.siteUrl)) }
            _uiState.update { it.copy(otherSites = otherSites) }
        }

        viewModelScope.launch {
            repository.getCurrentUser().collect { result ->
                if (result is Result.Success) {
                    val user = result.data
                    _uiState.update {
                        it.copy(
                            userName = user.fullName ?: user.email,
                            userEmail = user.email,
                            userRole = user.roles.firstOrNull { r ->
                                r.startsWith("HD")
                            } ?: user.roles.firstOrNull() ?: ""
                        )
                    }
                }
            }
        }
    }

    private fun displayUrl(siteUrl: String) = siteUrl.removePrefix("https://").removePrefix("http://")

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

    fun logout(onSiteChanged: () -> Unit = {}, onLogout: () -> Unit) {
        viewModelScope.launch {
            repository.clearCache()
            authRepository.logout()
            _uiState.update { it.copy(showLogoutDialog = false) }
            if (authRepository.isLoggedIn()) onSiteChanged() else onLogout()
        }
    }

    fun switchSite(profileId: String, onSiteChanged: () -> Unit) {
        viewModelScope.launch {
            authRepository.switchSite(profileId)
            onSiteChanged()
        }
    }
}
