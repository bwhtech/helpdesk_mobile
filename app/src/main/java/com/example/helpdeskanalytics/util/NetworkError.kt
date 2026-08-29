package com.example.helpdeskanalytics.util

sealed class NetworkError : Exception() {
    data class ApiError(val code: Int, override val message: String) : NetworkError()
    data object Unauthorized : NetworkError()
    data object Timeout : NetworkError()
    data object NoInternet : NetworkError()
    data class Unknown(override val cause: Throwable?) : NetworkError()
}
