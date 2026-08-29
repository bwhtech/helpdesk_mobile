package io.github.kaulith.helpdeskanalytics.data.mapper

import io.github.kaulith.helpdeskanalytics.data.local.database.entities.AgentEntity
import io.github.kaulith.helpdeskanalytics.data.local.database.entities.CommentEntity
import io.github.kaulith.helpdeskanalytics.data.local.database.entities.TeamEntity
import io.github.kaulith.helpdeskanalytics.data.local.database.entities.TicketEntity
import io.github.kaulith.helpdeskanalytics.data.local.database.entities.UserEntity
import io.github.kaulith.helpdeskanalytics.domain.model.Agent
import io.github.kaulith.helpdeskanalytics.domain.model.Comment
import io.github.kaulith.helpdeskanalytics.domain.model.Team
import io.github.kaulith.helpdeskanalytics.domain.model.Ticket
import io.github.kaulith.helpdeskanalytics.domain.model.User

fun TicketEntity.toDomain(): Ticket = Ticket(
    id = id,
    subject = subject,
    status = status,
    priority = priority,
    assignedTo = assignedTo,
    createdAt = createdAt,
    modifiedAt = modifiedAt,
    firstRespondedAt = firstRespondedAt,
    resolvedAt = resolvedAt,
    lastAgentResponseAt = lastAgentResponseAt,
    customerName = customerName,
    customerId = customerId,
    assignees = assignees,
    responseBy = responseBy,
    resolutionBy = resolutionBy,
    firstResponseTimeMinutes = firstResponseTimeMinutes,
    avgResponseTimeMinutes = avgResponseTimeMinutes,
    resolutionTimeHours = resolutionTimeHours,
    ticketType = ticketType,
    sla = sla,
    agreementStatus = agreementStatus,
    description = description
)

fun Ticket.toEntity(): TicketEntity = TicketEntity(
    id = id,
    subject = subject,
    status = status,
    priority = priority,
    assignedTo = assignedTo,
    createdAt = createdAt,
    modifiedAt = modifiedAt,
    firstRespondedAt = firstRespondedAt,
    resolvedAt = resolvedAt,
    lastAgentResponseAt = lastAgentResponseAt,
    customerName = customerName,
    customerId = customerId,
    assignees = assignees,
    responseBy = responseBy,
    resolutionBy = resolutionBy,
    firstResponseTimeMinutes = firstResponseTimeMinutes,
    avgResponseTimeMinutes = avgResponseTimeMinutes,
    resolutionTimeHours = resolutionTimeHours,
    ticketType = ticketType,
    sla = sla,
    agreementStatus = agreementStatus,
    description = description
)

fun UserEntity.toDomain(): User = User(
    email = email,
    fullName = fullName,
    roles = roles,
    hasTeamLeadPermission = hasTeamLeadPermission
)

fun User.toEntity(): UserEntity = UserEntity(
    email = email,
    fullName = fullName,
    roles = roles,
    hasTeamLeadPermission = hasTeamLeadPermission
)

fun TeamEntity.toDomain(): Team = Team(
    name = name,
    members = members
)

fun Team.toEntity(): TeamEntity = TeamEntity(
    name = name,
    members = members,
    cachedAt = System.currentTimeMillis()
)

fun AgentEntity.toDomain(): Agent = Agent(
    email = email,
    name = name,
    avatarUrl = avatarUrl
)

fun Agent.toEntity(): AgentEntity = AgentEntity(
    email = email,
    name = name,
    avatarUrl = avatarUrl,
    cachedAt = System.currentTimeMillis()
)

fun CommentEntity.toDomain(): Comment = Comment(
    name = name,
    content = content,
    commentedBy = commentedBy,
    createdAt = createdAt,
    commentType = commentType
)

fun Comment.toEntity(ticketId: String): CommentEntity = CommentEntity(
    name = name,
    ticketId = ticketId,
    content = content,
    commentedBy = commentedBy,
    createdAt = createdAt,
    commentType = commentType
)
