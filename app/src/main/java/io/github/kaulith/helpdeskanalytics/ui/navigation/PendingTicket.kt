package io.github.kaulith.helpdeskanalytics.ui.navigation

import io.github.kaulith.helpdeskanalytics.domain.model.TicketFocus

/** A notification tap, waiting for the graph to be ready (and for login, on a cold start). */
data class PendingTicket(val ticketId: String, val focus: TicketFocus? = null)
