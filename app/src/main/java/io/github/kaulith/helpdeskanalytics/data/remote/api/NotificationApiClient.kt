package io.github.kaulith.helpdeskanalytics.data.remote.api

import io.github.kaulith.helpdeskanalytics.BuildConfig
import io.github.kaulith.helpdeskanalytics.data.local.credentials.CredentialsManager
import io.github.kaulith.helpdeskanalytics.data.remote.interceptor.TokenAuthenticator
import io.github.kaulith.helpdeskanalytics.util.Constants
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Talks to the helpdesk_push app, which usually lives on the same bench as the
 * Helpdesk site the app reads from. On that shared bench the login key
 * authenticates normally. When the data site is a different bench, the key can't
 * authenticate here, so it travels as X-Remote-Token and helpdesk_push echoes it
 * back to the data site's get_logged_user to identify the caller.
 */
class NotificationApiClient(
    private val credentialsManager: CredentialsManager,
    private val oAuthClient: OAuthClient
) {
    val service: NotificationApiService by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val builder = chain.request().newBuilder().header("Accept", "application/json")
                credentialsManager.getAuthToken()?.let { token ->
                    val header = if (isPushBackendSite()) {
                        TokenAuthenticator.AUTHORIZATION_HEADER
                    } else {
                        TokenAuthenticator.REMOTE_TOKEN_HEADER
                    }
                    builder.header(header, token)
                }
                chain.proceed(builder.build())
            }
            .addInterceptor(loggingInterceptor)
            .authenticator(TokenAuthenticator(credentialsManager, oAuthClient))
            .connectTimeout(Constants.NETWORK_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(Constants.NETWORK_TIMEOUT, TimeUnit.MILLISECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(Constants.PUSH_BACKEND_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NotificationApiService::class.java)
    }

    private fun isPushBackendSite(): Boolean {
        val siteUrl = credentialsManager.getSiteUrl() ?: return false
        val absolute = if (siteUrl.startsWith("http", ignoreCase = true)) siteUrl else "https://$siteUrl"
        val siteHost = absolute.toHttpUrlOrNull()?.host ?: return false
        return siteHost.equals(PUSH_BACKEND_HOST, ignoreCase = true)
    }

    private companion object {
        val PUSH_BACKEND_HOST = Constants.PUSH_BACKEND_URL.toHttpUrlOrNull()?.host
    }
}
