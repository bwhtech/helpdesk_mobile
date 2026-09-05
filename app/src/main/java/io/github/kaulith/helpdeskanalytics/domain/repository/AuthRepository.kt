package io.github.kaulith.helpdeskanalytics.domain.repository

import io.github.kaulith.helpdeskanalytics.domain.model.User
import io.github.kaulith.helpdeskanalytics.util.Result

interface AuthRepository {
    suspend fun validateCredentials(siteUrl: String, apiKey: String, apiSecret: String): Result<User>

    /** The site's own OAuth client id, or null when the site does not publish one. */
    suspend fun discoverOAuthClientId(siteUrl: String): String?

    /** Returns the URL to open in the browser to authorize the app against a site. */
    fun beginOAuthLogin(siteUrl: String, clientId: String): String

    /** Finishes the sign-in the browser redirect came back from. */
    suspend fun completeOAuthLogin(code: String, state: String): Result<User>

    fun isLoggedIn(): Boolean
    suspend fun logout()
}
