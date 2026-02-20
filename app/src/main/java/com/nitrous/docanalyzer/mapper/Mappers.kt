package com.nitrous.docanalyzer.mapper

import com.nitrous.docanalyzer.model.ChatMessage
import com.nitrous.docanalyzer.model.ChatSession
import com.nitrous.docanalyzer.model.MessageRole
import com.nitrous.docanalyzer.network.dto.HistoryItemDto
import com.nitrous.docanalyzer.network.dto.SessionDto
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun SessionDto.toModel(): ChatSession {
    return ChatSession(
        id = this.id.toString(),
        title = this.title,
        createdAt = this.createdAt.toTimestamp()
    )
}

fun HistoryItemDto.toModel(): ChatMessage {
    return ChatMessage(
        id = this.id,
        sessionId = "", // Session ID is not part of history item, it's the parent
        content = this.text,
        role = if (this.role == "user") MessageRole.USER else MessageRole.ASSISTANT,
        timestamp = this.createdAt.toTimestamp()
    )
}

fun String.toTimestamp(): Long {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        sdf.parse(this)?.time ?: 0L
    } catch (_: Exception) {
        0L
    }
}
