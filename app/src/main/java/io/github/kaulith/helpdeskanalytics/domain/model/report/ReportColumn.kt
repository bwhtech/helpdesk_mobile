package io.github.kaulith.helpdeskanalytics.domain.model.report

import io.github.kaulith.helpdeskanalytics.domain.model.Ticket
import io.github.kaulith.helpdeskanalytics.domain.model.agentResolutionSla
import io.github.kaulith.helpdeskanalytics.domain.model.firstResponseSla
import io.github.kaulith.helpdeskanalytics.domain.model.resolutionSla
import com.google.gson.annotations.SerializedName
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

enum class ColumnType { TEXT, NUMBER, DATE, BOOL }

/**
 * A field of [Ticket] that can be projected into a report column. [key] is the
 * stable identifier persisted in saved templates, so never rename existing keys.
 *
 * [frappeField] is the matching column on `HD Ticket`. Columns without one are
 * computed in Kotlin, so the server can neither filter, group, nor aggregate
 * them, so those reports have to run over fetched rows.
 */
enum class ReportColumn(
    val key: String,
    val label: String,
    val type: ColumnType,
    val frappeField: String? = null,
    private val secondsPerUnit: Double = 1.0
) {
    @SerializedName(value = "id", alternate = ["ID"])
    ID("id", "Ticket ID", ColumnType.TEXT, "name"),

    @SerializedName(value = "subject", alternate = ["SUBJECT"])
    SUBJECT("subject", "Subject", ColumnType.TEXT, "subject"),

    @SerializedName(value = "status", alternate = ["STATUS"])
    STATUS("status", "Status", ColumnType.TEXT, "status"),

    @SerializedName(value = "priority", alternate = ["PRIORITY"])
    PRIORITY("priority", "Priority", ColumnType.TEXT, "priority"),

    @SerializedName(value = "agent", alternate = ["AGENT"])
    AGENT("agent", "Assigned agent", ColumnType.TEXT, "_assign"),

    @SerializedName(value = "customer", alternate = ["CUSTOMER"])
    CUSTOMER("customer", "Customer", ColumnType.TEXT, "customer"),

    @SerializedName(value = "ticket_type", alternate = ["TICKET_TYPE"])
    TICKET_TYPE("ticket_type", "Type", ColumnType.TEXT, "ticket_type"),

    @SerializedName(value = "sla", alternate = ["SLA"])
    SLA("sla", "SLA policy", ColumnType.TEXT, "sla"),

    @SerializedName(value = "sla_status", alternate = ["SLA_STATUS"])
    SLA_STATUS("sla_status", "SLA status", ColumnType.TEXT, "agreement_status"),

    @SerializedName(value = "first_response_sla", alternate = ["FIRST_RESPONSE_SLA"])
    FIRST_RESPONSE_SLA("first_response_sla", "First response SLA", ColumnType.TEXT),

    @SerializedName(value = "resolution_sla", alternate = ["RESOLUTION_SLA"])
    RESOLUTION_SLA("resolution_sla", "Resolution SLA", ColumnType.TEXT),

    @SerializedName(value = "agent_resolution_sla", alternate = ["AGENT_RESOLUTION_SLA"])
    AGENT_RESOLUTION_SLA("agent_resolution_sla", "Resolution SLA (agent)", ColumnType.TEXT),

    @SerializedName(value = "created", alternate = ["CREATED"])
    CREATED("created", "Created", ColumnType.DATE, "creation"),

    @SerializedName(value = "modified", alternate = ["MODIFIED"])
    MODIFIED("modified", "Last modified", ColumnType.DATE, "modified"),

    @SerializedName(value = "first_responded", alternate = ["FIRST_RESPONDED"])
    FIRST_RESPONDED("first_responded", "First responded", ColumnType.DATE, "first_responded_on"),

    @SerializedName(value = "resolved", alternate = ["RESOLVED"])
    RESOLVED("resolved", "Resolved", ColumnType.DATE, "resolution_date"),

    @SerializedName(value = "response_by", alternate = ["RESPONSE_BY"])
    RESPONSE_BY("response_by", "Response due", ColumnType.DATE, "response_by"),

    @SerializedName(value = "resolution_by", alternate = ["RESOLUTION_BY"])
    RESOLUTION_BY("resolution_by", "Resolution due", ColumnType.DATE, "resolution_by"),

    @SerializedName(value = "last_agent_reply", alternate = ["LAST_AGENT_REPLY"])
    LAST_AGENT_REPLY("last_agent_reply", "Last agent reply", ColumnType.DATE, "last_agent_response"),

    @SerializedName(value = "first_response_min", alternate = ["FIRST_RESPONSE_MIN"])
    FIRST_RESPONSE_MIN("first_response_min", "First response (min)", ColumnType.NUMBER, "first_response_time", 60.0),

    @SerializedName(value = "avg_response_min", alternate = ["AVG_RESPONSE_MIN"])
    AVG_RESPONSE_MIN("avg_response_min", "Avg response (min)", ColumnType.NUMBER, "avg_response_time", 60.0),

    @SerializedName(value = "resolution_hours", alternate = ["RESOLUTION_HOURS"])
    RESOLUTION_HOURS("resolution_hours", "Resolution (hrs)", ColumnType.NUMBER, "resolution_time", 3600.0),

    @SerializedName(value = "age_hours", alternate = ["AGE_HOURS"])
    AGE_HOURS("age_hours", "Age (hrs)", ColumnType.NUMBER),

    @SerializedName(value = "overdue", alternate = ["OVERDUE"])
    OVERDUE("overdue", "Overdue", ColumnType.BOOL);

    fun textValue(t: Ticket): String = when (this) {
        ID -> t.id
        SUBJECT -> t.subject
        STATUS -> t.status.displayName
        PRIORITY -> t.priority.displayName
        AGENT -> t.assignedTo ?: t.assignees.firstOrNull() ?: "Unassigned"
        // Must be the `customer` link this column filters and groups on, not the contact.
        CUSTOMER -> t.customerId ?: "-"
        TICKET_TYPE -> t.ticketType ?: "-"
        SLA -> t.sla ?: "-"
        SLA_STATUS -> t.agreementStatus ?: "-"
        FIRST_RESPONSE_SLA -> t.firstResponseSla().label
        RESOLUTION_SLA -> t.resolutionSla().label
        AGENT_RESOLUTION_SLA -> t.agentResolutionSla().label
        else -> ""
    }

    fun numberValue(t: Ticket): Double? = when (this) {
        FIRST_RESPONSE_MIN -> t.firstResponseTimeMinutes?.toDouble()
        AVG_RESPONSE_MIN -> t.avgResponseTimeMinutes?.toDouble()
        RESOLUTION_HOURS -> t.resolutionTimeHours?.toDouble()
        AGE_HOURS -> t.ageInHours().toDouble()
        else -> null
    }

    fun dateValue(t: Ticket): Instant? = when (this) {
        CREATED -> t.createdAt
        MODIFIED -> t.modifiedAt
        FIRST_RESPONDED -> t.firstRespondedAt
        RESOLVED -> t.resolvedAt
        RESPONSE_BY -> t.responseBy
        RESOLUTION_BY -> t.resolutionBy
        LAST_AGENT_REPLY -> t.lastAgentResponseAt
        else -> null
    }

    fun boolValue(t: Ticket): Boolean = when (this) {
        OVERDUE -> t.isOverdue()
        else -> false
    }

    /** Human-readable cell text used in the preview table and every export. */
    fun display(t: Ticket): String = when (type) {
        ColumnType.TEXT -> textValue(t)
        ColumnType.NUMBER -> numberValue(t)?.let { formatNumber(it) } ?: "-"
        ColumnType.DATE -> dateValue(t)?.let { formatDate(it) } ?: "-"
        ColumnType.BOOL -> if (boolValue(t)) "Yes" else "No"
    }

    /** Frappe stores durations in seconds; report columns show minutes or hours. */
    fun toDisplayUnit(serverValue: Double): Double = serverValue / secondsPerUnit

    fun toServerUnit(displayValue: Double): Double = displayValue * secondsPerUnit

    companion object {
        fun fromKey(key: String): ReportColumn? = entries.find { it.key == key }

        /** Columns the server can filter on; the rest are computed here. */
        val serverFilterable: List<ReportColumn> by lazy {
            entries.filter { it.frappeField != null && it.type != ColumnType.BOOL }
        }

        /** Text columns the server can `GROUP BY`. */
        val groupable: List<ReportColumn> by lazy {
            entries.filter { it.frappeField != null && it.type == ColumnType.TEXT }
        }

        /** Numeric columns the server can `SUM` and average. */
        val aggregatable: List<ReportColumn> by lazy {
            entries.filter { it.frappeField != null && it.type == ColumnType.NUMBER }
        }
    }
}

internal fun formatNumber(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else "%.1f".format(v)

private fun formatDate(i: Instant): String {
    val dt = i.toLocalDateTime(TimeZone.currentSystemDefault())
    fun pad(n: Int) = n.toString().padStart(2, '0')
    return "${dt.year}-${pad(dt.month.number)}-${pad(dt.day)} ${pad(dt.hour)}:${pad(dt.minute)}"
}
