package io.github.kaulith.helpdeskanalytics.di

import io.github.kaulith.helpdeskanalytics.data.local.credentials.CredentialsManager
import io.github.kaulith.helpdeskanalytics.data.remote.api.AgentSessionManager
import io.github.kaulith.helpdeskanalytics.data.remote.api.ApiServiceProvider
import io.github.kaulith.helpdeskanalytics.data.remote.api.NotificationApiClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val networkModule = module {
    single { CredentialsManager(androidContext()) }
    single { AgentSessionManager(get()) }
    single { ApiServiceProvider(get(), get()) }
    single { NotificationApiClient(get()) }
}
