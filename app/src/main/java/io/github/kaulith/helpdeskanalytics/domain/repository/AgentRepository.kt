package io.github.kaulith.helpdeskanalytics.domain.repository

import io.github.kaulith.helpdeskanalytics.domain.model.Agent
import io.github.kaulith.helpdeskanalytics.util.Result
import kotlinx.coroutines.flow.Flow

interface AgentRepository {
    fun getAgents(): Flow<Result<List<Agent>>>
    fun getActiveAgent(): Flow<Agent?>

    /**
     * True when writing as this agent would need a key minted for them first and the
     * signed-in user may mint one; Frappe only lets a System Manager issue keys.
     */
    suspend fun needsWriteKey(agent: Agent): Boolean

    /**
     * Selects an agent. Pass [provisionWriteKey] to mint their API key so writes
     * are attributed to them; without it the app reads as them and stays
     * read-only, because minting replaces the agent's existing API secret. A failed
     * mint still selects the agent read-only and returns the error.
     */
    suspend fun setActiveAgent(agent: Agent?, provisionWriteKey: Boolean = false): Result<Unit>

    /** Selects the signed-in user when they are an agent on the site; otherwise leaves every agent in view. */
    suspend fun selectLoginUserAsAgent()
}
