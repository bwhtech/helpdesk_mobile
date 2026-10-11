package io.github.kaulith.helpdeskanalytics.data.repository

import io.github.kaulith.helpdeskanalytics.data.local.credentials.CredentialsManager
import io.github.kaulith.helpdeskanalytics.data.local.preferences.PreferencesManager
import io.github.kaulith.helpdeskanalytics.data.local.profiles.ProfileStore
import io.github.kaulith.helpdeskanalytics.data.mapper.toDomain
import io.github.kaulith.helpdeskanalytics.data.remote.api.ApiServiceProvider
import io.github.kaulith.helpdeskanalytics.data.remote.api.DeviceRegistration
import io.github.kaulith.helpdeskanalytics.data.remote.api.OAuthClient
import io.github.kaulith.helpdeskanalytics.data.remote.toNetworkError
import io.github.kaulith.helpdeskanalytics.domain.model.SiteProfile
import io.github.kaulith.helpdeskanalytics.domain.model.User
import io.github.kaulith.helpdeskanalytics.domain.repository.AuthRepository
import io.github.kaulith.helpdeskanalytics.util.NetworkError
import io.github.kaulith.helpdeskanalytics.util.Result

class FrappeAuthRepository(
    private val credentialsManager: CredentialsManager,
    private val profileStore: ProfileStore,
    private val apiServiceProvider: ApiServiceProvider,
    private val preferencesManager: PreferencesManager,
    private val oAuthClient: OAuthClient,
    private val deviceRegistration: DeviceRegistration
) : AuthRepository {

    override suspend fun validateCredentials(
        siteUrl: String,
        apiKey: String,
        apiSecret: String
    ): Result<User> = signIn(siteUrl) {
        credentialsManager.saveCredentials(siteUrl, apiKey, apiSecret)
    }

    override suspend fun discoverOAuthClientId(siteUrl: String): String? =
        oAuthClient.discoverClientId(siteUrl)

    override fun beginOAuthLogin(siteUrl: String, clientId: String): String =
        oAuthClient.beginAuthorization(siteUrl, clientId)

    override suspend fun completeOAuthLogin(code: String, state: String): Result<User> {
        val request = credentialsManager.getOAuthRequest()
            ?: return Result.Error(NetworkError.Unknown(IllegalStateException("No sign-in is in progress")))
        credentialsManager.clearOAuthRequest()
        // A redirect carrying someone else's state is not the sign-in this app started.
        if (state != request.state) {
            return Result.Error(NetworkError.Unknown(IllegalStateException("Sign-in could not be verified")))
        }

        return try {
            val token = oAuthClient.exchangeCode(request.siteUrl, request.clientId, code, request.codeVerifier)
            val accessToken = token.accessToken ?: error("Server returned no access token")

            signIn(request.siteUrl) {
                credentialsManager.saveOAuthLogin(request.siteUrl, request.clientId, accessToken, token.refreshToken)
            }
        } catch (e: Exception) {
            Result.Error(e.toNetworkError())
        }
    }

    override fun isLoggedIn(): Boolean {
        return credentialsManager.hasCredentials()
    }

    override suspend fun logout() {
        deviceRegistration.unregisterCurrentDevice()
        revokeOAuthSession()
        preferencesManager.clearProfile()
        credentialsManager.clearCredentials()
        credentialsManager.signedInSites().keys.firstOrNull()?.let(::useProfile)
        apiServiceProvider.invalidate()
    }

    override fun otherSites(): List<SiteProfile> =
        credentialsManager.signedInSites()
            .filterKeys { it != profileStore.activeProfileId() }
            .map { (profileId, siteUrl) -> SiteProfile(profileId, siteUrl) }

    override suspend fun switchSite(profileId: String) {
        deviceRegistration.unregisterCurrentDevice()
        useProfile(profileId)
    }

    // Alerts come from the site in use alone, so the device registers where it lands.
    private fun useProfile(profileId: String) {
        profileStore.activate(profileId)
        apiServiceProvider.invalidate()
        deviceRegistration.registerNow()
    }

    /**
     * Signs in to a site in a profile of its own, whichever way the session was
     * obtained. A sign-in the site turns down goes back to the profile that was in
     * use, and takes a profile it started with it.
     */
    private suspend fun signIn(siteUrl: String, saveSession: () -> Unit): Result<User> {
        val previousProfileId = profileStore.activeProfileId()
        val isNewProfile = !credentialsManager.hasProfile(siteUrl)
        deviceRegistration.unregisterCurrentDevice()
        saveSession()
        apiServiceProvider.invalidate()
        return try {
            val user = loadSignedInUser()
            deviceRegistration.registerNow()
            Result.Success(user)
        } catch (e: Exception) {
            if (isNewProfile) credentialsManager.removeProfile() else credentialsManager.clearSession()
            previousProfileId?.let(::useProfile)
            apiServiceProvider.invalidate()
            Result.Error(e.toNetworkError())
        }
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
}
