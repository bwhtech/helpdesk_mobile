package io.github.kaulith.helpdeskanalytics.data.repository

import io.github.kaulith.helpdeskanalytics.data.local.credentials.CredentialsManager
import io.github.kaulith.helpdeskanalytics.data.local.preferences.PreferencesManager
import io.github.kaulith.helpdeskanalytics.data.mapper.toDomain
import io.github.kaulith.helpdeskanalytics.data.remote.api.ApiServiceProvider
import io.github.kaulith.helpdeskanalytics.domain.model.User
import io.github.kaulith.helpdeskanalytics.domain.repository.AuthRepository
import io.github.kaulith.helpdeskanalytics.util.NetworkError
import io.github.kaulith.helpdeskanalytics.util.Result
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

class FrappeAuthRepository(
    private val credentialsManager: CredentialsManager,
    private val apiServiceProvider: ApiServiceProvider,
    private val preferencesManager: PreferencesManager
) : AuthRepository {

    override suspend fun validateCredentials(
        siteUrl: String,
        apiKey: String,
        apiSecret: String
    ): Result<User> {
        return try {
            credentialsManager.saveCredentials(siteUrl, apiKey, apiSecret)
            apiServiceProvider.invalidate()

            val service = apiServiceProvider.getService()

            val loggedUserEmail = service.getLoggedUser().message
            val userDto = service.getUser(loggedUserEmail).data
            val user = userDto.toDomain()

            preferencesManager.setLoggedInUserEmail(loggedUserEmail)

            Result.Success(user)
        } catch (e: HttpException) {
            credentialsManager.clearCredentials()
            apiServiceProvider.invalidate()
            when (e.code()) {
                401, 403 -> Result.Error(NetworkError.Unauthorized)
                else -> Result.Error(NetworkError.ApiError(e.code(), e.message()))
            }
        } catch (e: SocketTimeoutException) {
            credentialsManager.clearCredentials()
            apiServiceProvider.invalidate()
            Result.Error(NetworkError.Timeout)
        } catch (e: IOException) {
            credentialsManager.clearCredentials()
            apiServiceProvider.invalidate()
            Result.Error(NetworkError.NoInternet)
        } catch (e: Exception) {
            credentialsManager.clearCredentials()
            apiServiceProvider.invalidate()
            Result.Error(NetworkError.Unknown(e))
        }
    }

    override fun isLoggedIn(): Boolean {
        return credentialsManager.hasCredentials()
    }

    override suspend fun logout() {
        credentialsManager.clearCredentials()
        apiServiceProvider.invalidate()
        preferencesManager.setLoggedInUserEmail(null)
    }
}
