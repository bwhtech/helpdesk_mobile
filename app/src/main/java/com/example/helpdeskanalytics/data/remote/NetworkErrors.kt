package com.example.helpdeskanalytics.data.remote

import com.example.helpdeskanalytics.util.NetworkError
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

fun Throwable.toNetworkError(): Exception = when (this) {
    is HttpException -> when (code()) {
        401, 403 -> NetworkError.Unauthorized
        else -> NetworkError.ApiError(code(), message())
    }
    is SocketTimeoutException -> NetworkError.Timeout
    is IOException -> NetworkError.NoInternet
    else -> NetworkError.Unknown(this)
}
