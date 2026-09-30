package io.github.kaulith.helpdeskanalytics.ui.screens

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.kaulith.helpdeskanalytics.domain.model.AgentPerformance
import io.github.kaulith.helpdeskanalytics.domain.model.LeaderboardPeriod
import io.github.kaulith.helpdeskanalytics.domain.model.Priority
import io.github.kaulith.helpdeskanalytics.domain.model.Status
import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportConfig
import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportTemplate
import io.github.kaulith.helpdeskanalytics.testing.FakeAgentRepository
import io.github.kaulith.helpdeskanalytics.testing.FakeAuthRepository
import io.github.kaulith.helpdeskanalytics.testing.FakeReportDataRepository
import io.github.kaulith.helpdeskanalytics.testing.FakeReportRepository
import io.github.kaulith.helpdeskanalytics.testing.FakeTeamRepository
import io.github.kaulith.helpdeskanalytics.testing.FakeTicketRepository
import io.github.kaulith.helpdeskanalytics.testing.ticket
import io.github.kaulith.helpdeskanalytics.ui.screens.analytics.AnalyticsScreen
import io.github.kaulith.helpdeskanalytics.ui.screens.analytics.AnalyticsViewModel
import io.github.kaulith.helpdeskanalytics.ui.screens.auth.LoginScreen
import io.github.kaulith.helpdeskanalytics.ui.screens.auth.LoginViewModel
import io.github.kaulith.helpdeskanalytics.ui.screens.auth.OAuthRedirectHolder
import io.github.kaulith.helpdeskanalytics.ui.screens.leaderboard.LeaderboardScreen
import io.github.kaulith.helpdeskanalytics.ui.screens.leaderboard.LeaderboardViewModel
import io.github.kaulith.helpdeskanalytics.ui.screens.reports.ReportBuilderScreen
import io.github.kaulith.helpdeskanalytics.ui.screens.reports.ReportBuilderViewModel
import io.github.kaulith.helpdeskanalytics.ui.screens.reports.ReportTemplatesViewModel
import io.github.kaulith.helpdeskanalytics.ui.screens.reports.ReportsListScreen
import io.github.kaulith.helpdeskanalytics.ui.screens.tickets.TicketListViewModel
import io.github.kaulith.helpdeskanalytics.ui.screens.tickets.TicketsScreen
import io.github.kaulith.helpdeskanalytics.ui.theme.HelpDeskAnalyticsTheme
import io.github.kaulith.helpdeskanalytics.util.Result
import kotlin.time.Clock
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Only screens whose pixels don't depend on the clock; the dashboard greets by the hour and
 * ticket detail prints dates, and neither takes a clock the test can fix.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = RobolectricDeviceQualifiers.MediumPhone)
class ScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val tickets = FakeTicketRepository().apply {
        tickets.value = listOf(
            ticket("104", Status.OPEN).copy(subject = "Invoice PDF shows the wrong currency", priority = Priority.HIGH),
            ticket("103", Status.REPLIED).copy(subject = "Password reset email never arrives"),
            ticket("102", Status.RESOLVED).copy(subject = "Export to CSV drops the last row"),
            ticket("101", Status.CLOSED).copy(subject = "Cannot add a second admin user", priority = Priority.URGENT)
        )
    }

    @Test
    fun login() {
        val viewModel = LoginViewModel(FakeAuthRepository(), FakeAgentRepository(), OAuthRedirectHolder())
        capture(hasText("Sign in")) { LoginScreen(onLoginSuccess = {}, viewModel = viewModel) }
    }

    @Test
    fun tickets() {
        val viewModel = TicketListViewModel(tickets, FakeAgentRepository())
        capture(hasText("Cannot add a second admin user")) { TicketsScreen({}, viewModel = viewModel) }
    }

    @Test
    fun analytics() {
        val viewModel = AnalyticsViewModel(tickets, FakeAgentRepository())
        capture(hasText("Open (1)")) { AnalyticsScreen(viewModel) }
    }

    @Test
    fun leaderboard() {
        tickets.agentPerformances(LeaderboardPeriod.AllTime).tryEmit(
            Result.Success(listOf(performance(1, "Ann Lee", 42), performance(2, "Bob Smith", 17)))
        )
        val viewModel = LeaderboardViewModel(tickets, FakeAgentRepository(), FakeTeamRepository())
        capture(hasText("Bob Smith")) { LeaderboardScreen(viewModel) }
    }

    @Test
    fun reports() {
        val reportRepository = FakeReportRepository().apply {
            templates.value = listOf(ReportTemplate(7, "Weekly backlog", ReportConfig(), Clock.System.now()))
        }
        val viewModel = ReportTemplatesViewModel(reportRepository)
        capture(hasText("Weekly backlog")) {
            ReportsListScreen(onBack = {}, onOpenTemplate = {}, viewModel = viewModel)
        }
    }

    @Test
    fun reportBuilder() {
        val viewModel =
            ReportBuilderViewModel(FakeReportDataRepository(), FakeReportRepository(), FakeAgentRepository())
        capture(hasText("Summary")) { ReportBuilderScreen(templateId = null, onBack = {}, viewModel = viewModel) }
    }

    private fun capture(loaded: SemanticsMatcher, screen: @Composable () -> Unit) {
        composeRule.setContent { HelpDeskAnalyticsTheme(themeMode = "light") { screen() } }
        composeRule.waitUntilAtLeastOneExists(loaded)
        composeRule.onRoot().captureRoboImage()
    }

    private fun performance(rank: Int, name: String, resolved: Int) = AgentPerformance(
        agentEmail = "${name.substringBefore(" ").lowercase()}@x.io",
        agentName = name,
        rank = rank,
        ticketsResolved = resolved,
        averageResponseTime = 45f,
        averageResolutionTime = 20f
    )
}
