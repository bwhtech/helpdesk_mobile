package io.github.kaulith.helpdeskanalytics.domain.model

import io.github.kaulith.helpdeskanalytics.data.metrics.MetricsCalculator
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A quick stat card counts with the preset and the list it opens filters with the same
 * preset, so the two can never disagree. These lock that down.
 */
class TicketPresetTest {

    private val now = Instant.parse("2026-09-09T12:00:00Z")
    private val zone = TimeZone.UTC

    private val openToday = ticket("1", Status.OPEN, createdAt = now - 2.hours)
    private val openOverdue = ticket(
        "2", Status.OPEN, createdAt = now - 5.days, responseBy = now - 1.hours
    )
    private val repliedOverdue = ticket(
        "3", Status.REPLIED, createdAt = now - 5.days, responseBy = now - 3.hours
    )
    private val resolvedToday = ticket("4", Status.RESOLVED, createdAt = now - 3.hours)
    private val closedToday = ticket("5", Status.CLOSED, createdAt = now - 4.hours)
    private val resolvedLastWeek = ticket("6", Status.RESOLVED, createdAt = now - 8.days)
    private val openInTime = ticket(
        "7", Status.OPEN, createdAt = now - 1.days, responseBy = now + 6.hours
    )

    private val tickets = listOf(
        openToday, openOverdue, repliedOverdue, resolvedToday,
        closedToday, resolvedLastWeek, openInTime
    )

    @Test
    fun `open preset picks only open tickets`() {
        assertEquals(
            listOf("1", "2", "7"),
            tickets.filter { TicketPreset.OPEN.matches(it, now, zone) }.map { it.id }
        )
    }

    @Test
    fun `overdue preset picks open and replied tickets past their response deadline`() {
        assertEquals(
            listOf("2", "3"),
            tickets.filter { TicketPreset.OVERDUE.matches(it, now, zone) }.map { it.id }
        )
    }

    @Test
    fun `resolved today preset picks today's resolved and closed tickets`() {
        assertEquals(
            listOf("4", "5"),
            tickets.filter { TicketPreset.RESOLVED_TODAY.matches(it, now, zone) }.map { it.id }
        )
    }

    @Test
    fun `every quick stat count equals the number of tickets its preset opens`() {
        val metrics = MetricsCalculator.computeMetrics(tickets, now)
        val zone = TimeZone.currentSystemDefault()

        assertEquals(
            tickets.count { TicketPreset.OPEN.matches(it, now, zone) },
            metrics.openTicketsCount
        )
        assertEquals(
            tickets.count { TicketPreset.OVERDUE.matches(it, now, zone) },
            metrics.overdueCount
        )
        assertEquals(
            tickets.count { TicketPreset.RESOLVED_TODAY.matches(it, now, zone) },
            metrics.today.ticketsResolved
        )
    }

    @Test
    fun `slugs survive the trip through a navigation argument`() {
        TicketPreset.entries.forEach {
            assertEquals(it, TicketPreset.fromSlug(it.slug))
        }
        assertEquals(null, TicketPreset.fromSlug(null))
        assertEquals(null, TicketPreset.fromSlug("nonsense"))
    }

    private fun ticket(
        id: String,
        status: Status,
        createdAt: Instant,
        responseBy: Instant? = null
    ) = Ticket(
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
        responseBy = responseBy,
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
