package com.example.helpdeskanalytics.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BootReceiver : BroadcastReceiver(), KoinComponent {

    private val deviceTokenManager: DeviceTokenManager by inject()

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                deviceTokenManager.scheduleRegistrationRefresh()
                deviceTokenManager.registerNow()
            }
        }
    }
}
