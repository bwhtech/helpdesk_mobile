package io.github.kaulith.helpdeskanalytics.di

import io.github.kaulith.helpdeskanalytics.data.local.credentials.CredentialsManager
import io.github.kaulith.helpdeskanalytics.data.remote.api.AgentSessionManager
import io.github.kaulith.helpdeskanalytics.data.remote.api.ApiServiceProvider
import io.github.kaulith.helpdeskanalytics.data.remote.api.FrappeAgentSessionManager
import io.github.kaulith.helpdeskanalytics.data.remote.api.FrappeApiServiceProvider
import io.github.kaulith.helpdeskanalytics.data.remote.api.NotificationApiClient
import io.github.kaulith.helpdeskanalytics.data.remote.api.OAuthClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

val networkModule = module {
    single { CredentialsManager(androidContext()) }
    single { FrappeAgentSessionManager(get(), get()) } bind AgentSessionManager::class
    single { OAuthClient(get()) }
    single { FrappeApiServiceProvider(get(), get(), get()) } bind ApiServiceProvider::class
    single { NotificationApiClient(get(), get()) }
}
