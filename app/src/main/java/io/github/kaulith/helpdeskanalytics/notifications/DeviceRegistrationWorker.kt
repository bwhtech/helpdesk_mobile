package io.github.kaulith.helpdeskanalytics.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DeviceRegistrationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val deviceTokenManager: DeviceTokenManager by inject()

    override suspend fun doWork(): Result =
        if (deviceTokenManager.registerCurrentDevice()) Result.success() else Result.retry()

    companion object {
        const val PERIODIC_WORK_NAME = "helpdesk_device_registration"
        const val ONE_SHOT_WORK_NAME = "helpdesk_device_registration_now"
    }
}
