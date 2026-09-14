package io.github.kaulith.helpdeskanalytics.domain.repository

import io.github.kaulith.helpdeskanalytics.domain.model.AgentPerformance
import io.github.kaulith.helpdeskanalytics.domain.model.Comment
import io.github.kaulith.helpdeskanalytics.domain.model.LeaderboardPeriod
import io.github.kaulith.helpdeskanalytics.domain.model.Priority
import io.github.kaulith.helpdeskanalytics.domain.model.Status
import io.github.kaulith.helpdeskanalytics.domain.model.Ticket
import io.github.kaulith.helpdeskanalytics.domain.model.TicketConversation
import io.github.kaulith.helpdeskanalytics.domain.model.User
import io.github.kaulith.helpdeskanalytics.util.Result
import kotlinx.coroutines.flow.Flow

interface TicketRepository {

    fun getTickets(
        status: Status? = null,
        priority: Priority? = null,
        assignedTo: String? = null
    ): Flow<Result<List<Ticket>>>

    fun getAgentPerformances(
        period: LeaderboardPeriod = LeaderboardPeriod.AllTime,
        force: Boolean = false
    ): Flow<Result<List<AgentPerformance>>>

    fun getCurrentUser(): Flow<Result<User>>

    suspend fun getTicketById(id: String, force: Boolean = false): Result<Ticket>

    suspend fun refresh(): Result<Unit>

    suspend fun clearCache(): Result<Unit>

    fun canWrite(): Boolean

    suspend fun updateTicketStatus(ticketId: String, status: Status): Result<Ticket>

    suspend fun updateTicketPriority(ticketId: String, priority: Priority): Result<Ticket>

    suspend fun getCachedComments(ticketId: String): List<Comment>

    /** Internal comments and the customer-facing email thread, from one request; comments are cached. */
    suspend fun getConversation(ticketId: String): Result<TicketConversation>

    /** Adds an internal note via the HD Ticket `new_comment` method. */
    suspend fun addComment(ticketId: String, content: String): Result<Unit>

    /** Emails a reply to the customer via the HD Ticket `reply_via_agent` method. */
    suspend fun sendReply(ticketId: String, message: String): Result<Unit>
}
