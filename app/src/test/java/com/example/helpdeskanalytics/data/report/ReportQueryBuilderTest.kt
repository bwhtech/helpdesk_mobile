package com.example.helpdeskanalytics.data.report

import com.example.helpdeskanalytics.domain.model.report.AggregateFunction
import com.example.helpdeskanalytics.domain.model.report.DETAIL_ROW_CAP
import com.example.helpdeskanalytics.domain.model.report.DateRangePreset
import com.example.helpdeskanalytics.domain.model.report.FilterOperator
import com.example.helpdeskanalytics.domain.model.report.ReportAggregate
import com.example.helpdeskanalytics.domain.model.report.ReportColumn
import com.example.helpdeskanalytics.domain.model.report.ReportConfig
import com.example.helpdeskanalytics.domain.model.report.ReportFilter
import com.example.helpdeskanalytics.domain.model.report.ReportMode
import com.example.helpdeskanalytics.domain.model.report.ReportQuery
import com.example.helpdeskanalytics.domain.model.report.SortDirection
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportQueryBuilderTest {

    private val today = LocalDate(2026, 7, 10)

    private fun summary(config: ReportConfig) =
        ReportQueryBuilder.build(config, today) as ReportQuery.Summary

    private fun detail(config: ReportConfig) =
        ReportQueryBuilder.build(config, today) as ReportQuery.Detail

    @Test
    fun `aggregates go over as dicts because the site rejects function strings`() {
        val query = summary(
            ReportConfig(
                mode = ReportMode.SUMMARY,
                groupBy = ReportColumn.STATUS,
                dateRange = DateRangePreset.ALL,
                aggregates = listOf(ReportAggregate(AggregateFunction.COUNT))
            )
        )

        assertEquals("""["status",{"COUNT":"*","as":"row_count"}]""", query.fields)
        assertEquals("status", query.groupBy)
        assertEquals("status asc", query.orderBy)
        assertNull(query.filters)
    }

    @Test
    fun `average is requested as a sum over a non-null count`() {
        val query = summary(
            ReportConfig(
                mode = ReportMode.SUMMARY,
                groupBy = ReportColumn.STATUS,
                dateRange = DateRangePreset.ALL,
                aggregates = listOf(ReportAggregate(AggregateFunction.AVG, ReportColumn.RESOLUTION_HOURS))
            )
        )

        assertTrue(query.fields.contains("""{"SUM":"resolution_time","as":"sum_0"}"""))
        assertTrue(query.fields.contains("""{"COUNT":"resolution_time","as":"count_0"}"""))
        assertFalse(query.fields.contains("AVG"))
        assertEquals("sum_0", query.sources.single().sumAlias)
        assertEquals("count_0", query.sources.single().countAlias)
    }

    @Test
    fun `sum needs no count alias`() {
        val query = summary(
            ReportConfig(
                mode = ReportMode.SUMMARY,
                groupBy = ReportColumn.STATUS,
                dateRange = DateRangePreset.ALL,
                aggregates = listOf(ReportAggregate(AggregateFunction.SUM, ReportColumn.RESOLUTION_HOURS))
            )
        )

        assertEquals("sum_0", query.sources.single().sumAlias)
        assertNull(query.sources.single().countAlias)
    }

    @Test
    fun `grouping by agent fans out over _assign`() {
        val query = summary(
            ReportConfig(
                mode = ReportMode.SUMMARY,
                groupBy = ReportColumn.AGENT,
                dateRange = DateRangePreset.ALL
            )
        )

        assertEquals("_assign", query.groupBy)
        assertEquals("_assign", query.groupField)
        assertTrue(query.fanOutAssignees)
    }

    @Test
    fun `a count aggregate reads the row count rather than a second alias`() {
        val query = summary(
            ReportConfig(
                mode = ReportMode.SUMMARY,
                groupBy = ReportColumn.STATUS,
                dateRange = DateRangePreset.ALL,
                aggregates = listOf(ReportAggregate(AggregateFunction.COUNT))
            )
        )

        assertNull(query.sources.single().sumAlias)
        assertEquals("row_count", query.sources.single().countAlias)
    }

    @Test
    fun `a derived column cannot be summed, so it yields no source`() {
        val query = summary(
            ReportConfig(
                mode = ReportMode.SUMMARY,
                groupBy = ReportColumn.STATUS,
                dateRange = DateRangePreset.ALL,
                aggregates = listOf(ReportAggregate(AggregateFunction.SUM, ReportColumn.AGE_HOURS))
            )
        )

        assertNull(query.sources.single().sumAlias)
        assertNull(query.sources.single().countAlias)
    }

    @Test
    fun `the date range bounds creation on both sides`() {
        val query = detail(ReportConfig(dateRange = DateRangePreset.LAST_7))

        assertEquals(
            """[["creation",">=","2026-07-03 00:00:00"],["creation","<=","2026-07-10 23:59:59"]]""",
            query.filters
        )
    }

    @Test
    fun `a number filter converts display units back to the seconds Frappe stores`() {
        val query = detail(
            ReportConfig(
                dateRange = DateRangePreset.ALL,
                filters = listOf(
                    ReportFilter(ReportColumn.RESOLUTION_HOURS, FilterOperator.GREATER_THAN, "2")
                )
            )
        )

        assertEquals("""[["resolution_time",">",7200.0]]""", query.filters)
    }

    @Test
    fun `an equals filter on a date covers the whole day`() {
        val query = detail(
            ReportConfig(
                dateRange = DateRangePreset.ALL,
                filters = listOf(
                    ReportFilter(ReportColumn.CREATED, FilterOperator.EQUALS, "2026-07-01")
                )
            )
        )

        assertEquals(
            """[["creation",">=","2026-07-01 00:00:00"],["creation","<=","2026-07-01 23:59:59"]]""",
            query.filters
        )
    }

    @Test
    fun `an agent filter matches any assignee in the json array`() {
        val query = detail(
            ReportConfig(
                dateRange = DateRangePreset.ALL,
                filters = listOf(
                    ReportFilter(ReportColumn.AGENT, FilterOperator.EQUALS, "rahul@frappe.io")
                )
            )
        )

        assertEquals("""[["_assign","like","%rahul@frappe.io%"]]""", query.filters)
    }

    @Test
    fun `filters on derived columns never reach the server`() {
        val config = ReportConfig(
            mode = ReportMode.SUMMARY,
            groupBy = ReportColumn.STATUS,
            dateRange = DateRangePreset.ALL,
            filters = listOf(ReportFilter(ReportColumn.OVERDUE, FilterOperator.EQUALS, "Yes"))
        )

        val query = summary(config)

        assertNull(query.filters)
        assertEquals(listOf(ReportColumn.OVERDUE), query.ignoredFilters)
    }

    @Test
    fun `a detail sort on a derived column falls back to creation`() {
        val query = detail(
            ReportConfig(
                dateRange = DateRangePreset.ALL,
                sortBy = ReportColumn.AGE_HOURS,
                sortDirection = SortDirection.ASC
            )
        )

        assertEquals("creation desc", query.orderBy)
        assertEquals(DETAIL_ROW_CAP, query.limit)
    }

    @Test
    fun `a detail sort on a real column names it`() {
        val query = detail(
            ReportConfig(
                dateRange = DateRangePreset.ALL,
                sortBy = ReportColumn.PRIORITY,
                sortDirection = SortDirection.ASC
            )
        )

        assertEquals("priority asc", query.orderBy)
    }

    @Test
    fun `a custom range with no dates picked covers everything`() {
        val query = detail(ReportConfig(dateRange = DateRangePreset.CUSTOM))

        assertNull(query.filters)
    }

    @Test
    fun `a custom range uses the dates picked`() {
        val query = detail(
            ReportConfig(
                dateRange = DateRangePreset.CUSTOM,
                customStart = LocalDate(2026, 1, 1),
                customEnd = LocalDate(2026, 1, 31)
            )
        )

        assertEquals(
            """[["creation",">=","2026-01-01 00:00:00"],["creation","<=","2026-01-31 23:59:59"]]""",
            query.filters
        )
    }
}
