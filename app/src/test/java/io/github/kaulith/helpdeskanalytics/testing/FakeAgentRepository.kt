package io.github.kaulith.helpdeskanalytics.testing

import io.github.kaulith.helpdeskanalytics.domain.model.Agent
import io.github.kaulith.helpdeskanalytics.domain.repository.AgentRepository
import io.github.kaulith.helpdeskanalytics.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class FakeAgentRepository : AgentRepository {

    val activeAgent = MutableStateFlow<Agent?>(null)

    override fun getAgents(): Flow<Result<List<Agent>>> = flowOf(Result.Success(emptyList()))

    override fun getActiveAgent(): Flow<Agent?> = activeAgent

    override suspend fun needsWriteKey(agent: Agent) = false

    override suspend fun setActiveAgent(agent: Agent?, provisionWriteKey: Boolean): Result<Unit> {
        activeAgent.value = agent
        return Result.Success(Unit)
    }

    override suspend fun selectLoginUserAsAgent() = Unit
}
