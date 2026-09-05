package io.github.kaulith.helpdeskanalytics.data.remote.api

import io.github.kaulith.helpdeskanalytics.data.remote.dto.FrappeMethodResponse
import io.github.kaulith.helpdeskanalytics.data.remote.dto.OAuthClientIdDto
import io.github.kaulith.helpdeskanalytics.data.remote.dto.OAuthTokenDto
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

// The endpoints used before there is a session: token exchange, which carries its
// own credentials, and client discovery. Both run on a client with no auth
// interceptor.
interface OAuthService {

    @FormUrlEncoded
    @POST("api/method/frappe.integrations.oauth2.get_token")
    suspend fun getToken(
        @Field("grant_type") grantType: String,
        @Field("client_id") clientId: String,
        @Field("code") code: String? = null,
        @Field("code_verifier") codeVerifier: String? = null,
        @Field("redirect_uri") redirectUri: String? = null,
        @Field("refresh_token") refreshToken: String? = null
    ): OAuthTokenDto

    @FormUrlEncoded
    @POST("api/method/frappe.integrations.oauth2.revoke_token")
    suspend fun revokeToken(@Field("token") token: String)

    @GET("api/method/helpdesk_push.api.get_oauth_client_id")
    suspend fun getClientId(): FrappeMethodResponse<OAuthClientIdDto>
}
