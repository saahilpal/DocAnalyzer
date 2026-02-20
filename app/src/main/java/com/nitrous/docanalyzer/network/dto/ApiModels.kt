package com.nitrous.docanalyzer.network.dto

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("data") val data: T?,
    @SerializedName("error") val error: Any? // Can be ApiErrorWrapper or String
)

data class ApiErrorWrapper(
    val code: String?,
    val message: String?,
    val retryable: Boolean?
)

data class HealthResponse(
    val status: String,
    val service: String,
    val uptime: Double,
    val queueSize: Int,
    val memoryUsage: MemoryUsageDto,
    val cpuLoad: CpuLoadDto
)

data class MemoryUsageDto(
    val rss: Long,
    val heapTotal: Long,
    val heapUsed: Long,
    val external: Long
)

data class CpuLoadDto(
    val oneMinute: Double,
    val fiveMinutes: Double,
    val fifteenMinutes: Double
)

data class SessionDto(
    val id: Int,
    val title: String,
    val createdAt: String,
    val lastMessageAt: String?,
    val lastMessagePreview: String?,
    val messageCount: Int,
    val pdfCount: Int
)

data class SessionDetailDto(
    val id: Int,
    val title: String,
    val createdAt: String,
    val lastMessageAt: String?,
    val lastMessagePreview: String?,
    val pdfs: List<PdfDto>
)

data class PdfDto(
    val id: Int,
    val sessionId: Int,
    val title: String,
    val filename: String,
    val path: String,
    val type: String,
    val status: String, // processing | indexed | failed
    val indexedChunks: Int,
    val createdAt: String
)

data class UploadResponse(
    val pdfId: Int,
    val sessionId: Int,
    val title: String,
    val status: String,
    val jobId: String,
    val progress: Int,
    val stage: String,
    val queuePosition: Int
)

data class ChatRequest(
    val message: String,
    val history: List<ChatMessageDto>? = null,
    val responseStyle: String? = "structured" // structured | plain
)

data class ChatMessageDto(
    val role: String, // user | assistant
    val text: String
)

data class ChatResponse(
    val answer: String?,
    val formattedAnswer: String?,
    val responseSchema: ResponseSchemaDto?,
    val responseStyle: String?,
    val sources: List<ChatSourceDto>?,
    val usedChunksCount: Int?,
    val sessionTitle: String?,
    // Fields for async response
    val jobId: String?,
    val sessionId: Int?,
    val status: String?,
    val progress: Int?,
    val stage: String?,
    val queuePosition: Int?
)

data class ResponseSchemaDto(
    val format: String?,
    val sections: List<ResponseSectionDto>?
)

data class ResponseSectionDto(
    val title: String?,
    val content: String?
)

data class ChatSourceDto(
    val pdfId: Int,
    val chunkId: String,
    val score: Double
)

data class JobDto(
    val id: String,
    val type: String,
    val status: String, // queued | processing | completed | failed
    val progress: Int,
    val stage: String,
    val queuePosition: Int,
    val attempts: Int,
    val result: ChatResponse?,
    val error: String?,
    val metrics: Any?
)

data class HistoryItemDto(
    val id: String,
    val role: String,
    val text: String,
    val createdAt: String
)
