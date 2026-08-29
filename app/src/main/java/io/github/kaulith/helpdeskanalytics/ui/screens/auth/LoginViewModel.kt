package io.github.kaulith.helpdeskanalytics.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kaulith.helpdeskanalytics.domain.repository.AuthRepository
import io.github.kaulith.helpdeskanalytics.util.NetworkError
import io.github.kaulith.helpdeskanalytics.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val siteUrl: String = "https://support.frappe.io",
    val apiKey: String = "",
    val apiSecret: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val siteUrlError: String? = null,
    val apiKeyError: String? = null,
    val apiSecretError: String? = null,
    val generalError: String? = null,
    val isLoginSuccess: Boolean = false
)

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onSiteUrlChange(value: String) {
        _uiState.update { it.copy(siteUrl = value, siteUrlError = null, generalError = null) }
    }

    fun onApiKeyChange(value: String) {
        _uiState.update { it.copy(apiKey = value, apiKeyError = null, generalError = null) }
    }

    fun onApiSecretChange(value: String) {
        _uiState.update { it.copy(apiSecret = value, apiSecretError = null, generalError = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun login() {
        if (!validate()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }

            val result = authRepository.validateCredentials(
                siteUrl = _uiState.value.siteUrl.trim(),
                apiKey = _uiState.value.apiKey.trim(),
                apiSecret = _uiState.value.apiSecret.trim()
            )

            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, isLoginSuccess = true) }
                }
                is Result.Error -> {
                    val errorMsg = when (result.exception) {
                        is NetworkError.Unauthorized -> "Invalid API key or secret"
                        is NetworkError.Timeout -> "Connection timed out. Check your site URL."
                        is NetworkError.NoInternet -> "No internet connection"
                        is NetworkError.ApiError -> "Server error: ${result.exception.message}"
                        is NetworkError.Unknown -> loginFailureMessage(result.exception.cause)
                        else -> "Connection failed. Verify your site URL and credentials."
                    }
                    _uiState.update { it.copy(isLoading = false, generalError = errorMsg) }
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun loginFailureMessage(cause: Throwable?): String {
        cause ?: return "Connection failed. Verify your site URL and credentials."
        val detail = cause.message?.takeIf { it.isNotBlank() } ?: cause::class.java.name
        return "Login failed: $detail"
    }

    private fun validate(): Boolean {
        var isValid = true
        val current = _uiState.value

        if (current.siteUrl.isBlank()) {
            _uiState.update { it.copy(siteUrlError = "Site URL is required") }
            isValid = false
        } else if (!current.siteUrl.trim().startsWith("https://")) {
            _uiState.update { it.copy(siteUrlError = "URL must start with https://") }
            isValid = false
        }

        if (current.apiKey.isBlank()) {
            _uiState.update { it.copy(apiKeyError = "API key is required") }
            isValid = false
        }

        if (current.apiSecret.isBlank()) {
            _uiState.update { it.copy(apiSecretError = "API secret is required") }
            isValid = false
        }

        return isValid
    }
}
