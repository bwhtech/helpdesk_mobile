package com.example.helpdeskanalytics.data.report

import com.example.helpdeskanalytics.domain.model.report.AggregateFunction
import com.example.helpdeskanalytics.domain.model.report.AggregateSource
import com.example.helpdeskanalytics.domain.model.report.ColumnType
import com.example.helpdeskanalytics.domain.model.report.DETAIL_ROW_CAP
import com.example.helpdeskanalytics.domain.model.report.FilterOperator
import com.example.helpdeskanalytics.domain.model.report.ReportColumn
import com.example.helpdeskanalytics.domain.model.report.ReportConfig
import com.example.helpdeskanalytics.domain.model.report.ReportFilter
import com.example.helpdeskanalytics.domain.model.report.ReportMode
import com.example.helpdeskanalytics.domain.model.report.ReportQuery
import com.example.helpdeskanalytics.domain.model.report.SortDirection
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

private const val ROW_COUNT_ALIAS = "row_count"
private const val ASSIGN_FIELD = "_assign"

/**
 * Turns a [ReportConfig] into Frappe query parameters.
 *
 * Aggregates go over as dicts (`{"COUNT":"*"}`); the live site rejects SQL
 * functions written as strings. Every one carries an `as` alias so the response
 * key is ours rather than a back-ticked expression.
 */
object ReportQueryBuilder {

    fun build(
        config: ReportConfig,
        today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
    ): ReportQuery {
        val bounds = config.dateBounds(today)
        return when (config.mode) {
            ReportMode.DETAIL -> buildDetail(config, bounds)
            ReportMode.SUMMARY -> buildSummary(config, bounds)
        }
    }

    private fun buildDetail(
        config: ReportConfig,
        bounds: ClosedRange<LocalDate>?
    ): ReportQuery.Detail {
        val direction = if (config.sortDirection == SortDirection.ASC) "asc" else "desc"
        val orderBy = config.sortBy?.frappeField?.let { "$it $direction" } ?: "creation desc"
        return ReportQuery.Detail(
            filters = filtersJson(config.filters, bounds),
            orderBy = orderBy,
            limit = DETAIL_ROW_CAP
        )
    }

    private fun buildSummary(
        config: ReportConfig,
        bounds: ClosedRange<LocalDate>?
    ): ReportQuery.Summary {
        val groupField = config.groupBy?.frappeField
        val fields = JsonArray()
        groupField?.let { fields.add(it) }
        fields.add(functionField("COUNT", "*", ROW_COUNT_ALIAS))

        val sources = config.aggregates.mapIndexed { index, aggregate ->
            val serverField = aggregate.column?.frappeField
            when {
                aggregate.function == AggregateFunction.COUNT ->
                    AggregateSource(null, ROW_COUNT_ALIAS)
                serverField == null -> AggregateSource(null, null)
                else -> {
                    val sumAlias = "sum_$index"
                    fields.add(functionField("SUM", serverField, sumAlias))
                    if (aggregate.function == AggregateFunction.SUM) {
                        AggregateSource(sumAlias, null)
                    } else {
                        val countAlias = "count_$index"
                        fields.add(functionField("COUNT", serverField, countAlias))
                        AggregateSource(sumAlias, countAlias)
                    }
                }
            }
        }

        // A grouped query has to name a grouped column, or Frappe's default
        // `creation desc` lands in ORDER BY while absent from GROUP BY.
        return ReportQuery.Summary(
            fields = fields.toString(),
            filters = filtersJson(config.filters, bounds),
            groupBy = groupField,
            orderBy = groupField?.let { "$it asc" },
            groupField = groupField,
            sources = sources,
            fanOutAssignees = config.groupBy == ReportColumn.AGENT,
            ignoredFilters = config.filters.filter { it.column.frappeField == null }.map { it.column }
        )
    }

    private fun functionField(function: String, argument: String, alias: String) =
        JsonObject().apply {
            addProperty(function, argument)
            addProperty("as", alias)
        }

    private fun filtersJson(
        filters: List<ReportFilter>,
        bounds: ClosedRange<LocalDate>?
    ): String? {
        val conditions = JsonArray()
        bounds?.let {
            conditions.add(condition("creation", ">=", "${it.start} 00:00:00"))
            conditions.add(condition("creation", "<=", "${it.endInclusive} 23:59:59"))
        }
        filters.forEach { filter -> serverConditions(filter).forEach(conditions::add) }
        return if (conditions.isEmpty()) null else conditions.toString()
    }

    private fun serverConditions(filter: ReportFilter): List<JsonArray> {
        val field = filter.column.frappeField ?: return emptyList()
        if (filter.column.type == ColumnType.BOOL) return emptyList()

        // `_assign` holds a JSON array of emails, so an agent match is a substring match.
        if (field == ASSIGN_FIELD) {
            val pattern = "%${filter.value}%"
            return when (filter.operator) {
                FilterOperator.NOT_EQUALS -> listOf(condition(field, "not like", pattern))
                FilterOperator.EQUALS, FilterOperator.CONTAINS ->
                    listOf(condition(field, "like", pattern))
                else -> emptyList()
            }
        }

        return when (filter.column.type) {
            ColumnType.NUMBER -> {
                val typed = filter.value.trim().toDoubleOrNull() ?: return emptyList()
                val target = filter.column.toServerUnit(typed)
                numberOperator(filter.operator)?.let { listOf(condition(field, it, target)) }
                    ?: emptyList()
            }
            ColumnType.DATE -> {
                val day = runCatching { LocalDate.parse(filter.value.trim()) }.getOrNull()
                    ?: return emptyList()
                when (filter.operator) {
                    FilterOperator.EQUALS -> listOf(
                        condition(field, ">=", "$day 00:00:00"),
                        condition(field, "<=", "$day 23:59:59")
                    )
                    FilterOperator.GREATER_THAN -> listOf(condition(field, ">", "$day 23:59:59"))
                    FilterOperator.LESS_THAN -> listOf(condition(field, "<", "$day 00:00:00"))
                    else -> emptyList()
                }
            }
            else -> when (filter.operator) {
                FilterOperator.EQUALS -> listOf(condition(field, "=", filter.value))
                FilterOperator.NOT_EQUALS -> listOf(condition(field, "!=", filter.value))
                FilterOperator.CONTAINS -> listOf(condition(field, "like", "%${filter.value}%"))
                else -> emptyList()
            }
        }
    }

    private fun numberOperator(operator: FilterOperator): String? = when (operator) {
        FilterOperator.EQUALS -> "="
        FilterOperator.NOT_EQUALS -> "!="
        FilterOperator.GREATER_THAN -> ">"
        FilterOperator.LESS_THAN -> "<"
        FilterOperator.CONTAINS -> null
    }

    private fun condition(field: String, operator: String, value: String) = JsonArray().apply {
        add(field)
        add(operator)
        add(value)
    }

    private fun condition(field: String, operator: String, value: Double) = JsonArray().apply {
        add(field)
        add(operator)
        add(value)
    }
}
