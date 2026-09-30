package io.github.kaulith.helpdeskanalytics.ui.screens.tickets

import android.app.Application
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.kaulith.helpdeskanalytics.domain.model.Status
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
class TicketsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val repository = FakeTicketRepository()
    private val openTicket = hasContentDescription("1, s, status Open", substring = true)
    private val closedTicket = hasContentDescription("2, s, status Closed", substring = true)

    @Test
    fun `a failed load shows the error with a retry`() {
        repository.ticketsError = IllegalStateException("Site unreachable")
        show()

        composeRule.onNodeWithText("Site unreachable").assertExists()
        composeRule.onNodeWithText("Retry").assertExists()
    }

    @Test
    fun `a status chip narrows the list`() {
        repository.tickets.value = listOf(ticket("1", Status.OPEN), ticket("2", Status.CLOSED))
        show()
        composeRule.waitUntilExactlyOneExists(openTicket)

        composeRule.onNode(hasText("Closed") and isSelectable()).performClick()

        composeRule.waitUntilDoesNotExist(openTicket)
        composeRule.onNode(closedTicket).assertExists()
    }

    @Test
    fun `tapping a ticket opens it`() {
        repository.tickets.value = listOf(ticket("1", Status.OPEN))
        var opened: String? = null
        show { opened = it }
        composeRule.waitUntilExactlyOneExists(openTicket)

        composeRule.onNode(openTicket).performClick()

        assertEquals("1", opened)
    }

    private fun show(onTicketClick: (String) -> Unit = {}) {
        val viewModel = TicketListViewModel(repository, FakeAgentRepository())
        composeRule.setContent { TicketsScreen(onTicketClick, viewModel = viewModel) }
    }
}
