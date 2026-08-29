package com.example.helpdeskanalytics.di

import com.example.helpdeskanalytics.BuildConfig
import com.example.helpdeskanalytics.data.local.preferences.PreferencesManager
import com.example.helpdeskanalytics.data.update.UpdateChecker
import com.example.helpdeskanalytics.data.sync.SyncManager
import com.example.helpdeskanalytics.notifications.DeviceTokenManager
import com.example.helpdeskanalytics.notifications.NotificationHelper
import com.example.helpdeskanalytics.util.NetworkMonitor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { PreferencesManager(androidContext()) }
    single { NetworkMonitor(androidContext()) }
    single { SyncManager(androidContext()) }
    single { DeviceTokenManager(androidContext(), get(), get()) }
    single { NotificationHelper(androidContext()) }
    single { UpdateChecker(BuildConfig.VERSION_NAME) }
}
