package io.github.kaulith.helpdeskanalytics.domain.model.report

import io.github.kaulith.helpdeskanalytics.domain.model.Ticket
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn

enum class FilterOperator(val label: String) {
    @SerializedName("EQUALS")
    EQUALS("is"),

    @SerializedName("NOT_EQUALS")
    NOT_EQUALS("is not"),

    @SerializedName("CONTAINS")
    CONTAINS("contains"),

    @SerializedName("GREATER_THAN")
    GREATER_THAN("greater than"),

    @SerializedName("LESS_THAN")
    LESS_THAN("less than");

    companion object {
        fun forType(type: ColumnType): List<FilterOperator> = when (type) {
            ColumnType.TEXT -> listOf(EQUALS, NOT_EQUALS, CONTAINS)
            ColumnType.NUMBER -> listOf(EQUALS, NOT_EQUALS, GREATER_THAN, LESS_THAN)
            ColumnType.BOOL -> listOf(EQUALS)
            ColumnType.DATE -> listOf(EQUALS, GREATER_THAN, LESS_THAN)
        }
    }
}

data class ReportFilter(
    @SerializedName("column") val column: ReportColumn,
    @SerializedName("operator") val operator: FilterOperator,
    @SerializedName("value") val value: String
)

enum class SortDirection {
    @SerializedName("ASC")
    ASC,

    @SerializedName("DESC")
    DESC
}

enum class DateRangePreset(val label: String) {
    @SerializedName("ALL")
    ALL("All time"),

    @SerializedName("TODAY")
    TODAY("Today"),

    @SerializedName("LAST_7")
    LAST_7("Last 7 days"),

    @SerializedName("LAST_30")
    LAST_30("Last 30 days"),

    @SerializedName("THIS_MONTH")
    THIS_MONTH("This month"),

    @SerializedName("CUSTOM")
    CUSTOM("Custom range")
}

/** How a report reads the data: one row per ticket, or one row per group. */
enum class ReportMode(val label: String) {
    @SerializedName("DETAIL")
    DETAIL("Tickets"),

    @SerializedName("SUMMARY")
    SUMMARY("Summary")
}

enum class AggregateFunction(val label: String) {
    @SerializedName("COUNT")
    COUNT("Count"),

    @SerializedName("SUM")
    SUM("Sum"),

    @SerializedName("AVG")
    AVG("Average")
}

/** [column] is null for [AggregateFunction.COUNT], which counts rows rather than values. */
data class ReportAggregate(
    @SerializedName("function") val function: AggregateFunction,
    @SerializedName("column") val column: ReportColumn? = null
) {
    val label: String
        get() = column?.let { "${function.label} of ${it.label}" } ?: "Ticket count"
}

enum class ChartType(val label: String) {
    @SerializedName("NONE")
    NONE("No chart"),

    @SerializedName("BAR")
    BAR("Bar chart"),

    @SerializedName("DONUT")
    DONUT("Donut chart")
}

/** Full definition of a report: what a saved template stores. */
data class ReportConfig(
    @SerializedName("columns") val columns: List<ReportColumn> = DEFAULT_COLUMNS,
    @SerializedName("filters") val filters: List<ReportFilter> = emptyList(),
    @SerializedName("groupBy") val groupBy: ReportColumn? = null,
    @SerializedName("sortBy") val sortBy: ReportColumn? = ReportColumn.CREATED,
    @SerializedName("sortDirection") val sortDirection: SortDirection = SortDirection.DESC,
    @SerializedName("dateRange") val dateRange: DateRangePreset = DateRangePreset.LAST_30,
    @SerializedName("mode") val mode: ReportMode = ReportMode.DETAIL,
    @SerializedName("aggregates") val aggregates: List<ReportAggregate> = DEFAULT_AGGREGATES,
    @SerializedName("chartType") val chartType: ChartType = ChartType.NONE,
    @SerializedName("customStart")
    @field:JsonAdapter(LocalDateAdapter::class)
    val customStart: LocalDate? = null,
    @SerializedName("customEnd")
    @field:JsonAdapter(LocalDateAdapter::class)
    val customEnd: LocalDate? = null
) {
    /** Inclusive local-date bounds on ticket creation, or null to cover every ticket. */
    fun dateBounds(
        today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
    ): ClosedRange<LocalDate>? = when (dateRange) {
        DateRangePreset.ALL -> null
        DateRangePreset.TODAY -> today..today
        DateRangePreset.LAST_7 -> today.minus(7, DateTimeUnit.DAY)..today
        DateRangePreset.LAST_30 -> today.minus(30, DateTimeUnit.DAY)..today
        DateRangePreset.THIS_MONTH -> LocalDate(today.year, today.monthNumber, 1)..today
        DateRangePreset.CUSTOM -> customStart?.let { start -> customEnd?.let { start..it } }
    }

    companion object {
        val DEFAULT_COLUMNS = listOf(
            ReportColumn.ID,
            ReportColumn.SUBJECT,
            ReportColumn.STATUS,
            ReportColumn.PRIORITY,
            ReportColumn.AGENT,
            ReportColumn.CREATED
        )

        val DEFAULT_AGGREGATES = listOf(ReportAggregate(AggregateFunction.COUNT))
    }
}

/** [config] is null when the stored JSON could not be read. */
data class ReportTemplate(
    val id: Long,
    val name: String,
    val config: ReportConfig?,
    val updatedAt: Instant
)

/** One summary bucket. [values] lines up with `ReportConfig.aggregates`, unscaled. */
data class SummaryRow(
    val label: String,
    val values: List<Double?>
)

/** What a report query returned, before projection into display text. */
sealed interface ReportData {
    data class Detail(
        val tickets: List<Ticket>,
        val serverTotal: Int,
        val truncated: Boolean
    ) : ReportData

    data class Summary(
        val rows: List<SummaryRow>,
        val ignoredFilters: List<ReportColumn>
    ) : ReportData
}

/** One group of rows. [label] is null for an ungrouped report. */
data class ReportGroup(
    val label: String?,
    val rows: List<List<String>>
)

data class ChartPoint(val label: String, val value: Double)

/** Computed report ready for display and export. */
data class ReportResult(
    val headers: List<String>,
    val groups: List<ReportGroup>,
    val totalRows: Int,
    /** Tickets matching the server-side filters, before the detail row cap. */
    val serverTotal: Int? = null,
    val truncated: Boolean = false,
    val chart: List<ChartPoint> = emptyList(),
    /** Filters the summary query could not push to the server, so they went unapplied. */
    val ignoredFilters: List<ReportColumn> = emptyList()
)
