package io.github.kaulith.helpdeskanalytics.ui.navigation

import android.app.Application
import android.content.Intent
import android.net.Uri
import io.github.kaulith.helpdeskanalytics.domain.model.TicketFocus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PendingTicketTest {

    @Test
    fun `a tap on a notification the app built opens its ticket`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("helpdesk://ticket/76093?focus=comment"))

        assertEquals(PendingTicket("76093", TicketFocus.COMMENT), PendingTicket.from(intent))
    }

    @Test
    fun `a tap on a notification fcm painted opens its ticket`() {
        val intent = Intent().putExtra("ticketId", "76093").putExtra("type", "customer_reply")

        assertEquals(PendingTicket("76093", TicketFocus.REPLY), PendingTicket.from(intent))
    }

    @Test
    fun `an oauth redirect or a plain launch is not a ticket`() {
        assertNull(PendingTicket.from(Intent(Intent.ACTION_VIEW, Uri.parse("helpdesk://oauth/callback?code=abc"))))
        assertNull(PendingTicket.from(Intent(Intent.ACTION_MAIN)))
    }
}
