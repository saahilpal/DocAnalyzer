package com.nitrous.docanalyzer.network

import com.nitrous.docanalyzer.network.dto.ApiResponse
import com.nitrous.docanalyzer.network.dto.ApiError
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException

abstract class BaseRepository {

    protected suspend fun <T> safeApiCall(call: suspend () -> Response<ApiResponse<T>>): NetworkResult<T> {
        return try {
            val response = call()
            val body = response.body()
            
            if (body != null) {
                if (body.ok) {
                    @Suppress("UNCHECKED_CAST")
                    NetworkResult.Success(body.data as T)
                } else {
                    val error = body.error
                    if (error != null) {
                        NetworkResult.ApiError(
                            error.code ?: "UNKNOWN_CODE", 
                            error.message ?: "Unknown API error", 
                            error.retryable ?: false
                        )
                    } else {
                        NetworkResult.Exception("API marked as not ok but no error object provided")
                    }
                }
            } else {
                val errorBody = response.errorBody()?.string()
                if (errorBody != null) {
                    try {
                        val errorResponse = RetrofitClient.gson.fromJson(errorBody, ApiResponse::class.java)
                        @Suppress("UNCHECKED_CAST")
                        val error = (errorResponse as ApiResponse<T>).error
                        if (error != null) {
                            NetworkResult.ApiError(
                                error.code ?: "UNKNOWN_CODE", 
                                error.message ?: "Error parsing failed", 
                                error.retryable ?: false
                            )
                        } else {
                            NetworkResult.Exception("Error parsing failed: No error object")
                        }
                    } catch (e: Exception) {
                        NetworkResult.Exception("HTTP ${response.code()}: ${response.message()}")
                    }
                } else {
                    NetworkResult.Exception("HTTP ${response.code()}: ${response.message()}")
                }
            }
        } catch (e: Exception) {
            when (e) {
                is UnknownHostException -> NetworkResult.NetworkError("No internet connection")
                is SocketTimeoutException -> NetworkResult.NetworkError("Server timed out")
                else -> NetworkResult.Exception(e.message ?: "Unknown error")
            }
        }
    }
}
