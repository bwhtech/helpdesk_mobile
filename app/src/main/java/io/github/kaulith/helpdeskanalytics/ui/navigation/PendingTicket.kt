package io.github.kaulith.helpdeskanalytics.ui.navigation

import android.content.Intent
import io.github.kaulith.helpdeskanalytics.domain.model.TicketFocus

/** A notification tap, waiting for the graph to be ready (and for login, on a cold start). */
data class PendingTicket(val ticketId: String, val focus: TicketFocus? = null) {

    companion object {
        // A tray notification FCM paints itself carries the ticket as extras; one the app
        // builds carries the helpdesk://ticket/{id} deep link, which a running activity
        // receives in onNewIntent without the nav graph ever seeing it.
        fun from(intent: Intent): PendingTicket? {
            intent.data?.takeIf { it.host == TICKET_HOST }?.let { link ->
                val ticketId = link.lastPathSegment ?: return null
                return PendingTicket(ticketId, TicketFocus.fromSlug(link.getQueryParameter("focus")))
            }
            val ticketId = intent.getStringExtra(TICKET_ID_EXTRA)?.takeIf { it.isNotBlank() } ?: return null
            return PendingTicket(ticketId, TicketFocus.fromPushType(intent.getStringExtra(TYPE_EXTRA)))
        }

        /**
         * The activity keeps its last intent, so one that still names an opened ticket
         * opens it again whenever a nav graph is built later (after signing back in)
         * or the activity is recreated.
         */
        fun clearFrom(intent: Intent) {
            if (intent.data?.host == TICKET_HOST) intent.data = null
            intent.removeExtra(TICKET_ID_EXTRA)
            intent.removeExtra(TYPE_EXTRA)
        }

        private const val TICKET_HOST = "ticket"
        private const val TICKET_ID_EXTRA = "ticketId"
        private const val TYPE_EXTRA = "type"
    }
}
