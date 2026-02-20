package com.nitrous.docanalyzer.network

sealed class NetworkResult<T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error<T>(val code: String, val message: String, val retryable: Boolean) : NetworkResult<T>()
    data class Exception<T>(val message: String) : NetworkResult<T>()
    data class RateLimit<T>(val message: String) : NetworkResult<T>()
}
