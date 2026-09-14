package io.github.kaulith.helpdeskanalytics.data.remote.api

import io.github.kaulith.helpdeskanalytics.data.local.credentials.CredentialsManager
import io.github.kaulith.helpdeskanalytics.data.local.preferences.PreferencesManager
import io.github.kaulith.helpdeskanalytics.util.Result
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class FrappeAgentSessionManager(
    private val credentialsManager: CredentialsManager,
    private val preferencesManager: PreferencesManager,
    private val httpClient: OkHttpClient
) : AgentSessionManager {
    @Volatile
    private var activeAgentEmail: String? = credentialsManager.getActiveAgentEmail()

    override fun tokenForRequest(isWrite: Boolean): String? {
        val email = activeAgentEmail
        if (isWrite) {
            return if (email != null) credentialsManager.getAgentToken(email) else null
        }
        return credentialsManager.getAuthToken()
    }

    override fun canWrite(): Boolean {
        val email = activeAgentEmail ?: return false
        return credentialsManager.hasAgentKeys(email)
    }

    override suspend fun needsWriteKey(email: String): Boolean =
        !isLoginUser(email) && !credentialsManager.hasAgentKeys(email)

    override suspend fun activate(email: String, provisionWriteKey: Boolean) {
        if (isLoginUser(email)) {
            credentialsManager.setAgentUsesLoginSession(email)
        } else if (provisionWriteKey && !credentialsManager.hasAgentKeys(email)) {
            mintKeys(email)
        }
        credentialsManager.setActiveAgentEmail(email)
        activeAgentEmail = email
    }

    override fun deactivate() {
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
        val baseUrl = credentialsManager.siteBaseUrl()
            ?: throw IllegalStateException("No site URL configured")
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FrappeApiService::class.java)
    }
}
