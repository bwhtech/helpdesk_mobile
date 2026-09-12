package io.github.kaulith.helpdeskanalytics.domain.model

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * The dashboard quick stats and the ticket list read the same predicate, so a card's
 * count and the list it opens can never drift apart.
 */
enum class TicketPreset(val slug: String, val label: String) {
    OPEN("open", "Open"),
    RESOLVED_TODAY("resolved_today", "Resolved today"),
    OVERDUE("overdue", "Overdue");

    fun matches(
        ticket: Ticket,
        now: Instant = Clock.System.now(),
        zone: TimeZone = TimeZone.currentSystemDefault()
    ): Boolean = when (this) {
        OPEN -> ticket.status == Status.OPEN
        RESOLVED_TODAY ->
            ticket.createdAt.toLocalDateTime(zone).date == now.toLocalDateTime(zone).date &&
                ticket.isResolved()
        OVERDUE -> ticket.isOverdue(now)
    }

    companion object {
        fun fromSlug(slug: String?): TicketPreset? = entries.find { it.slug == slug }
    }
}
