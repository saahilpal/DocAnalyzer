package com.nitrous.docanalyzer.network

import com.nitrous.docanalyzer.network.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @GET("health")
    suspend fun getHealth(): Response<ApiResponse<HealthResponse>>

    @GET("ping")
    suspend fun ping(): Response<ApiResponse<Map<String, Boolean>>>

    @GET("sessions")
    suspend fun getSessions(): Response<ApiResponse<List<SessionDto>>>

    @POST("sessions")
    suspend fun createSession(@Body body: Map<String, String>): Response<ApiResponse<SessionDto>>

    @GET("sessions/{sessionId}")
    suspend fun getSession(@Path("sessionId") sessionId: Int): Response<ApiResponse<SessionDetailDto>>

    @DELETE("sessions/{sessionId}")
    suspend fun deleteSession(@Path("sessionId") sessionId: Int): Response<ApiResponse<Map<String, Any>>>

    @GET("sessions/{sessionId}/pdfs")
    suspend fun getSessionPdfs(@Path("sessionId") sessionId: Int): Response<ApiResponse<List<PdfDto>>>

    @Multipart
    @POST("sessions/{sessionId}/pdfs")
    suspend fun uploadPdf(
        @Path("sessionId") sessionId: Int,
        @Part file: MultipartBody.Part,
        @Part("title") title: RequestBody? = null
    ): Response<ApiResponse<UploadResponse>>

    @GET("pdfs/{pdfId}")
    suspend fun getPdf(@Path("pdfId") pdfId: Int): Response<ApiResponse<PdfDto>>

    @DELETE("pdfs/{pdfId}")
    suspend fun deletePdf(
        @Path("pdfId") pdfId: Int,
        @Query("removeFile") removeFile: Boolean = false
    ): Response<ApiResponse<Map<String, Any>>>

    @POST("sessions/{sessionId}/chat")
    suspend fun chat(
        @Path("sessionId") sessionId: Int,
        @Body request: ChatRequest,
        @Query("stream") stream: Boolean = false
    ): Response<ApiResponse<ChatResponse>>

    @GET("jobs/{jobId}")
    suspend fun getJob(@Path("jobId") jobId: String): Response<ApiResponse<JobDto>>

    @GET("sessions/{sessionId}/history")
    suspend fun getHistory(
        @Path("sessionId") sessionId: Int,
        @Query("limit") limit: String? = null,
        @Query("offset") offset: String? = null
    ): Response<ApiResponse<List<HistoryItemDto>>>

    @DELETE("sessions/{sessionId}/history")
    suspend fun clearHistory(@Path("sessionId") sessionId: Int): Response<ApiResponse<Map<String, Boolean>>>
}
