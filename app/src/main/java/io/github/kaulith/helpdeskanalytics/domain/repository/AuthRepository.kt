package io.github.kaulith.helpdeskanalytics.domain.repository

import io.github.kaulith.helpdeskanalytics.domain.model.SiteProfile
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

    /** Signs out of the site in use. Another signed-in site, when there is one, takes over. */
    suspend fun logout()

    /** The signed-in sites other than the one in use. */
    fun otherSites(): List<SiteProfile>

    /** Puts another signed-in site in use. */
    suspend fun switchSite(profileId: String)
}
