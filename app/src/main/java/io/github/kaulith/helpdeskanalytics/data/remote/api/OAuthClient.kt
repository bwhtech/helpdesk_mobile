package io.github.kaulith.helpdeskanalytics.data.remote.api

import android.net.Uri
import android.util.Base64
import android.util.Log
import io.github.kaulith.helpdeskanalytics.data.local.credentials.CredentialsManager
import io.github.kaulith.helpdeskanalytics.data.remote.dto.OAuthTokenDto
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * The authorization-code half of the Frappe OAuth2 flow: builds the authorize URL,
 * trades the returned code for tokens, and refreshes them when they expire.
 */
class OAuthClient(private val credentialsManager: CredentialsManager) {

    private var cachedBaseUrl: String? = null
    private var cachedService: OAuthService? = null

    /**
     * Starts a sign-in. The site URL, client id, state and code verifier are stored
     * before the browser opens; the redirect can come back to a fresh process.
     */
    fun beginAuthorization(siteUrl: String, clientId: String): String {
        val site = siteUrl.trimEnd('/')
        val verifier = randomUrlSafe(VERIFIER_BYTES)
        val state = randomUrlSafe(STATE_BYTES)

        credentialsManager.saveSiteUrl(site)
        credentialsManager.saveOAuthRequest(clientId, state, verifier)

        return Uri.parse(site)
            .buildUpon()
            .appendEncodedPath(AUTHORIZE_PATH)
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", SCOPE)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge", codeChallenge(verifier))
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
            .toString()
    }

    /**
     * Asks the site for its own client id, which helpdesk_push publishes. Returns
     * null for a site running without it, leaving the user to supply the id.
     */
    suspend fun discoverClientId(siteUrl: String): String? =
        try {
            service(siteUrl.trimEnd('/')).getClientId().message.clientId?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.d(TAG, "Client id discovery failed", e)
            null
        }

    suspend fun exchangeCode(siteUrl: String, clientId: String, code: String, codeVerifier: String): OAuthTokenDto =
        service(siteUrl).getToken(
            grantType = "authorization_code",
            clientId = clientId,
            code = code,
            codeVerifier = codeVerifier,
            redirectUri = REDIRECT_URI
        )

    /**
     * Refreshes the stored session and returns the new Authorization value. Runs on
     * OkHttp's own thread from the authenticator, so it blocks rather than suspends.
     */
    fun refreshSession(): String? {
        val siteUrl = credentialsManager.getSiteUrl() ?: return null
        val clientId = credentialsManager.getOAuthClientId() ?: return null
        val refreshToken = credentialsManager.getRefreshToken() ?: return null

        return try {
            val token = runBlocking {
                service(siteUrl).getToken(
                    grantType = "refresh_token",
                    clientId = clientId,
                    refreshToken = refreshToken
                )
            }
            val accessToken = token.accessToken ?: return null
            credentialsManager.saveOAuthSession(
                accessToken = accessToken,
                refreshToken = token.refreshToken ?: refreshToken,
                expiresInSeconds = token.expiresIn
            )
            "Bearer $accessToken"
        } catch (e: HttpException) {
            // The refresh token itself was rejected, so the session is unrecoverable.
            // A transport failure is not, and must leave it in place to retry.
            if (e.code() in REFRESH_REJECTED) credentialsManager.clearOAuthSession()
            null
        } catch (e: Exception) {
            Log.d(TAG, "Token refresh failed", e)
            null
        }
    }

    suspend fun revoke(siteUrl: String, accessToken: String) {
        service(siteUrl).revokeToken(accessToken)
    }

    @Synchronized
    private fun service(siteUrl: String): OAuthService {
        val baseUrl = if (siteUrl.endsWith("/")) siteUrl else "$siteUrl/"
        cachedService?.takeIf { cachedBaseUrl == baseUrl }?.let { return it }

        val service = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OAuthService::class.java)

        cachedBaseUrl = baseUrl
        cachedService = service
        return service
    }

    private fun randomUrlSafe(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        SecureRandom().nextBytes(bytes)
        return bytes.urlSafeBase64()
    }

    private fun codeChallenge(verifier: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(Charsets.US_ASCII))
            .urlSafeBase64()

    private fun ByteArray.urlSafeBase64(): String =
        Base64.encodeToString(this, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    companion object {
        const val REDIRECT_URI = "helpdesk://oauth/callback"
        const val REDIRECT_HOST = "oauth"

        private const val TAG = "OAuthClient"
        private const val AUTHORIZE_PATH = "api/method/frappe.integrations.oauth2.authorize"
        private const val SCOPE = "all"
        private const val VERIFIER_BYTES = 48
        private const val STATE_BYTES = 16
        private val REFRESH_REJECTED = setOf(400, 401)
    }
}
