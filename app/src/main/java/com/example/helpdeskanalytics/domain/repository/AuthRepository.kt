package com.example.helpdeskanalytics.domain.repository

import com.example.helpdeskanalytics.domain.model.User
import com.example.helpdeskanalytics.util.Result

interface AuthRepository {
    suspend fun validateCredentials(siteUrl: String, apiKey: String, apiSecret: String): Result<User>
    fun isLoggedIn(): Boolean
    suspend fun logout()
}
