package com.example.helpdeskanalytics.data.remote.api

import com.example.helpdeskanalytics.data.remote.dto.NotificationApiResponse
import com.example.helpdeskanalytics.data.remote.dto.RegisterDeviceRequest
import com.example.helpdeskanalytics.data.remote.dto.UnregisterDeviceRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface NotificationApiService {

    @POST("api/method/helpdesk_push.api.register_device")
    suspend fun registerDevice(@Body request: RegisterDeviceRequest): NotificationApiResponse

    @POST("api/method/helpdesk_push.api.unregister_device")
    suspend fun unregisterDevice(@Body request: UnregisterDeviceRequest): NotificationApiResponse
}
