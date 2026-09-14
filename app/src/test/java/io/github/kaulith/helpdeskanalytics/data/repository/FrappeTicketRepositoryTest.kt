package io.github.kaulith.helpdeskanalytics.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import io.github.kaulith.helpdeskanalytics.data.local.database.AppDatabase
import io.github.kaulith.helpdeskanalytics.data.local.preferences.PreferencesManager
import io.github.kaulith.helpdeskanalytics.data.mapper.toEntity
import io.github.kaulith.helpdeskanalytics.data.remote.dto.RoleDto
import io.github.kaulith.helpdeskanalytics.data.remote.dto.TicketDto
import io.github.kaulith.helpdeskanalytics.data.remote.dto.UserDto
import io.github.kaulith.helpdeskanalytics.domain.model.User
import io.github.kaulith.helpdeskanalytics.testing.FakeAgentSessionManager
import io.github.kaulith.helpdeskanalytics.testing.FakeApiServiceProvider
import io.github.kaulith.helpdeskanalytics.testing.FakeFrappeApiService
import io.github.kaulith.helpdeskanalytics.util.Constants
import io.github.kaulith.helpdeskanalytics.util.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class FrappeTicketRepositoryTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val preferencesManager = PreferencesManager(context)
    private val service = FakeFrappeApiService()
    private val repository = FrappeTicketRepository(
        apiServiceProvider = FakeApiServiceProvider(service),
        ticketDao = database.ticketDao(),
        userDao = database.userDao(),
        commentDao = database.commentDao(),
        preferencesManager = preferencesManager,
        agentSessionManager = FakeAgentSessionManager()
    )

    @After
    fun tearDown() = runBlocking {
        preferencesManager.clearAll()
        database.close()
    }

    @Test
    fun `an agent's tickets do not replace the cached tickets of every agent`() = runBlocking {
        service.tickets = listOf(
            ticketDto("77595", assign = """["rahul.agrawal@frappe.io"]"""),
            ticketDto("77596", assign = """["ann@x.io"]"""),
            ticketDto("77597", assign = null)
        )
        repository.refresh()
        val lastSync = preferencesManager.lastSync.first()

        val agentTickets = repository.getTickets(assignedTo = "ann@x.io").toList()

        assertEquals(listOf("77596"), (agentTickets.last() as Result.Success).data.map { it.id })
        assertEquals(
            setOf("77595", "77596", "77597"),
            database.ticketDao().getAllTickets().first().map { it.id }.toSet()
        )
        assertEquals(lastSync, preferencesManager.lastSync.first())
    }

    @Test
    fun `screens opening on a stale cache share one ticket fetch`() = runBlocking {
        service.tickets = listOf(ticketDto("77595", assign = null))
        repository.refresh()
        preferencesManager.setLastSync(System.currentTimeMillis() - Constants.CACHE_TTL_TICKETS - 1)
        val gate = CompletableDeferred<Unit>()
        service.ticketsGate = gate
        val fetchesBefore = service.ticketFetches

        val screens = List(SCREEN_COUNT) { async { repository.getTickets().toList() } }
        withTimeoutOrNull(SECOND_FETCH_TIMEOUT_MS) {
            while (service.ticketFetches - fetchesBefore < 2) delay(POLL_INTERVAL_MS)
        }
        gate.complete(Unit)

        screens.awaitAll().forEach { emissions ->
            assertEquals(listOf("77595"), (emissions.last() as Result.Success).data.map { it.id })
        }
        assertEquals(1, service.ticketFetches - fetchesBefore)
    }

    @Test
    fun `the user profile refreshes once a day even while tickets keep syncing`() = runBlocking {
        val now = System.currentTimeMillis()
        val cachedUser = User(
            email = "ann@x.io",
            fullName = "Ann",
            roles = listOf("Agent"),
            hasTeamLeadPermission = false
        )
        database.userDao().upsertUser(cachedUser.toEntity())
        preferencesManager.setUserSyncedAt(now - Constants.CACHE_TTL_USER - 1)
        preferencesManager.setLastSync(now)
        service.user = UserDto(
            name = "ann@x.io",
            fullName = "Ann",
            email = "ann@x.io",
            roles = listOf(RoleDto("Agent"), RoleDto("HD Team Lead"))
        )

        val user = repository.getCurrentUser().toList().last()

        assertEquals(1, service.userFetches)
        assertTrue((user as Result.Success).data.hasTeamLeadPermission)
    }

    private fun ticketDto(name: String, assign: String?) = TicketDto(
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
        assign = assign,
        responseBy = null,
        resolutionBy = null,
        firstResponseTime = null,
        avgResponseTime = null,
        resolutionTime = null,
        ticketType = "UI/UX",
        sla = null,
        agreementStatus = null,
        description = null
    )

    private companion object {
        const val SCREEN_COUNT = 3
        const val SECOND_FETCH_TIMEOUT_MS = 500L
        const val POLL_INTERVAL_MS = 10L
    }
}
