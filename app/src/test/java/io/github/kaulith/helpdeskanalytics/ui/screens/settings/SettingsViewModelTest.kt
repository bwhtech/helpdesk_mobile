package io.github.kaulith.helpdeskanalytics.ui.screens.settings

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.github.kaulith.helpdeskanalytics.data.local.preferences.PreferencesManager
import io.github.kaulith.helpdeskanalytics.testing.FakeAgentRepository
import io.github.kaulith.helpdeskanalytics.testing.FakeAuthRepository
import io.github.kaulith.helpdeskanalytics.testing.FakeTicketRepository
import io.github.kaulith.helpdeskanalytics.testing.MainDispatcherRule
import io.github.kaulith.helpdeskanalytics.testing.credentialsManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Robolectric for the DataStore and credentials files. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val repository = FakeTicketRepository()
    private val authRepository = FakeAuthRepository()
    private val credentialsManager = credentialsManager(context)
    private val viewModel by lazy {
        SettingsViewModel(
            PreferencesManager(context),
            repository,
            authRepository,
            credentialsManager,
            FakeAgentRepository()
        )
    }

    @Test
    fun `the account shows the site without its scheme and the helpdesk role`() = runTest {
        credentialsManager.saveSiteUrl("https://support.x.io")
        repository.currentUser = repository.currentUser.copy(roles = listOf("System Manager", "HD Agent"))

        val state = viewModel.uiState.first { it.userRole.isNotEmpty() }

        assertEquals("support.x.io", state.serverUrl)
        assertEquals("HD Agent", state.userRole)
    }

    @Test
    fun `a picked theme is kept`() = runTest {
        viewModel.setThemeMode("dark")

        assertEquals("dark", viewModel.uiState.first { it.themeMode != "system" }.themeMode)
    }

    @Test
    fun `disconnecting signs out and clears the cache`() = runTest {
        val disconnected = CompletableDeferred<Unit>()
        viewModel.logout { disconnected.complete(Unit) }
        disconnected.await()

        assertTrue(authRepository.isLoggedOut)
        assertTrue(repository.isCacheCleared)
    }
}
