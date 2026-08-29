package com.example.helpdeskanalytics.data.remote.interceptor

import com.example.helpdeskanalytics.data.local.credentials.CredentialsManager
import com.example.helpdeskanalytics.data.remote.api.AgentSessionManager
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val agentSessionManager: AgentSessionManager,
    private val credentialsManager: CredentialsManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // Requests that already carry their own Authorization (key minting) pass through.
        if (original.header("Authorization") != null) {
            return chain.proceed(original)
        }

        // Ticket bodies carry customer-supplied <img> tags, so an image request can point
        // at any host; the API token goes only to the site the credentials belong to.
        if (!original.url.host.equals(siteHost(), ignoreCase = true)) {
            return chain.proceed(original)
        }

        val isWrite = original.method !in READ_METHODS
        val token = agentSessionManager.tokenForRequest(isWrite)
            ?: return chain.proceed(original)

        val request = original.newBuilder()
            .header("Authorization", token)
            .header("Accept", "application/json")
            .build()

        return chain.proceed(request)
    }

    private fun siteHost(): String? {
        val siteUrl = credentialsManager.getSiteUrl() ?: return null
        val absolute = if (siteUrl.startsWith("http", ignoreCase = true)) siteUrl else "https://$siteUrl"
        return absolute.toHttpUrlOrNull()?.host
    }

    companion object {
        private val READ_METHODS = setOf("GET", "HEAD")
    }
}
