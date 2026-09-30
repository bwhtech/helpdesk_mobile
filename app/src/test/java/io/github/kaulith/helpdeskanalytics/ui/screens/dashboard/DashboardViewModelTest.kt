package io.github.kaulith.helpdeskanalytics.ui.screens.dashboard

import io.github.kaulith.helpdeskanalytics.domain.model.Agent
import io.github.kaulith.helpdeskanalytics.domain.model.Priority
import io.github.kaulith.helpdeskanalytics.domain.model.Status
import io.github.kaulith.helpdeskanalytics.domain.model.Ticket
import io.github.kaulith.helpdeskanalytics.testing.FakeAgentRepository
import io.github.kaulith.helpdeskanalytics.testing.FakeTicketRepository
import io.github.kaulith.helpdeskanalytics.testing.MainDispatcherRule
import kotlin.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Metrics compute on `Dispatchers.Default`, so these wait for the target state rather than
 * only advancing the scheduler.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeTicketRepository()
    private val agentRepository = FakeAgentRepository()

    @Test
    fun `metrics count the loaded tickets and the header greets the user`() = runTest {
        repository.tickets.value = listOf(ticket("1", Status.OPEN), ticket("2", Status.CLOSED))
        val viewModel = DashboardViewModel(repository, agentRepository)

        val state = viewModel.uiState.first { it.metrics != null && it.userName != null }

        assertEquals(1, state.metrics?.openTicketsCount)
        assertEquals("Ann", state.userName)
    }

    @Test
    fun `selecting an agent reloads metrics for their tickets`() = runTest {
        val viewModel = DashboardViewModel(repository, agentRepository)
        viewModel.uiState.first { it.metrics != null }

        agentRepository.activeAgent.value = Agent(email = "bob@x.io", name = "Bob")

        assertEquals("bob@x.io", viewModel.uiState.first { it.activeAgent != null }.activeAgent?.email)
        advanceUntilIdle()
        assertEquals(listOf(null, "bob@x.io"), repository.requestedAssignees)
    }

    @Test
    fun `a failed load shows the error and stops loading`() = runTest {
        repository.ticketsError = IllegalStateException("Site unreachable")
        val viewModel = DashboardViewModel(repository, agentRepository)

        val state = viewModel.uiState.first { it.error != null }

        assertEquals("Site unreachable", state.error)
        assertEquals(false, state.isLoading)
    }

    private fun ticket(id: String, status: Status) = Clock.System.now().let { now ->
        Ticket(
            id = id,
            subject = "s",
            status = status,
            priority = Priority.LOW,
            assignedTo = null,
            createdAt = now,
            modifiedAt = now,
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
}
