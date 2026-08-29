package com.example.helpdeskanalytics.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RegisterDeviceRequest(
    @SerializedName("device_token") val deviceToken: String,
    @SerializedName("agent_email") val agentEmail: String
)

data class UnregisterDeviceRequest(
    @SerializedName("device_token") val deviceToken: String
)

data class NotificationApiResponse(
    val message: NotificationResult? = null
)

data class NotificationResult(
    val success: Boolean = false,
    val agent: String? = null
)
