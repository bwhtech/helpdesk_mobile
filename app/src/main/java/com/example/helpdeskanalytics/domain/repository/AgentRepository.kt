package com.example.helpdeskanalytics.domain.repository

import com.example.helpdeskanalytics.domain.model.Agent
import com.example.helpdeskanalytics.util.Result
import kotlinx.coroutines.flow.Flow

interface AgentRepository {
    fun getAgents(): Flow<Result<List<Agent>>>
    fun getActiveAgent(): Flow<Agent?>

    /** Selects an agent and provisions their API key, so writes are attributed to them. */
    suspend fun setActiveAgent(agent: Agent?): Result<Unit>

    suspend fun refreshAgents(): Result<List<Agent>>
}
