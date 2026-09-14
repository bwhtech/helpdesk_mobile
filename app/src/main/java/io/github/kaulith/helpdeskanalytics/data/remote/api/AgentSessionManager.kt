package io.github.kaulith.helpdeskanalytics.data.remote.api

import io.github.kaulith.helpdeskanalytics.util.Result

/**
 * Decides which API token each request carries. Reads run as the signed-in user;
 * writes run as the selected agent. Another agent's key is provisioned once from
 * the signed-in session via frappe's generate_keys and cached encrypted; the
 * signed-in account writes with the session it signed in with.
 */
interface AgentSessionManager {

    /**
     * Reads run as the signed-in user. Writes run as the active agent's own key,
     * and only that; it never falls back to the signed-in identity, so a write can't
     * be silently attributed to the login account when the agent has no minted key.
     */
    fun tokenForRequest(isWrite: Boolean): String?

    fun hasActiveAgent(): Boolean

    /** True only when the active agent has a token of their own, so writes can be attributed. */
    fun canWrite(): Boolean

    /** True when writing as this agent would need a key minted for them first. */
    suspend fun needsWriteKey(email: String): Boolean

    /**
     * Selects an agent. The account the user signed in as writes with the signed-in
     * session itself, because generate_keys rotates a user's api_secret and minting
     * for that account would invalidate the credentials the app is running on.
     * Minting another agent's write key is opt-in for the same rotation reason, and
     * is best-effort even then: if the signed-in user lacks System Manager, the agent
     * is still selected for read + notifications and the app stays read-only for writes.
     */
    suspend fun activate(email: String, provisionWriteKey: Boolean = false): Result<Unit>

    fun deactivate()
}
