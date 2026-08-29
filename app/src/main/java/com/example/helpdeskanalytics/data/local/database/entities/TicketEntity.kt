package com.example.helpdeskanalytics.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.helpdeskanalytics.domain.model.Priority
import com.example.helpdeskanalytics.domain.model.Status
import kotlinx.datetime.Instant

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
