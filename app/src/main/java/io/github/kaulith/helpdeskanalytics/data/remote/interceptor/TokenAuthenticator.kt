package io.github.kaulith.helpdeskanalytics.data.remote.interceptor

import io.github.kaulith.helpdeskanalytics.data.local.credentials.CredentialsManager
import io.github.kaulith.helpdeskanalytics.data.remote.api.OAuthClient
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Refreshes an expired OAuth access token when a request comes back 401 and replays
 * it. API keys never expire, so a request that carried one is left to fail.
 */
class TokenAuthenticator(
    private val credentialsManager: CredentialsManager,
    private val oAuthClient: OAuthClient
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val tokenHeader = TOKEN_HEADERS.firstOrNull {
            response.request.header(it)?.startsWith(BEARER_PREFIX) == true
        } ?: return null
        val failedToken = response.request.header(tokenHeader) ?: return null
        if (priorResponseCount(response) > 1) return null

        val token = synchronized(this) {
            // A parallel request may have refreshed already; reuse that rather than
            // spending the refresh token a second time.
            val current = credentialsManager.getAuthToken()
            if (current != null && current != failedToken) current else oAuthClient.refreshSession()
        } ?: return null

        return response.request.newBuilder()
            .header(tokenHeader, token)
            .build()
    }

    private fun priorResponseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    companion object {
        const val AUTHORIZATION_HEADER = "Authorization"

        // The push bench can't authenticate the data site's token, so it travels
        // under its own header for helpdesk_push to verify against that site.
        const val REMOTE_TOKEN_HEADER = "X-Remote-Token"

        private const val BEARER_PREFIX = "Bearer "
        private val TOKEN_HEADERS = listOf(AUTHORIZATION_HEADER, REMOTE_TOKEN_HEADER)
    }
}
