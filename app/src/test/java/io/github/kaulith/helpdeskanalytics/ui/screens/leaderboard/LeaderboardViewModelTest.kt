package io.github.kaulith.helpdeskanalytics.ui.screens.leaderboard

import io.github.kaulith.helpdeskanalytics.domain.model.AgentPerformance
import io.github.kaulith.helpdeskanalytics.domain.model.LeaderboardPeriod
import io.github.kaulith.helpdeskanalytics.testing.FakeAgentRepository
import io.github.kaulith.helpdeskanalytics.testing.FakeTeamRepository
import io.github.kaulith.helpdeskanalytics.testing.FakeTicketRepository
import io.github.kaulith.helpdeskanalytics.testing.MainDispatcherRule
import io.github.kaulith.helpdeskanalytics.util.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `a slow load for the previous period does not replace the new one`() = runTest {
        val repository = FakeTicketRepository()
        val viewModel = LeaderboardViewModel(repository, FakeAgentRepository(), FakeTeamRepository())
        advanceUntilIdle()

        viewModel.setPeriod(LeaderboardPeriod.Week)
        advanceUntilIdle()
        val week = listOf(performance("ann@x.io", ticketsResolved = 3))
        repository.agentPerformances(LeaderboardPeriod.Week).emit(Result.Success(week))
        repository.agentPerformances(LeaderboardPeriod.AllTime)
            .emit(Result.Success(listOf(performance("bob@x.io", ticketsResolved = 40))))
        advanceUntilIdle()

        assertEquals(week, viewModel.uiState.value.agents)
    }

    private fun performance(email: String, ticketsResolved: Int) = AgentPerformance(
        agentEmail = email,
        agentName = email.substringBefore("@"),
        rank = 1,
        ticketsResolved = ticketsResolved,
        averageResponseTime = 0f,
        averageResolutionTime = 0f
    )
}
