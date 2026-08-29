package com.example.helpdeskanalytics.data.remote.api

import com.example.helpdeskanalytics.data.local.credentials.CredentialsManager
import com.example.helpdeskanalytics.util.Result
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Decides which API token each request carries. Reads run as the admin (the
 * login key); writes run as the selected agent. An agent's key is provisioned
 * once from the admin key via frappe's generate_keys and cached encrypted.
 */
class AgentSessionManager(
    private val credentialsManager: CredentialsManager
) {
    @Volatile
    private var activeAgentEmail: String? = credentialsManager.getActiveAgentEmail()

    /**
     * Reads run as the admin (login key). Writes run as the active agent's own key,
     * and only that; it never falls back to the admin identity, so a write can't be
     * silently attributed to the login account when the agent has no minted key.
     */
    fun tokenForRequest(isWrite: Boolean): String? {
        val email = activeAgentEmail
        if (isWrite) {
            return if (email != null) credentialsManager.getAgentToken(email) else null
        }
        return credentialsManager.getAuthToken()
    }

    fun hasActiveAgent(): Boolean = activeAgentEmail != null

    /** True only when the active agent has a real minted key, so writes can be attributed. */
    fun canWrite(): Boolean {
        val email = activeAgentEmail ?: return false
        return credentialsManager.hasAgentKeys(email)
    }

    /**
     * Selects an agent. Minting the agent's write key is best-effort: if the login
     * key lacks System Manager (can't provision), the agent is still selected for
     * read + notifications and the app stays read-only for writes.
     */
    suspend fun activate(email: String): Result<Unit> {
        if (!credentialsManager.hasAgentKeys(email)) {
            mintKeys(email)
        }
        credentialsManager.setActiveAgentEmail(email)
        activeAgentEmail = email
        return Result.Success(Unit)
    }

    fun deactivate() {
        credentialsManager.setActiveAgentEmail(null)
        activeAgentEmail = null
    }

    private suspend fun mintKeys(email: String): Result<Unit> {
        return try {
            val adminToken = credentialsManager.getAuthToken()
                ?: return Result.Error(IllegalStateException("Not signed in"))
            val service = buildService()
            val secret = service.generateKeys(adminToken, email).message.apiSecret
                ?: return Result.Error(IllegalStateException("Server returned no api_secret"))
            val key = service.getUserApiKey(adminToken, email).data.apiKey
                ?: return Result.Error(IllegalStateException("Agent has no api_key"))
            credentialsManager.saveAgentKeys(email, key, secret)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // A bare Retrofit with no auth interceptor; minting passes the admin token explicitly.
    private fun buildService(): FrappeApiService {
        val raw = credentialsManager.getSiteUrl()
            ?: throw IllegalStateException("No site URL configured")
        val base = if (raw.endsWith("/")) raw else "$raw/"
        return Retrofit.Builder()
            .baseUrl(base)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FrappeApiService::class.java)
    }
}
