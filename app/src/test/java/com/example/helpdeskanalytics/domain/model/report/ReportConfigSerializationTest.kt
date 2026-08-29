package com.example.helpdeskanalytics.domain.model.report

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportConfigSerializationTest {

    private val gson = Gson()

    @Test
    fun `templates saved before the key rename still open`() {
        val legacy = """
            {"columns":["ID","STATUS","CREATED"],
             "filters":[{"column":"PRIORITY","operator":"EQUALS","value":"Urgent"}],
             "groupBy":"AGENT",
             "sortBy":"RESOLUTION_HOURS",
             "sortDirection":"ASC",
             "dateRange":"THIS_MONTH"}
        """.trimIndent()

        val config = gson.fromJson(legacy, ReportConfig::class.java)

        assertEquals(
            listOf(ReportColumn.ID, ReportColumn.STATUS, ReportColumn.CREATED),
            config.columns
        )
        assertEquals(ReportColumn.AGENT, config.groupBy)
        assertEquals(ReportColumn.RESOLUTION_HOURS, config.sortBy)
        assertEquals(SortDirection.ASC, config.sortDirection)
        assertEquals(DateRangePreset.THIS_MONTH, config.dateRange)
        assertEquals(ReportColumn.PRIORITY, config.filters.single().column)
        assertEquals(FilterOperator.EQUALS, config.filters.single().operator)
    }

    @Test
    fun `columns now persist by their stable key`() {
        val json = gson.toJson(ReportConfig(columns = listOf(ReportColumn.STATUS)))

        assertEquals(true, json.contains(""""columns":["status"]"""))
        assertEquals(false, json.contains("STATUS"))
    }

    @Test
    fun `every column key survives a round trip`() {
        ReportColumn.entries.forEach { column ->
            val config = ReportConfig(columns = listOf(column), groupBy = column)
            val restored = gson.fromJson(gson.toJson(config), ReportConfig::class.java)
            assertEquals(column, restored.columns.single())
            assertEquals(column, restored.groupBy)
        }
    }
}
