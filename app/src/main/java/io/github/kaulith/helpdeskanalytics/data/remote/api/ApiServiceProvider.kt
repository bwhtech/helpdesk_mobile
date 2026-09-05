package io.github.kaulith.helpdeskanalytics.data.remote.api

import io.github.kaulith.helpdeskanalytics.BuildConfig
import io.github.kaulith.helpdeskanalytics.data.local.credentials.CredentialsManager
import io.github.kaulith.helpdeskanalytics.data.remote.interceptor.AuthInterceptor
import io.github.kaulith.helpdeskanalytics.data.remote.interceptor.TokenAuthenticator
import io.github.kaulith.helpdeskanalytics.util.Constants
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiServiceProvider(
    private val credentialsManager: CredentialsManager,
    private val agentSessionManager: AgentSessionManager,
    private val oAuthClient: OAuthClient
) {
    private var cachedBaseUrl: String? = null
    private var cachedService: FrappeApiService? = null

    @Synchronized
    fun getService(): FrappeApiService {
        val currentUrl = credentialsManager.getSiteUrl()
            ?: throw IllegalStateException("No site URL configured")

        if (cachedService != null && cachedBaseUrl == currentUrl) {
            return cachedService!!
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val dispatcher = Dispatcher().apply {
            maxRequestsPerHost = Constants.MAX_REQUESTS_PER_HOST
        }

        val client = OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .addInterceptor(AuthInterceptor(agentSessionManager, credentialsManager))
            .addInterceptor(loggingInterceptor)
            .authenticator(TokenAuthenticator(credentialsManager, oAuthClient))
            .connectTimeout(Constants.NETWORK_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(Constants.NETWORK_TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(Constants.NETWORK_TIMEOUT, TimeUnit.MILLISECONDS)
            .build()

        val baseUrl = if (currentUrl.endsWith("/")) currentUrl else "$currentUrl/"

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        cachedBaseUrl = currentUrl
        cachedService = retrofit.create(FrappeApiService::class.java)
        return cachedService!!
    }

    fun invalidate() {
        cachedService = null
        cachedBaseUrl = null
    }

    /** Site URL with a trailing slash, for resolving relative file URLs. */
    fun siteBaseUrl(): String? {
        val url = credentialsManager.getSiteUrl() ?: return null
        return if (url.endsWith("/")) url else "$url/"
    }
}
