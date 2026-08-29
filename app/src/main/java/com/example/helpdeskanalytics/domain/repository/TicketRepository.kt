package com.example.helpdeskanalytics.domain.repository

import com.example.helpdeskanalytics.domain.model.AgentPerformance
import com.example.helpdeskanalytics.domain.model.Comment
import com.example.helpdeskanalytics.domain.model.Communication
import com.example.helpdeskanalytics.domain.model.LeaderboardPeriod
import com.example.helpdeskanalytics.domain.model.Priority
import com.example.helpdeskanalytics.domain.model.Status
import com.example.helpdeskanalytics.domain.model.Ticket
import com.example.helpdeskanalytics.domain.model.TicketMetrics
import com.example.helpdeskanalytics.domain.model.User
import com.example.helpdeskanalytics.util.Result
import kotlinx.coroutines.flow.Flow

interface TicketRepository {

    fun getTickets(): Flow<Result<List<Ticket>>>

    fun getTickets(
        status: Status? = null,
        priority: Priority? = null,
        assignedTo: String? = null
    ): Flow<Result<List<Ticket>>>

    suspend fun searchTickets(query: String): Result<List<Ticket>>

    fun getDashboardMetrics(): Flow<Result<TicketMetrics>>

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

    suspend fun updateTicketAssignment(ticketId: String, agentEmail: String): Result<Ticket>

    fun getComments(ticketId: String): Flow<Result<List<Comment>>>

    /** Adds an internal note via the HD Ticket `new_comment` method. */
    suspend fun addComment(ticketId: String, content: String): Result<Unit>

    /** The customer-facing email thread on the ticket. */
    suspend fun getCommunications(ticketId: String): Result<List<Communication>>

    /** Emails a reply to the customer via the HD Ticket `reply_via_agent` method. */
    suspend fun sendReply(ticketId: String, message: String): Result<Unit>
}
