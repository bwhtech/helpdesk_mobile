package com.example.helpdeskanalytics.domain.model

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class SlaTest {

    private val deadline = Instant.parse("2026-05-27T14:06:05Z")

    @Test
    fun `agent reply inside the deadline meets the SLA`() {
        val ticket = ticket(
            lastAgentResponseAt = deadline - 1.hours,
            resolvedAt = deadline + 6.hours
        )

        assertEquals(SlaState.MET, ticket.agentResolutionSla(now = deadline + 7.hours))
    }

    @Test
    fun `a customer closing late does not breach a ticket the agent answered in time`() {
        val ticket = ticket(
            lastAgentResponseAt = deadline - 19.days,
            resolvedAt = deadline + 1.hours
        )

        assertEquals(SlaState.BREACHED, ticket.resolutionSla(now = deadline + 2.hours))
        assertEquals(SlaState.MET, ticket.agentResolutionSla(now = deadline + 2.hours))
    }

    @Test
    fun `agent reply past the deadline breaches the SLA`() {
        val ticket = ticket(
            lastAgentResponseAt = deadline + 25.hours,
            resolvedAt = deadline + 2.days
        )

        assertEquals(SlaState.BREACHED, ticket.agentResolutionSla(now = deadline + 3.days))
    }

    @Test
    fun `an unanswered ticket is due until its deadline passes`() {
        val ticket = ticket(lastAgentResponseAt = null, resolvedAt = null)

        assertEquals(SlaState.DUE, ticket.agentResolutionSla(now = deadline - 1.hours))
        assertEquals(SlaState.BREACHED, ticket.agentResolutionSla(now = deadline + 1.hours))
    }

    @Test
    fun `a ticket still waiting on the customer counts once the agent has replied in time`() {
        val ticket = ticket(lastAgentResponseAt = deadline - 2.days, resolvedAt = null)

        assertEquals(SlaState.MET, ticket.agentResolutionSla(now = deadline + 5.days))
    }

    @Test
    fun `a ticket with no resolution target has no SLA`() {
        val ticket = ticket(lastAgentResponseAt = null, resolvedAt = null, resolutionBy = null)

        assertEquals(SlaState.NONE, ticket.agentResolutionSla(now = deadline))
    }

    private fun ticket(
        lastAgentResponseAt: Instant?,
        resolvedAt: Instant?,
        resolutionBy: Instant? = deadline
    ) = Ticket(
        id = "68562",
        subject = "s",
        status = Status.CLOSED,
        priority = Priority.LOW,
        assignedTo = null,
        createdAt = Instant.fromEpochSeconds(0),
        modifiedAt = Instant.fromEpochSeconds(0),
        firstRespondedAt = null,
        resolvedAt = resolvedAt,
        lastAgentResponseAt = lastAgentResponseAt,
        customerName = null,
        customerId = null,
        assignees = emptyList(),
        responseBy = null,
        resolutionBy = resolutionBy,
        firstResponseTimeMinutes = null,
        avgResponseTimeMinutes = null,
        resolutionTimeHours = null,
        ticketType = null,
        sla = null,
        agreementStatus = null,
        description = null
    )
}
