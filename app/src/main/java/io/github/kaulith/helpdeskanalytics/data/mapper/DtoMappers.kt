package io.github.kaulith.helpdeskanalytics.data.mapper

import io.github.kaulith.helpdeskanalytics.data.remote.dto.AgentDto
import io.github.kaulith.helpdeskanalytics.data.remote.dto.AttachmentDto
import io.github.kaulith.helpdeskanalytics.data.remote.dto.CommentDto
import io.github.kaulith.helpdeskanalytics.data.remote.dto.CommunicationDto
import io.github.kaulith.helpdeskanalytics.data.remote.dto.TeamDto
import io.github.kaulith.helpdeskanalytics.data.remote.dto.TicketDto
import io.github.kaulith.helpdeskanalytics.data.remote.dto.UserDto
import io.github.kaulith.helpdeskanalytics.domain.model.Agent
import io.github.kaulith.helpdeskanalytics.domain.model.Attachment
import io.github.kaulith.helpdeskanalytics.domain.model.Comment
import io.github.kaulith.helpdeskanalytics.domain.model.Communication
import io.github.kaulith.helpdeskanalytics.domain.model.Priority
import io.github.kaulith.helpdeskanalytics.domain.model.Status
import io.github.kaulith.helpdeskanalytics.domain.model.Team
import io.github.kaulith.helpdeskanalytics.domain.model.Ticket
import io.github.kaulith.helpdeskanalytics.domain.model.User
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

private val gson = Gson()
private val stringListType: Type = object : TypeToken<List<String>>() {}.type

fun TicketDto.toDomain(): Ticket {
    val assignees = parseAssignees(assign)
    val createdInstant = parseFrappeDateTime(creation)
    val modifiedInstant = parseFrappeDateTime(modified)
    val resolvedAgent = agent ?: assignees.firstOrNull()

    return Ticket(
        id = name,
        subject = subject ?: "(No subject)",
        status = parseStatus(status),
        priority = parsePriority(priority),
        assignedTo = resolvedAgent,
        createdAt = createdInstant,
        modifiedAt = modifiedInstant,
        firstRespondedAt = firstRespondedOn?.let { parseFrappeDateTimeOrNull(it) },
        resolvedAt = resolutionDate?.let { parseFrappeDateTimeOrNull(it) },
        lastAgentResponseAt = lastAgentResponse?.let { parseFrappeDateTimeOrNull(it) },
        customerName = contact,
        customerId = customer ?: raisedBy,
        assignees = assignees,
        responseBy = responseBy?.let { parseFrappeDateTimeOrNull(it) },
        resolutionBy = resolutionBy?.let { parseFrappeDateTimeOrNull(it) },
        firstResponseTimeMinutes = firstResponseTime?.let { (it / 60).toFloat() },
        avgResponseTimeMinutes = avgResponseTime?.let { (it / 60).toFloat() },
        resolutionTimeHours = resolutionTime?.let { (it / 3600).toFloat() },
        ticketType = ticketType,
        sla = sla,
        agreementStatus = agreementStatus,
        description = description
    )
}

fun UserDto.toDomain(): User {
    val roleNames = roles?.mapNotNull { it.role } ?: emptyList()
    return User(
        email = name,
        fullName = fullName,
        roles = roleNames,
        hasTeamLeadPermission = roleNames.contains("HD Team Lead") || roleNames.contains("HD Manager")
    )
}

private fun parseFrappeDateTime(dateStr: String?): Instant {
    if (dateStr.isNullOrBlank()) return Instant.DISTANT_PAST
    return parseFrappeDateTimeOrNull(dateStr) ?: Instant.DISTANT_PAST
}

private fun parseFrappeDateTimeOrNull(dateStr: String): Instant? {
    return try {
        val normalized = dateStr.trim().replace(" ", "T")
        val ldt = LocalDateTime.parse(normalized)
        ldt.toInstant(TimeZone.UTC)
    } catch (_: Exception) {
        null
    }
}

