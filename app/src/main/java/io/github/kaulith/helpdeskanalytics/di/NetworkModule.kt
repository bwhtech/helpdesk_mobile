package io.github.kaulith.helpdeskanalytics.di

import io.github.kaulith.helpdeskanalytics.data.local.credentials.CredentialsManager
import io.github.kaulith.helpdeskanalytics.data.remote.api.AgentSessionManager
import io.github.kaulith.helpdeskanalytics.data.remote.api.ApiServiceProvider
import io.github.kaulith.helpdeskanalytics.data.remote.api.FrappeAgentSessionManager
import io.github.kaulith.helpdeskanalytics.data.remote.api.FrappeApiServiceProvider
import io.github.kaulith.helpdeskanalytics.data.remote.api.NotificationApiClient
import io.github.kaulith.helpdeskanalytics.data.remote.api.OAuthClient
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val networkModule = module {
    singleOf(::CredentialsManager)
    singleOf(::FrappeAgentSessionManager) { bind<AgentSessionManager>() }
    singleOf(::OAuthClient)
    singleOf(::FrappeApiServiceProvider) { bind<ApiServiceProvider>() }
    singleOf(::NotificationApiClient)
}
