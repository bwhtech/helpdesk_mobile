package io.github.kaulith.helpdeskanalytics.ui.screens.tickets

import io.github.kaulith.helpdeskanalytics.domain.model.Priority
import io.github.kaulith.helpdeskanalytics.domain.model.Status
import io.github.kaulith.helpdeskanalytics.domain.model.Ticket
import io.github.kaulith.helpdeskanalytics.domain.model.TicketPreset
import io.github.kaulith.helpdeskanalytics.testing.FakeAgentRepository
import io.github.kaulith.helpdeskanalytics.testing.FakeTicketRepository
import io.github.kaulith.helpdeskanalytics.testing.MainDispatcherRule
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TicketListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val createdAt = Instant.parse("2026-09-09T12:00:00Z")
    private val repository = FakeTicketRepository().apply {
        tickets.value = listOf(ticket("1", Status.OPEN), ticket("2", Status.OPEN))
    }

    @Test
    fun `cycling a status shows the updated ticket`() = runTest {
        val viewModel = TicketListViewModel(repository, FakeAgentRepository())
        advanceUntilIdle()

        viewModel.cycleStatus(viewModel.uiState.value.tickets.first { it.id == "1" })
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(Status.REPLIED, state.tickets.first { it.id == "1" }.status)
        assertEquals(Status.REPLIED, state.filteredTickets.first { it.id == "1" }.status)
    }

    @Test
    fun `bulk status change counts only the tickets that updated`() = runTest {
        repository.failingTicketIds += "2"
        val viewModel = TicketListViewModel(repository, FakeAgentRepository())
        advanceUntilIdle()

        viewModel.toggleSelected("1")
        viewModel.toggleSelected("2")
        viewModel.bulkSetStatus(Status.RESOLVED)
        advanceUntilIdle()

        assertEquals(TicketListEvent.BulkStatusChanged(updated = 1, selected = 2), viewModel.events.first())
        assertEquals(Status.RESOLVED, viewModel.uiState.value.tickets.first { it.id == "1" }.status)
        assertEquals(Status.OPEN, viewModel.uiState.value.tickets.first { it.id == "2" }.status)
    }

    @Test
    fun `reopening with the same preset keeps a cleared chip cleared`() = runTest {
        val viewModel = TicketListViewModel(repository, FakeAgentRepository())
        advanceUntilIdle()

        viewModel.openWithPreset(TicketPreset.OVERDUE)
        viewModel.onPresetChange(null)
        viewModel.openWithPreset(TicketPreset.OVERDUE)

        assertNull(viewModel.uiState.value.preset)
    }

    private fun ticket(id: String, status: Status) = Ticket(
        id = id,
        subject = "s",
        status = status,
        priority = Priority.LOW,
        assignedTo = null,
        createdAt = createdAt,
        modifiedAt = createdAt,
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
