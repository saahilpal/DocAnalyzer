package com.nitrous.docanalyzer.repository

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
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.File

sealed class ChatStreamEvent {
    data class Token(val text: String) : ChatStreamEvent()
    data class Done(val response: ChatResponse) : ChatStreamEvent()
    data class Error(val message: String) : ChatStreamEvent()
}

class DocRepository(private val apiService: ApiService, private val okHttpClient: okhttp3.OkHttpClient) : BaseRepository() {

    suspend fun getHealth() = safeApiCall { apiService.getHealth() }

    suspend fun ping() = safeApiCall { apiService.ping() }

    suspend fun getSessions() = safeApiCall { apiService.getSessions() }

    suspend fun createSession(title: String) = safeApiCall { 
        apiService.createSession(mapOf("title" to title)) 
    }

    suspend fun getSession(sessionId: Int) = safeApiCall { 
        apiService.getSession(sessionId) 
    }

    suspend fun deleteSession(sessionId: Int) = safeApiCall { 
        apiService.deleteSession(sessionId) 
    }

    suspend fun getSessionPdfs(sessionId: Int) = safeApiCall { 
        apiService.getSessionPdfs(sessionId) 
    }

    suspend fun uploadPdf(sessionId: Int, file: File, title: String? = null) = safeApiCall {
        val requestFile = file.asRequestBody("application/pdf".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val titleBody = title?.toRequestBody("text/plain".toMediaTypeOrNull())
        apiService.uploadPdf(sessionId, body, titleBody)
    }

    suspend fun getPdf(pdfId: Int) = safeApiCall { apiService.getPdf(pdfId) }

    suspend fun deletePdf(pdfId: Int, removeFile: Boolean) = safeApiCall { 
        apiService.deletePdf(pdfId, removeFile) 
    }

    suspend fun chatSync(sessionId: Int, message: String, history: List<ChatMessageDto>? = null) = safeApiCall {
        apiService.chat(sessionId, ChatRequest(message, history), stream = false)
    }

    fun streamChat(sessionId: Int, message: String, history: List<ChatMessageDto>? = null): Flow<ChatStreamEvent> = callbackFlow {
        val chatRequest = ChatRequest(message, history)
        val json = RetrofitClient.gson.toJson(chatRequest)
        val body = json.toRequestBody("application/json".toMediaTypeOrNull())
        
        val request = Request.Builder()
            .url("${RetrofitClient.BASE_URL}sessions/$sessionId/chat?stream=true")
            .header("Accept", "text/event-stream")
            .post(body)
            .build()

        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    val response = RetrofitClient.gson.fromJson(data, ApiResponse::class.java)
                    if (response.ok) {
                        when (type) {
                            "token" -> {
                                val tokenData = RetrofitClient.gson.fromJson(RetrofitClient.gson.toJson(response.data), Map::class.java)
                                (tokenData["token"] as? String)?.let { trySend(ChatStreamEvent.Token(it)) }
                            }
                            "done" -> {
                                val chatResponse = RetrofitClient.gson.fromJson(RetrofitClient.gson.toJson(response.data), ChatResponse::class.java)
                                trySend(ChatStreamEvent.Done(chatResponse))
                                eventSource.cancel()
                                close()
                            }
                        }
                    } else {
                        trySend(ChatStreamEvent.Error("Streaming error"))
                    }
                } catch (e: Exception) {
                    trySend(ChatStreamEvent.Error(e.message ?: "Unknown SSE error"))
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                trySend(ChatStreamEvent.Error(t?.message ?: "SSE Connection failed"))
                close(t)
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }

        val eventSource = EventSources.createFactory(okHttpClient).newEventSource(request, listener)
        awaitClose { eventSource.cancel() }
    }

    suspend fun getJob(jobId: String) = safeApiCall { apiService.getJob(jobId) }

    suspend fun getHistory(sessionId: Int, limit: Int? = null, offset: Int? = null) = safeApiCall { 
        apiService.getHistory(sessionId, limit?.toString(), offset?.toString()) 
    }

    suspend fun clearHistory(sessionId: Int) = safeApiCall { 
        apiService.clearHistory(sessionId) 
    }
}
