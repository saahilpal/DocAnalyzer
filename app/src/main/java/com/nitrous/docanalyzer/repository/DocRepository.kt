package com.nitrous.docanalyzer.repository

import android.util.Log
import com.nitrous.docanalyzer.network.*
import com.nitrous.docanalyzer.network.dto.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.File

sealed class ChatStreamEvent {
    data class Ready(val sessionId: Int) : ChatStreamEvent()
    data class Progress(val stage: String, val progress: Int) : ChatStreamEvent()
    data class Token(val text: String) : ChatStreamEvent()
    data class Done(val response: ChatResponse) : ChatStreamEvent()
    data class Error(val code: String, val message: String) : ChatStreamEvent()
}

class DocRepository(private val apiService: ApiService, private val okHttpClient: okhttp3.OkHttpClient) : BaseRepository() {
    private val TAG = "AUDIT_DocRepo"

    suspend fun getSessions(): NetworkResult<List<SessionDto>> {
        return safeApiCall { apiService.getSessions() }
    }

    suspend fun searchSessions(query: String): NetworkResult<List<SessionDto>> {
        return safeApiCall { apiService.searchSessions(query) }
    }

    suspend fun createSession(title: String? = null): NetworkResult<SessionDto> {
        val body = if (title != null) mapOf("title" to title) else emptyMap()
        return safeApiCall { apiService.createSession(body) }
    }

    suspend fun updateSession(sessionId: Int, title: String): NetworkResult<SessionDto> {
        return safeApiCall { apiService.updateSession(sessionId, mapOf("title" to title)) }
    }

    suspend fun getSessionDetails(sessionId: Int): NetworkResult<SessionDto> {
        return safeApiCall { apiService.getSessionDetails(sessionId) }
    }

    suspend fun deleteSession(sessionId: Int): NetworkResult<Map<String, Any>> {
        return safeApiCall { apiService.deleteSession(sessionId) }
    }

    suspend fun getSessionMeta(sessionId: Int): NetworkResult<SessionMetaDto> {
        return safeApiCall { apiService.getSessionMeta(sessionId) }
    }

    suspend fun getHistory(sessionId: Int): NetworkResult<List<HistoryItemDto>> {
        return safeApiCall { apiService.getChatHistory(sessionId) }
    }

    suspend fun getSessionPdfs(sessionId: Int): NetworkResult<List<PdfDto>> {
        return safeApiCall { apiService.getSessionPdfs(sessionId) }
    }

