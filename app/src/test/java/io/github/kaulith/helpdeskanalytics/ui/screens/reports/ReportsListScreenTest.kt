package io.github.kaulith.helpdeskanalytics.ui.screens.reports

import android.app.Application
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportConfig
import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportTemplate
import io.github.kaulith.helpdeskanalytics.testing.FakeReportRepository
import kotlin.time.Clock
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ReportsListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val reportRepository = FakeReportRepository().apply {
        templates.value = listOf(ReportTemplate(7, "Weekly backlog", ReportConfig(), Clock.System.now()))
    }
    private var opened: Long? = null

    @Test
    fun `tapping a saved report opens it`() {
        show()

        composeRule.onNodeWithText("Weekly backlog").performClick()

        assertEquals(7L, opened)
    }

    @Test
    fun `a report is deleted only after confirming`() {
        show()

        composeRule.onNodeWithContentDescription("Report options").performClick()
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.onNodeWithText("Weekly backlog").assertExists()
        composeRule.onNodeWithText("Delete").performClick()

        composeRule.waitUntilDoesNotExist(hasText("Weekly backlog"))
    }

    private fun show() {
        val viewModel = ReportTemplatesViewModel(reportRepository)
        composeRule.setContent {
            ReportsListScreen(onBack = {}, onOpenTemplate = { opened = it }, viewModel = viewModel)
        }
    }
}
