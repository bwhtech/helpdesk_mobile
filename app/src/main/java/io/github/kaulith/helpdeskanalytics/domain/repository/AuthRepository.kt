package io.github.kaulith.helpdeskanalytics.domain.repository

import io.github.kaulith.helpdeskanalytics.domain.model.User
import io.github.kaulith.helpdeskanalytics.util.Result

interface AuthRepository {
    suspend fun validateCredentials(siteUrl: String, apiKey: String, apiSecret: String): Result<User>
    fun isLoggedIn(): Boolean
    suspend fun logout()
}
