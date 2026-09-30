package io.github.kaulith.helpdeskanalytics.testing

import io.github.kaulith.helpdeskanalytics.domain.model.User
import io.github.kaulith.helpdeskanalytics.domain.repository.AuthRepository
import io.github.kaulith.helpdeskanalytics.util.Result

class FakeAuthRepository : AuthRepository {

    var discoveredClientId: String? = null
    var loginResult: Result<User> = Result.Success(
        User(email = "ann@x.io", fullName = "Ann", roles = emptyList(), hasTeamLeadPermission = true)
    )

    override suspend fun validateCredentials(siteUrl: String, apiKey: String, apiSecret: String) = loginResult

    override suspend fun discoverOAuthClientId(siteUrl: String) = discoveredClientId

    override fun beginOAuthLogin(siteUrl: String, clientId: String) = "$siteUrl/authorize?client_id=$clientId"

    override suspend fun completeOAuthLogin(code: String, state: String) = loginResult

    override fun isLoggedIn() = false

    override suspend fun logout() = Unit
}
