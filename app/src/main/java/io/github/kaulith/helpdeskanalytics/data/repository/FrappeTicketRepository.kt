package io.github.kaulith.helpdeskanalytics.data.repository

import io.github.kaulith.helpdeskanalytics.data.local.database.dao.CommentDao
import io.github.kaulith.helpdeskanalytics.data.local.database.dao.TicketDao
import io.github.kaulith.helpdeskanalytics.data.local.database.dao.UserDao
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
import io.github.kaulith.helpdeskanalytics.domain.model.Communication
import io.github.kaulith.helpdeskanalytics.domain.model.LeaderboardPeriod
import io.github.kaulith.helpdeskanalytics.domain.model.Priority
import io.github.kaulith.helpdeskanalytics.domain.model.Status
import io.github.kaulith.helpdeskanalytics.domain.model.Ticket
import io.github.kaulith.helpdeskanalytics.domain.model.TicketMetrics
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

    override fun canWrite(): Boolean = agentSessionManager.canWrite()

    private fun readOnlyError() =
        Result.Error(IllegalStateException("Read-only: select an agent with API access to make changes"))

    override fun getTickets(): Flow<Result<List<Ticket>>> = flow {
        emit(Result.Loading)

        // Emit cached Room data first
        val cached = ticketDao.getAllTickets().first()
        if (cached.isNotEmpty()) {
            emit(Result.Success(cached.map { it.toDomain() }))
        }

        // Check TTL: if fresh enough, stop
        val lastSync = preferencesManager.lastSync.first()
        val now = System.currentTimeMillis()
        if (cached.isNotEmpty() && (now - lastSync) < Constants.CACHE_TTL_TICKETS) {
            return@flow
        }

        // Fetch from API
        try {
            val service = apiServiceProvider.getService()
            val response = service.getTickets()
            val tickets = response.data.map { dto -> dto.toDomain() }
            ticketDao.replaceAll(tickets.map { it.toEntity() })
            preferencesManager.setLastSync(System.currentTimeMillis())
            emit(Result.Success(tickets))
        } catch (e: Exception) {
            if (cached.isNotEmpty()) {
                // Already emitted cached data above
            } else {
                emit(Result.Error(mapException(e)))
            }
        }
    }.flowOn(Dispatchers.Default)

    override fun getTickets(
        status: Status?,
        priority: Priority?,
        assignedTo: String?
    ): Flow<Result<List<Ticket>>> = flow {
        emit(Result.Loading)

        // Emit cached Room data first (filtered)
        val cached = ticketDao.getAllTickets().first()
        val cachedFiltered = cached.map { it.toDomain() }.filter { ticket ->
            (status == null || ticket.status == status) &&
                    (priority == null || ticket.priority == priority) &&
                    (assignedTo == null || ticket.isAssignedTo(assignedTo))
        }
        if (cached.isNotEmpty()) {
            emit(Result.Success(cachedFiltered))
        }

        // Check TTL
        val lastSync = preferencesManager.lastSync.first()
        val now = System.currentTimeMillis()
        if (cached.isNotEmpty() && (now - lastSync) < Constants.CACHE_TTL_TICKETS) {
            return@flow
        }

        // Fetch tickets from API with server-side filter when agent is specified
        try {
            val service = apiServiceProvider.getService()
            val apiFilters = buildApiFilters(assignedTo = assignedTo, status = status, priority = priority)
            val response = service.getTickets(filters = apiFilters)
            val tickets = response.data.map { dto -> dto.toDomain() }
            ticketDao.replaceAll(tickets.map { it.toEntity() })
            preferencesManager.setLastSync(System.currentTimeMillis())
            emit(Result.Success(tickets))
        } catch (e: Exception) {
            if (cached.isNotEmpty()) {
                // Already emitted cached data above
            } else {
                emit(Result.Error(mapException(e)))
            }
        }
    }.flowOn(Dispatchers.Default)

    override suspend fun searchTickets(query: String): Result<List<Ticket>> =
        withContext(Dispatchers.Default) {
            val cached = ticketDao.getAllTickets().first()
            val results = cached.map { it.toDomain() }.filter { ticket ->
                ticket.subject.contains(query, ignoreCase = true) ||
                        ticket.id.contains(query, ignoreCase = true)
            }
            Result.Success(results)
        }

    override fun getDashboardMetrics(): Flow<Result<TicketMetrics>> = flow {
        emit(Result.Loading)

        // Emit from cached data first
        val cached = ticketDao.getAllTickets().first()
        if (cached.isNotEmpty()) {
            val metrics = MetricsCalculator.computeMetrics(cached.map { it.toDomain() })
            emit(Result.Success(metrics))
        }

        // Check TTL
        val lastSync = preferencesManager.lastSync.first()
        val now = System.currentTimeMillis()
        if (cached.isNotEmpty() && (now - lastSync) < Constants.CACHE_TTL_TICKETS) {
            return@flow
        }

        // Fetch from API
        try {
            val service = apiServiceProvider.getService()
            val response = service.getTickets()
            val tickets = response.data.map { dto -> dto.toDomain() }
            ticketDao.replaceAll(tickets.map { it.toEntity() })
            preferencesManager.setLastSync(System.currentTimeMillis())
            val metrics = MetricsCalculator.computeMetrics(tickets)
            emit(Result.Success(metrics))
        } catch (e: Exception) {
            if (cached.isEmpty()) {
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
        val lastSync = preferencesManager.lastSync.first()
        val now = System.currentTimeMillis()
        if (cached != null && (now - lastSync) < Constants.CACHE_TTL_USER) {
            return@flow
        }

        try {
            val service = apiServiceProvider.getService()
            val email = service.getLoggedUser().message
            val userDto = service.getUser(email).data
            val user = userDto.toDomain()
            userDao.upsertUser(user.toEntity())
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
            val ticket = dto.toDomain()
            ticketDao.upsertTickets(listOf(ticket.toEntity()))
            Result.Success(ticket)
        } catch (e: Exception) {
            cachedTicket?.let { Result.Success(it.toDomain()) } ?: Result.Error(mapException(e))
        }
    }

    override suspend fun refresh(): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val service = apiServiceProvider.getService()
            val response = service.getTickets()
            val tickets = response.data.map { dto -> dto.toDomain() }
            ticketDao.replaceAll(tickets.map { it.toEntity() })
            preferencesManager.setLastSync(System.currentTimeMillis())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(mapException(e))
        }
    }

    override suspend fun clearCache(): Result<Unit> {
        ticketDao.deleteAllTickets()
        userDao.deleteAllUsers()
        commentDao.deleteAllComments()
        preferencesManager.setLastSync(0L)
        return Result.Success(Unit)
    }

    override suspend fun updateTicketStatus(ticketId: String, status: Status): Result<Ticket> {
        if (!agentSessionManager.canWrite()) return readOnlyError()
        // Optimistic update: update Room immediately
        val existing = ticketDao.getTicketById(ticketId)
        if (existing != null) {
            ticketDao.upsertTickets(listOf(existing.copy(status = status)))
        }

        return try {
            val service = apiServiceProvider.getService()
            val dto = service.updateTicket(ticketId, UpdateTicketRequest(status = status.value)).data
            val ticket = dto.toDomain()
            ticketDao.upsertTickets(listOf(ticket.toEntity()))
            Result.Success(ticket)
        } catch (e: Exception) {
            // Rollback
            if (existing != null) {
                ticketDao.upsertTickets(listOf(existing))
            }
            Result.Error(mapException(e))
        }
    }

    override suspend fun updateTicketPriority(ticketId: String, priority: Priority): Result<Ticket> {
        if (!agentSessionManager.canWrite()) return readOnlyError()
        val existing = ticketDao.getTicketById(ticketId)
        if (existing != null) {
            ticketDao.upsertTickets(listOf(existing.copy(priority = priority)))
        }

        return try {
            val service = apiServiceProvider.getService()
            val dto = service.updateTicket(ticketId, UpdateTicketRequest(priority = priority.value)).data
            val ticket = dto.toDomain()
            ticketDao.upsertTickets(listOf(ticket.toEntity()))
            Result.Success(ticket)
        } catch (e: Exception) {
            if (existing != null) {
                ticketDao.upsertTickets(listOf(existing))
            }
            Result.Error(mapException(e))
        }
    }

    override suspend fun updateTicketAssignment(ticketId: String, agentEmail: String): Result<Ticket> {
        if (!agentSessionManager.canWrite()) return readOnlyError()
        val existing = ticketDao.getTicketById(ticketId)
        if (existing != null) {
            ticketDao.upsertTickets(listOf(existing.copy(assignedTo = agentEmail)))
        }

        return try {
            val service = apiServiceProvider.getService()
            val dto = service.updateTicket(ticketId, UpdateTicketRequest(agent = agentEmail)).data
            val ticket = dto.toDomain()
            ticketDao.upsertTickets(listOf(ticket.toEntity()))
            Result.Success(ticket)
        } catch (e: Exception) {
            if (existing != null) {
                ticketDao.upsertTickets(listOf(existing))
            }
            Result.Error(mapException(e))
        }
    }

    override fun getComments(ticketId: String): Flow<Result<List<Comment>>> = flow {
        emit(Result.Loading)

        // Emit cached comments first
        val cached = commentDao.getCommentsForTicket(ticketId).first()
        if (cached.isNotEmpty()) {
            emit(Result.Success(cached.map { it.toDomain() }))
        }

        // Fetch from API
        try {
            val service = apiServiceProvider.getService()
            val activities = service.getTicketActivities(ticketId).message
            val baseUrl = apiServiceProvider.siteBaseUrl().orEmpty()
            val comments = activities.comments.orEmpty().map { it.toDomain(baseUrl) }
            commentDao.deleteCommentsForTicket(ticketId)
            commentDao.upsertComments(comments.map { it.toEntity(ticketId) })
            emit(Result.Success(comments))
        } catch (e: Exception) {
            if (cached.isEmpty()) {
                emit(Result.Error(mapException(e)))
            }
        }
    }

    override suspend fun addComment(ticketId: String, content: String): Result<Unit> {
        if (!agentSessionManager.canWrite()) return readOnlyError()
        return try {
            val service = apiServiceProvider.getService()
            service.runDocMethod(
                RunDocMethodRequest(
                    dn = ticketId,
                    method = "new_comment",
                    args = mapOf("content" to content)
                )
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(mapException(e))
        }
    }

    override suspend fun getCommunications(ticketId: String): Result<List<Communication>> {
        return try {
            val service = apiServiceProvider.getService()
            val activities = service.getTicketActivities(ticketId).message
            val baseUrl = apiServiceProvider.siteBaseUrl().orEmpty()
            Result.Success(activities.communications.orEmpty().map { it.toDomain(baseUrl) })
        } catch (e: Exception) {
            Result.Error(mapException(e))
        }
    }

    override suspend fun sendReply(ticketId: String, message: String): Result<Unit> {
        if (!agentSessionManager.canWrite()) return readOnlyError()
        return try {
            val service = apiServiceProvider.getService()
            service.runDocMethod(
                RunDocMethodRequest(
                    dn = ticketId,
                    method = "reply_via_agent",
                    args = mapOf("message" to message)
                )
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(mapException(e))
        }
    }

    private fun buildApiFilters(
        assignedTo: String? = null,
        status: Status? = null,
        priority: Priority? = null
    ): String? {
        val filters = mutableListOf<String>()
        assignedTo?.let { filters.add("""["_assign","like","%$it%"]""") }
        status?.let { filters.add("""["status","=","${it.value}"]""") }
        priority?.let { filters.add("""["priority","=","${it.value}"]""") }
        return if (filters.isEmpty()) null else "[${filters.joinToString(",")}]"
    }

    private fun mapException(e: Exception): Exception = e.toNetworkError()
}
