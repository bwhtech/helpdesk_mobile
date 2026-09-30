package io.github.kaulith.helpdeskanalytics.testing

import io.github.kaulith.helpdeskanalytics.domain.model.Agent
import io.github.kaulith.helpdeskanalytics.domain.repository.AgentRepository
import io.github.kaulith.helpdeskanalytics.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class FakeAgentRepository : AgentRepository {

    val activeAgent = MutableStateFlow<Agent?>(null)
    var agents: List<Agent> = emptyList()
    var isLoginUserSelected = false
    val agentsNeedingWriteKey = mutableSetOf<String>()
    val writeKeysProvisioned = mutableListOf<String>()
    var switchError: Throwable? = null

    override fun getAgents(): Flow<Result<List<Agent>>> = flowOf(Result.Success(agents))

    override fun getActiveAgent(): Flow<Agent?> = activeAgent

    override suspend fun needsWriteKey(agent: Agent) = agent.email in agentsNeedingWriteKey

    override suspend fun setActiveAgent(agent: Agent?, provisionWriteKey: Boolean): Result<Unit> {
        switchError?.let { return Result.Error(it) }
        if (agent != null && provisionWriteKey) writeKeysProvisioned += agent.email
        activeAgent.value = agent
        return Result.Success(Unit)
    }

    override suspend fun selectLoginUserAsAgent() {
        isLoginUserSelected = true
    }
}
