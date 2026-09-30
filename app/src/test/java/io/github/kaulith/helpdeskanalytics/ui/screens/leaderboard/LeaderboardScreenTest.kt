package io.github.kaulith.helpdeskanalytics.ui.screens.leaderboard

import android.app.Application
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.kaulith.helpdeskanalytics.domain.model.AgentPerformance
import io.github.kaulith.helpdeskanalytics.domain.model.LeaderboardPeriod
import io.github.kaulith.helpdeskanalytics.testing.FakeAgentRepository
import io.github.kaulith.helpdeskanalytics.testing.FakeTeamRepository
import io.github.kaulith.helpdeskanalytics.testing.FakeTicketRepository
import io.github.kaulith.helpdeskanalytics.util.Result
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class LeaderboardScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val repository = FakeTicketRepository()

    @Test
    fun `a failed load shows the error`() {
        repository.agentPerformances(LeaderboardPeriod.AllTime)
            .tryEmit(Result.Error(IllegalStateException("Site unreachable")))
        show()

        composeRule.waitUntilExactlyOneExists(hasText("Couldn't load leaderboard"))
    }

    @Test
    fun `picking a period ranks that period`() {
        repository.agentPerformances(LeaderboardPeriod.AllTime).tryEmit(Result.Success(listOf(performance("Bob"))))
        repository.agentPerformances(LeaderboardPeriod.Week).tryEmit(Result.Success(listOf(performance("Ann"))))
        show()
        composeRule.waitUntilExactlyOneExists(hasText("Bob"))

        composeRule.onNodeWithText("All time").performClick()
        composeRule.onNodeWithText("Week").performClick()

        composeRule.waitUntilExactlyOneExists(hasText("Ann"))
    }

    private fun performance(name: String) = AgentPerformance(
        agentEmail = "${name.lowercase()}@x.io",
        agentName = name,
        rank = 1,
        ticketsResolved = 1,
        averageResponseTime = 0f,
        averageResolutionTime = 0f
    )

    private fun show() = composeRule.setContent {
        LeaderboardScreen(LeaderboardViewModel(repository, FakeAgentRepository(), FakeTeamRepository()))
    }
}
