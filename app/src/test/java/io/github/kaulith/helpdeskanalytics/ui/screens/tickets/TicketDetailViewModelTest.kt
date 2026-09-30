package io.github.kaulith.helpdeskanalytics.ui.screens.tickets

import io.github.kaulith.helpdeskanalytics.domain.model.Agent
import io.github.kaulith.helpdeskanalytics.domain.model.Priority
import io.github.kaulith.helpdeskanalytics.domain.model.Status
import io.github.kaulith.helpdeskanalytics.domain.model.Ticket
import io.github.kaulith.helpdeskanalytics.testing.FakeAgentRepository
import io.github.kaulith.helpdeskanalytics.testing.FakeTicketRepository
import io.github.kaulith.helpdeskanalytics.testing.MainDispatcherRule
import kotlin.time.Instant
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

@OptIn(ExperimentalCoroutinesApi::class)
class TicketDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeTicketRepository().apply {
        tickets.value = listOf(ticket("1", Status.OPEN))
    }
    private val agentRepository = FakeAgentRepository()
    private val ann = Agent(email = "ann@x.io", name = "Ann")

    @Test
    fun `a missing ticket shows the error`() = runTest {
        val viewModel = loadedViewModel("404")

        val state = viewModel.uiState.value
        assertEquals("404", state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `writing without an active agent asks for one and writes nothing`() = runTest {
        val viewModel = loadedViewModel("1")

        viewModel.updateStatus("1", Status.RESOLVED)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showSelectAgentPrompt)
        assertEquals(Status.OPEN, state.ticket?.status)
        assertNull(state.snackbarMessage)
    }

    @Test
    fun `a status change applies the saved ticket`() = runTest {
        agentRepository.activeAgent.value = ann
        val viewModel = loadedViewModel("1")

        viewModel.updateStatus("1", Status.RESOLVED)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(Status.RESOLVED, state.ticket?.status)
        assertEquals("Status updated to Resolved", state.snackbarMessage)
        assertFalse(state.isUpdating)
    }

    @Test
    fun `a failed priority change keeps the ticket and says so`() = runTest {
        agentRepository.activeAgent.value = ann
        repository.failingTicketIds += "1"
        val viewModel = loadedViewModel("1")

        viewModel.updatePriority("1", Priority.URGENT)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(Priority.LOW, state.ticket?.priority)
        assertEquals("Failed to update priority", state.snackbarMessage)
        assertFalse(state.isUpdating)
    }

    @Test
    fun `a sent reply clears the busy flag and confirms`() = runTest {
        agentRepository.activeAgent.value = ann
        val viewModel = loadedViewModel("1")

        viewModel.sendReply("1", "Fixed in the latest update")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Reply sent", state.snackbarMessage)
        assertFalse(state.isSendingReply)
    }

    private fun TestScope.loadedViewModel(ticketId: String) =
        TicketDetailViewModel(repository, agentRepository).also {
            it.loadTicket(ticketId)
            advanceUntilIdle()
        }

    private fun ticket(id: String, status: Status) = Ticket(
        id = id,
        subject = "s",
        status = status,
        priority = Priority.LOW,
        assignedTo = null,
        createdAt = Instant.parse("2026-09-09T12:00:00Z"),
        modifiedAt = Instant.parse("2026-09-09T12:00:00Z"),
        firstRespondedAt = null,
        resolvedAt = null,
        lastAgentResponseAt = null,
        customerName = null,
        customerId = null,
        assignees = emptyList(),
        responseBy = null,
        resolutionBy = null,
        firstResponseTimeMinutes = null,
        avgResponseTimeMinutes = null,
        resolutionTimeHours = null,
        ticketType = null,
        sla = null,
        agreementStatus = null,
        description = null
    )
}
