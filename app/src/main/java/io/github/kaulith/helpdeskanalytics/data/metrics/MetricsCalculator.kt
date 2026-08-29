package io.github.kaulith.helpdeskanalytics.data.metrics

import io.github.kaulith.helpdeskanalytics.domain.model.AgentPerformance
import io.github.kaulith.helpdeskanalytics.domain.model.PeriodMetrics
import io.github.kaulith.helpdeskanalytics.domain.model.Priority
import io.github.kaulith.helpdeskanalytics.domain.model.ResponseTimePercentiles
import io.github.kaulith.helpdeskanalytics.domain.model.Status
import io.github.kaulith.helpdeskanalytics.domain.model.Ticket
import io.github.kaulith.helpdeskanalytics.domain.model.TicketMetrics
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

object MetricsCalculator {

    fun computeMetrics(tickets: List<Ticket>, now: Instant = Clock.System.now()): TicketMetrics {
        val tz = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(tz).date
        val startOfWeek = today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY)
        val startOfMonth = today.minus(today.dayOfMonth - 1, DateTimeUnit.DAY)

        val todayTickets = tickets.filter {
            it.createdAt.toLocalDateTime(tz).date == today
        }
        val weekTickets = tickets.filter {
            it.createdAt.toLocalDateTime(tz).date >= startOfWeek
        }
        val monthTickets = tickets.filter {
            it.createdAt.toLocalDateTime(tz).date >= startOfMonth
        }

        val openTickets = tickets.filter { it.status == Status.OPEN }
        val urgentOpen = openTickets.filter { it.priority == Priority.URGENT }
        val overdueTickets = tickets.filter { it.isOverdue(now) }

        val dueToday = openTickets.count { ticket ->
            ticket.resolutionBy?.let {
                it.toLocalDateTime(tz).date == today
            } ?: false
        }

        val responseTimes = tickets.mapNotNull { it.firstResponseTimeMinutes }.sorted()
        val avgResponse = responseTimes.average().toFloat().takeIf { !it.isNaN() } ?: 0f
        val avgResolution = tickets.mapNotNull { it.resolutionTimeHours }
            .average().toFloat().takeIf { !it.isNaN() } ?: 0f

        return TicketMetrics(
            today = periodMetrics(todayTickets),
            thisWeek = periodMetrics(weekTickets),
            thisMonth = periodMetrics(monthTickets),
            averageResponseTime = avgResponse,
            averageResolutionTime = avgResolution,
            openTicketsCount = openTickets.size,
            urgentOpenCount = urgentOpen.size,
            overdueCount = overdueTickets.size,
            dueToday = dueToday,
            responseTimePercentiles = computePercentiles(responseTimes)
        )
    }

    fun computeAgentPerformances(
        tickets: List<Ticket>,
        currentUserEmail: String?,
        agentNames: Map<String, String> = emptyMap()
    ): List<AgentPerformance> {
        val byAgent = ticketsByAgent(tickets)

        return byAgent.map { (agentEmail, agentTickets) ->
            val resolved = agentTickets.count {
                it.status == Status.RESOLVED || it.status == Status.CLOSED
            }
            agentPerformance(agentEmail, resolved, agentTickets, agentNames, currentUserEmail)
        }.rankByTicketsResolved()
    }

    /**
     * The app only ever holds the newest slice of tickets, so [resolvedCounts] must come
     * from the server to rank on all-time totals. Averages stay windowed to the cache.
     */
    fun rankAgents(
        resolvedCounts: Map<String, Int>,
        tickets: List<Ticket>,
        agentNames: Map<String, String>,
        currentUserEmail: String?
    ): List<AgentPerformance> {
        val byAgent = ticketsByAgent(tickets)

        return resolvedCounts.map { (agentEmail, resolved) ->
            agentPerformance(
                agentEmail, resolved, byAgent[agentEmail].orEmpty(), agentNames, currentUserEmail
            )
        }.rankByTicketsResolved()
    }

    private fun ticketsByAgent(tickets: List<Ticket>): Map<String, List<Ticket>> = tickets
        .flatMap { ticket ->
            ticket.assignees.ifEmpty { listOfNotNull(ticket.assignedTo) }.map { it to ticket }
        }
        .groupBy({ it.first }, { it.second })

    private fun agentPerformance(
        agentEmail: String,
        ticketsResolved: Int,
        agentTickets: List<Ticket>,
        agentNames: Map<String, String>,
        currentUserEmail: String?
    ) = AgentPerformance(
        agentEmail = agentEmail,
        agentName = agentNames[agentEmail] ?: agentEmail.substringBefore("@"),
        rank = 0,
        ticketsResolved = ticketsResolved,
        averageResponseTime = agentTickets.mapNotNull { it.firstResponseTimeMinutes }
            .average().toFloat().takeIf { !it.isNaN() } ?: 0f,
        averageResolutionTime = agentTickets.mapNotNull { it.resolutionTimeHours }
            .average().toFloat().takeIf { !it.isNaN() } ?: 0f,
        isCurrentUser = agentEmail == currentUserEmail
    )

    private fun List<AgentPerformance>.rankByTicketsResolved(): List<AgentPerformance> =
        sortedByDescending { it.ticketsResolved }
            .mapIndexed { index, perf -> perf.copy(rank = index + 1) }

    private fun periodMetrics(tickets: List<Ticket>): PeriodMetrics {
        val resolved = tickets.count {
            it.status == Status.RESOLVED || it.status == Status.CLOSED
        }
        val open = tickets.count {
            it.status == Status.OPEN || it.status == Status.REPLIED
        }
        val total = tickets.size
        val trendPct = if (total > 0) {
            ((resolved.toFloat() / total) * 100f) - 50f
        } else 0f

        return PeriodMetrics(
            ticketsCreated = total,
            ticketsResolved = resolved,
            trendPercentage = trendPct,
            openTickets = open
        )
    }

    private fun computePercentiles(sorted: List<Float>): ResponseTimePercentiles {
        if (sorted.isEmpty()) return ResponseTimePercentiles(0f, 0f, 0f)
        return ResponseTimePercentiles(
            p50 = percentile(sorted, 50),
            p90 = percentile(sorted, 90),
            p95 = percentile(sorted, 95)
        )
    }

    private fun percentile(sorted: List<Float>, p: Int): Float {
        if (sorted.isEmpty()) return 0f
        val index = (p / 100.0 * (sorted.size - 1)).toInt().coerceIn(0, sorted.size - 1)
        return sorted[index]
    }
}
