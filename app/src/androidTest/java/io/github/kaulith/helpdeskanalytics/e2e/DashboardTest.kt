package io.github.kaulith.helpdeskanalytics.e2e

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class DashboardTest : AppTest() {

    @Test
    fun the_open_quick_stat_lists_open_tickets() {
        signIn()

        composeRule.onNode(hasContentDescription("Open: 1, opens the ticket list")).performClick()

        composeRule.waitUntilExactlyOneExists(ticket("104"), TIMEOUT)
        composeRule.onNode(ticket("101")).assertDoesNotExist()
    }

    @Test
    fun the_analytics_tab_counts_each_status() {
        signIn()

        openTab("Analytics")

        composeRule.waitUntilExactlyOneExists(hasText("Open (1)"), TIMEOUT)
    }

    @Test
    fun the_ranking_tab_lists_agents() {
        signIn()

        openTab("Ranking")

        composeRule.waitUntilAtLeastOneExists(hasText("Ann Lee"), TIMEOUT)
    }
}
