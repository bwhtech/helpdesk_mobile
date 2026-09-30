package io.github.kaulith.helpdeskanalytics.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.kaulith.helpdeskanalytics.domain.repository.TicketRepository
import io.github.kaulith.helpdeskanalytics.util.Result as RepositoryResult
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val repository: TicketRepository by inject()

    // A retry re-fetches every ticket on a 30s backoff, which spends enough background CPU
    // to trip the OS resource limit and get the app killed. The next interval is the retry.
    override suspend fun doWork(): Result {
        return when (repository.refresh()) {
            is RepositoryResult.Success -> Result.success()
            is RepositoryResult.Error -> Result.failure()
            is RepositoryResult.Loading -> Result.success()
        }
    }

    companion object {
        const val WORK_NAME = "helpdesk_sync"
    }
}
