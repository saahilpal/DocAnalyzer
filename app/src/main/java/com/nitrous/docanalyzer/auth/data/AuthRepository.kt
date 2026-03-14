package com.nitrous.docanalyzer.auth.data

import com.nitrous.docanalyzer.network.BaseRepository
import com.nitrous.docanalyzer.network.NetworkResult
import com.nitrous.docanalyzer.network.ApiService
import com.nitrous.docanalyzer.network.dto.LoginResponse
import com.nitrous.docanalyzer.network.dto.UserDto
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class AuthRepository(
    private val apiService: ApiService,
    private val authManager: AuthManager
) : BaseRepository() {

    // SECTION 3 — AUTH SYSTEM

    suspend fun register(name: String, email: String, password: String): NetworkResult<Map<String, String>> {
        return safeApiCall { 
            apiService.register(mapOf("name" to name, "email" to email, "password" to password)) 
        }
    }

    suspend fun sendOtp(email: String): NetworkResult<Map<String, String>> {
        return safeApiCall {
            apiService.sendOtp(mapOf("email" to email))
        }
    }

    suspend fun verifyOtp(email: String, otp: String): NetworkResult<Map<String, String>> {
        return safeApiCall { 
            apiService.verifyOtp(mapOf("email" to email, "otp" to otp)) 
        }
    }

    suspend fun login(email: String, password: String): NetworkResult<LoginResponse> {
        val result = safeApiCall { apiService.login(mapOf("email" to email, "password" to password)) }
        if (result is NetworkResult.Success) {
            val data = result.data
            authManager.saveSession(
                accessToken = data.accessToken ?: "",
                refreshToken = data.refreshToken ?: "",
                expiresAt = data.expiresAt ?: ""
            )
        }
        return result
    }

    suspend fun requestReset(email: String): NetworkResult<Map<String, String>> {
        return safeApiCall {
            apiService.requestReset(mapOf("email" to email))
        }
    }

    suspend fun resetPassword(email: String, otp: String, newPassword: String): NetworkResult<Map<String, String>> {
        return safeApiCall {
            apiService.resetPassword(mapOf("email" to email, "otp" to otp, "newPassword" to newPassword))
        }
    }

    suspend fun getMe(): NetworkResult<UserDto> {
        return safeApiCall { apiService.getMe() }
    }

    suspend fun changeEmail(newEmail: String): NetworkResult<Map<String, String>> {
        return safeApiCall { apiService.changeEmail(mapOf("newEmail" to newEmail)) }
    }

    suspend fun verifyChangeEmail(newEmail: String, otp: String): NetworkResult<Map<String, String>> {
        return safeApiCall { apiService.verifyChangeEmail(mapOf("newEmail" to newEmail, "otp" to otp)) }
    }

    // SECTION 4 — LOGOUT UX REQUIREMENT
    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    fun logout() {
        // 1. clear tokens immediately
        authManager.clearSession()
        
        // 2. navigate instantly (handled by UI observing authManager.authState)

        // 3. send logout request in background
        GlobalScope.launch(Dispatchers.IO) {
            try {
                apiService.logout()
            } catch (e: Exception) {
                // Background error ignored as per requirement
            }
        }
    }

    fun isLoggedIn(): Boolean = authManager.isLoggedIn()
}
