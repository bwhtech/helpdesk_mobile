package io.github.kaulith.helpdeskanalytics.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import io.github.kaulith.helpdeskanalytics.data.local.database.AppDatabase
import io.github.kaulith.helpdeskanalytics.data.local.preferences.PreferencesManager
import io.github.kaulith.helpdeskanalytics.data.remote.dto.AgentDto
import io.github.kaulith.helpdeskanalytics.domain.model.Agent
import io.github.kaulith.helpdeskanalytics.testing.FakeAgentSessionManager
import io.github.kaulith.helpdeskanalytics.testing.FakeApiServiceProvider
import io.github.kaulith.helpdeskanalytics.testing.FakeFrappeApiService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class FrappeAgentRepositoryTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val preferencesManager = PreferencesManager(context)
    private val service = FakeFrappeApiService().apply {
        agents = listOf(agentDto("rahul.agrawal@frappe.io", "Rahul Agrawal"), agentDto("ann@x.io", "Ann"))
    }
    private val repository = FrappeAgentRepository(
        apiServiceProvider = FakeApiServiceProvider(service),
        agentDao = database.agentDao(),
        preferencesManager = preferencesManager,
        agentSessionManager = FakeAgentSessionManager()
    )

    @After
    fun tearDown() = runBlocking {
        preferencesManager.clearAll()
        database.close()
    }

    @Test
    fun `a login user who is an agent becomes the active agent`() = runBlocking {
        preferencesManager.setLoggedInUserEmail("Ann@x.io")

        repository.selectLoginUserAsAgent()

        assertEquals(Agent(email = "ann@x.io", name = "Ann"), repository.getActiveAgent().first())
    }

    @Test
    fun `a login user who is not an agent leaves every agent in view`() = runBlocking {
        preferencesManager.setLoggedInUserEmail("hussain@erpnext.com")

        repository.selectLoginUserAsAgent()

        assertNull(repository.getActiveAgent().first())
    }

    private fun agentDto(email: String, name: String) =
        AgentDto(name = email, agentName = name, user = email, isActive = 1, userImage = null)
}
