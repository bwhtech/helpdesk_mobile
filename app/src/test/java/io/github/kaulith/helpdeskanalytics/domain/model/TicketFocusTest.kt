package io.github.kaulith.helpdeskanalytics.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TicketFocusTest {

    @Test
    fun `push types map to the section the notification is about`() {
        assertEquals(TicketFocus.REPLY, TicketFocus.fromPushType("customer_reply"))
        assertEquals(TicketFocus.COMMENT, TicketFocus.fromPushType("new_comment"))
    }

    @Test
    fun `notifications that point at no single item carry no focus`() {
        assertNull(TicketFocus.fromPushType("new_assignment"))
        assertNull(TicketFocus.fromPushType("sla_warning"))
        assertNull(TicketFocus.fromPushType(null))
    }

    @Test
    fun `slugs survive the trip through the deep link`() {
        TicketFocus.entries.forEach { assertEquals(it, TicketFocus.fromSlug(it.slug)) }
        assertNull(TicketFocus.fromSlug("nonsense"))
    }
}