    suspend fun uploadPdf(sessionId: Int, file: File): NetworkResult<UploadResponse> {
        val requestFile = file.asRequestBody("application/pdf".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        return safeApiCall { apiService.uploadPdf(sessionId, body) }
    }

    suspend fun getPdf(pdfId: Int): NetworkResult<PdfDto> {
        return safeApiCall { apiService.getPdf(pdfId) }
    }

    suspend fun deletePdf(pdfId: Int): NetworkResult<Map<String, Any>> {
        return safeApiCall { apiService.deletePdf(pdfId) }
    }

    suspend fun getJob(jobId: String): NetworkResult<JobResponse> {
        return safeApiCall { apiService.getJob(jobId) }
    }

    suspend fun getMe(): NetworkResult<UserDto> {
        return safeApiCall { apiService.getMe() }
    }

    suspend fun logout(): NetworkResult<Map<String, Boolean>> {
        return safeApiCall { apiService.logout() }
    }

    fun pollJob(jobId: String): Flow<NetworkResult<JobResponse>> = flow {
        while (true) {
            val result = getJob(jobId)
            emit(result)
            if (result is NetworkResult.Success) {
                val status = result.data.status
                if (status == "completed" || status == "failed") break
            } else if (result is NetworkResult.ApiError && !result.retryable) {
                break
            }
            delay(1500)
        }
    }

    fun streamChat(sessionId: Int, request: ChatRequest): Flow<ChatStreamEvent> = callbackFlow {
        val baseUrl = try {
            RuntimeConfigManager.baseUrl
        } catch (e: Exception) {
            ""
        }
        val url = "${baseUrl}sessions/$sessionId/chat?stream=true"
        val jsonRequest = RetrofitClient.gson.toJson(request)
        
        Log.d(TAG, "STEP 1: Outgoing SSE Request - URL: $url")
        Log.d(TAG, "STEP 1: Request Body: $jsonRequest")

        val httpRequest = Request.Builder()
            .url(url)
            .post(jsonRequest.toRequestBody("application/json".toMediaTypeOrNull()))
            .header("Accept", "text/event-stream")
            .build()

        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: okhttp3.Response) {
                Log.d(TAG, "STEP 2: SSE connection opened. Response Code: ${response.code}")
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                Log.d(TAG, "STEP 3: Raw SSE Event Received -> event: ${type ?: "null"}, data: $data")
                
                try {
                    val chatEvent = RetrofitClient.gson.fromJson(data, ChatEvent::class.java)
                    val eventType = type ?: chatEvent.type
                    
                    Log.d(TAG, "STEP 4: Parsed Event Type: $eventType")

                    when (eventType) {
                        "ready" -> {
                            trySend(ChatStreamEvent.Ready(sessionId))
                        }
                        "progress" -> {
                            val progressData = chatEvent.data
                            if (progressData != null) {
                                trySend(ChatStreamEvent.Progress(progressData.stage ?: "", progressData.progress ?: 0))
                            }
                        }
                        "token" -> {
                            val token = chatEvent.data?.token ?: ""
                            Log.d(TAG, "STEP 5: Token extracted: [$token]")
                            trySend(ChatStreamEvent.Token(token))
                        }
                        "done" -> {
                            Log.d(TAG, "STEP 6: 'done' event detected. Finalizing stream.")
                            trySend(ChatStreamEvent.Done(ChatResponse(
                                answer = chatEvent.data?.answer,
                                sessionTitle = chatEvent.data?.sessionTitle
                            )))
                            close()
                        }
                        "ping" -> {
                            // CASE 1: Handle Heartbeat events by safely ignoring
                            Log.d(TAG, "SSE Heartbeat (ping) received")
                        }
                        "error" -> {
                            // CASE 2 & 5: Handle logic error but allow subsequent 'done' if provided
                            Log.e(TAG, "STEP 7: SSE logic error event - ${chatEvent.error?.message}")
                            trySend(ChatStreamEvent.Error(chatEvent.error?.code ?: "STREAM_ERROR", chatEvent.error?.message ?: "Unknown error"))
                            // We no longer close() immediately on error event type to allow backend 'done' cleanup
                        }
                        else -> {
                            Log.w(TAG, "Unknown Event Type: $eventType")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "STEP 4: Critical Parsing Failure - ${e.message}")
                }
            }

            override fun onClosed(eventSource: EventSource) {
                Log.d(TAG, "STEP 7: SSE connection closed normally (onClosed)")
                close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: okhttp3.Response?) {
                Log.e(TAG, "STEP 7: SSE Failure (onFailure) - Error: ${t?.message}, Code: ${response?.code}")
                
                if (response != null && response.code != 200) {
                    try {
                        val errorBody = response.body?.string()
                        if (!errorBody.isNullOrBlank()) {
                            val apiResponse = RetrofitClient.gson.fromJson(errorBody, ApiResponse::class.java)
                            val error = apiResponse.error
                            if (error != null) {
                                trySend(ChatStreamEvent.Error(error.code ?: "HTTP_${response.code}", error.message ?: "Unknown error"))
                                close()
                                return
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse error body: ${e.message}")
                    }
                }

                trySend(ChatStreamEvent.Error("CONNECTION_FAILED", t?.message ?: "SSE Failure"))
                close(t)
            }
        }

        val eventSource = EventSources.createFactory(okHttpClient)
            .newEventSource(httpRequest, listener)

        awaitClose { 
            Log.d(TAG, "STEP 8: awaitClose - Cancelling event source")
            eventSource.cancel() 
        }
    }
}
