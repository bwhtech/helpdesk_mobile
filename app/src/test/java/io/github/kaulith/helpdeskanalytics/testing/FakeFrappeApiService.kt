package io.github.kaulith.helpdeskanalytics.testing

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.github.kaulith.helpdeskanalytics.data.remote.api.FrappeApiService
import io.github.kaulith.helpdeskanalytics.data.remote.dto.AgentDto
import io.github.kaulith.helpdeskanalytics.data.remote.dto.FrappeListResponse
import io.github.kaulith.helpdeskanalytics.data.remote.dto.FrappeMethodResponse
import io.github.kaulith.helpdeskanalytics.data.remote.dto.FrappeSingleResponse
import io.github.kaulith.helpdeskanalytics.data.remote.dto.GenerateKeysResponse
import io.github.kaulith.helpdeskanalytics.data.remote.dto.RunDocMethodRequest
import io.github.kaulith.helpdeskanalytics.data.remote.dto.TeamDto
import io.github.kaulith.helpdeskanalytics.data.remote.dto.TicketActivitiesDto
import io.github.kaulith.helpdeskanalytics.data.remote.dto.TicketDto
import io.github.kaulith.helpdeskanalytics.data.remote.dto.TimeZoneDto
import io.github.kaulith.helpdeskanalytics.data.remote.dto.UpdateTicketRequest
import io.github.kaulith.helpdeskanalytics.data.remote.dto.UserApiKeyDto
import io.github.kaulith.helpdeskanalytics.data.remote.dto.UserDto

/** Serves [tickets] and [user]; an endpoint a test does not set up fails loudly. */
class FakeFrappeApiService : FrappeApiService {

    var tickets: List<TicketDto> = emptyList()
    var user: UserDto? = null
    var userFetches = 0
        private set

    override suspend fun getLoggedUser() = FrappeMethodResponse(checkNotNull(user).name)

    override suspend fun getTimeZone() = FrappeMethodResponse(TimeZoneDto(timeZone = "Asia/Kolkata"))

    override suspend fun getUser(email: String): FrappeSingleResponse<UserDto> {
        userFetches++
        return FrappeSingleResponse(checkNotNull(user))
    }

    override suspend fun getTickets(
        fields: String,
        limit: Int,
        orderBy: String,
        filters: String?
    ): FrappeListResponse<TicketDto> {
        val assignee = filters?.let { ASSIGN_FILTER.find(it)?.groupValues?.get(1) }
        return FrappeListResponse(tickets.filter { assignee == null || it.assign.orEmpty().contains(assignee) })
    }

    override suspend fun getTicketCount(doctype: String, filters: String): FrappeMethodResponse<Int> = notFaked()

    override suspend fun getTicketSummary(
        doctype: String,
        fields: String,
        filters: String?,
        groupBy: String?,
        orderBy: String?,
        limit: Int
    ): FrappeMethodResponse<List<JsonObject>> = notFaked()

    override suspend fun getTicket(name: String): FrappeSingleResponse<TicketDto> = notFaked()

    override suspend fun updateTicket(name: String, request: UpdateTicketRequest): FrappeSingleResponse<TicketDto> =
        notFaked()

    override suspend fun getTicketActivities(ticket: String): FrappeMethodResponse<TicketActivitiesDto> = notFaked()

    override suspend fun runDocMethod(request: RunDocMethodRequest): FrappeMethodResponse<JsonElement?> = notFaked()

    override suspend fun getTeams(fields: String, limit: Int, orderBy: String): FrappeListResponse<TeamDto> =
        notFaked()

    override suspend fun getTeam(name: String): FrappeSingleResponse<TeamDto> = notFaked()

    override suspend fun generateKeys(authorization: String, user: String): FrappeMethodResponse<GenerateKeysResponse> =
        notFaked()

    override suspend fun getUserApiKey(
        authorization: String,
        email: String,
        fields: String
    ): FrappeSingleResponse<UserApiKeyDto> = notFaked()

    override suspend fun getAgents(
        fields: String,
        filters: String,
        limit: Int,
        orderBy: String
    ): FrappeListResponse<AgentDto> = notFaked()

    private fun notFaked(): Nothing = throw UnsupportedOperationException("Not faked")

    private companion object {
        val ASSIGN_FILTER = Regex(""""_assign","like","%(.+?)%"""")
    }
}
