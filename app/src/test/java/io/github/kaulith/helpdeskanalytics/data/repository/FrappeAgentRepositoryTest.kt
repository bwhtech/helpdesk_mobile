package io.github.kaulith.helpdeskanalytics.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import io.github.kaulith.helpdeskanalytics.data.local.database.AppDatabase
import io.github.kaulith.helpdeskanalytics.data.local.database.entities.UserEntity
import io.github.kaulith.helpdeskanalytics.data.local.preferences.PreferencesManager
import io.github.kaulith.helpdeskanalytics.data.remote.dto.AgentDto
import io.github.kaulith.helpdeskanalytics.domain.model.Agent
import io.github.kaulith.helpdeskanalytics.testing.FakeAgentSessionManager
import io.github.kaulith.helpdeskanalytics.testing.FakeApiServiceProvider
import io.github.kaulith.helpdeskanalytics.testing.FakeFrappeApiService
import io.github.kaulith.helpdeskanalytics.util.Result
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    private val agentSessionManager = FakeAgentSessionManager()
    private val repository = FrappeAgentRepository(
        apiServiceProvider = FakeApiServiceProvider(service),
        agentDao = database.agentDao(),
        preferencesManager = preferencesManager,
        agentSessionManager = agentSessionManager,
        userDao = database.userDao()
    )
    private val ann = Agent(email = "ann@x.io", name = "Ann")

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

    @Test
    fun `only a system manager is offered a write key`() = runBlocking {
        agentSessionManager.isWriteKeyMissing = true

        signInWithRoles("HD Agent")
        assertFalse(repository.needsWriteKey(ann))

        signInWithRoles("HD Agent", "System Manager")
        assertTrue(repository.needsWriteKey(ann))
    }

    @Test
    fun `a write key is still offered before the signed in roles load`() = runBlocking {
        agentSessionManager.isWriteKeyMissing = true

        assertTrue(repository.needsWriteKey(ann))
    }

    @Test
    fun `a refused write key selects the agent read only and says why`() = runBlocking {
        val refused = Result.Error(IllegalStateException("Not permitted"))
        agentSessionManager.activation = refused

        assertEquals(refused, repository.setActiveAgent(ann, provisionWriteKey = true))
        assertEquals(ann, repository.getActiveAgent().first())
    }

    private suspend fun signInWithRoles(vararg roles: String) = database.userDao().upsertUser(
        UserEntity(
            email = "hussain@erpnext.com",
            fullName = "Hussain",
            roles = roles.toList(),
            hasTeamLeadPermission = false
        )
    )

    private fun agentDto(email: String, name: String) =
        AgentDto(name = email, agentName = name, user = email, isActive = 1, userImage = null)
}
