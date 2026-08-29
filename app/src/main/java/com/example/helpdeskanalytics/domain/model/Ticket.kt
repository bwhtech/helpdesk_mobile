package com.example.helpdeskanalytics.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

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
        return when (status) {
            Status.OPEN, Status.REPLIED -> {
                responseBy?.let { currentTime > it } ?: false
            }
            else -> false
        }
    }

    fun ageInHours(currentTime: Instant = Clock.System.now()): Long {
        return (currentTime - createdAt).inWholeHours
    }

    fun isUrgentOpen(): Boolean {
        return priority == Priority.URGENT &&
                (status == Status.OPEN || status == Status.REPLIED)
    }
}
