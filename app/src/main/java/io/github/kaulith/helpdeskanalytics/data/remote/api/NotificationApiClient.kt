package io.github.kaulith.helpdeskanalytics.data.remote.api

import io.github.kaulith.helpdeskanalytics.data.local.credentials.CredentialsManager
import io.github.kaulith.helpdeskanalytics.data.remote.interceptor.TokenAuthenticator
import io.github.kaulith.helpdeskanalytics.util.Constants
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Talks to the helpdesk_push app on the Helpdesk site the app is signed in to,
 * where the login key authenticates normally. The one site that is polled from
 * the push bench instead registers there: its key can't authenticate on that
 * bench, so it travels as X-Remote-Token and helpdesk_push echoes it back to the
 * site's get_logged_user to identify the caller. No other site's key leaves it.
 */
class NotificationApiClient(
    private val credentialsManager: CredentialsManager,
    private val oAuthClient: OAuthClient,
    private val httpClient: OkHttpClient
) {
    private var cachedBaseUrl: String? = null
    private var cachedService: NotificationApiService? = null

    val service: NotificationApiService
        @Synchronized get() {
            val baseUrl = pushSiteUrl()
            cachedService?.takeIf { cachedBaseUrl == baseUrl }?.let { return it }
            return buildService(baseUrl).also {
                cachedBaseUrl = baseUrl
                cachedService = it
            }
        }

    private fun buildService(baseUrl: String): NotificationApiService {
        val client = httpClient.newBuilder()
            .addInterceptor { chain ->
                val builder = chain.request().newBuilder().header("Accept", "application/json")
                credentialsManager.getAuthToken()?.let { token ->
                    val header = if (chain.request().url.host.equals(siteHost(), ignoreCase = true)) {
                        TokenAuthenticator.AUTHORIZATION_HEADER
                    } else {
                        TokenAuthenticator.REMOTE_TOKEN_HEADER
                    }
                    builder.header(header, token)
                }
                chain.proceed(builder.build())
            }
            .authenticator(TokenAuthenticator(credentialsManager, oAuthClient))
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NotificationApiService::class.java)
    }

    private fun pushSiteUrl(): String =
        credentialsManager.siteBaseUrl()
            ?.takeUnless { siteHost().equals(Constants.POLLED_SITE_HOST, ignoreCase = true) }
            ?: Constants.PUSH_BACKEND_URL

    private fun siteHost(): String? = credentialsManager.siteBaseUrl()?.toHttpUrlOrNull()?.host
}
