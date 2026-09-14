package io.github.kaulith.helpdeskanalytics.di

import io.github.kaulith.helpdeskanalytics.BuildConfig
import io.github.kaulith.helpdeskanalytics.data.local.preferences.PreferencesManager
import io.github.kaulith.helpdeskanalytics.data.update.UpdateChecker
import io.github.kaulith.helpdeskanalytics.data.sync.SyncManager
import io.github.kaulith.helpdeskanalytics.notifications.DeviceTokenManager
import io.github.kaulith.helpdeskanalytics.notifications.NotificationHelper
import io.github.kaulith.helpdeskanalytics.ui.screens.auth.OAuthRedirectHolder
import io.github.kaulith.helpdeskanalytics.util.NetworkMonitor
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    singleOf(::PreferencesManager)
    singleOf(::NetworkMonitor)
    singleOf(::SyncManager)
    singleOf(::DeviceTokenManager)
    singleOf(::NotificationHelper)
    single { UpdateChecker(BuildConfig.VERSION_NAME, get()) }
    singleOf(::OAuthRedirectHolder)
}
