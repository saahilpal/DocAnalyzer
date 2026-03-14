package com.nitrous.docanalyzer.auth.data

import com.nitrous.docanalyzer.auth.data.dto.*
import com.nitrous.docanalyzer.network.dto.ApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginResponse>>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<RegisterResponse>>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<ApiResponse<Map<String, String>>>

    @POST("auth/request-reset")
    suspend fun requestReset(@Body request: RequestResetRequest): Response<ApiResponse<RegisterResponse>>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<ApiResponse<RegisterResponse>>

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): Response<ApiResponse<LoginResponse>>
}
