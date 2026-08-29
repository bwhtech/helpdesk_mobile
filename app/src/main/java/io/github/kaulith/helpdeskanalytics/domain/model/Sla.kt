package io.github.kaulith.helpdeskanalytics.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/** Outcome of a single SLA target (first response or resolution) for one ticket. */
enum class SlaState(val label: String) {
    MET("Met"),
    BREACHED("Breached"),
    DUE("Due"),
    NONE("No SLA")
}

/**
 * First-response SLA outcome. [Ticket.responseBy] is the deadline Frappe already
 * computed against the SLA's business hours, so a direct comparison is accurate.
 */
fun Ticket.firstResponseSla(now: Instant = Clock.System.now()): SlaState {
    val deadline = responseBy ?: return SlaState.NONE
    val responded = firstRespondedAt
    return when {
        responded != null -> if (responded <= deadline) SlaState.MET else SlaState.BREACHED
        now > deadline -> SlaState.BREACHED
        else -> SlaState.DUE
    }
}

/** Resolution SLA outcome, compared against [Ticket.resolutionBy]. */
fun Ticket.resolutionSla(now: Instant = Clock.System.now()): SlaState {
    val deadline = resolutionBy ?: return SlaState.NONE
    val resolved = resolvedAt
    return when {
        resolved != null -> if (resolved <= deadline) SlaState.MET else SlaState.BREACHED
        now > deadline -> SlaState.BREACHED
        else -> SlaState.DUE
    }
}

/**
 * Resolution SLA judged on the agent's own last reply rather than [Ticket.resolvedAt].
 *
 * [resolutionSla] reads `resolution_date`, which is stamped by whoever closed the ticket.
 * A customer who sits on a finished ticket past the deadline breaches it there even though
 * the agent answered in time, so that reading overstates breaches.
 */
fun Ticket.agentResolutionSla(now: Instant = Clock.System.now()): SlaState {
    val deadline = resolutionBy ?: return SlaState.NONE
    val lastReply = lastAgentResponseAt ?: return if (now > deadline) SlaState.BREACHED else SlaState.DUE
    return if (lastReply <= deadline) SlaState.MET else SlaState.BREACHED
}
