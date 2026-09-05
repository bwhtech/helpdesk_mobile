package io.github.kaulith.helpdeskanalytics.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OAuthTokenDto(
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("refresh_token") val refreshToken: String?,
    @SerializedName("expires_in") val expiresIn: Long?,
    @SerializedName("token_type") val tokenType: String?
)

// helpdesk_push publishes the site's OAuth client id so the app does not have to
// ask the user for it. Sites without the app installed have no such endpoint.
data class OAuthClientIdDto(
    @SerializedName("client_id") val clientId: String?
)
