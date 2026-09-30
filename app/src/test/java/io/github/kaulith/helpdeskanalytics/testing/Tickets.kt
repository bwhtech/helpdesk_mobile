package io.github.kaulith.helpdeskanalytics.testing

import io.github.kaulith.helpdeskanalytics.domain.model.Priority
import io.github.kaulith.helpdeskanalytics.domain.model.Status
import io.github.kaulith.helpdeskanalytics.domain.model.Ticket
import kotlin.time.Clock

fun ticket(id: String, status: Status) = Clock.System.now().let { now ->
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
