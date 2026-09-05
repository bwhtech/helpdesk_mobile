package io.github.kaulith.helpdeskanalytics.data.repository

import io.github.kaulith.helpdeskanalytics.data.local.credentials.CredentialsManager
import io.github.kaulith.helpdeskanalytics.data.local.preferences.PreferencesManager
import io.github.kaulith.helpdeskanalytics.data.mapper.toDomain
import io.github.kaulith.helpdeskanalytics.data.remote.api.ApiServiceProvider
import io.github.kaulith.helpdeskanalytics.data.remote.api.OAuthClient
import io.github.kaulith.helpdeskanalytics.domain.model.User
import io.github.kaulith.helpdeskanalytics.domain.repository.AuthRepository
import io.github.kaulith.helpdeskanalytics.util.NetworkError
import io.github.kaulith.helpdeskanalytics.util.Result
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

class FrappeAuthRepository(
    private val credentialsManager: CredentialsManager,
    private val apiServiceProvider: ApiServiceProvider,
    private val preferencesManager: PreferencesManager,
    private val oAuthClient: OAuthClient
) : AuthRepository {

    override suspend fun validateCredentials(
        siteUrl: String,
        apiKey: String,
        apiSecret: String
    ): Result<User> {
        return try {
            credentialsManager.saveCredentials(siteUrl, apiKey, apiSecret)
            apiServiceProvider.invalidate()
            Result.Success(loadSignedInUser())
        } catch (e: Exception) {
            credentialsManager.clearCredentials()
            apiServiceProvider.invalidate()
            Result.Error(toNetworkError(e))
        }
    }

    override suspend fun discoverOAuthClientId(siteUrl: String): String? =
        oAuthClient.discoverClientId(siteUrl)

    override fun beginOAuthLogin(siteUrl: String, clientId: String): String =
        oAuthClient.beginAuthorization(siteUrl, clientId)

    override suspend fun completeOAuthLogin(code: String, state: String): Result<User> {
        val expectedState = credentialsManager.getOAuthState()
        val codeVerifier = credentialsManager.getOAuthVerifier()
        val siteUrl = credentialsManager.getSiteUrl()
        val clientId = credentialsManager.getOAuthClientId()

        if (expectedState == null || codeVerifier == null || siteUrl == null || clientId == null) {
            return Result.Error(NetworkError.Unknown(IllegalStateException("No sign-in is in progress")))
        }
        // A redirect carrying someone else's state is not the sign-in this app started.
        if (state != expectedState) {
            credentialsManager.clearOAuthRequest()
            return Result.Error(NetworkError.Unknown(IllegalStateException("Sign-in could not be verified")))
        }

        return try {
            val token = oAuthClient.exchangeCode(siteUrl, clientId, code, codeVerifier)
            val accessToken = token.accessToken ?: error("Server returned no access token")

            credentialsManager.saveOAuthSession(accessToken, token.refreshToken, token.expiresIn)
            credentialsManager.clearOAuthRequest()
            apiServiceProvider.invalidate()

            Result.Success(loadSignedInUser())
        } catch (e: Exception) {
            credentialsManager.clearOAuthSession()
            credentialsManager.clearOAuthRequest()
            apiServiceProvider.invalidate()
            Result.Error(toNetworkError(e))
        }
    }

    override fun isLoggedIn(): Boolean {
        return credentialsManager.hasCredentials()
    }

    override suspend fun logout() {
        revokeOAuthSession()
        credentialsManager.clearCredentials()
        apiServiceProvider.invalidate()
        preferencesManager.setLoggedInUserEmail(null)
    }

    private suspend fun loadSignedInUser(): User {
        val service = apiServiceProvider.getService()
        val email = service.getLoggedUser().message
        val user = service.getUser(email).data.toDomain()
        preferencesManager.setLoggedInUserEmail(email)
        return user
    }

    // Best effort: a site that refuses the revoke still loses the tokens locally.
    private suspend fun revokeOAuthSession() {
        val siteUrl = credentialsManager.getSiteUrl() ?: return
        val accessToken = credentialsManager.getAccessToken() ?: return
        runCatching { oAuthClient.revoke(siteUrl, accessToken) }
    }

    private fun toNetworkError(e: Exception): NetworkError = when (e) {
        is HttpException -> when (e.code()) {
            401, 403 -> NetworkError.Unauthorized
            else -> NetworkError.ApiError(e.code(), e.message())
        }
        is SocketTimeoutException -> NetworkError.Timeout
        is IOException -> NetworkError.NoInternet
        else -> NetworkError.Unknown(e)
    }
}
