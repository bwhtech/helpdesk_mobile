package io.github.kaulith.helpdeskanalytics.data.remote.api

import com.google.gson.JsonElement
import io.github.kaulith.helpdeskanalytics.data.remote.dto.FrappeMethodResponse
import io.github.kaulith.helpdeskanalytics.data.remote.dto.NotificationApiResponse
import io.github.kaulith.helpdeskanalytics.data.remote.dto.RegisterDeviceRequest
import io.github.kaulith.helpdeskanalytics.data.remote.dto.UnregisterDeviceRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface NotificationApiService {

    @POST("api/method/helpdesk_push.api.register_device")
    suspend fun registerDevice(@Body request: RegisterDeviceRequest): NotificationApiResponse

    @POST("api/method/helpdesk_push.api.unregister_device")
    suspend fun unregisterDevice(@Body request: UnregisterDeviceRequest): NotificationApiResponse

    // Keyed by app name; any signed in user may ask.
    @GET("api/method/frappe.utils.change_log.get_versions")
    suspend fun getInstalledApps(): FrappeMethodResponse<Map<String, JsonElement>>
}
