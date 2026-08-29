package com.example.helpdeskanalytics.di

import com.example.helpdeskanalytics.data.local.credentials.CredentialsManager
import com.example.helpdeskanalytics.data.remote.api.AgentSessionManager
import com.example.helpdeskanalytics.data.remote.api.ApiServiceProvider
import com.example.helpdeskanalytics.data.remote.api.NotificationApiClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val networkModule = module {
    single { CredentialsManager(androidContext()) }
    single { AgentSessionManager(get()) }
    single { ApiServiceProvider(get(), get()) }
    single { NotificationApiClient(get()) }
}
