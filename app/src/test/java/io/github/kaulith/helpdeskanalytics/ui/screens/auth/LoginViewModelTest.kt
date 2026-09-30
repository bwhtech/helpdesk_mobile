package io.github.kaulith.helpdeskanalytics.ui.screens.auth

import android.app.Application
import android.net.Uri
import io.github.kaulith.helpdeskanalytics.testing.FakeAgentRepository
import io.github.kaulith.helpdeskanalytics.testing.FakeAuthRepository
import io.github.kaulith.helpdeskanalytics.testing.MainDispatcherRule
import io.github.kaulith.helpdeskanalytics.util.NetworkError
import io.github.kaulith.helpdeskanalytics.util.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Robolectric only for `Uri`, which the OAuth redirect arrives as. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val agentRepository = FakeAgentRepository()
    private val redirectHolder = OAuthRedirectHolder()
    private val viewModel by lazy { LoginViewModel(authRepository, agentRepository, redirectHolder) }

    @Test
    fun `a site url without https is rejected before any request`() = runTest {
        viewModel.onSiteUrlChange("http://helpdesk.example.com")
        viewModel.signIn()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("URL must start with https://", state.siteUrlError)
        assertNull(state.authorizationUrl)
    }

    @Test
    fun `a site without a published client id asks for one`() = runTest {
        viewModel.signIn()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.needsClientId)
        assertEquals("This site did not publish a client ID. Enter it manually.", state.clientIdError)
        assertFalse(state.isLoading)
    }

    @Test
    fun `a discovered client id opens the authorize url`() = runTest {
        authRepository.discoveredClientId = "a1b2c3"
        viewModel.signIn()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("a1b2c3", state.clientId)
        assertEquals("https://support.frappe.io/authorize?client_id=a1b2c3", state.authorizationUrl)
    }

    @Test
    fun `api key sign in needs both key and secret`() = runTest {
        viewModel.signInWithApiKey()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("API key is required", state.apiKeyError)
        assertEquals("API secret is required", state.apiSecretError)
        assertFalse(agentRepository.isLoginUserSelected)
    }

    @Test
    fun `a rejected api key says so`() = runTest {
        authRepository.loginResult = Result.Error(NetworkError.Unauthorized)
        signInWithApiKey()

        val state = viewModel.uiState.value
        assertEquals("Invalid API key or secret", state.generalError)
        assertFalse(state.isLoginSuccess)
    }

    @Test
    fun `a valid api key signs in and selects the user as agent`() = runTest {
        signInWithApiKey()

        assertTrue(viewModel.uiState.value.isLoginSuccess)
        assertTrue(agentRepository.isLoginUserSelected)
    }

    @Test
    fun `a cancelled browser sign in says so`() = runTest {
        viewModel
        advanceUntilIdle()
        redirectHolder.submit(Uri.parse("helpdesk://oauth?error=access_denied"))
        advanceUntilIdle()

        assertEquals("Sign-in was cancelled", viewModel.uiState.value.generalError)
        assertNull(redirectHolder.redirect.value)
    }

    @Test
    fun `a redirect with code and state completes the sign in`() = runTest {
        viewModel
        advanceUntilIdle()
        redirectHolder.submit(Uri.parse("helpdesk://oauth?code=xyz&state=abc"))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLoginSuccess)
        assertTrue(agentRepository.isLoginUserSelected)
    }

    private fun TestScope.signInWithApiKey() {
        viewModel.onApiKeyChange("0a1b2c3d4e5f6a7")
        viewModel.onApiSecretChange("7f6e5d4c3b2a1f0")
        viewModel.signInWithApiKey()
        advanceUntilIdle()
    }
}
