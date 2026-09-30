package io.github.kaulith.helpdeskanalytics.ui.screens.reports

import android.app.Application
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.kaulith.helpdeskanalytics.testing.FakeAgentRepository
import io.github.kaulith.helpdeskanalytics.testing.FakeReportDataRepository
import io.github.kaulith.helpdeskanalytics.testing.FakeReportRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ReportBuilderScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `a summary report offers measures`() {
        composeRule.setContent {
            ReportBuilderScreen(
                templateId = null,
                onBack = {},
                viewModel = ReportBuilderViewModel(
                    FakeReportDataRepository(),
                    FakeReportRepository(),
                    FakeAgentRepository()
                )
            )
        }
        composeRule.waitUntilExactlyOneExists(hasText("Summary"))

        composeRule.onNodeWithText("Summary").performClick()

        composeRule.waitUntilExactlyOneExists(hasText("Add measure"))
    }
}
