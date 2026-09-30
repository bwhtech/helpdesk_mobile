package io.github.kaulith.helpdeskanalytics.e2e

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.performScrollToNode
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class LoginTest : AppTest() {

    @Test
    fun signing_in_with_an_api_key_opens_the_dashboard() {
        signIn()
    }

    @Test
    fun disconnecting_returns_to_sign_in() {
        signIn()
        openTab("Settings")

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Disconnect"))
        composeRule.onNodeWithText("Disconnect").performClick()
        composeRule.onNode(hasText("Disconnect") and hasClickAction() and hasAnyAncestor(isDialog())).performClick()

        composeRule.waitUntilExactlyOneExists(hasText("Use an API key instead"), TIMEOUT)
    }
}
