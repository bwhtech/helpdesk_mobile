package com.example.helpdeskanalytics.data.remote.dto

import com.google.gson.annotations.SerializedName

data class FrappeListResponse<T>(
    @SerializedName("data") val data: List<T>
)

data class FrappeSingleResponse<T>(
    @SerializedName("data") val data: T
)

data class FrappeMethodResponse<T>(
    @SerializedName("message") val message: T
)

data class TicketDto(
    @SerializedName("name") val name: String,
    @SerializedName("subject") val subject: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("priority") val priority: String?,
    @SerializedName("agent") val agent: String?,
    @SerializedName("creation") val creation: String?,
    @SerializedName("modified") val modified: String?,
    @SerializedName("first_responded_on") val firstRespondedOn: String?,
    @SerializedName("resolution_date") val resolutionDate: String?,
    @SerializedName("last_agent_response") val lastAgentResponse: String?,
    @SerializedName("raised_by") val raisedBy: String?,
    @SerializedName("contact") val contact: String?,
    @SerializedName("customer") val customer: String?,
    @SerializedName("_assign") val assign: String?,
    @SerializedName("response_by") val responseBy: String?,
    @SerializedName("resolution_by") val resolutionBy: String?,
    @SerializedName("first_response_time") val firstResponseTime: Double?,
    @SerializedName("avg_response_time") val avgResponseTime: Double?,
    @SerializedName("resolution_time") val resolutionTime: Double?,
    @SerializedName("ticket_type") val ticketType: String?,
    @SerializedName("sla") val sla: String?,
    @SerializedName("agreement_status") val agreementStatus: String?,
    @SerializedName("description") val description: String?
)

data class UserDto(
    @SerializedName("name") val name: String,
    @SerializedName("full_name") val fullName: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("roles") val roles: List<RoleDto>?
)

data class RoleDto(
    @SerializedName("role") val role: String?
)

// A File attached to a comment or communication.
data class AttachmentDto(
    @SerializedName("name") val name: String,
    @SerializedName("file_url") val fileUrl: String?,
    @SerializedName("file_name") val fileName: String?
)

// Maps the HD Ticket Comment doctype (internal agent notes).
data class CommentDto(
    @SerializedName("name") val name: String,
    @SerializedName("content") val content: String?,
    @SerializedName("commented_by") val commentedBy: String?,
    @SerializedName("creation") val creation: String?,
    @SerializedName("attachments") val attachments: List<AttachmentDto>? = null
)

// Maps the Communication doctype (the customer email thread on a ticket).
data class CommunicationDto(
    @SerializedName("name") val name: String,
    @SerializedName("content") val content: String?,
    @SerializedName("sender") val sender: String?,
    @SerializedName("recipients") val recipients: String?,
    @SerializedName("cc") val cc: String?,
    @SerializedName("sent_or_received") val sentOrReceived: String?,
    @SerializedName("creation") val creation: String?,
    @SerializedName("subject") val subject: String?,
    @SerializedName("attachments") val attachments: List<AttachmentDto>? = null
)

// Payload of helpdesk's get_ticket_activities: the ticket conversation.
data class TicketActivitiesDto(
    @SerializedName("comments") val comments: List<CommentDto>? = null,
    @SerializedName("communications") val communications: List<CommunicationDto>? = null
)

// frappe.core.doctype.user.user.generate_keys response.
data class GenerateKeysResponse(
    @SerializedName("api_secret") val apiSecret: String?
)

// Minimal User payload for reading back an agent's api_key.
data class UserApiKeyDto(
    @SerializedName("api_key") val apiKey: String?
)

// Body for frappe's run_doc_method, which runs a whitelisted controller method on a document.
data class RunDocMethodRequest(
    @SerializedName("dt") val dt: String = "HD Ticket",
    @SerializedName("dn") val dn: String,
    @SerializedName("method") val method: String,
    @SerializedName("args") val args: Map<String, String>
)

data class UpdateTicketRequest(
    @SerializedName("status") val status: String? = null,
    @SerializedName("priority") val priority: String? = null,
    @SerializedName("agent") val agent: String? = null
)

data class TeamDto(
    @SerializedName("name") val name: String,
    @SerializedName("team_name") val teamName: String?,
    @SerializedName("users") val users: List<TeamMemberDto>?
)

data class TeamMemberDto(
    @SerializedName("user") val user: String?
)

data class AgentDto(
    @SerializedName("name") val name: String,
    @SerializedName("agent_name") val agentName: String?,
    @SerializedName("user") val user: String?,
    @SerializedName("is_active") val isActive: Int?,
    @SerializedName("user_image") val userImage: String?
)
