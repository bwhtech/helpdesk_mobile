package io.github.kaulith.helpdeskanalytics.domain.model

/**
 * What a notification was actually about, so opening the ticket lands on the item
 * that triggered it instead of the top of the page.
 */
enum class TicketFocus(val slug: String) {
    REPLY("reply"),
    COMMENT("comment");

    companion object {
        fun fromSlug(slug: String?): TicketFocus? = entries.find { it.slug == slug }

        /** Push payload `type`, as sent by helpdesk_push. */
        fun fromPushType(type: String?): TicketFocus? = when (type) {
            "customer_reply" -> REPLY
            "new_comment" -> COMMENT
            else -> null
        }
    }
}
