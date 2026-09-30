package io.github.kaulith.helpdeskanalytics.ui.agent

import android.app.Application
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.kaulith.helpdeskanalytics.domain.model.Agent
import io.github.kaulith.helpdeskanalytics.testing.FakeAgentRepository
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AgentSwitcherTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `read only selects an agent without issuing a key`() {
        val bob = Agent(email = "bob@x.io", name = "Bob")
        val agentRepository = FakeAgentRepository().apply {
            agents = listOf(bob)
            agentsNeedingWriteKey += bob.email
        }
        val viewModel = AgentSwitcherViewModel(agentRepository).apply { show() }
        composeRule.setContent { AgentSwitcher(viewModel) }

        composeRule.onNodeWithText("Bob").performClick()
        composeRule.waitUntilExactlyOneExists(hasText("Write as Bob?"))
        composeRule.onNodeWithText("Read only").performClick()
        composeRule.waitForIdle()

        assertEquals(bob, agentRepository.activeAgent.value)
        assertEquals(emptyList<String>(), agentRepository.writeKeysProvisioned)
    }
}
