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

    override suspend fun doWork(): Result {
        return when (repository.refresh()) {
            is RepositoryResult.Success -> Result.success()
            is RepositoryResult.Error -> Result.retry()
            is RepositoryResult.Loading -> Result.success()
        }
    }

    companion object {
        const val WORK_NAME = "helpdesk_sync"
    }
}
