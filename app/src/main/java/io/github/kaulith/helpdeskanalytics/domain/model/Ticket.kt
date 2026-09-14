package io.github.kaulith.helpdeskanalytics.domain.model

import kotlin.time.Clock
import kotlin.time.Instant

data class Ticket(
    val id: String,
    val subject: String,
    val status: Status,
    val priority: Priority,
    val assignedTo: String?,
    val createdAt: Instant,
    val modifiedAt: Instant,
    val firstRespondedAt: Instant?,
    val resolvedAt: Instant?,
    val lastAgentResponseAt: Instant?,
    val customerName: String?,
    val customerId: String?,
    val assignees: List<String>,
    val responseBy: Instant?,
    val resolutionBy: Instant?,
    val firstResponseTimeMinutes: Float?,
    val avgResponseTimeMinutes: Float?,
    val resolutionTimeHours: Float?,
    val ticketType: String?,
    val sla: String?,
    val agreementStatus: String?,
    val description: String?
) {
    fun isAssignedTo(agentEmail: String): Boolean {
        return agentEmail in assignees
    }

    fun isOverdue(currentTime: Instant = Clock.System.now()): Boolean {
        return isPending() && responseBy?.let { currentTime > it } == true
    }

    fun ageInHours(currentTime: Instant = Clock.System.now()): Long {
        return (currentTime - createdAt).inWholeHours
    }

    fun isPending(): Boolean {
        return status == Status.OPEN || status == Status.REPLIED
    }

    fun isResolved(): Boolean {
        return status == Status.RESOLVED || status == Status.CLOSED
    }
}
