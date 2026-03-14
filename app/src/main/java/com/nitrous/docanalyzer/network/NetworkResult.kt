package com.nitrous.docanalyzer.network

sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class ApiError(val code: String, val message: String, val retryable: Boolean) : NetworkResult<Nothing>()
    data class NetworkError(val message: String) : NetworkResult<Nothing>()
    data class RateLimit(val message: String, val retryAfterSeconds: Int) : NetworkResult<Nothing>()
    data class Unauthorized(val message: String) : NetworkResult<Nothing>()
    data class Exception(val message: String) : NetworkResult<Nothing>()
}
