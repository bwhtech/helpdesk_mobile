package io.github.kaulith.helpdeskanalytics.data.remote.api

import io.github.kaulith.helpdeskanalytics.data.local.credentials.CredentialsManager
import io.github.kaulith.helpdeskanalytics.data.local.preferences.PreferencesManager
import io.github.kaulith.helpdeskanalytics.util.Result
import kotlinx.coroutines.flow.first
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Decides which API token each request carries. Reads run as the signed-in user;
 * writes run as the selected agent. Another agent's key is provisioned once from
 * the signed-in session via frappe's generate_keys and cached encrypted; the
 * signed-in account writes with the session it signed in with.
 */
class AgentSessionManager(
    private val credentialsManager: CredentialsManager,
    private val preferencesManager: PreferencesManager
) {
    @Volatile
    private var activeAgentEmail: String? = credentialsManager.getActiveAgentEmail()

    /**
     * Reads run as the signed-in user. Writes run as the active agent's own key,
     * and only that; it never falls back to the signed-in identity, so a write can't
     * be silently attributed to the login account when the agent has no minted key.
     */
    fun tokenForRequest(isWrite: Boolean): String? {
        val email = activeAgentEmail
        if (isWrite) {
            return if (email != null) credentialsManager.getAgentToken(email) else null
        }
        return credentialsManager.getAuthToken()
    }

    fun hasActiveAgent(): Boolean = activeAgentEmail != null

    /** True only when the active agent has a token of their own, so writes can be attributed. */
    fun canWrite(): Boolean {
        val email = activeAgentEmail ?: return false
        return credentialsManager.hasAgentKeys(email)
    }

    /** True when writing as this agent would need a key minted for them first. */
    suspend fun needsWriteKey(email: String): Boolean =
        !isLoginUser(email) && !credentialsManager.hasAgentKeys(email)

    /**
     * Selects an agent. The account the user signed in as writes with the signed-in
     * session itself, because generate_keys rotates a user's api_secret and minting
     * for that account would invalidate the credentials the app is running on.
     * Minting another agent's write key is opt-in for the same rotation reason, and
     * is best-effort even then: if the signed-in user lacks System Manager, the agent
     * is still selected for read + notifications and the app stays read-only for writes.
     */
    suspend fun activate(email: String, provisionWriteKey: Boolean = false): Result<Unit> {
        if (isLoginUser(email)) {
            credentialsManager.setAgentUsesLoginSession(email)
        } else if (provisionWriteKey && !credentialsManager.hasAgentKeys(email)) {
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

    private suspend fun isLoginUser(email: String): Boolean =
        email.equals(preferencesManager.loggedInUserEmail.first(), ignoreCase = true)

    private suspend fun mintKeys(email: String): Result<Unit> {
        return try {
            val sessionToken = credentialsManager.getAuthToken()
                ?: return Result.Error(IllegalStateException("Not signed in"))
            val service = buildService()
            val secret = service.generateKeys(sessionToken, email).message.apiSecret
                ?: return Result.Error(IllegalStateException("Server returned no api_secret"))
            val key = service.getUserApiKey(sessionToken, email).data.apiKey
                ?: return Result.Error(IllegalStateException("Agent has no api_key"))
            credentialsManager.saveAgentKeys(email, key, secret)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // A bare Retrofit with no auth interceptor; minting passes the session token explicitly.
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
