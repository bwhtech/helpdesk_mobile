package io.github.kaulith.helpdeskanalytics.domain.model.filter

import io.github.kaulith.helpdeskanalytics.domain.model.Priority
import io.github.kaulith.helpdeskanalytics.domain.model.Status
import io.github.kaulith.helpdeskanalytics.domain.model.Ticket
import kotlin.time.Instant

enum class FilterFieldType { SELECT, TEXT, LINK, NUMBER, DATE }

enum class FilterOperator(val label: String) {
    EQUALS("Equals"),
    NOT_EQUALS("Not Equals"),
    LIKE("Like"),
    NOT_LIKE("Not Like"),
    IN("In"),
    NOT_IN("Not In"),
    IS("Is"),
    LT("<"),
    GT(">"),
    LTE("<="),
    GTE(">="),
    BETWEEN("Between"),
    TIMESPAN("Timespan");

    companion object {
        fun forType(type: FilterFieldType): List<FilterOperator> = when (type) {
            FilterFieldType.SELECT -> listOf(EQUALS, NOT_EQUALS, IN, NOT_IN, IS)
            FilterFieldType.TEXT -> listOf(EQUALS, NOT_EQUALS, LIKE, NOT_LIKE, IN, NOT_IN, IS)
            FilterFieldType.LINK -> listOf(LIKE, NOT_LIKE, IS)
            FilterFieldType.NUMBER -> listOf(EQUALS, NOT_EQUALS, LT, GT, LTE, GTE, IN, NOT_IN, IS)
            FilterFieldType.DATE -> listOf(EQUALS, IS, LT, GT, LTE, GTE, BETWEEN, TIMESPAN)
        }
    }
}

enum class Timespan(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    THIS_YEAR("This Year"),
}

/** "Is" / "Is not set" value tokens, shared by the value control and the evaluator. */
const val IS_SET = "set"
const val IS_NOT_SET = "not set"

/**
 * A field of [T] the user can filter on. Each accessor fills only what its [type]
 * needs; [list] lets one field expose several values (e.g. an agent's teams).
 */
data class FilterableField<T>(
    val fieldname: String,
    val label: String,
    val type: FilterFieldType,
    val options: List<String> = emptyList(),
    val text: (T) -> String? = { null },
    val list: (T) -> List<String> = { emptyList() },
    val number: (T) -> Double? = { null },
    val instant: (T) -> Instant? = { null },
) {
    val defaultOperator: FilterOperator
        get() = when (type) {
            FilterFieldType.SELECT -> FilterOperator.EQUALS
            FilterFieldType.TEXT, FilterFieldType.LINK -> FilterOperator.LIKE
            FilterFieldType.NUMBER -> FilterOperator.EQUALS
            FilterFieldType.DATE -> FilterOperator.TIMESPAN
        }
}

data class FilterCondition<T>(
    val field: FilterableField<T>,
    val operator: FilterOperator,
    val value: String,
)

object TicketFilterFields {
    val ALL: List<FilterableField<Ticket>> = listOf(
        FilterableField(
            fieldname = "status",
            label = "Status",
            type = FilterFieldType.SELECT,
            options = Status.entries.map { it.displayName },
            text = { it.status.displayName },
        ),
        FilterableField(
            fieldname = "priority",
            label = "Priority",
            type = FilterFieldType.SELECT,
            options = Priority.entries.map { it.displayName },
            text = { it.priority.displayName },
        ),
        FilterableField(
            fieldname = "_assign",
            label = "Assigned To",
            type = FilterFieldType.LINK,
            list = { it.assignees },
        ),
        FilterableField(
            fieldname = "customer",
            label = "Customer",
            type = FilterFieldType.TEXT,
            text = { it.customerName },
        ),
        FilterableField(
            fieldname = "subject",
            label = "Subject",
            type = FilterFieldType.TEXT,
            text = { it.subject },
        ),
        FilterableField(
            fieldname = "ticket_type",
            label = "Ticket Type",
            type = FilterFieldType.TEXT,
            text = { it.ticketType },
        ),
        FilterableField(
            fieldname = "creation",
            label = "Created",
            type = FilterFieldType.DATE,
            instant = { it.createdAt },
        ),
    )

    val STATUS = ALL.first { it.fieldname == "status" }
    val PRIORITY = ALL.first { it.fieldname == "priority" }
}
