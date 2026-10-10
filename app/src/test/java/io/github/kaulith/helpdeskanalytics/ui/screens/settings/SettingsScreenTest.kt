package io.github.kaulith.helpdeskanalytics.ui.screens.settings

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import io.github.kaulith.helpdeskanalytics.data.local.preferences.PreferencesManager
import io.github.kaulith.helpdeskanalytics.testing.FakeAgentRepository
import io.github.kaulith.helpdeskanalytics.testing.FakeAuthRepository
import io.github.kaulith.helpdeskanalytics.testing.FakeTicketRepository
import io.github.kaulith.helpdeskanalytics.testing.credentialsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `disconnect asks before signing out`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val authRepository = FakeAuthRepository()
        val viewModel = SettingsViewModel(
            PreferencesManager(context),
            FakeTicketRepository(),
            authRepository,
            credentialsManager(context),
            FakeAgentRepository()
        )
        composeRule.setContent { SettingsScreen(viewModel = viewModel) }

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Disconnect"))
        composeRule.onNodeWithText("Disconnect").performClick()
        assertFalse(authRepository.isLoggedOut)
        composeRule.onNode(hasText("Disconnect") and hasClickAction() and hasAnyAncestor(isDialog())).performClick()

        composeRule.waitUntil { authRepository.isLoggedOut }
    }

    @Test
    fun `send test notification posts one`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val viewModel = SettingsViewModel(
            PreferencesManager(context),
            FakeTicketRepository(),
            FakeAuthRepository(),
            credentialsManager(context),
            FakeAgentRepository()
        )
        composeRule.setContent { SettingsScreen(viewModel = viewModel) }

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Send test notification"))
        composeRule.onNodeWithText("Send test notification").performClick()

        val posted = shadowOf(context.getSystemService(NotificationManager::class.java)).allNotifications
        assertEquals(1, posted.size)
    }
}
