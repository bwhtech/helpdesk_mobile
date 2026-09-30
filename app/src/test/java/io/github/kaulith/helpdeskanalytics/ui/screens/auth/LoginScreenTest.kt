package io.github.kaulith.helpdeskanalytics.ui.screens.auth

import android.app.Application
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import io.github.kaulith.helpdeskanalytics.testing.FakeAgentRepository
import io.github.kaulith.helpdeskanalytics.testing.FakeAuthRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class LoginScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var isSignedIn = false

    @Test
    fun `a missing site url is flagged on the field`() {
        show()

        field("Site URL").performTextClearance()
        composeRule.onNodeWithText("Sign in").performScrollTo().performClick()

        composeRule.onNodeWithText("Site URL is required").assertExists()
    }

    @Test
    fun `an api key signs in`() {
        show()

        composeRule.onNodeWithText("Use an API key instead").performClick()
        field("API Key").performTextInput("0a1b2c3d4e5f6a7")
        field("API Secret").performTextInput("7f6e5d4c3b2a1f0")
        composeRule.onNodeWithText("Sign in").performScrollTo().performClick()

        composeRule.waitUntil { isSignedIn }
    }

    private fun field(label: String) = composeRule.onNode(hasSetTextAction() and hasText(label))

    private fun show() {
        val viewModel = LoginViewModel(FakeAuthRepository(), FakeAgentRepository(), OAuthRedirectHolder())
        composeRule.setContent { LoginScreen(onLoginSuccess = { isSignedIn = true }, viewModel = viewModel) }
    }
}
