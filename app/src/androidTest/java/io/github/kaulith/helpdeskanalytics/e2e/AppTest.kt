package io.github.kaulith.helpdeskanalytics.e2e

import android.Manifest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.rule.GrantPermissionRule
import io.github.kaulith.helpdeskanalytics.MainActivity
import org.junit.Rule

/** Launches the app logged out, with a fresh database, against [FakeSite]. */
@OptIn(ExperimentalTestApi::class)
abstract class AppTest {

    @get:Rule(order = 0)
    val notifications: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 2)
    val screenshotOnFailure = ScreenshotOnFailure()

    protected fun signIn() {
        // A tap lands by position and misses while the keyboard slides the form up; tap before typing, submit with Done.
        composeRule.onNodeWithText("Use an API key instead").performClick()
        composeRule.onNode(hasSetTextAction() and hasText("Site URL")).performTextReplacement(FakeSite.url)
        composeRule.onNode(hasSetTextAction() and hasText("API Key")).performTextInput("a1b2c3d4e5f6a7b")
        composeRule.onNode(hasSetTextAction() and hasText("API Secret")).performTextInput("7f6e5d4c3b2a1f0")
        composeRule.onNode(hasSetTextAction() and hasText("API Secret")).performImeAction()
        composeRule.waitUntilExactlyOneExists(hasText("Hi, Ann"), TIMEOUT)
    }

    protected fun openTab(label: String) = composeRule.onNode(hasText(label) and isSelectable()).performClick()

    protected fun ticket(id: String) = hasContentDescription("$id, ", substring = true)

    protected companion object {
        const val TIMEOUT = 10_000L
    }
}
