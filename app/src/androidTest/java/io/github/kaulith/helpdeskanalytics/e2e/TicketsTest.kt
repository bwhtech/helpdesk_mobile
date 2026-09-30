package io.github.kaulith.helpdeskanalytics.e2e

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TicketsTest : AppTest() {

    @Test
    fun the_list_shows_synced_tickets() {
        openTickets()

        composeRule.onNode(ticket("101")).assertExists()
    }

    @Test
    fun a_status_chip_narrows_the_list() {
        openTickets()

        composeRule.onNode(hasText("Closed") and isSelectable()).performScrollTo().performClick()

        composeRule.waitUntilDoesNotExist(ticket("104"), TIMEOUT)
        composeRule.onNode(ticket("101")).assertExists()
    }

    @Test
    fun opening_a_ticket_shows_its_conversation() {
        openTickets()

        composeRule.onNode(ticket("104")).performClick()

        composeRule.waitUntilAtLeastOneExists(hasText("checking the currency", substring = true), TIMEOUT)
    }

    @Test
    fun a_sent_reply_is_confirmed() {
        openTickets()
        composeRule.onNode(ticket("104")).performClick()

        composeRule.onNode(hasSetTextAction()).performTextInput("Fixed in the latest update")
        composeRule.onNodeWithText("Send").performClick()

        composeRule.waitUntilExactlyOneExists(hasText("Reply sent"), TIMEOUT)
    }

    private fun openTickets() {
        signIn()
        openTab("Tickets")
        composeRule.waitUntilExactlyOneExists(ticket("104"), TIMEOUT)
    }
}
