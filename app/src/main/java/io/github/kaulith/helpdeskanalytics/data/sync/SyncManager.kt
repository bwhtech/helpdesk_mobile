package io.github.kaulith.helpdeskanalytics.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.github.kaulith.helpdeskanalytics.util.Constants
import java.util.concurrent.TimeUnit

class SyncManager(private val context: Context) {

    fun schedulePeriodic() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            Constants.SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    fun cancelSync() {
        WorkManager.getInstance(context).cancelUniqueWork(SyncWorker.WORK_NAME)
    }
}
