package io.github.kaulith.helpdeskanalytics.util

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val data: Any? = null) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}
