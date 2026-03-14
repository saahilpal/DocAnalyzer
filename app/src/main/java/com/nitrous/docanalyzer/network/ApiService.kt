package com.nitrous.docanalyzer.network

import com.nitrous.docanalyzer.network.dto.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // AUTH ENDPOINTS
    @POST("auth/register")
    suspend fun register(@Body body: Map<String, String>): Response<ApiResponse<Map<String, String>>>

    @POST("auth/send-otp")
    suspend fun sendOtp(@Body body: Map<String, String>): Response<ApiResponse<Map<String, String>>>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body body: Map<String, String>): Response<ApiResponse<Map<String, String>>>

    @POST("auth/login")
    suspend fun login(@Body body: Map<String, String>): Response<ApiResponse<LoginResponse>>

    @POST("auth/refresh")
    suspend fun refresh(@Body body: Map<String, String>): Response<ApiResponse<LoginResponse>>

    @POST("auth/request-reset")
    suspend fun requestReset(@Body body: Map<String, String>): Response<ApiResponse<Map<String, String>>>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body body: Map<String, String>): Response<ApiResponse<Map<String, String>>>

    @GET("auth/me")
    suspend fun getMe(): Response<ApiResponse<UserDto>>

    @POST("auth/change-email")
    suspend fun changeEmail(@Body body: Map<String, String>): Response<ApiResponse<Map<String, String>>>

    @POST("auth/change-email/verify")
    suspend fun verifyChangeEmail(@Body body: Map<String, String>): Response<ApiResponse<Map<String, String>>>

    @GET("auth/sessions")
    suspend fun getAuthSessions(): Response<ApiResponse<AuthSessionsResponse>>

    @DELETE("auth/sessions/{id}")
    suspend fun deleteAuthSession(@Path("id") id: String): Response<ApiResponse<Map<String, Boolean>>>

    @DELETE("auth/session")
    suspend fun logout(): Response<ApiResponse<Map<String, Boolean>>>

    // SESSION ENDPOINTS
    @GET("sessions")
    suspend fun getSessions(): Response<ApiResponse<List<SessionDto>>>

    @POST("sessions")
    suspend fun createSession(@Body body: Map<String, String>): Response<ApiResponse<SessionDto>>

    @GET("sessions/search")
    suspend fun searchSessions(@Query("q") query: String): Response<ApiResponse<List<SessionDto>>>

    @PATCH("sessions/{sessionId}")
    suspend fun updateSession(
        @Path("sessionId") sessionId: Int,
        @Body body: Map<String, String>
    ): Response<ApiResponse<SessionDto>>

    @GET("sessions/{sessionId}/meta")
    suspend fun getSessionMeta(@Path("sessionId") sessionId: Int): Response<ApiResponse<SessionMetaDto>>

    @GET("sessions/{sessionId}")
    suspend fun getSessionDetails(@Path("sessionId") sessionId: Int): Response<ApiResponse<SessionDto>>

    @DELETE("sessions/{sessionId}")
    suspend fun deleteSession(@Path("sessionId") sessionId: Int): Response<ApiResponse<Map<String, Any>>>

    // PDF ENDPOINTS
    @Multipart
    @POST("sessions/{sessionId}/pdfs")
    suspend fun uploadPdf(
        @Path("sessionId") sessionId: Int,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<UploadResponse>>

    @GET("sessions/{sessionId}/pdfs")
    suspend fun getSessionPdfs(@Path("sessionId") sessionId: Int): Response<ApiResponse<List<PdfDto>>>

    @GET("pdfs/{pdfId}")
    suspend fun getPdf(@Path("pdfId") pdfId: Int): Response<ApiResponse<PdfDto>>

    @DELETE("pdfs/{pdfId}")
    suspend fun deletePdf(@Path("pdfId") pdfId: Int): Response<ApiResponse<Map<String, Any>>>

    // CHAT ENDPOINTS
    @POST("sessions/{sessionId}/chat")
    suspend fun chat(
        @Path("sessionId") sessionId: Int,
        @Query("stream") stream: Boolean = false,
        @Body request: ChatRequest
    ): Response<ApiResponse<ChatResponse>>

    @GET("sessions/{sessionId}/history")
    suspend fun getChatHistory(@Path("sessionId") sessionId: Int): Response<ApiResponse<List<HistoryItemDto>>>

    @DELETE("sessions/{sessionId}/history")
    suspend fun deleteChatHistory(@Path("sessionId") sessionId: Int): Response<ApiResponse<Map<String, Any>>>

    // JOB ENDPOINT
    @GET("jobs/{jobId}")
    suspend fun getJob(@Path("jobId") jobId: String): Response<ApiResponse<JobResponse>>
}
