package io.github.kaulith.helpdeskanalytics.data.remote.api

import io.github.kaulith.helpdeskanalytics.data.remote.dto.FrappeListResponse
import io.github.kaulith.helpdeskanalytics.data.remote.dto.FrappeMethodResponse
import io.github.kaulith.helpdeskanalytics.data.remote.dto.FrappeSingleResponse
import io.github.kaulith.helpdeskanalytics.data.remote.dto.AgentDto
import io.github.kaulith.helpdeskanalytics.data.remote.dto.GenerateKeysResponse
import io.github.kaulith.helpdeskanalytics.data.remote.dto.RunDocMethodRequest
import io.github.kaulith.helpdeskanalytics.data.remote.dto.TeamDto
import io.github.kaulith.helpdeskanalytics.data.remote.dto.TicketActivitiesDto
import io.github.kaulith.helpdeskanalytics.data.remote.dto.TicketDto
import io.github.kaulith.helpdeskanalytics.data.remote.dto.UpdateTicketRequest
import io.github.kaulith.helpdeskanalytics.data.remote.dto.UserApiKeyDto
import io.github.kaulith.helpdeskanalytics.data.remote.dto.UserDto
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface FrappeApiService {

    @GET("api/method/frappe.auth.get_logged_user")
    suspend fun getLoggedUser(): FrappeMethodResponse<String>

    @GET("api/resource/User/{email}")
    suspend fun getUser(@Path("email") email: String): FrappeSingleResponse<UserDto>

    @GET("api/resource/HD%20Ticket")
    suspend fun getTickets(
        @Query("fields") fields: String = TICKET_FIELDS,
        @Query("limit_page_length") limit: Int = 9999,
        @Query("order_by") orderBy: String = "creation desc",
        @Query("filters") filters: String? = null
    ): FrappeListResponse<TicketDto>

    @GET("api/method/frappe.client.get_count")
    suspend fun getTicketCount(
        @Query("doctype") doctype: String = "HD Ticket",
        @Query("filters") filters: String
    ): FrappeMethodResponse<Int>

    // Grouped/aggregated ticket reports. Column names vary per query, so the rows
    // stay untyped. `limit_page_length` defaults to 20 server-side; 0 lifts it.
    @GET("api/method/frappe.client.get_list")
    suspend fun getTicketSummary(
        @Query("doctype") doctype: String = "HD Ticket",
        @Query("fields") fields: String,
        @Query("filters") filters: String? = null,
        @Query("group_by") groupBy: String? = null,
        @Query("order_by") orderBy: String? = null,
        @Query("limit_page_length") limit: Int = 0
    ): FrappeMethodResponse<List<JsonObject>>

    @GET("api/resource/HD%20Ticket/{name}")
    suspend fun getTicket(@Path("name") name: String): FrappeSingleResponse<TicketDto>

    @PUT("api/resource/HD%20Ticket/{name}")
    suspend fun updateTicket(
        @Path("name") name: String,
        @Body request: UpdateTicketRequest
    ): FrappeSingleResponse<TicketDto>

    // Helpdesk's own endpoint for a ticket's conversation. Returns comments +
    // communications together, and runs server-side so it isn't blocked by the
    // generic REST permissions on the Communication doctype.
    @GET("api/method/helpdesk.helpdesk.doctype.hd_ticket.api.get_ticket_activities")
    suspend fun getTicketActivities(
        @Query("ticket") ticket: String
    ): FrappeMethodResponse<TicketActivitiesDto>

    // Runs a whitelisted HD Ticket controller method (reply_via_agent, new_comment).
    @POST("api/method/run_doc_method")
    suspend fun runDocMethod(@Body request: RunDocMethodRequest): FrappeMethodResponse<JsonElement?>

    @GET("api/resource/HD%20Team")
    suspend fun getTeams(
        @Query("fields") fields: String = TEAM_FIELDS,
        @Query("limit_page_length") limit: Int = 100,
        @Query("order_by") orderBy: String = "name asc"
    ): FrappeListResponse<TeamDto>

    @GET("api/resource/HD%20Team/{name}")
    suspend fun getTeam(
        @Path("name") name: String
    ): FrappeSingleResponse<TeamDto>

    // Mints/rotates an agent's API secret. Caller must pass an admin (System
    // Manager) token explicitly, bypassing the default auth interceptor.
    @POST("api/method/frappe.core.doctype.user.user.generate_keys")
    suspend fun generateKeys(
        @Header("Authorization") authorization: String,
        @Query("user") user: String
    ): FrappeMethodResponse<GenerateKeysResponse>

    @GET("api/resource/User/{email}")
    suspend fun getUserApiKey(
        @Header("Authorization") authorization: String,
        @Path("email") email: String,
        @Query("fields") fields: String = "[\"api_key\"]"
    ): FrappeSingleResponse<UserApiKeyDto>

    @GET("api/resource/HD%20Agent")
    suspend fun getAgents(
        @Query("fields") fields: String = AGENT_FIELDS,
        @Query("filters") filters: String = """[["is_active","=","1"]]""",
        @Query("limit_page_length") limit: Int = 200,
        @Query("order_by") orderBy: String = "agent_name asc"
    ): FrappeListResponse<AgentDto>

    companion object {
        const val TEAM_FIELDS = "[\"name\",\"team_name\"]"

        const val AGENT_FIELDS = "[\"name\",\"agent_name\",\"user\",\"is_active\",\"user_image\"]"

        const val TICKET_FIELDS = "[\"name\",\"subject\",\"status\",\"priority\"," +
                "\"creation\",\"modified\",\"first_responded_on\"," +
                "\"resolution_date\",\"last_agent_response\"," +
                "\"raised_by\",\"contact\",\"customer\"," +
                "\"_assign\",\"response_by\",\"resolution_by\"," +
                "\"first_response_time\",\"avg_response_time\",\"resolution_time\"," +
                "\"ticket_type\",\"sla\",\"agreement_status\"]"
    }
}
