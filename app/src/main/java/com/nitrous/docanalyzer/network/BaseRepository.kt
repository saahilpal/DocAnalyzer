package com.nitrous.docanalyzer.network

import com.google.gson.Gson
import com.nitrous.docanalyzer.network.dto.ApiResponse
import retrofit2.Response

abstract class BaseRepository {
    protected val gson = Gson()

    protected suspend fun <T> safeApiCall(call: suspend () -> Response<ApiResponse<T>>): NetworkResult<T> {
        return try {
            val response = call()
            val body = response.body()
            
            if (response.isSuccessful && body != null) {
                if (body.ok) {
                    NetworkResult.Success(body.data as T)
                } else {
                    parseErrorObject<T>(body.error)
                }
            } else {
                val errorBody = response.errorBody()?.string()
                if (errorBody != null) {
                    parseErrorString<T>(errorBody)
                } else {
                    NetworkResult.Error(response.code().toString(), response.message(), false)
                }
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e.message ?: "Network exception")
        }
    }

    private fun <T> parseErrorObject(error: Any?): NetworkResult<T> {
        return when (error) {
            is String -> NetworkResult.RateLimit(error)
            is Map<*, *> -> {
                val code = error["code"] as? String ?: "UNKNOWN"
                val message = error["message"] as? String ?: "Unknown error"
                val retryable = error["retryable"] as? Boolean ?: false
                NetworkResult.Error(code, message, retryable)
            }
            else -> NetworkResult.Exception("Unknown error format")
        }
    }

    private fun <T> parseErrorString(errorBody: String): NetworkResult<T> {
        return try {
            val apiResponse = gson.fromJson(errorBody, ApiResponse::class.java)
            parseErrorObject(apiResponse.error)
        } catch (e: Exception) {
            if (errorBody.contains("Rate limit exceeded")) {
                NetworkResult.RateLimit(errorBody)
            } else {
                NetworkResult.Exception("Raw error: $errorBody")
            }
        }
    }
}
