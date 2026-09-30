package io.github.kaulith.helpdeskanalytics.ui.screens.tickets

import android.app.Application
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.github.kaulith.helpdeskanalytics.domain.model.Agent
import io.github.kaulith.helpdeskanalytics.domain.model.Status
import io.github.kaulith.helpdeskanalytics.testing.FakeAgentRepository
import io.github.kaulith.helpdeskanalytics.testing.FakeTicketRepository
import io.github.kaulith.helpdeskanalytics.testing.ticket
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TicketDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val repository = FakeTicketRepository().apply { tickets.value = listOf(ticket("1", Status.OPEN)) }
    private val agentRepository = FakeAgentRepository()

    @Test
    fun `a missing ticket shows the error`() {
        show("404")

        composeRule.onNodeWithText("Couldn't load ticket").assertExists()
    }

    @Test
    fun `a read only agent gets no composer`() {
        repository.isWritable = false
        show("1")

        composeRule.onNodeWithText("Read-only: replies and comments need an agent with API access").assertExists()
    }

    @Test
    fun `replying with no agent selected asks for one`() {
        show("1")

        sendReply()

        composeRule.onNodeWithText("Select an agent first").assertExists()
    }

    @Test
    fun `a sent reply is confirmed`() {
        agentRepository.activeAgent.value = Agent(email = "ann@x.io", name = "Ann")
        show("1")

        sendReply()

        composeRule.waitUntilExactlyOneExists(hasText("Reply sent"))
    }

    private fun show(ticketId: String) = composeRule.setContent {
        TicketDetailScreen(ticketId, onBack = {}, viewModel = TicketDetailViewModel(repository, agentRepository))
    }

    private fun sendReply() {
        composeRule.onNode(hasSetTextAction()).performTextInput("Fixed in the latest update")
        composeRule.onNodeWithText("Send").performClick()
    }
}
