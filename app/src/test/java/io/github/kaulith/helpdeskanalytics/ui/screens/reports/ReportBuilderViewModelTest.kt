package io.github.kaulith.helpdeskanalytics.ui.screens.reports

import io.github.kaulith.helpdeskanalytics.domain.model.report.AggregateFunction
import io.github.kaulith.helpdeskanalytics.domain.model.report.DateRangePreset
import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportAggregate
import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportColumn
import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportConfig
import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportMode
import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportTemplate
import io.github.kaulith.helpdeskanalytics.testing.FakeAgentRepository
import io.github.kaulith.helpdeskanalytics.testing.FakeReportDataRepository
import io.github.kaulith.helpdeskanalytics.testing.FakeReportRepository
import io.github.kaulith.helpdeskanalytics.testing.MainDispatcherRule
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * The state combines on `Dispatchers.Default`, so every test ends by waiting for its final state;
 * a recompute still running after `Dispatchers.resetMain` fails whichever test runs next.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportBuilderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dataRepository = FakeReportDataRepository()
    private val reportRepository = FakeReportRepository()
    private val viewModel by lazy {
        ReportBuilderViewModel(dataRepository, reportRepository, FakeAgentRepository())
    }

    @Test
    fun `a summary regroups on status when the current group cannot be bucketed`() = runTest {
        viewModel.setGroupBy(ReportColumn.AGE_HOURS)
        viewModel.setMode(ReportMode.SUMMARY)

        assertEquals(ReportColumn.STATUS, config { it.mode == ReportMode.SUMMARY }.groupBy)
    }

    @Test
    fun `a summary keeps a group the server can bucket`() = runTest {
        viewModel.setGroupBy(ReportColumn.PRIORITY)
        viewModel.setMode(ReportMode.SUMMARY)

        assertEquals(ReportColumn.PRIORITY, config { it.mode == ReportMode.SUMMARY }.groupBy)
    }

    @Test
    fun `picking columns reuses the fetched rows while a new date range refetches`() = runTest {
        viewModel.uiState.first { !it.isLoading }

        viewModel.toggleColumn(ReportColumn.CUSTOMER)
        advanceUntilIdle()
        assertEquals(1, dataRepository.queries.size)

        viewModel.setDateRange(DateRangePreset.LAST_7)
        advanceUntilIdle()
        assertEquals(2, dataRepository.queries.size)
        config { it.dateRange == DateRangePreset.LAST_7 && ReportColumn.CUSTOMER in it.columns }
    }

    @Test
    fun `the last column and the last aggregate stay`() = runTest {
        viewModel.setColumns(listOf(ReportColumn.ID))
        viewModel.toggleColumn(ReportColumn.ID)
        viewModel.removeAggregate(0)

        val config = config { it.columns == listOf(ReportColumn.ID) }
        assertEquals(listOf(ReportColumn.ID), config.columns)
        assertEquals(listOf(ReportAggregate(AggregateFunction.COUNT)), config.aggregates)
    }

    @Test
    fun `saving an opened template updates it in place`() = runTest {
        reportRepository.templates.value = listOf(template(7, ReportConfig()))
        viewModel.loadTemplate(7)
        viewModel.uiState.first { it.templateName == "Weekly backlog" }

        viewModel.setDateRange(DateRangePreset.LAST_7)
        viewModel.saveTemplate("Weekly backlog") {}
        advanceUntilIdle()

        val saved = reportRepository.templates.value.single()
        assertEquals(7L, saved.id)
        assertEquals(DateRangePreset.LAST_7, saved.config?.dateRange)
        config { it.dateRange == DateRangePreset.LAST_7 }
    }

    @Test
    fun `an unreadable saved report keeps its error after the default report loads`() = runTest {
        reportRepository.templates.value = listOf(template(7, null))
        viewModel.loadTemplate(7)
        advanceUntilIdle()

        assertEquals("This saved report can't be opened", viewModel.uiState.first { !it.isLoading }.error)
    }

    private suspend fun config(predicate: (ReportConfig) -> Boolean) =
        viewModel.uiState.first { !it.isLoading && predicate(it.config) }.config

    private fun template(id: Long, config: ReportConfig?) =
        ReportTemplate(id, "Weekly backlog", config, Instant.fromEpochMilliseconds(0))
}
