package io.github.kaulith.helpdeskanalytics.data.remote.api

import io.github.kaulith.helpdeskanalytics.data.local.credentials.CredentialsManager
import io.github.kaulith.helpdeskanalytics.data.remote.interceptor.AuthInterceptor
import io.github.kaulith.helpdeskanalytics.data.remote.interceptor.TokenAuthenticator
import io.github.kaulith.helpdeskanalytics.util.Constants
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.datetime.TimeZone

class FrappeApiServiceProvider(
    private val credentialsManager: CredentialsManager,
    private val agentSessionManager: AgentSessionManager,
    private val oAuthClient: OAuthClient,
    private val httpClient: OkHttpClient
) : ApiServiceProvider {
    private var cachedBaseUrl: String? = null
    private var cachedService: FrappeApiService? = null

    @Volatile
    private var cachedTimeZone: TimeZone? = null

    @Synchronized
    override fun getService(): FrappeApiService {
        val baseUrl = credentialsManager.siteBaseUrl()
            ?: throw IllegalStateException("No site URL configured")

        if (cachedService != null && cachedBaseUrl == baseUrl) {
            return cachedService!!
        }

        val dispatcher = Dispatcher().apply {
            maxRequestsPerHost = Constants.MAX_REQUESTS_PER_HOST
        }

        val client = httpClient.newBuilder()
            .dispatcher(dispatcher)
            .addInterceptor(AuthInterceptor(agentSessionManager, credentialsManager))
            .authenticator(TokenAuthenticator(credentialsManager, oAuthClient))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        cachedBaseUrl = baseUrl
        cachedService = retrofit.create(FrappeApiService::class.java)
        cachedTimeZone = null
        return cachedService!!
    }

    override fun invalidate() {
        cachedService = null
        cachedBaseUrl = null
        cachedTimeZone = null
    }

    override suspend fun siteTimeZone(): TimeZone {
        cachedTimeZone?.let { return it }
        val service = getService()
        return TimeZone.of(service.getTimeZone().message.timeZone ?: DEFAULT_SITE_TIME_ZONE)
            .also { cachedTimeZone = it }
    }

    override fun siteBaseUrl(): String? = credentialsManager.siteBaseUrl()

    private companion object {
        // Frappe's own fallback in get_system_timezone when System Settings has none.
        const val DEFAULT_SITE_TIME_ZONE = "Asia/Kolkata"
    }
}
