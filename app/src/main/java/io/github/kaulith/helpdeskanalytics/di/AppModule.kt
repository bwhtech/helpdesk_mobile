package io.github.kaulith.helpdeskanalytics.di

import io.github.kaulith.helpdeskanalytics.BuildConfig
import io.github.kaulith.helpdeskanalytics.data.local.preferences.PreferencesManager
import io.github.kaulith.helpdeskanalytics.data.update.UpdateChecker
import io.github.kaulith.helpdeskanalytics.data.sync.SyncManager
import io.github.kaulith.helpdeskanalytics.notifications.DeviceTokenManager
import io.github.kaulith.helpdeskanalytics.notifications.NotificationHelper
import io.github.kaulith.helpdeskanalytics.ui.screens.auth.OAuthRedirectHolder
import io.github.kaulith.helpdeskanalytics.util.NetworkMonitor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { PreferencesManager(androidContext()) }
    single { NetworkMonitor(androidContext()) }
    single { SyncManager(androidContext()) }
    single { DeviceTokenManager(androidContext(), get(), get()) }
    single { NotificationHelper(androidContext()) }
    single { UpdateChecker(BuildConfig.VERSION_NAME) }
    single { OAuthRedirectHolder() }
}
