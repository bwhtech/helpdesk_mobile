package com.example.helpdeskanalytics.data.report

import com.example.helpdeskanalytics.domain.model.Ticket
import com.example.helpdeskanalytics.domain.model.report.ChartPoint
import com.example.helpdeskanalytics.domain.model.report.ChartType
import com.example.helpdeskanalytics.domain.model.report.ColumnType
import com.example.helpdeskanalytics.domain.model.report.FilterOperator
import com.example.helpdeskanalytics.domain.model.report.ReportAggregate
import com.example.helpdeskanalytics.domain.model.report.ReportColumn
import com.example.helpdeskanalytics.domain.model.report.ReportConfig
import com.example.helpdeskanalytics.domain.model.report.ReportData
import com.example.helpdeskanalytics.domain.model.report.ReportFilter
import com.example.helpdeskanalytics.domain.model.report.ReportGroup
import com.example.helpdeskanalytics.domain.model.report.ReportResult
import com.example.helpdeskanalytics.domain.model.report.SortDirection
import com.example.helpdeskanalytics.domain.model.report.SummaryRow
import com.example.helpdeskanalytics.domain.model.report.formatNumber
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Projects what a report query returned into display text. Detail rows are
 * re-filtered here because columns computed in Kotlin (age, overdue, SLA
 * standing) have no server-side equivalent to filter on.
 */
object ReportEngine {

    fun build(config: ReportConfig, data: ReportData): ReportResult = when (data) {
        is ReportData.Detail -> buildDetail(config, data)
        is ReportData.Summary -> buildSummary(config, data)
    }

    private fun buildDetail(config: ReportConfig, data: ReportData.Detail): ReportResult {
        val bounds = config.dateBounds()
        var rows = data.tickets.filter { bounds == null || it.createdWithin(bounds) }

        config.filters.forEach { filter ->
            rows = rows.filter { matches(it, filter) }
        }

        config.sortBy?.let { column ->
            val comparator = comparatorFor(column)
            rows = rows.sortedWith(
                if (config.sortDirection == SortDirection.ASC) comparator else comparator.reversed()
            )
        }

        val columns = config.columns
        val labelOrder: Comparator<String> =
            if (config.sortDirection == SortDirection.ASC) naturalOrder() else reverseOrder()
        val groups = config.groupBy?.let { groupColumn ->
            rows.groupBy { groupColumn.display(it) }
                .toSortedMap(labelOrder)
                .map { (label, group) ->
                    ReportGroup(label, group.map { t -> columns.map { c -> c.display(t) } })
                }
        } ?: listOf(
            ReportGroup(null, rows.map { t -> columns.map { c -> c.display(t) } })
        )

        return ReportResult(
            headers = columns.map { it.label },
            groups = groups,
            totalRows = rows.size,
            serverTotal = data.serverTotal,
            truncated = data.truncated
        )
    }

    private fun buildSummary(config: ReportConfig, data: ReportData.Summary): ReportResult {
        val aggregates = config.aggregates
        val rows = sortSummary(config, data.rows)
        val cells = rows.map { row ->
            listOf(row.label) + row.values.mapIndexed { index, value ->
                value?.let { formatNumber(scale(aggregates[index], it)) } ?: "-"
            }
        }
        val chart = if (config.chartType == ChartType.NONE) {
            emptyList()
        } else {
            rows.mapNotNull { row ->
                val value = row.values.firstOrNull() ?: return@mapNotNull null
                ChartPoint(row.label, scale(aggregates.first(), value))
            }
        }

        return ReportResult(
            headers = listOf(config.groupBy?.label ?: "Total") + aggregates.map { it.label },
            groups = listOf(ReportGroup(null, cells)),
            totalRows = cells.size,
            chart = chart,
            ignoredFilters = data.ignoredFilters
        )
    }

    /** Summaries rank by their leading aggregate; the group label only breaks ties. */
    private fun sortSummary(config: ReportConfig, rows: List<SummaryRow>): List<SummaryRow> {
        val byValue = compareBy(nullsFirst<Double>()) { row: SummaryRow -> row.values.firstOrNull() }
        val ordered = byValue.thenBy { it.label }
        return rows.sortedWith(
            if (config.sortDirection == SortDirection.ASC) ordered else ordered.reversed()
        )
    }

    private fun scale(aggregate: ReportAggregate, value: Double): Double =
        aggregate.column?.toDisplayUnit(value) ?: value

    private fun comparatorFor(col: ReportColumn): Comparator<Ticket> = when (col.type) {
        ColumnType.NUMBER -> compareBy(nullsFirst<Double>()) { t -> col.numberValue(t) }
        ColumnType.DATE -> compareBy(nullsFirst<Instant>()) { t -> col.dateValue(t) }
        ColumnType.BOOL -> compareBy { t -> col.boolValue(t) }
        ColumnType.TEXT -> compareBy { t -> col.textValue(t).lowercase() }
    }

    private fun matches(ticket: Ticket, filter: ReportFilter): Boolean {
        val col = filter.column
        if (col == ReportColumn.AGENT) return matchesAgent(ticket, filter)
        return when (col.type) {
            ColumnType.NUMBER -> {
                val target = filter.value.trim().toDoubleOrNull() ?: return false
                val actual = col.numberValue(ticket)
                when (filter.operator) {
                    FilterOperator.EQUALS -> actual == target
                    FilterOperator.NOT_EQUALS -> actual != target
                    FilterOperator.GREATER_THAN -> (actual ?: return false) > target
                    FilterOperator.LESS_THAN -> (actual ?: return false) < target
                    FilterOperator.CONTAINS -> false
                }
            }
            ColumnType.DATE -> {
                val target = runCatching { LocalDate.parse(filter.value.trim()) }.getOrNull()
                    ?: return false
                val actual = col.dateValue(ticket)?.localDate() ?: return false
                when (filter.operator) {
                    FilterOperator.EQUALS -> actual == target
                    FilterOperator.NOT_EQUALS -> actual != target
                    FilterOperator.GREATER_THAN -> actual > target
                    FilterOperator.LESS_THAN -> actual < target
                    FilterOperator.CONTAINS -> false
                }
            }
            ColumnType.BOOL -> {
                val target = filter.value.equals("yes", true) || filter.value.equals("true", true)
                col.boolValue(ticket) == target
            }
            ColumnType.TEXT -> {
                val actual = col.display(ticket)
                val target = filter.value.trim()
                if (target.isEmpty()) return true
                when (filter.operator) {
                    FilterOperator.EQUALS -> actual.equals(target, true)
                    FilterOperator.NOT_EQUALS -> !actual.equals(target, true)
                    FilterOperator.CONTAINS -> actual.contains(target, true)
                    else -> true
                }
            }
        }
    }

    /** Mirrors the server's `_assign like %value%`: any assignee counts, not just the first. */
    private fun matchesAgent(ticket: Ticket, filter: ReportFilter): Boolean {
        val target = filter.value.trim()
        if (target.isEmpty()) return true
        val assignees = ticket.assignees.ifEmpty { listOfNotNull(ticket.assignedTo) }
        val hit = assignees.any { it.contains(target, true) }
        return when (filter.operator) {
            FilterOperator.NOT_EQUALS -> !hit
            FilterOperator.EQUALS, FilterOperator.CONTAINS -> hit
            else -> true
        }
    }
}

private fun Instant.localDate(): LocalDate =
    toLocalDateTime(TimeZone.currentSystemDefault()).date

private fun Ticket.createdWithin(bounds: ClosedRange<LocalDate>): Boolean =
    createdAt.localDate() in bounds
