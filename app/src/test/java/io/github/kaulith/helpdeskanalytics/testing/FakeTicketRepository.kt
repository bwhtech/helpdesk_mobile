package io.github.kaulith.helpdeskanalytics.testing

import io.github.kaulith.helpdeskanalytics.domain.model.AgentPerformance
import io.github.kaulith.helpdeskanalytics.domain.model.Comment
import io.github.kaulith.helpdeskanalytics.domain.model.LeaderboardPeriod
import io.github.kaulith.helpdeskanalytics.domain.model.Priority
import io.github.kaulith.helpdeskanalytics.domain.model.Status
import io.github.kaulith.helpdeskanalytics.domain.model.Ticket
import io.github.kaulith.helpdeskanalytics.domain.model.TicketConversation
import io.github.kaulith.helpdeskanalytics.domain.model.User
import io.github.kaulith.helpdeskanalytics.domain.repository.TicketRepository
import io.github.kaulith.helpdeskanalytics.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Writes return the changed ticket without emitting it on [tickets], so a test sees
 * only what the ViewModel applies itself.
 */
class FakeTicketRepository : TicketRepository {

    val tickets = MutableStateFlow<List<Ticket>>(emptyList())
    val failingTicketIds = mutableSetOf<String>()
    private val agentPerformancesByPeriod =
        mutableMapOf<LeaderboardPeriod, MutableSharedFlow<Result<List<AgentPerformance>>>>()

    fun agentPerformances(period: LeaderboardPeriod) =
        agentPerformancesByPeriod.getOrPut(period) { MutableSharedFlow(replay = 1) }

    override fun getTickets(status: Status?, priority: Priority?, assignedTo: String?): Flow<Result<List<Ticket>>> =
        tickets.map { Result.Success(it) }

    override fun getAgentPerformances(period: LeaderboardPeriod, force: Boolean) = agentPerformances(period)

    override fun getCurrentUser(): Flow<Result<User>> = flowOf(
        Result.Success(User(email = "ann@x.io", fullName = "Ann", roles = emptyList(), hasTeamLeadPermission = true))
    )

    override suspend fun getTicketById(id: String, force: Boolean): Result<Ticket> =
        tickets.value.find { it.id == id }?.let { Result.Success(it) } ?: Result.Error(NoSuchElementException(id))

    override suspend fun refresh(): Result<Unit> = Result.Success(Unit)

    override suspend fun clearCache(): Result<Unit> = Result.Success(Unit)

    override fun canWrite() = true

    override suspend fun updateTicketStatus(ticketId: String, status: Status) =
        update(ticketId) { it.copy(status = status) }

    override suspend fun updateTicketPriority(ticketId: String, priority: Priority) =
        update(ticketId) { it.copy(priority = priority) }

    override suspend fun getCachedComments(ticketId: String): List<Comment> = emptyList()

    override suspend fun getConversation(ticketId: String): Result<TicketConversation> =
        Result.Success(TicketConversation(emptyList(), emptyList()))

    override suspend fun addComment(ticketId: String, content: String): Result<Unit> = Result.Success(Unit)

    override suspend fun sendReply(ticketId: String, message: String): Result<Unit> = Result.Success(Unit)

    private fun update(ticketId: String, change: (Ticket) -> Ticket): Result<Ticket> {
        val ticket = tickets.value.find { it.id == ticketId }
        return if (ticket == null || ticketId in failingTicketIds) {
            Result.Error(IllegalStateException("Could not update $ticketId"))
        } else {
            Result.Success(change(ticket))
        }
    }
}
