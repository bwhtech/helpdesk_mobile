package io.github.kaulith.helpdeskanalytics.ui.agent

import io.github.kaulith.helpdeskanalytics.domain.model.Agent
import io.github.kaulith.helpdeskanalytics.testing.FakeAgentRepository
import io.github.kaulith.helpdeskanalytics.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** Minting a key replaces the agent's API secret, so it never happens without a confirmation. */
@OptIn(ExperimentalCoroutinesApi::class)
class AgentSwitcherViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val agentRepository = FakeAgentRepository()
    private val viewModel by lazy { AgentSwitcherViewModel(agentRepository) }
    private val bob = Agent(email = "bob@x.io", name = "Bob")

    @Test
    fun `an agent without a key is asked about before anything changes`() = runTest {
        agentRepository.agentsNeedingWriteKey += bob.email

        viewModel.setActiveAgent(bob)
        advanceUntilIdle()

        assertEquals(bob, viewModel.uiState.value.writeKeyPromptAgent)
        assertNull(agentRepository.activeAgent.value)
    }

    @Test
    fun `confirming mints the key and switches`() = runTest {
        agentRepository.agentsNeedingWriteKey += bob.email
        viewModel.setActiveAgent(bob)
        advanceUntilIdle()

        viewModel.confirmWriteKey()
        advanceUntilIdle()

        assertEquals(bob, viewModel.uiState.value.activeAgent)
        assertEquals(listOf(bob.email), agentRepository.writeKeysProvisioned)
        assertNull(viewModel.uiState.value.writeKeyPromptAgent)
    }

    @Test
    fun `skipping switches read only and leaves the secret alone`() = runTest {
        agentRepository.agentsNeedingWriteKey += bob.email
        viewModel.setActiveAgent(bob)
        advanceUntilIdle()

        viewModel.skipWriteKey()
        advanceUntilIdle()

        assertEquals(bob, viewModel.uiState.value.activeAgent)
        assertEquals(emptyList<String>(), agentRepository.writeKeysProvisioned)
    }

    @Test
    fun `an agent that already has a key switches straight away`() = runTest {
        viewModel.setActiveAgent(bob)
        advanceUntilIdle()

        assertEquals(bob, viewModel.uiState.value.activeAgent)
        assertNull(viewModel.uiState.value.writeKeyPromptAgent)
    }

    @Test
    fun `a failed switch says why`() = runTest {
        agentRepository.switchError = IllegalStateException("Not permitted")

        viewModel.setActiveAgent(bob)
        advanceUntilIdle()

        assertEquals("Not permitted", viewModel.uiState.value.agentSwitchError)
        assertEquals(false, viewModel.uiState.value.isSwitchingAgent)
    }
}