private fun parseStatus(value: String?): Status {
    if (value.isNullOrBlank()) return Status.OPEN
    return try {
        Status.fromValue(value)
    } catch (_: IllegalArgumentException) {
        Status.OPEN
    }
}

private fun parsePriority(value: String?): Priority {
    if (value.isNullOrBlank()) return Priority.MEDIUM
    return try {
        Priority.fromValue(value)
    } catch (_: IllegalArgumentException) {
        Priority.MEDIUM
    }
}

private fun parseAssignees(assignJson: String?): List<String> {
    if (assignJson.isNullOrBlank() || assignJson == "[]") return emptyList()
    return try {
        gson.fromJson<List<String>>(assignJson, stringListType) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}

fun TeamDto.toDomain(): Team = Team(
    name = teamName ?: name,
    members = users?.mapNotNull { it.user } ?: emptyList()
)

fun AgentDto.toDomain(): Agent = Agent(
    email = name,
    name = agentName ?: name.substringBefore("@"),
    avatarUrl = userImage
)

fun CommentDto.toDomain(baseUrl: String): Comment = Comment(
    name = name,
    content = content ?: "",
    commentedBy = commentedBy ?: "Unknown",
    createdAt = parseFrappeDateTime(creation),
    commentType = "Comment",
    attachments = mergeAttachments(attachments, content, baseUrl)
)

fun CommunicationDto.toDomain(baseUrl: String): Communication = Communication(
    name = name,
    content = content ?: "",
    sender = sender ?: "Unknown",
    sentByAgent = sentOrReceived.equals("Sent", ignoreCase = true),
    createdAt = parseFrappeDateTime(creation),
    subject = subject,
    attachments = mergeAttachments(attachments, content, baseUrl)
)

private val IMAGE_EXTENSIONS =
    setOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "heic", "heif", "svg")

private val IMG_TAG_REGEX =
    Regex("""<img[^>]+?src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)

/** File attachments + images embedded inline in the HTML body, deduplicated by URL. */
private fun mergeAttachments(
    fileAttachments: List<AttachmentDto>?,
    html: String?,
    baseUrl: String
): List<Attachment> {
    val files = fileAttachments.orEmpty().map { it.toDomain(baseUrl) }
    val inline = extractInlineImages(html.orEmpty(), baseUrl)
    return (files + inline).distinctBy { it.url }
}

/** Pulls <img> sources out of an HTML body so embedded screenshots are visible. */
private fun extractInlineImages(html: String, baseUrl: String): List<Attachment> {
    if (html.isEmpty()) return emptyList()
    return IMG_TAG_REGEX.findAll(html).mapNotNull { match ->
        val src = match.groupValues[1].trim()
        val nameFromPath = src.substringAfterLast('/').substringBefore('?').ifBlank { "Inline image" }
        when {
            src.isEmpty() -> null
            src.startsWith("cid:", ignoreCase = true) -> null
            src.startsWith("data:", ignoreCase = true) ->
                Attachment(name = "inline", url = src, fileName = "Inline image", isImage = true)
            src.startsWith("http", ignoreCase = true) ->
                Attachment(name = src, url = src, fileName = nameFromPath, isImage = true)
            else -> {
                val absolute = baseUrl.trimEnd('/') + "/" + src.trimStart('/')
                Attachment(name = absolute, url = absolute, fileName = nameFromPath, isImage = true)
            }
        }
    }.toList()
}

private fun AttachmentDto.toDomain(baseUrl: String): Attachment {
    val relative = fileUrl.orEmpty()
    val absolute = when {
        relative.isEmpty() -> ""
        relative.startsWith("http", ignoreCase = true) -> relative
        else -> baseUrl.trimEnd('/') + "/" + relative.trimStart('/')
    }
    val resolvedName = (fileName ?: relative.substringAfterLast('/')).ifBlank { "Attachment" }
    val extension = resolvedName.substringAfterLast('.', "").lowercase()
    return Attachment(
        name = name,
        url = absolute,
        fileName = resolvedName,
        isImage = extension in IMAGE_EXTENSIONS
    )
}
