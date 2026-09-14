package io.github.kaulith.helpdeskanalytics.data.repository

import io.github.kaulith.helpdeskanalytics.data.local.database.dao.CommentDao
import io.github.kaulith.helpdeskanalytics.data.local.database.dao.TicketDao
import io.github.kaulith.helpdeskanalytics.data.local.database.dao.UserDao
import io.github.kaulith.helpdeskanalytics.data.local.database.entities.TicketEntity
import io.github.kaulith.helpdeskanalytics.data.local.preferences.PreferencesManager
import io.github.kaulith.helpdeskanalytics.data.mapper.toDomain
import io.github.kaulith.helpdeskanalytics.data.mapper.toEntity
import io.github.kaulith.helpdeskanalytics.data.metrics.MetricsCalculator
import io.github.kaulith.helpdeskanalytics.data.remote.api.AgentSessionManager
import io.github.kaulith.helpdeskanalytics.data.remote.api.ApiServiceProvider
import io.github.kaulith.helpdeskanalytics.data.remote.api.FrappeApiService
import io.github.kaulith.helpdeskanalytics.data.remote.dto.RunDocMethodRequest
import io.github.kaulith.helpdeskanalytics.data.remote.dto.UpdateTicketRequest
import io.github.kaulith.helpdeskanalytics.data.remote.toNetworkError
import io.github.kaulith.helpdeskanalytics.domain.model.AgentPerformance
import io.github.kaulith.helpdeskanalytics.domain.model.Comment
import io.github.kaulith.helpdeskanalytics.domain.model.LeaderboardPeriod
import io.github.kaulith.helpdeskanalytics.domain.model.Priority
import io.github.kaulith.helpdeskanalytics.domain.model.Status
import io.github.kaulith.helpdeskanalytics.domain.model.Ticket
import io.github.kaulith.helpdeskanalytics.domain.model.TicketConversation
import io.github.kaulith.helpdeskanalytics.domain.model.User
import io.github.kaulith.helpdeskanalytics.domain.repository.TicketRepository
import io.github.kaulith.helpdeskanalytics.util.Constants
import io.github.kaulith.helpdeskanalytics.util.Result
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val gson = Gson()

private const val ALL_TIME_KEY = "all"

// Field names are pinned because R8 renames them and this JSON outlives the build.
private data class AgentCounts(
    @SerializedName("names") val names: Map<String, String>,
    @SerializedName("resolved") val resolved: Map<String, Int>
)

private data class TimestampedCounts(val counts: AgentCounts, val fetchedAt: Long)

private data class TimestampedTickets(val tickets: List<Ticket>, val fetchedAt: Long)

/** Bounds go in the key so a "today" ranking cached yesterday is never reused. */
private fun periodKey(bounds: ClosedRange<LocalDate>?): String =
    bounds?.let { "${it.start}:${it.endInclusive}" } ?: ALL_TIME_KEY

