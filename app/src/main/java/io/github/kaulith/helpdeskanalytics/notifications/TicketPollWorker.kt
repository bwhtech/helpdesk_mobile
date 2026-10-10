package io.github.kaulith.helpdeskanalytics.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TicketPollWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val ticketPoller: TicketPoller by inject()

    // A failed poll is left to the next period; the state it compares against is untouched.
    override suspend fun doWork(): Result {
        ticketPoller.poll()
        return Result.success()
    }

    companion object {
        const val PERIODIC_WORK_NAME = "helpdesk_ticket_poll"
    }
}
