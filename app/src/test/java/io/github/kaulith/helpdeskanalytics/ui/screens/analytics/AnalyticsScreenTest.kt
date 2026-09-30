package io.github.kaulith.helpdeskanalytics.ui.screens.analytics

import android.app.Application
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.kaulith.helpdeskanalytics.domain.model.Status
import io.github.kaulith.helpdeskanalytics.testing.FakeAgentRepository
import io.github.kaulith.helpdeskanalytics.testing.FakeTicketRepository
import io.github.kaulith.helpdeskanalytics.testing.ticket
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AnalyticsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val repository = FakeTicketRepository()

    @Test
    fun `a failed load shows the error`() {
        repository.ticketsError = IllegalStateException("Site unreachable")
        show()

        composeRule.waitUntilExactlyOneExists(hasText("Couldn't load analytics"))
    }

    @Test
    fun `the status distribution counts each status`() {
        repository.tickets.value =
            listOf(ticket("1", Status.OPEN), ticket("2", Status.OPEN), ticket("3", Status.CLOSED))
        show()

        composeRule.waitUntilExactlyOneExists(hasText("Open (2)"))
        composeRule.waitUntilExactlyOneExists(hasText("Closed (1)"))
    }

    private fun show() = composeRule.setContent {
        AnalyticsScreen(AnalyticsViewModel(repository, FakeAgentRepository()))
    }
}
