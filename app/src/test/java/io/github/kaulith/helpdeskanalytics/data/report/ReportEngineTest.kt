package io.github.kaulith.helpdeskanalytics.data.report

import io.github.kaulith.helpdeskanalytics.domain.model.Priority
import io.github.kaulith.helpdeskanalytics.domain.model.Status
import io.github.kaulith.helpdeskanalytics.domain.model.Ticket
import io.github.kaulith.helpdeskanalytics.domain.model.report.AggregateFunction
import io.github.kaulith.helpdeskanalytics.domain.model.report.ChartPoint
import io.github.kaulith.helpdeskanalytics.domain.model.report.ChartType
import io.github.kaulith.helpdeskanalytics.domain.model.report.DateRangePreset
import io.github.kaulith.helpdeskanalytics.domain.model.report.FilterOperator
import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportAggregate
import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportColumn
import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportConfig
import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportData
import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportFilter
import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportMode
import io.github.kaulith.helpdeskanalytics.domain.model.report.SortDirection
import io.github.kaulith.helpdeskanalytics.domain.model.report.SummaryRow
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportEngineTest {

    private val now = Clock.System.now()

    private val base = Ticket(
        id = "1",
        subject = "s",
        status = Status.OPEN,
        priority = Priority.LOW,
        assignedTo = null,
        createdAt = now,
        modifiedAt = now,
        firstRespondedAt = null,
        resolvedAt = null,
        lastAgentResponseAt = null,
        customerName = null,
        customerId = null,
        assignees = emptyList(),
        responseBy = null,
        resolutionBy = null,
        firstResponseTimeMinutes = null,
        avgResponseTimeMinutes = null,
        resolutionTimeHours = null,
        ticketType = null,
        sla = null,
        agreementStatus = null,
        description = null
    )

    private val twoHoursInSeconds = 2.hours.inWholeSeconds.toDouble()

    private val idOnly = ReportConfig(
        columns = listOf(ReportColumn.ID),
        sortBy = null,
        dateRange = DateRangePreset.ALL
    )

    @Test
    fun `the date range drops tickets created outside it`() {
        val tickets = listOf(base, base.copy(id = "2", createdAt = now - 3.days))

        val result = detail(idOnly.copy(dateRange = DateRangePreset.TODAY), tickets)

        assertEquals(listOf(listOf("1")), result.groups.single().rows)
        assertEquals(1, result.totalRows)
    }

    @Test
    fun `an agent filter matches any assignee, like the server does`() {
        val tickets = listOf(
            base.copy(assignees = listOf("ann@x.io", "bob@x.io")),
            base.copy(id = "2", assignees = listOf("ann@x.io"))
        )
        val bob = ReportFilter(ReportColumn.AGENT, FilterOperator.EQUALS, "bob")

        assertEquals(listOf(listOf("1")), detail(idOnly.copy(filters = listOf(bob)), tickets).groups.single().rows)
        assertEquals(
            listOf(listOf("2")),
            detail(idOnly.copy(filters = listOf(bob.copy(operator = FilterOperator.NOT_EQUALS))), tickets)
                .groups.single().rows
        )
    }

    @Test
    fun `a computed column filters locally`() {
        val tickets = listOf(base, base.copy(id = "2", responseBy = now - 1.hours))
        val overdue = ReportFilter(ReportColumn.OVERDUE, FilterOperator.EQUALS, "Yes")

        assertEquals(listOf(listOf("2")), detail(idOnly.copy(filters = listOf(overdue)), tickets).groups.single().rows)
    }

    @Test
    fun `sorting puts missing numbers first when ascending`() {
        val tickets = listOf(
            base.copy(resolutionTimeHours = 5f),
            base.copy(id = "2"),
            base.copy(id = "3", resolutionTimeHours = 2f)
        )
        val config = idOnly.copy(sortBy = ReportColumn.RESOLUTION_HOURS, sortDirection = SortDirection.ASC)

        assertEquals(listOf(listOf("2"), listOf("3"), listOf("1")), detail(config, tickets).groups.single().rows)
        assertEquals(
            listOf(listOf("1"), listOf("3"), listOf("2")),
            detail(config.copy(sortDirection = SortDirection.DESC), tickets).groups.single().rows
        )
    }

    @Test
    fun `groups follow the sort direction and keep every row`() {
        val tickets = listOf(
            base,
            base.copy(id = "2", status = Status.CLOSED),
            base.copy(id = "3")
        )
        val config = idOnly.copy(groupBy = ReportColumn.STATUS, sortDirection = SortDirection.ASC)

        val result = detail(config, tickets)

        assertEquals(listOf("Closed", "Open"), result.groups.map { it.label })
        assertEquals(listOf(listOf("1"), listOf("3")), result.groups.last().rows)
        assertEquals(tickets.size, result.totalRows)
    }

    @Test
    fun `a summary ranks by its first aggregate and shows display units`() {
        val config = ReportConfig(
            mode = ReportMode.SUMMARY,
            groupBy = ReportColumn.PRIORITY,
            aggregates = listOf(
                ReportAggregate(AggregateFunction.COUNT),
                ReportAggregate(AggregateFunction.AVG, ReportColumn.RESOLUTION_HOURS)
            )
        )
        val rows = listOf(
            SummaryRow("Low", listOf(1.0, twoHoursInSeconds)),
            SummaryRow("Urgent", listOf(2.0, null))
        )

        val result = ReportEngine.build(config, ReportData.Summary(rows, emptyList()))

        assertEquals(listOf("Priority", "Ticket count", "Average of Resolution (hrs)"), result.headers)
        assertEquals(
            listOf(listOf("Urgent", "2", "-"), listOf("Low", "1", "2")),
            result.groups.single().rows
        )
    }

    @Test
    fun `a chart plots the first aggregate only when one is picked`() {
        val rows = listOf(SummaryRow("Open", listOf(2.0)), SummaryRow("Closed", listOf(null)))
        val summary = ReportConfig(mode = ReportMode.SUMMARY, groupBy = ReportColumn.STATUS)

        assertEquals(
            emptyList<ChartPoint>(),
            ReportEngine.build(summary, ReportData.Summary(rows, emptyList())).chart
        )
        assertEquals(
            listOf(ChartPoint("Open", 2.0)),
            ReportEngine.build(summary.copy(chartType = ChartType.BAR), ReportData.Summary(rows, emptyList()))
                .chart
        )
    }

    private fun detail(config: ReportConfig, tickets: List<Ticket>) =
        ReportEngine.build(config, ReportData.Detail(tickets, serverTotal = tickets.size, truncated = false))
}
