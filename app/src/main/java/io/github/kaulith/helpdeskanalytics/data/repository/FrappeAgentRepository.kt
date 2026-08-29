package io.github.kaulith.helpdeskanalytics.data.repository

import io.github.kaulith.helpdeskanalytics.data.local.database.dao.AgentDao
import io.github.kaulith.helpdeskanalytics.data.local.preferences.PreferencesManager
import io.github.kaulith.helpdeskanalytics.data.mapper.toDomain
import io.github.kaulith.helpdeskanalytics.data.mapper.toEntity
import io.github.kaulith.helpdeskanalytics.data.remote.api.AgentSessionManager
import io.github.kaulith.helpdeskanalytics.data.remote.api.ApiServiceProvider
import io.github.kaulith.helpdeskanalytics.domain.model.Agent
import io.github.kaulith.helpdeskanalytics.domain.repository.AgentRepository
import io.github.kaulith.helpdeskanalytics.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class FrappeAgentRepository(
    private val apiServiceProvider: ApiServiceProvider,
    private val agentDao: AgentDao,
    private val preferencesManager: PreferencesManager,
    private val agentSessionManager: AgentSessionManager
) : AgentRepository {

    override fun getAgents(): Flow<Result<List<Agent>>> = flow {
        emit(Result.Loading)

        val cached = agentDao.getAllAgents().first()
        if (cached.isNotEmpty()) {
            emit(Result.Success(cached.map { it.toDomain() }))
        }

        try {
            val service = apiServiceProvider.getService()
            val response = service.getAgents()
            val agents = response.data.map { it.toDomain() }
            agentDao.deleteAll()
            agentDao.insertAll(agents.map { it.toEntity() })
            emit(Result.Success(agents))
        } catch (e: Exception) {
            if (cached.isEmpty()) {
                emit(Result.Error(e))
            }
        }
    }

    override fun getActiveAgent(): Flow<Agent?> {
        return combine(
            preferencesManager.activeAgentEmail,
            preferencesManager.activeAgentName
        ) { email, name ->
            if (email != null && name != null) {
                Agent(email = email, name = name)
            } else {
                null
            }
        }
    }

    override suspend fun needsWriteKey(agent: Agent): Boolean =
        agentSessionManager.needsWriteKey(agent.email)

    override suspend fun setActiveAgent(agent: Agent?, provisionWriteKey: Boolean): Result<Unit> {
        if (agent != null) {
            val activated = agentSessionManager.activate(agent.email, provisionWriteKey)
            if (activated is Result.Error) return activated
        } else {
            agentSessionManager.deactivate()
        }
        preferencesManager.setActiveAgent(agent?.email, agent?.name)
        return Result.Success(Unit)
    }

    override suspend fun refreshAgents(): Result<List<Agent>> {
        return try {
            val service = apiServiceProvider.getService()
            val response = service.getAgents()
            val agents = response.data.map { it.toDomain() }
            agentDao.deleteAll()
            agentDao.insertAll(agents.map { it.toEntity() })
            Result.Success(agents)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