class FrappeTicketRepository(
    private val apiServiceProvider: ApiServiceProvider,
    private val ticketDao: TicketDao,
    private val userDao: UserDao,
    private val commentDao: CommentDao,
    private val preferencesManager: PreferencesManager,
    private val agentSessionManager: AgentSessionManager
) : TicketRepository {

    private val agentCountsMutex = Mutex()
    private val agentCountRequests = Semaphore(Constants.MAX_REQUESTS_PER_HOST)
    private val agentCountsByPeriod = mutableMapOf<String, TimestampedCounts>()

    // Kept out of Room, which holds the newest tickets across all agents; an agent's
    // own fetch reaches further back and must not replace that window.
    private val agentTicketsMutex = Mutex()
    private val agentTicketsByEmail = mutableMapOf<String, TimestampedTickets>()

    private val ticketFetchMutex = Mutex()

    override fun canWrite(): Boolean = agentSessionManager.canWrite()

    private fun readOnlyError() =
        Result.Error(IllegalStateException("Read-only: select an agent with API access to make changes"))

    override fun getTickets(
        status: Status?,
        priority: Priority?,
        assignedTo: String?
    ): Flow<Result<List<Ticket>>> = flow {
        emit(Result.Loading)

        // Emit cached data first: the agent's own fetch if there is one, else Room
        val agentCached = assignedTo?.let { agentTicketsMutex.withLock { agentTicketsByEmail[it] } }
        val cached = agentCached?.tickets ?: ticketDao.getAllTickets().first().map { it.toDomain() }
        if (cached.isNotEmpty()) {
            emit(Result.Success(cached.filter { it.matches(status, priority, assignedTo) }))
        }

        // Check TTL
        val syncedAt = if (assignedTo == null) preferencesManager.lastSync.first() else agentCached?.fetchedAt ?: 0L
        val now = System.currentTimeMillis()
        if (cached.isNotEmpty() && (now - syncedAt) < Constants.CACHE_TTL_TICKETS) {
            return@flow
        }

        // Fetch tickets from API with server-side filter when agent is specified
        try {
            val tickets = fetchTicketsUnlessSyncedSince(assignedTo, syncedAt)
            emit(Result.Success(tickets.filter { it.matches(status, priority, assignedTo) }))
        } catch (e: Exception) {
            if (cached.isNotEmpty()) {
                // Already emitted cached data above
            } else {
                emit(Result.Error(mapException(e)))
            }
        }
    }.flowOn(Dispatchers.Default)

    override fun getAgentPerformances(
        period: LeaderboardPeriod,
        force: Boolean
    ): Flow<Result<List<AgentPerformance>>> = flow {
        emit(Result.Loading)

        val currentUserEmail = userDao.getCurrentUser().first()?.email
        val bounds = period.bounds()
        val key = periodKey(bounds)

        // Averages come from the ticket cache, so they only cover its window.
        val windowTickets = ticketDao.getAllTickets().first()
            .map { it.toDomain() }
            .filter { bounds == null || it.finishedWithin(bounds) }

        val cached = cachedAgentCounts(key)
        if (cached != null) {
            emit(Result.Success(
                MetricsCalculator.rankAgents(
                    cached.counts.resolved, windowTickets, cached.counts.names, currentUserEmail
                )
            ))
        }

        val isFresh = cached != null &&
                System.currentTimeMillis() - cached.fetchedAt < Constants.CACHE_TTL_TICKETS
        if (!force && isFresh) {
            return@flow
        }

        try {
            val counts = refreshAgentCounts(key, bounds, force)
            emit(Result.Success(
                MetricsCalculator.rankAgents(counts.resolved, windowTickets, counts.names, currentUserEmail)
            ))
        } catch (e: Exception) {
            if (cached == null) {
                if (windowTickets.isNotEmpty()) {
                    emit(Result.Success(
                        MetricsCalculator.computeAgentPerformances(windowTickets, currentUserEmail)
                    ))
                } else {
                    emit(Result.Error(mapException(e)))
                }
            }
        }
    }.flowOn(Dispatchers.Default)

    private suspend fun cachedAgentCounts(key: String): TimestampedCounts? = agentCountsMutex.withLock {
        agentCountsByPeriod[key]
            ?: if (key == ALL_TIME_KEY) {
                storedAllTimeCounts()?.also { agentCountsByPeriod[key] = it }
            } else {
                null
            }
    }

    private suspend fun storedAllTimeCounts(): TimestampedCounts? =
        preferencesManager.agentCounts.first()?.let { json ->
            try {
                gson.fromJson(json, AgentCounts::class.java)
                    ?.takeIf { it.names.isNotEmpty() && it.resolved.isNotEmpty() }
                    ?.let { TimestampedCounts(it, preferencesManager.agentCountsSyncedAt.first()) }
            } catch (_: Exception) {
                null
            }
        }

    // Costs one get_count request per agent, so it is deduped and cached per period.
    private suspend fun refreshAgentCounts(
        key: String,
        bounds: ClosedRange<LocalDate>?,
        force: Boolean
    ): AgentCounts = agentCountsMutex.withLock {
        val cached = agentCountsByPeriod[key]
        if (!force && cached != null &&
            System.currentTimeMillis() - cached.fetchedAt < Constants.CACHE_TTL_TICKETS
        ) {
            return@withLock cached.counts
        }

        val service = apiServiceProvider.getService()
        val names = service.getAgents().data
            .mapNotNull { agent -> agent.user?.let { it to (agent.agentName ?: it) } }
            .toMap()
        val resolved = coroutineScope {
            names.keys.map { email ->
                async { email to agentTicketCount(service, email, bounds) }
            }.awaitAll()
        }.toMap()

        val fetchedAt = System.currentTimeMillis()
        AgentCounts(names, resolved).also {
            agentCountsByPeriod[key] = TimestampedCounts(it, fetchedAt)
            if (key == ALL_TIME_KEY) {
                preferencesManager.setAgentCounts(gson.toJson(it), fetchedAt)
            }
        }
    }

    private suspend fun agentTicketCount(
        service: FrappeApiService,
        agentEmail: String,
        bounds: ClosedRange<LocalDate>?
    ): Int = finishedFilters(agentEmail, bounds).sumOf { filters ->
        agentCountRequests.withPermit { service.getTicketCount(filters = filters).message }
    }

    /**
     * Two fifths of closed tickets never get a `resolution_date`, because Helpdesk skips it when
     * the SLA was paused, so `modified` is the only record of when those were closed. Counting
     * both and summing keeps the exact date wherever one exists.
     */
    private fun finishedFilters(agentEmail: String, bounds: ClosedRange<LocalDate>?): List<String> {
        val assigned = """["_assign","like","%$agentEmail%"]"""
        val resolvedOrClosed = """["status","in",["${Status.RESOLVED.value}","${Status.CLOSED.value}"]]"""
        if (bounds == null) {
            return listOf(jsonArray(assigned, resolvedOrClosed))
        }
        return listOf(
            jsonArray(assigned, resolvedOrClosed, between("resolution_date", bounds)),
            jsonArray(
                assigned,
                """["status","=","${Status.CLOSED.value}"]""",
                """["resolution_date","is","not set"]""",
                between("modified", bounds)
            )
        )
    }

    private fun between(field: String, bounds: ClosedRange<LocalDate>) =
        """["$field","between",["${bounds.start}","${bounds.endInclusive}"]]"""

    private fun jsonArray(vararg conditions: String) =
        conditions.joinToString(",", prefix = "[", postfix = "]")

    private fun Ticket.matches(status: Status?, priority: Priority?, assignedTo: String?): Boolean =
        (status == null || this.status == status) &&
                (priority == null || this.priority == priority) &&
                (assignedTo == null || isAssignedTo(assignedTo))

    private fun Ticket.finishedWithin(bounds: ClosedRange<LocalDate>): Boolean {
        val finished = resolvedAt ?: modifiedAt.takeIf { status == Status.CLOSED } ?: return false
        return finished.toLocalDateTime(TimeZone.currentSystemDefault()).date in bounds
    }

    override fun getCurrentUser(): Flow<Result<User>> = flow {
        emit(Result.Loading)

        // Emit cached user first
        val cached = userDao.getCurrentUser().first()
        if (cached != null) {
            emit(Result.Success(cached.toDomain()))
        }

        // Check TTL
        val userSyncedAt = preferencesManager.userSyncedAt.first()
        val now = System.currentTimeMillis()
        if (cached != null && (now - userSyncedAt) < Constants.CACHE_TTL_USER) {
            return@flow
        }

        try {
            val service = apiServiceProvider.getService()
            val email = service.getLoggedUser().message
            val userDto = service.getUser(email).data
            val user = userDto.toDomain()
            userDao.upsertUser(user.toEntity())
            preferencesManager.setUserSyncedAt(System.currentTimeMillis())
            emit(Result.Success(user))
        } catch (e: Exception) {
            if (cached == null) {
                emit(Result.Error(mapException(e)))
            }
        }
    }.flowOn(Dispatchers.Default)

    override suspend fun getTicketById(id: String, force: Boolean): Result<Ticket> {
        // List fetches omit description, so a cached row without one is incomplete.
        val cachedTicket = ticketDao.getTicketById(id)
        if (!force && cachedTicket?.description != null) {
            return Result.Success(cachedTicket.toDomain())
        }

        return try {
            val service = apiServiceProvider.getService()
            val dto = service.getTicket(id).data
            val ticket = dto.toDomain(apiServiceProvider.siteTimeZone())
            ticketDao.upsertTickets(listOf(ticket.toEntity()))
            Result.Success(ticket)
        } catch (e: Exception) {
            cachedTicket?.let { Result.Success(it.toDomain()) } ?: Result.Error(mapException(e))
        }
    }

    override suspend fun refresh(): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            fetchAllTickets()
            agentTicketsMutex.withLock { agentTicketsByEmail.clear() }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(mapException(e))
        }
    }

    // Screens opening together on a stale cache all get here; the first one fetches and
    // the rest, once it releases the lock, read what it stored instead of fetching again.
    private suspend fun fetchTicketsUnlessSyncedSince(assignedTo: String?, syncedAt: Long): List<Ticket> =
        ticketFetchMutex.withLock {
            if (assignedTo == null) {
                if (preferencesManager.lastSync.first() == syncedAt) {
                    fetchAllTickets()
                } else {
                    ticketDao.getAllTickets().first().map { it.toDomain() }
                }
            } else {
                val stored = agentTicketsMutex.withLock { agentTicketsByEmail[assignedTo] }
                if (stored == null || stored.fetchedAt == syncedAt) fetchAgentTickets(assignedTo) else stored.tickets
            }
        }

    private suspend fun fetchAllTickets(): List<Ticket> {
        val siteTimeZone = apiServiceProvider.siteTimeZone()
        val tickets = apiServiceProvider.getService().getTickets().data.map { dto -> dto.toDomain(siteTimeZone) }
        ticketDao.replaceAll(tickets.map { it.toEntity() })
        preferencesManager.setLastSync(System.currentTimeMillis())
        return tickets
    }

    private suspend fun fetchAgentTickets(agentEmail: String): List<Ticket> {
        val siteTimeZone = apiServiceProvider.siteTimeZone()
        val tickets = apiServiceProvider.getService()
            .getTickets(filters = """[["_assign","like","%$agentEmail%"]]""")
            .data.map { dto -> dto.toDomain(siteTimeZone) }
        agentTicketsMutex.withLock {
            agentTicketsByEmail[agentEmail] = TimestampedTickets(tickets, System.currentTimeMillis())
        }
        return tickets
    }

    private suspend fun replaceInAgentTickets(ticket: Ticket) = agentTicketsMutex.withLock {
        agentTicketsByEmail.replaceAll { _, entry ->
            entry.copy(tickets = entry.tickets.map { if (it.id == ticket.id) ticket else it })
        }
    }

    override suspend fun clearCache(): Result<Unit> {
        ticketDao.deleteAllTickets()
        userDao.deleteAllUsers()
        commentDao.deleteAllComments()
        agentTicketsMutex.withLock { agentTicketsByEmail.clear() }
        preferencesManager.setLastSync(0L)
        return Result.Success(Unit)
    }

    override suspend fun updateTicketStatus(ticketId: String, status: Status): Result<Ticket> =
        updateTicket(ticketId, UpdateTicketRequest(status = status.value)) { it.copy(status = status) }

    override suspend fun updateTicketPriority(ticketId: String, priority: Priority): Result<Ticket> =
        updateTicket(ticketId, UpdateTicketRequest(priority = priority.value)) { it.copy(priority = priority) }

    private suspend fun updateTicket(
        ticketId: String,
        request: UpdateTicketRequest,
        optimisticChange: (TicketEntity) -> TicketEntity
    ): Result<Ticket> {
        if (!agentSessionManager.canWrite()) return readOnlyError()
        val existing = ticketDao.getTicketById(ticketId)
        if (existing != null) {
            ticketDao.upsertTickets(listOf(optimisticChange(existing)))
        }

        return try {
            val dto = apiServiceProvider.getService().updateTicket(ticketId, request).data
            val ticket = dto.toDomain(apiServiceProvider.siteTimeZone())
            ticketDao.upsertTickets(listOf(ticket.toEntity()))
            replaceInAgentTickets(ticket)
            Result.Success(ticket)
        } catch (e: Exception) {
            if (existing != null) {
                ticketDao.upsertTickets(listOf(existing))
            }
            Result.Error(mapException(e))
        }
    }

    override suspend fun getCachedComments(ticketId: String): List<Comment> =
        commentDao.getCommentsForTicket(ticketId).first().map { it.toDomain() }

    override suspend fun getConversation(ticketId: String): Result<TicketConversation> {
        return try {
            val activities = apiServiceProvider.getService().getTicketActivities(ticketId).message
            val baseUrl = apiServiceProvider.siteBaseUrl().orEmpty()
            val siteTimeZone = apiServiceProvider.siteTimeZone()
            val comments = activities.comments.orEmpty().map { it.toDomain(baseUrl, siteTimeZone) }
            commentDao.deleteCommentsForTicket(ticketId)
            commentDao.upsertComments(comments.map { it.toEntity(ticketId) })
            val communications = activities.communications.orEmpty().map { it.toDomain(baseUrl, siteTimeZone) }
            Result.Success(TicketConversation(comments, communications))
        } catch (e: Exception) {
            Result.Error(mapException(e))
        }
    }

    override suspend fun addComment(ticketId: String, content: String): Result<Unit> =
        runTicketMethod(ticketId, method = "new_comment", args = mapOf("content" to content))

    override suspend fun sendReply(ticketId: String, message: String): Result<Unit> =
        runTicketMethod(ticketId, method = "reply_via_agent", args = mapOf("message" to message))

    private suspend fun runTicketMethod(ticketId: String, method: String, args: Map<String, String>): Result<Unit> {
        if (!agentSessionManager.canWrite()) return readOnlyError()
        return try {
            val request = RunDocMethodRequest(dn = ticketId, method = method, args = args)
            apiServiceProvider.getService().runDocMethod(request)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(mapException(e))
        }
    }

    private fun mapException(e: Exception): Exception = e.toNetworkError()
}
