package io.github.kaulith.helpdeskanalytics.ui.screens.analytics

import io.github.kaulith.helpdeskanalytics.domain.model.Priority
import io.github.kaulith.helpdeskanalytics.domain.model.ResponseTimePercentiles
import io.github.kaulith.helpdeskanalytics.domain.model.Status
import io.github.kaulith.helpdeskanalytics.domain.model.Ticket
import io.github.kaulith.helpdeskanalytics.testing.FakeAgentRepository
import io.github.kaulith.helpdeskanalytics.testing.FakeTicketRepository
import io.github.kaulith.helpdeskanalytics.testing.MainDispatcherRule
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Recompute runs on `Dispatchers.Default`, so these wait for the target state instead of advancing the scheduler. */
class AnalyticsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `response percentiles follow the selected range`() = runTest {
        val now = Clock.System.now()
        val repository = FakeTicketRepository().apply {
            tickets.value = listOf(
                ticket("1", createdAt = now - 2.hours, firstResponseTimeMinutes = 500f),
                ticket("2", createdAt = now - 60.days, firstResponseTimeMinutes = 10f)
            )
        }
        val viewModel = AnalyticsViewModel(repository, FakeAgentRepository())
        viewModel.uiState.first { it.totalTickets == 2 }

        viewModel.setTimeRange(TimeRange.WEEK)
        val week = viewModel.uiState.first { it.totalTickets == 1 }

        assertEquals(ResponseTimePercentiles(p50 = 500f, p90 = 500f, p95 = 500f), week.responseTimePercentiles)
    }

    private fun ticket(id: String, createdAt: Instant, firstResponseTimeMinutes: Float) = Ticket(
        id = id,
        subject = "s",
        status = Status.RESOLVED,
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
        firstResponseTimeMinutes = firstResponseTimeMinutes,
        avgResponseTimeMinutes = null,
        resolutionTimeHours = null,
        ticketType = null,
        sla = null,
        agreementStatus = null,
        description = null
    )
}
