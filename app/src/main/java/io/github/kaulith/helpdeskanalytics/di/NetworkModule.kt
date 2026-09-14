package io.github.kaulith.helpdeskanalytics.di

import io.github.kaulith.helpdeskanalytics.BuildConfig
import io.github.kaulith.helpdeskanalytics.data.local.credentials.CredentialsManager
import io.github.kaulith.helpdeskanalytics.data.remote.api.AgentSessionManager
import io.github.kaulith.helpdeskanalytics.data.remote.api.ApiServiceProvider
import io.github.kaulith.helpdeskanalytics.data.remote.api.FrappeAgentSessionManager
import io.github.kaulith.helpdeskanalytics.data.remote.api.FrappeApiServiceProvider
import io.github.kaulith.helpdeskanalytics.data.remote.api.NotificationApiClient
import io.github.kaulith.helpdeskanalytics.data.remote.api.OAuthClient
import io.github.kaulith.helpdeskanalytics.util.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val networkModule = module {
    single {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(Constants.NETWORK_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(Constants.NETWORK_TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(Constants.NETWORK_TIMEOUT, TimeUnit.MILLISECONDS)
            .build()
    }
    singleOf(::CredentialsManager)
    singleOf(::FrappeAgentSessionManager) { bind<AgentSessionManager>() }
    singleOf(::OAuthClient)
    singleOf(::FrappeApiServiceProvider) { bind<ApiServiceProvider>() }
    singleOf(::NotificationApiClient)
}
