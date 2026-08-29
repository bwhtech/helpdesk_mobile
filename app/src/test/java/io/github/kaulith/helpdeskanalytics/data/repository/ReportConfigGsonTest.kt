package io.github.kaulith.helpdeskanalytics.data.repository

import io.github.kaulith.helpdeskanalytics.domain.model.report.ChartType
import io.github.kaulith.helpdeskanalytics.domain.model.report.DateRangePreset
import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportConfig
import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportMode
import com.google.gson.Gson
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportConfigGsonTest {

    private val gson = Gson()

    @Test
    fun `a custom range round-trips as ISO text`() {
        val config = ReportConfig(
            dateRange = DateRangePreset.CUSTOM,
            customStart = LocalDate(2026, 1, 1),
            customEnd = LocalDate(2026, 1, 31)
        )

        val json = gson.toJson(config)
        val restored = gson.fromJson(json, ReportConfig::class.java)

        assertTrue(json.contains(""""customStart":"2026-01-01""""))
        assertEquals(LocalDate(2026, 1, 1), restored.customStart)
        assertEquals(LocalDate(2026, 1, 31), restored.customEnd)
        assertEquals(LocalDate(2026, 1, 1)..LocalDate(2026, 1, 31), restored.dateBounds())
    }

    @Test
    fun `a template saved before summaries existed opens with detail defaults`() {
        val legacy = """{"columns":["status"],"dateRange":"LAST_30","sortDirection":"DESC"}"""

        val restored = gson.fromJson(legacy, ReportConfig::class.java).withDefaults()

        assertEquals(ReportMode.DETAIL, restored.mode)
        assertEquals(ReportConfig.DEFAULT_AGGREGATES, restored.aggregates)
        assertEquals(ChartType.NONE, restored.chartType)
        assertNull(restored.customStart)
        assertEquals(DateRangePreset.LAST_30, restored.dateRange)
    }

    @Test
    fun `an empty stored config falls back rather than carrying nulls`() {
        val restored = gson.fromJson("{}", ReportConfig::class.java).withDefaults()

        assertEquals(ReportConfig.DEFAULT_COLUMNS, restored.columns)
        assertEquals(emptyList<Any>(), restored.filters)
        assertEquals(ReportMode.DETAIL, restored.mode)
        assertEquals(DateRangePreset.LAST_30, restored.dateRange)
    }
}
