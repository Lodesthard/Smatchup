package com.example.smatchup.domain.model

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class NetworkError(val cause: Throwable) : ApiResult<Nothing>()
    data class RateLimited(val retryAfter: Long?) : ApiResult<Nothing>()
    object Unauthorized : ApiResult<Nothing>()
    data class ParseError(val msg: String) : ApiResult<Nothing>()
    object NotFound : ApiResult<Nothing>()

    inline fun <R> map(transform: (T) -> R): ApiResult<R> = when (this) {
        is Success -> Success(transform(data))
        is NetworkError -> this
        is RateLimited -> this
        is ParseError -> this
        is Unauthorized -> this
        is NotFound -> this
    }

    fun getOrNull(): T? = (this as? Success)?.data
}
