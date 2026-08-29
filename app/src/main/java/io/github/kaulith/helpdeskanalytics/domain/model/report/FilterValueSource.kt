package io.github.kaulith.helpdeskanalytics.domain.model.report

import io.github.kaulith.helpdeskanalytics.domain.model.Priority
import io.github.kaulith.helpdeskanalytics.domain.model.SlaState
import io.github.kaulith.helpdeskanalytics.domain.model.Status

/** [value] is what the filter compares against; [label] is what the picker shows. */
data class FilterOption(val value: String, val label: String)

/** Where a filter's value picker gets its choices. */
sealed interface FilterValueSource {
    /** Typed in by hand: free text, a number, or a date. */
    data object Typed : FilterValueSource

    /** A short set known without asking the server. */
    data class Fixed(val options: List<FilterOption>) : FilterValueSource

    /** The agents on this Helpdesk, labelled by name and filtered by email. */
    data object Agents : FilterValueSource

    /** Values actually present on tickets, read back with a `group_by` on [frappeField]. */
    data class Distinct(val frappeField: String) : FilterValueSource
}

/**
 * Mirrors what Frappe's own filter row does: a Select offers its options, a Check
 * offers Yes/No, and `_assign` is a picker over users rather than a typed email.
 */
fun ReportColumn.valueSource(): FilterValueSource = when (this) {
    ReportColumn.STATUS ->
        FilterValueSource.Fixed(Status.entries.map { FilterOption(it.value, it.displayName) })
    ReportColumn.PRIORITY ->
        FilterValueSource.Fixed(Priority.entries.map { FilterOption(it.value, it.displayName) })
    ReportColumn.OVERDUE ->
        FilterValueSource.Fixed(listOf(FilterOption("Yes", "Yes"), FilterOption("No", "No")))
    ReportColumn.FIRST_RESPONSE_SLA, ReportColumn.RESOLUTION_SLA,
    ReportColumn.AGENT_RESOLUTION_SLA ->
        FilterValueSource.Fixed(SlaState.entries.map { FilterOption(it.label, it.label) })
    ReportColumn.AGENT -> FilterValueSource.Agents
    ReportColumn.CUSTOMER, ReportColumn.TICKET_TYPE, ReportColumn.SLA, ReportColumn.SLA_STATUS ->
        FilterValueSource.Distinct(checkNotNull(frappeField))
    else -> FilterValueSource.Typed
}

/**
 * Frappe drops a link field back to a plain text box when the condition is `like`,
 * because a substring search over a value you picked from a list is meaningless.
 */
fun ReportColumn.offersOptions(operator: FilterOperator): Boolean = when (valueSource()) {
    FilterValueSource.Typed -> false
    is FilterValueSource.Distinct -> operator != FilterOperator.CONTAINS
    else -> true
}
