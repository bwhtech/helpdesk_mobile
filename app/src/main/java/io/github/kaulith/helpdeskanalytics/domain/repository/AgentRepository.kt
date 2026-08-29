package io.github.kaulith.helpdeskanalytics.domain.repository

import io.github.kaulith.helpdeskanalytics.domain.model.Agent
import io.github.kaulith.helpdeskanalytics.util.Result
import kotlinx.coroutines.flow.Flow

interface AgentRepository {
    fun getAgents(): Flow<Result<List<Agent>>>
    fun getActiveAgent(): Flow<Agent?>

    /** True when writing as this agent would need a key minted for them first. */
    suspend fun needsWriteKey(agent: Agent): Boolean

    /**
     * Selects an agent. Pass [provisionWriteKey] to mint their API key so writes
     * are attributed to them; without it the app reads as them and stays
     * read-only, because minting replaces the agent's existing API secret.
     */
    suspend fun setActiveAgent(agent: Agent?, provisionWriteKey: Boolean = false): Result<Unit>

    suspend fun refreshAgents(): Result<List<Agent>>
}
