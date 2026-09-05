package io.github.kaulith.helpdeskanalytics.ui.screens.auth

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kaulith.helpdeskanalytics.domain.repository.AuthRepository
import io.github.kaulith.helpdeskanalytics.util.NetworkError
import io.github.kaulith.helpdeskanalytics.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val siteUrl: String = "https://support.frappe.io",
    val clientId: String = "",
    val apiKey: String = "",
    val apiSecret: String = "",
    val useApiKey: Boolean = false,
    val needsClientId: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val siteUrlError: String? = null,
    val clientIdError: String? = null,
    val apiKeyError: String? = null,
    val apiSecretError: String? = null,
    val generalError: String? = null,
    val authorizationUrl: String? = null,
    val isLoginSuccess: Boolean = false
)

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val oAuthRedirectHolder: OAuthRedirectHolder
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            oAuthRedirectHolder.redirect.filterNotNull().collect { redirect ->
                oAuthRedirectHolder.clear()
                completeOAuth(redirect)
            }
        }
    }

    fun onSiteUrlChange(value: String) {
        _uiState.update { it.copy(siteUrl = value, siteUrlError = null, generalError = null) }
    }

    fun onClientIdChange(value: String) {
        _uiState.update { it.copy(clientId = value, clientIdError = null, generalError = null) }
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

    fun toggleApiKeyEntry() {
        _uiState.update {
            it.copy(
                useApiKey = !it.useApiKey,
                siteUrlError = null,
                clientIdError = null,
                apiKeyError = null,
                apiSecretError = null,
                generalError = null
            )
        }
    }

    /**
     * Resolves the site's client id, then hands the authorize URL to the screen,
     * which opens it in the browser.
     */
    fun signIn() {
        if (!validateSiteUrl()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }

            val siteUrl = _uiState.value.siteUrl.trim()
            val typedClientId = _uiState.value.clientId.trim()
            val clientId = typedClientId.ifBlank { authRepository.discoverOAuthClientId(siteUrl) }

            if (clientId.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        needsClientId = true,
                        clientIdError = "This site did not publish a client ID. Enter it manually."
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    clientId = clientId,
                    authorizationUrl = authRepository.beginOAuthLogin(siteUrl, clientId)
                )
            }
        }
    }

    fun onAuthorizationLaunched() {
        _uiState.update { it.copy(authorizationUrl = null) }
    }

    fun onBrowserUnavailable() {
        _uiState.update { it.copy(generalError = "No browser available to sign in with") }
    }

    fun signInWithApiKey() {
        if (!validateApiKeyFields()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }

            val result = authRepository.validateCredentials(
                siteUrl = _uiState.value.siteUrl.trim(),
                apiKey = _uiState.value.apiKey.trim(),
                apiSecret = _uiState.value.apiSecret.trim()
            )
            applyResult(result, unauthorizedMessage = "Invalid API key or secret")
        }
    }

    private suspend fun completeOAuth(redirect: Uri) {
        redirect.getQueryParameter("error")?.let { error ->
            _uiState.update { it.copy(isLoading = false, generalError = authorizationErrorMessage(error)) }
            return
        }

        val code = redirect.getQueryParameter("code")
        val state = redirect.getQueryParameter("state")
        if (code == null || state == null) {
            _uiState.update { it.copy(isLoading = false, generalError = "Sign-in was not completed") }
            return
        }

        _uiState.update { it.copy(isLoading = true, generalError = null) }
        applyResult(
            authRepository.completeOAuthLogin(code, state),
            unauthorizedMessage = "The site rejected the sign-in"
        )
    }

    private fun applyResult(result: Result<*>, unauthorizedMessage: String) {
        when (result) {
            is Result.Success -> _uiState.update { it.copy(isLoading = false, isLoginSuccess = true) }
            is Result.Error -> {
                val message = when (result.exception) {
                    is NetworkError.Unauthorized -> unauthorizedMessage
                    is NetworkError.Timeout -> "Connection timed out. Check your site URL."
                    is NetworkError.NoInternet -> "No internet connection"
                    is NetworkError.ApiError -> "Server error: ${result.exception.message}"
                    is NetworkError.Unknown -> loginFailureMessage(result.exception.cause)
                    else -> "Connection failed. Verify your site URL and credentials."
                }
                _uiState.update { it.copy(isLoading = false, generalError = message) }
            }
            is Result.Loading -> {}
        }
    }

    private fun authorizationErrorMessage(error: String): String = when (error) {
        "access_denied" -> "Sign-in was cancelled"
        "invalid_client" -> "The site does not recognise this client ID"
        else -> "Sign-in failed: $error"
    }

    private fun loginFailureMessage(cause: Throwable?): String {
        cause ?: return "Connection failed. Verify your site URL and credentials."
        val detail = cause.message?.takeIf { it.isNotBlank() } ?: cause::class.java.name
        return "Login failed: $detail"
    }

    private fun validateSiteUrl(): Boolean {
        val siteUrl = _uiState.value.siteUrl.trim()
        val error = when {
            siteUrl.isBlank() -> "Site URL is required"
            !siteUrl.startsWith("https://") -> "URL must start with https://"
            else -> null
        }
        _uiState.update { it.copy(siteUrlError = error) }
        return error == null
    }

    private fun validateApiKeyFields(): Boolean {
        val current = _uiState.value
        val keyError = if (current.apiKey.isBlank()) "API key is required" else null
        val secretError = if (current.apiSecret.isBlank()) "API secret is required" else null
        _uiState.update { it.copy(apiKeyError = keyError, apiSecretError = secretError) }
        return validateSiteUrl() && keyError == null && secretError == null
    }
}
