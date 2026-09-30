package io.github.kaulith.helpdeskanalytics.ui.screens.dashboard

import android.app.Application
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.kaulith.helpdeskanalytics.domain.model.Agent
import io.github.kaulith.helpdeskanalytics.domain.model.Status
import io.github.kaulith.helpdeskanalytics.domain.model.TicketPreset
import io.github.kaulith.helpdeskanalytics.testing.FakeAgentRepository
import io.github.kaulith.helpdeskanalytics.testing.FakeTicketRepository
import io.github.kaulith.helpdeskanalytics.testing.ticket
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DashboardScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val repository = FakeTicketRepository()
    private val agentRepository = FakeAgentRepository()

    @Test
    fun `a failed load shows the error with a retry`() {
        repository.ticketsError = IllegalStateException("Site unreachable")
        show()

        composeRule.onNodeWithText("Site unreachable").assertExists()
        composeRule.onNodeWithText("Retry").assertExists()
    }

    @Test
    fun `a quick stat opens its preset`() {
        repository.tickets.value = listOf(ticket("1", Status.OPEN))
        var opened: TicketPreset? = null
        show { opened = it }

        val openStat = hasContentDescription("Open: 1, opens the ticket list")
        composeRule.waitUntilExactlyOneExists(openStat)
        composeRule.onNode(openStat).performClick()

        assertEquals(TicketPreset.OPEN, opened)
    }

    @Test
    fun `the greeting names the selected agent`() {
        agentRepository.activeAgent.value = Agent(email = "bob@x.io", name = "Bob Smith")
        show()

        composeRule.waitUntilExactlyOneExists(hasText("Hi, Bob"))
    }

    private fun show(onOpenPreset: (TicketPreset) -> Unit = {}) = composeRule.setContent {
        DashboardScreen(onOpenPreset, DashboardViewModel(repository, agentRepository))
    }
}
