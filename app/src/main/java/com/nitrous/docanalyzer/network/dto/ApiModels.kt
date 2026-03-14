package com.nitrous.docanalyzer.network.dto

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("data") val data: T?,
    @SerializedName("error") val error: ApiError? = null
)

data class ApiError(
    @SerializedName("code") val code: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("retryable") val retryable: Boolean?
)

data class UserDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("created_at") val createdAtAlternative: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
    @SerializedName("updated_at") val updatedAtAlternative: String? = null,
    @SerializedName("isActive") val isActive: Boolean? = null,
    @SerializedName("is_active") val isActiveAlternative: Boolean? = null
) {
    val normalizedCreatedAt: String get() = createdAt ?: createdAtAlternative ?: ""
    val normalizedIsActive: Boolean get() = isActive ?: isActiveAlternative ?: true
}

data class LoginResponse(
    @SerializedName("accessToken") val accessToken: String?,
    @SerializedName("refreshToken") val refreshToken: String?,
    @SerializedName("expiresAt") val expiresAt: String?,
    @SerializedName("user") val user: UserDto?
)

data class SessionDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("title") val title: String?,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
    @SerializedName("user_id") val userId: Int? = null,
    @SerializedName("last_message") val lastMessage: String? = null,
    @SerializedName("last_message_at") val lastMessageAtSnake: String? = null,
    @SerializedName("lastMessagePreview") val lastMessagePreview: String? = null,
    @SerializedName("lastMessageAt") val lastMessageAtCamel: String? = null,
    @SerializedName("messageCount") val messageCount: Int? = null,
    @SerializedName("pdfCount") val pdfCount: Int? = null
) {
    // UI Compatibility
    val normalizedCreatedAt: String get() = createdAt ?: lastMessageAtCamel ?: lastMessageAtSnake ?: ""
    val normalizedUpdatedAt: String get() = updatedAt ?: ""
}

data class HistoryItemDto(
    @SerializedName("id") val id: String?,
    @SerializedName("sessionId") val sessionId: Int?,
    @SerializedName("role") val role: String?, // user | assistant | system
    @SerializedName("text") val text: String?,
    @SerializedName("createdAt") val createdAt: String?
)

data class SessionMetaDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("title") val title: String?,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("messageCount") val messageCount: Int?,
    @SerializedName("pdfCount") val pdfCount: Int?
) {
    val normalizedCreatedAt: String get() = createdAt ?: ""
    val normalizedUpdatedAt: String get() = updatedAt ?: ""
    val documentCount: Int get() = pdfCount ?: 0
}

data class PdfDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("sessionId") val sessionId: Int? = null,
    @SerializedName("session_id") val sessionIdAlternative: Int? = null,
    @SerializedName("title") val title: String?,
    @SerializedName("filename") val filename: String? = null,
    @SerializedName("path") val path: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("status") val status: String?, // processing | indexed | failed
    @SerializedName("indexedChunks") val indexedChunks: Int? = null,
    @SerializedName("createdAt") val createdAt: String? = null
) {
    val normalizedSessionId: Int get() = sessionId ?: sessionIdAlternative ?: -1
}

data class UploadResponse(
    @SerializedName("pdfId") val pdfId: Int? = null,
    @SerializedName("sessionId") val sessionId: Int? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("jobId") val jobId: String?,
    @SerializedName("progress") val progress: Int? = null,
    @SerializedName("stage") val stage: String? = null,
    @SerializedName("queuePosition") val queuePosition: Int? = null
)

data class JobResponse(
    @SerializedName("id") val id: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("status") val status: String?, // queued | processing | completed | failed
    @SerializedName("progress") val progress: Int?,
    @SerializedName("stage") val stage: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
    @SerializedName("queuePosition") val queuePosition: Int? = null,
    @SerializedName("result") val result: Any? = null,
    @SerializedName("error") val error: String? = null
)

data class MessageDto(
    @SerializedName("role") val role: String?,
    @SerializedName("text") val text: String?
)

data class ChatRequest(
    @SerializedName("message") val message: String,
    @SerializedName("history") val history: List<MessageDto> = emptyList(),
    @SerializedName("responseStyle") val responseStyle: String = "plain"
)

data class ChatResponse(
    @SerializedName("answer") val answer: String? = null,
    @SerializedName("formattedAnswer") val formattedAnswer: String? = null,
    @SerializedName("sessionTitle") val sessionTitle: String? = null,
    @SerializedName("sources") val sources: List<Any>? = null
)

// SSE Events
data class ChatEvent(
    @SerializedName("ok") val ok: Boolean? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("data") val data: ChatEventData? = null,
    @SerializedName("error") val error: ApiError? = null
)

data class ChatEventData(
    @SerializedName("sessionId") val sessionId: Int? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("stage") val stage: String? = null,
    @SerializedName("progress") val progress: Int? = null,
    @SerializedName("token") val token: String? = null,
    @SerializedName("answer") val answer: String? = null,
    @SerializedName("formattedAnswer") val formattedAnswer: String? = null,
    @SerializedName("sessionTitle") val sessionTitle: String? = null,
    @SerializedName("sources") val sources: List<Any>? = null
)

data class AuthSessionDto(
    @SerializedName("id") val id: String?,
    @SerializedName("device_info") val deviceInfo: String?,
    @SerializedName("ip_address") val ipAddress: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("last_used_at") val lastUsedAt: String?
)

data class AuthSessionsResponse(
    @SerializedName("sessions") val sessions: List<AuthSessionDto>
)
