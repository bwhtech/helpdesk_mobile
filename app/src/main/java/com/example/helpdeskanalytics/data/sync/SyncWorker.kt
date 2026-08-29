package com.example.helpdeskanalytics.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.helpdeskanalytics.domain.repository.TicketRepository
import com.example.helpdeskanalytics.util.Result
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val repository: TicketRepository by inject()

    override suspend fun doWork(): Result {
        return when (repository.refresh()) {
            is com.example.helpdeskanalytics.util.Result.Success -> Result.success()
            is com.example.helpdeskanalytics.util.Result.Error -> Result.retry()
            is com.example.helpdeskanalytics.util.Result.Loading -> Result.success()
        }
    }

    companion object {
        const val WORK_NAME = "helpdesk_sync"
    }
}
