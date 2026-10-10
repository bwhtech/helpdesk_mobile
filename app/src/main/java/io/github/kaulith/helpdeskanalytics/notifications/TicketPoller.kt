package io.github.kaulith.helpdeskanalytics.notifications

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.github.kaulith.helpdeskanalytics.data.local.preferences.PreferencesManager
import io.github.kaulith.helpdeskanalytics.data.remote.api.ApiServiceProvider
import io.github.kaulith.helpdeskanalytics.domain.model.TicketFocus
import kotlinx.coroutines.flow.first

/**
 * Stands in for push on a Helpdesk site that runs without helpdesk_push: compares
 * the agent's assigned tickets with the previous poll and raises the alerts the
 * push app would have sent. The first poll for an agent only records what is there.
 */
class TicketPoller(
    private val apiServiceProvider: ApiServiceProvider,
    private val preferencesManager: PreferencesManager,
    private val notificationHelper: NotificationHelper
) {
    suspend fun poll() {
        val agent = preferencesManager.activeAgentEmail.first()
            ?: preferencesManager.loggedInUserEmail.first()
            ?: return
        val tickets = apiServiceProvider.getService()
            .getTickets(fields = FIELDS, filters = """[["_assign","like","%$agent%"]]""")
            .data
        val replies = tickets.associate { it.name to it.lastCustomerResponse.orEmpty() }
        val subjects = tickets.associate { it.name to it.subject.orEmpty() }

        val previous = preferencesManager.ticketPollState.first()
            ?.let { gson.fromJson(it, State::class.java) }
            ?.takeIf { it.agent == agent }
        if (previous != null) {
            for (ticket in replies.keys - previous.replies.keys) {
                notificationHelper.showNotification(
                    title = "New ticket assigned #$ticket",
                    body = subjects.getValue(ticket),
                    ticketId = ticket
                )
            }
            for (ticket in previous.replies.keys - replies.keys) {
                notificationHelper.showNotification(
                    title = "Unassigned from ticket #$ticket",
                    body = "",
                    ticketId = ticket
                )
            }
            val replied = replies.filter { (ticket, reply) ->
                reply.isNotEmpty() && ticket in previous.replies && previous.replies[ticket] != reply
            }
            for (ticket in replied.keys) {
                notificationHelper.showNotification(
                    title = "Customer replied #$ticket",
                    body = subjects.getValue(ticket),
                    channelId = NotificationHelper.CHANNEL_TICKET_REPLIES,
                    ticketId = ticket,
                    focus = TicketFocus.REPLY
                )
            }
        }

        preferencesManager.setTicketPollState(gson.toJson(State(agent, replies)))
    }

    /** Each assigned ticket with the time of its last customer reply, blank when there is none. */
    private data class State(
        @SerializedName("agent") val agent: String,
        @SerializedName("replies") val replies: Map<String, String>
    )

    private companion object {
        const val FIELDS = """["name","subject","last_customer_response"]"""
        val gson = Gson()
    }
}
