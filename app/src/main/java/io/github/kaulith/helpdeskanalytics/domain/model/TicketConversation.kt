package io.github.kaulith.helpdeskanalytics.domain.model

/** A ticket's internal comments and its customer-facing email thread. */
data class TicketConversation(
    val comments: List<Comment>,
    val communications: List<Communication>
)
