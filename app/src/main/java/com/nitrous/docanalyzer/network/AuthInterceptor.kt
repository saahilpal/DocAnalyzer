package com.nitrous.docanalyzer.network

import android.util.Log
import com.nitrous.docanalyzer.auth.data.AuthManager
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject

class AuthInterceptor(private val authManager: AuthManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val accessToken = authManager.getAccessToken()
        val isStream = originalRequest.url.toString().contains("chat?stream=true")

        val requestBuilder = originalRequest.newBuilder()
            .header("Content-Type", "application/json")

        if (accessToken != null) {
            requestBuilder.header("Authorization", "Bearer $accessToken")
            if (isStream) {
                Log.d("AUDIT_Auth", "STEP 1: Authorization header added to stream request")
            }
        } else if (isStream) {
            Log.w("AUDIT_Auth", "STEP 1: WARNING - No access token found for stream request")
        }

        if (isStream) {
            Log.d("AUDIT_Request", "STEP 1: Outgoing Request URL: ${originalRequest.url}")
            // Body logging is tricky for SSE because it's a POST with body
            // But we already log it in DocRepository
        }

        var response = chain.proceed(requestBuilder.build())

        if (isStream) {
            Log.d("AUDIT_Response", "STEP 2: Initial Response Received. Code: ${response.code}")
        }

        // Rate Limit Handling
        if (response.code == 429) {
            val retryAfter = response.header("Retry-After")?.toLongOrNull() ?: 1L
            response.close() // Close before sleeping to free connection
            Thread.sleep(retryAfter * 1000)
            return intercept(chain)
        }

        // Token Refresh Logic
        if (response.code == 401 && accessToken != null) {
            // CRITICAL: Close the failing response IMMEDIATELY before blocking on the lock
            response.close() 
            
            val path = originalRequest.url.encodedPath
            // Avoid infinite loops if refresh or login fails
            if (!path.contains("auth/refresh") && !path.contains("auth/login")) {
                synchronized(this) {
                    val currentToken = authManager.getAccessToken()
                    // If token was already refreshed by another thread, retry original instantly
                    if (currentToken != accessToken && currentToken != null) {
                        return chain.proceed(originalRequest.newBuilder()
                            .header("Authorization", "Bearer $currentToken")
                            .build())
                    }

                    val refreshToken = authManager.getRefreshToken()
                    if (refreshToken != null) {
                        val baseUrl = try {
                            RuntimeConfigManager.baseUrl
                        } catch (e: Exception) {
                            ""
                        }

                        if (baseUrl.isNotBlank()) {
                            // Use a fresh client instance to avoid any chain pollution
                            val refreshClient = OkHttpClient.Builder().build()
                            val refreshRequest = Request.Builder()
                                .url(baseUrl + "auth/refresh")
                                .header("Content-Type", "application/json")
                                .post("{\"refreshToken\":\"$refreshToken\"}".toRequestBody("application/json".toMediaType()))
                                .build()

                            try {
                                val refreshResponse = refreshClient.newCall(refreshRequest).execute()
                                if (refreshResponse.isSuccessful) {
                                    val body = refreshResponse.body?.string()
                                    val json = JSONObject(body ?: "")
                                    if (json.optBoolean("ok")) {
                                        val data = json.getJSONObject("data")
                                        val newAccess = data.getString("accessToken")
                                        val newRefresh = data.getString("refreshToken")
                                        val expiresAt = data.optString("expiresAt", "")
                                        
                                        authManager.saveSession(newAccess, newRefresh, expiresAt)
                                        
                                        refreshResponse.close()

                                        return chain.proceed(originalRequest.newBuilder()
                                            .header("Authorization", "Bearer $newAccess")
                                            .build())
                                    }
                                }
                                refreshResponse.close()
                            } catch (e: Exception) {
                                // Network failure during refresh
                            }
                        }
                    }
                    // Refresh failed or no refresh token -> force logout
                    authManager.clearSession()
                }
            }
        }

        return response
    }
}
