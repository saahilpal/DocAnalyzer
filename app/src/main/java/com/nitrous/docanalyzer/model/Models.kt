package com.nitrous.docanalyzer.model

import java.util.UUID

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Chat",
    val createdAt: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val content: String,
    val role: MessageRole,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT,
    val attachedFiles: List<UploadFile> = emptyList()
)

enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}

enum class MessageStatus {
    SENDING, SENT, ERROR, TYPING
}

data class UploadFile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val size: Long,
    val progress: Float = 0f,
    val status: UploadStatus = UploadStatus.QUEUED
)

enum class UploadStatus {
    QUEUED, UPLOADING, PROCESSING, INDEXED, FAILED
}
