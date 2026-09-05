package io.github.kaulith.helpdeskanalytics.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.kaulith.helpdeskanalytics.domain.model.Priority
import io.github.kaulith.helpdeskanalytics.domain.model.Status
import kotlin.time.Instant

@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey val id: String,
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
)
