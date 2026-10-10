package io.github.kaulith.helpdeskanalytics.notifications

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import io.github.kaulith.helpdeskanalytics.data.local.preferences.PreferencesManager
import io.github.kaulith.helpdeskanalytics.data.remote.dto.TicketDto
import io.github.kaulith.helpdeskanalytics.testing.FakeApiServiceProvider
import io.github.kaulith.helpdeskanalytics.testing.FakeFrappeApiService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TicketPollerTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val preferencesManager = PreferencesManager(context)
    private val service = FakeFrappeApiService()
    private val poller = TicketPoller(FakeApiServiceProvider(service), preferencesManager, NotificationHelper(context))

    @Before
    fun setUp() = runBlocking {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        preferencesManager.setLoggedInUserEmail("rahul.agrawal@frappe.io")
    }

    @After
    fun tearDown() = runBlocking { preferencesManager.clearAll() }

    @Test
    fun `a ticket assigned since the last poll raises an alert`() = runBlocking {
        service.tickets = listOf(ticketDto("77595"))
        poller.poll()

        service.tickets = listOf(ticketDto("77595"), ticketDto("77596"))
        poller.poll()

        assertEquals(listOf("New ticket assigned #77596"), postedTitles())
    }

    @Test
    fun `a ticket taken away since the last poll raises an alert`() = runBlocking {
        service.tickets = listOf(ticketDto("77595"), ticketDto("77596"))
        poller.poll()

        service.tickets = listOf(ticketDto("77595"))
        poller.poll()

        assertEquals(listOf("Unassigned from ticket #77596"), postedTitles())
    }

    @Test
    fun `a customer reply since the last poll raises an alert`() = runBlocking {
        service.tickets = listOf(ticketDto("77595", lastCustomerResponse = "2026-09-13 22:16:57.203114"))
        poller.poll()

        service.tickets = listOf(ticketDto("77595", lastCustomerResponse = "2026-09-14 09:41:02.118530"))
        poller.poll()

        assertEquals(listOf("Customer replied #77595"), postedTitles())
    }

    private fun postedTitles() =
        shadowOf(context.getSystemService(NotificationManager::class.java)).allNotifications
            .map { it.extras.getString(Notification.EXTRA_TITLE) }

    private fun ticketDto(name: String, lastCustomerResponse: String? = null) = TicketDto(
        name = name,
        subject = "making pdf",
        status = "Open",
        priority = "Low",
        agent = null,
        creation = "2026-09-06 11:02:40.518734",
        modified = "2026-09-13 22:16:57.203114",
        firstRespondedOn = null,
        resolutionDate = null,
        lastAgentResponse = null,
        raisedBy = "firstmoondubai@hotmail.com",
        contact = null,
        customer = null,
        assign = """["rahul.agrawal@frappe.io"]""",
        responseBy = null,
        resolutionBy = null,
        firstResponseTime = null,
        avgResponseTime = null,
        resolutionTime = null,
        ticketType = null,
        sla = null,
        agreementStatus = null,
        description = null,
        lastCustomerResponse = lastCustomerResponse
    )
}
